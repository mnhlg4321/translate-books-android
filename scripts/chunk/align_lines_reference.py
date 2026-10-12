"""Aligner v2: length + edge symbols + glossary anchors (RAW source term <-> DRAFT target term) + numbers.
Moves 1-1, 1-2, 2-1, 1-0, 0-1. Reports per chapter: non-1:1 beads, unanchored stretches, chunk cuts."""
import csv, glob, io, math, os, re, sys
getattr(sys.stdout, "reconfigure", lambda **k: None)(encoding="utf-8")
B = r"D:\Ebooks\JAKUAKU MONSTER"
G = os.path.join(B, "1.GLOSSARIES", "JAKUAKU_MONSTER_VOL1_GLOSSARY_FINAL_v1.0.0", "chapter_glossaries")
def rd(p): return open(p, encoding="utf-8-sig").read()
def nb(t): return [l.strip() for l in t.split("\n") if l.strip()]
OPEN = "「『【（(〝《◇◆＊*─―"
def sig(line):
    return (line[0] if line[0] in OPEN else ".", line[-1] if line[-1] in "」』】）)〟》" else ".")
def glossary(n):
    p = glob.glob(os.path.join(G, n + "_*.csv"))
    if not p: return []
    rows = list(csv.DictReader(io.StringIO(rd(p[0]))))
    return [(r["source"].strip(), r["target"].strip().lower()) for r in rows if r.get("source", "").strip() and r.get("target", "").strip()]
ZEN = str.maketrans("０１２３４５６７８９", "0123456789")
def anchors_raw(text, gl): return {t for s, t in gl if s in text} | set(re.findall(r"\d+", text.translate(ZEN)))
def anchors_draft(text, gl):
    low = text.lower()
    return {t for s, t in gl if t in low} | set(re.findall(r"\d+", text.translate(ZEN)))

def align(raw, draft, gl, split_pen=3.0, skip_pen=6.0):
    R, D = len(raw), len(draft)
    ratio = sum(map(len, draft)) / max(1, sum(map(len, raw)))
    ar = [anchors_raw(x, gl) for x in raw]; ad = [anchors_draft(x, gl) for x in draft]
    def cost(i, a, j, b):
        lr = sum(len(x) for x in raw[i:a]); ld = sum(len(x) for x in draft[j:b])
        c = abs(math.log((ld + 1) / (ratio * lr + 1)))
        if sig(raw[i])[0] != sig(draft[j])[0]: c += 1.5
        if sig(raw[a - 1])[1] != sig(draft[b - 1])[1]: c += 1.0
        A = set().union(*ar[i:a]); Dd = set().union(*ad[j:b])
        if A or Dd: c += 2.0 * len(A ^ Dd) / max(1, len(A | Dd)) - 1.0 * len(A & Dd) / max(1, len(A | Dd))
        return c
    INF = float("inf"); C = [[INF] * (D + 1) for _ in range(R + 1)]; P = [[None] * (D + 1) for _ in range(R + 1)]; C[0][0] = 0
    for i in range(R + 1):
        for j in range(D + 1):
            if C[i][j] == INF: continue
            for di, dj, pen in ((1, 1, 0), (1, 2, split_pen), (2, 1, split_pen), (1, 0, skip_pen), (0, 1, skip_pen)):
                a, b = i + di, j + dj
                if a > R or b > D: continue
                c = C[i][j] + pen + (cost(i, a, j, b) if di and dj else 0)
                if c < C[a][b]: C[a][b] = c; P[a][b] = (i, j)
    beads, i, j = [], R, D
    while (i, j) != (0, 0):
        pi, pj = P[i][j]; beads.append((pi, i, pj, j)); i, j = pi, pj
    return beads[::-1], ar, ad

def report(n):
    r = nb(rd(glob.glob(os.path.join(B, "3.RAW", n + "_*.txt"))[0])); d = nb(rd(glob.glob(os.path.join(B, "4.DRAFT", n + "_*.txt"))[0]))
    gl = glossary(n); beads, ar, ad = align(r, d, gl)
    non = [(b[0] + 1, b[2] + 1, b[1] - b[0], b[3] - b[2]) for b in beads if (b[1] - b[0], b[3] - b[2]) != (1, 1)]
    # anchor agreement of 1-1 beads that carry anchors
    agree = tot = 0
    for b in beads:
        if (b[1] - b[0], b[3] - b[2]) == (1, 1) and (ar[b[0]] or ad[b[2]]):
            tot += 1; agree += 1 if ar[b[0]] & ad[b[2]] else 0
    return r, d, beads, non, agree, tot

if __name__ == "__main__":
    for i in range(1, 29):
        n = f"{i:03d}"
        r, d, beads, non, agree, tot = report(n)
        print(n, f"raw {len(r)} draft {len(d)}", "non-1:1 (rawLine,draftLine,r,d):", non[:6], f"anchor agreement {agree}/{tot}")


def decode(data):
    """Decode like the app's FileUtil: UTF-16 LE/BE and UTF-8 BOMs, strict UTF-8, then Windows-31J."""
    if data.startswith(b"\xff\xfe"): return data[2:].decode("utf-16-le")
    if data.startswith(b"\xfe\xff"): return data[2:].decode("utf-16-be")
    if data.startswith(b"\xef\xbb\xbf"): return data[3:].decode("utf-8")
    try: return data.decode("utf-8")
    except UnicodeDecodeError: return data.decode("cp932")


def cut_points(beads, raw, budget=900, guard=2):
    """Chunk cuts: after >= budget RAW characters, only where `guard` consecutive 1-1 pairs stand on each side.
    guard=2 gave 133/133 correct cuts on 18 chapters with a FINAL-derived answer (8 LN + 10 WN); guard=1 had one wrong
    cut (WN 043, inside a run of DRAFT merges). Returns (raw_line_end, draft_line_end) pairs."""
    def one(b): return (b[1] - b[0], b[3] - b[2]) == (1, 1)
    out, chars = [], 0
    for k, b in enumerate(beads):
        chars += sum(len(x) for x in raw[b[0]:b[1]])
        if chars < budget or k + guard >= len(beads): continue
        if all(one(beads[j]) for j in range(max(0, k - guard + 1), k + 1)) and all(one(beads[j]) for j in range(k + 1, k + 1 + guard)):
            out.append((b[1], b[3])); chars = 0
    return out


SERIES_RATIO = 2.54  # VI/JP character ratio; the app learns it from the user's previously accepted pairs (default ja->vi)


def verdict(raw, draft, gl, beads=None):
    """Chapter verdict before any API call: 'OK' | 'WARN' | 'BLOCK', with the reasons.
    Measured on LN 28 + WN 85 correct pairs, 55 wrong-chapter pairs (n vs n+1) and 11 other-edition pairs:
    wrong chapter 55/55 BLOCK (with and without glossary); correct 0 BLOCK, 3-5 WARN; other edition 10-11/11 WARN/BLOCK."""
    if beads is None:
        beads, ar, ad = align(raw, draft, gl)
    else:
        ar = [anchors_raw(x, gl) for x in raw]; ad = [anchors_draft(x, gl) for x in draft]
    one = [b for b in beads if (b[1] - b[0], b[3] - b[2]) == (1, 1)]
    terms = {t for s, t in gl}
    anch = [b for b in one if (ar[b[0]] | ad[b[2]]) & terms]          # glossary terms only; digits only help alignment
    name = sum(1 for b in anch if ar[b[0]] & ad[b[2]] & terms) / len(anch) if len(anch) >= 15 else None
    edge = sum(1 for b in one if sig(raw[b[0]]) == sig(draft[b[2]])) / max(1, len(one))
    skips = sum(1 for b in beads if (b[1] - b[0]) == 0 or (b[3] - b[2]) == 0) / max(1, len(beads))
    dev = abs(sum(map(len, draft)) / max(1, sum(map(len, raw))) / SERIES_RATIO - 1)
    reasons = []
    if name is not None and name < 0.35: reasons.append(("BLOCK", "NAME_MATCH", name))
    elif name is not None and name < 0.6: reasons.append(("WARN", "NAME_MATCH", name))
    if edge < 0.95: reasons.append(("BLOCK", "EDGE_SYMBOLS", edge))
    elif edge < 0.97: reasons.append(("WARN", "EDGE_SYMBOLS", edge))
    if skips > 0.05: reasons.append(("BLOCK", "UNPAIRED_LINES", skips))
    elif skips > 0.015: reasons.append(("WARN", "UNPAIRED_LINES", skips))
    if dev > 0.35: reasons.append(("BLOCK", "LENGTH_RATIO", dev))
    elif dev > 0.2: reasons.append(("WARN", "LENGTH_RATIO", dev))
    level = "BLOCK" if any(r[0] == "BLOCK" for r in reasons) else "WARN" if reasons else "OK"
    return level, reasons
