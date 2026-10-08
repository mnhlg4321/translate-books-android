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
