#!/usr/bin/env python3
"""Offline diagnosis of the pronoun (xung ho) misses of the chunk edit flow, plus the known-error suite and its clean control set.

Nothing here calls a provider or the app. Inputs are private files produced elsewhere (per-line finder facts from
EditorialPairPronounMissDiagnosisTest, the adjudicated point files of the live runs, the replays of the stored model answers
through the current rules). Book text stays in the private directories; the public outputs carry ids, hashes, layers, labels
and counts only. FINAL is only a reference used to find candidate lines; it never decides a label.

Layers (see EDITORIAL_PRONOUN_RECOVERY_V4_19.md):
  S1 the finder did not flag the line      S2 flagged, the model kept the DRAFT form
  S3 the guard blocked a correct fix       S4 a wrong fix passed the guard      S5 label uncertain
"""
from __future__ import annotations

import argparse
import ast
import base64
import difflib
import hashlib
import itertools
import json
import re  # noqa: F401
import sys
import unicodedata
from collections import Counter
from pathlib import Path
from typing import Any

SCHEMA = "pronoun-miss-diagnosis-v1"
SUITE_SCHEMA = "pronoun-known-miss-suite-v1"

# Copy of AddressChecklist.WORDS; a test compares it with the Java source so the two cannot drift apart silently.
WORDS = frozenset({"cậu", "bạn", "anh", "chị", "em", "cô", "ông", "bà", "mày", "ngươi", "ngài", "chú", "bác",
                   "thầy", "tớ", "tao", "ta", "tui", "tôi"})

S1_REASONS = ("S1_NO_DIALOGUE", "S1_NO_ROW_COVERS", "S1_ROW_NOT_PROJECTED", "S1_ROW_INCOMPLETE",
              "S1_ROW_SCOPE_NO_QUOTE", "S1_CONFLICT", "S1_LEXICON_GAP", "S1_ALLOWED_WORD_ONLY")

LABELS = ("ĐÚNG", "SÓT", "SAI", "CHƯA CHẮC")


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def tokens(text: str) -> list[str]:
    """Lower-cased runs of letters, NFC; the same notion of a word as AddressChecklist.tokens."""
    out: list[str] = []
    word: list[str] = []
    for ch in unicodedata.normalize("NFC", text):
        if ch.isalpha():
            word.append(ch.lower())
        elif word:
            out.append("".join(word))
            word = []
    if word:
        out.append("".join(word))
    return out


def has_letters(line: str) -> bool:
    return any(c.isalpha() for c in line)


# --------------------------------------------------------------------------------------------------------------- S1

def classify_line(fact: dict[str, Any]) -> str:
    """Why the finder did or did not put an ADDRESS point on one DRAFT line, from the facts of that line only.

    The chain follows FixPointFinder: the aligned RAW must carry a quotation, a projected (cue-selected) row must cover the
    paragraph, the row must have both self and call, the paragraph must not be in a conflict, and the DRAFT line must use an
    address word of the fixed lexicon that the row does not allow.
    """
    if "ADDRESS" in fact.get("flagged", []):
        return "FLAGGED"
    if not fact.get("dialogue"):
        return "S1_NO_DIALOGUE"
    rows = fact.get("rows", [])
    if not rows:
        return "S1_NO_ROW_COVERS"
    projected = [r for r in rows if r.get("projected")]
    if not projected:
        return "S1_ROW_NOT_PROJECTED"
    complete = [r for r in projected if r.get("complete")]
    if not complete:
        return "S1_ROW_INCOMPLETE"
    scoped = [r for r in complete if r.get("coversDialogue")]
    if not scoped:
        return "S1_ROW_SCOPE_NO_QUOTE"
    if any(r.get("unexpected") for r in scoped):
        return "S1_CONFLICT"  # everything else would have flagged it, so only the conflict skip is left
    if not fact.get("lex"):
        return "S1_LEXICON_GAP"
    return "S1_ALLOWED_WORD_ONLY"


def address_delta(draft: str, final: str, extra_words: set[str]) -> tuple[list[str], list[str]]:
    """Address-like words removed from the DRAFT line and added by the owner's FINAL line (multiset difference)."""
    vocab = WORDS | extra_words
    d, f = Counter(tokens(draft)), Counter(tokens(final))
    removed = sorted(t for t in (d - f) if t in vocab)
    added = sorted(t for t in (f - d) if t in vocab)
    return removed, added


def align_final(draft_lines: list[str], final_lines: list[str]) -> list[tuple[str, str | None]]:
    """Pair every DRAFT line with its FINAL counterpart. State is SAME, CHANGED, DELETED or UNPAIRED."""
    out: list[tuple[str, str | None]] = [("UNPAIRED", None)] * len(draft_lines)
    sm = difflib.SequenceMatcher(None, draft_lines, final_lines, autojunk=False)
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            for k in range(i2 - i1):
                out[i1 + k] = ("SAME", final_lines[j1 + k])
        elif tag == "delete":
            for i in range(i1, i2):
                out[i] = ("DELETED", None)
        elif tag == "replace":
            if i2 - i1 == j2 - j1:
                for k in range(i2 - i1):
                    out[i1 + k] = ("CHANGED", final_lines[j1 + k])
            else:
                used: set[int] = set()
                for i in range(i1, i2):
                    best, best_ratio = None, 0.6
                    for j in range(j1, j2):
                        if j in used:
                            continue
                        ratio = difflib.SequenceMatcher(None, draft_lines[i], final_lines[j]).ratio()
                        if ratio > best_ratio:
                            best, best_ratio = j, ratio
                    if best is not None:
                        used.add(best)
                        out[i] = ("CHANGED", final_lines[best])
    return out


def read_facts(path: Path) -> tuple[dict[str, dict[str, Any]], dict[str, list[dict[str, Any]]]]:
    meta: dict[str, dict[str, Any]] = {}
    lines: dict[str, list[dict[str, Any]]] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        if not raw.strip():
            continue
        obj = json.loads(raw)
        if obj.get("meta"):
            meta[obj["ch"]] = obj
        else:
            lines.setdefault(obj["ch"], []).append(obj)
    for ch in lines:
        lines[ch].sort(key=lambda o: o["d"])
    return meta, lines


def row_words(facts: list[dict[str, Any]]) -> set[str]:
    out: set[str] = set()
    for f in facts:
        for r in f.get("rows", []):
            out.update(tokens(r.get("self", "")))
            out.update(tokens(r.get("call", "")))
    return out


def analyze_chapter(ch: str, facts: list[dict[str, Any]], final_text: str | None) -> dict[str, Any]:
    reasons = Counter(classify_line(f) for f in facts)
    result: dict[str, Any] = {
        "lines": len(facts),
        "dialogueLines": sum(1 for f in facts if f.get("dialogue")),
        "linesWithLexiconWord": sum(1 for f in facts if f.get("lex")),
        "addressFlaggedLines": reasons["FLAGGED"],
        "reasonsOverAllLines": {k: reasons[k] for k in ("FLAGGED",) + S1_REASONS},
        "candidates": [],
    }
    if final_text is None:
        return result
    final_lines = [l.strip() for l in final_text.replace("\r\n", "\n").replace("\r", "\n").split("\n") if has_letters(l)]
    draft_lines = [f["text"] for f in facts]
    pairing = align_final(draft_lines, final_lines)
    extra = row_words(facts)
    owner_changed = owner_addr = covered = 0
    by_reason: Counter[str] = Counter()
    cands: list[dict[str, Any]] = []
    for f, (state, final_line) in zip(facts, pairing):
        if state != "CHANGED" or final_line is None or final_line == f["text"]:
            continue
        owner_changed += 1
        removed, added = address_delta(f["text"], final_line, extra)
        if not (removed or added):
            continue
        owner_addr += 1
        reason = classify_line(f)
        if reason == "FLAGGED":
            covered += 1
        by_reason[reason] += 1
        cands.append({"ch": ch, "d": f["d"], "chunk": f.get("chunk"), "reason": reason, "removed": removed, "added": added,
                      "draftSha256": sha256(f["text"]), "rawSha256": sha256(f.get("rawText", "")),
                      "text": f["text"], "final": final_line, "rawText": f.get("rawText", "")})
    result.update({
        "ownerChangedLines": owner_changed,
        "ownerChangedWithAddressDelta": owner_addr,
        "addressDeltaCovered": covered,
        "addressDeltaUncoveredByReason": {k: by_reason[k] for k in S1_REASONS if by_reason[k]},
        "candidates": cands,
    })
    return result


def analyze(facts_path: Path, final_root: Path | None) -> dict[str, Any]:
    meta, lines = read_facts(facts_path)
    chapters: dict[str, Any] = {}
    for ch in sorted(lines):
        if meta.get(ch) and meta[ch]["draftLines"] != len(lines[ch]):
            raise SystemExit(f"chapter {ch}: facts cover {len(lines[ch])} of {meta[ch]['draftLines']} DRAFT lines")
        final_text = None
        if final_root is not None:
            p = final_root / f"{ch}_FINAL_QA_JAKUAKU_MONSTER_VOL1.txt"
            if p.is_file():
                final_text = p.read_text(encoding="utf-8-sig")
        chapters[ch] = analyze_chapter(ch, lines[ch], final_text)
        if ch in meta:
            m = meta[ch]
            chapters[ch].update({"rows": m["rows"], "rowsIncomplete": m["rowsIncomplete"], "chunks": m["chunks"], "conflicts": m["conflicts"]})
    total = Counter()
    unc: Counter[str] = Counter()
    for c in chapters.values():
        for k in ("lines", "dialogueLines", "linesWithLexiconWord", "addressFlaggedLines", "ownerChangedLines",
                  "ownerChangedWithAddressDelta", "addressDeltaCovered", "rows", "rowsIncomplete"):
            total[k] += c.get(k, 0)
        unc.update(c.get("addressDeltaUncoveredByReason", {}))
    return {"schema": SCHEMA, "chapters": chapters, "total": dict(total), "addressDeltaUncoveredByReason": dict(unc)}


def public_summary(analysis: dict[str, Any]) -> dict[str, Any]:
    chapters = {}
    for ch, c in analysis["chapters"].items():
        chapters[ch] = {k: v for k, v in c.items() if k != "candidates"}
        chapters[ch]["candidateIds"] = [f"{ch}:d{x['d']}:{x['reason']}:{x['draftSha256'][:12]}" for x in c.get("candidates", [])]
    return {"schema": SCHEMA, "chapters": chapters, "total": analysis["total"],
            "addressDeltaUncoveredByReason": analysis["addressDeltaUncoveredByReason"]}


# ------------------------------------------------------------------------------------------------------- suite

def layer_and_group(label: str, event: str, live_status: str, live_reason: str) -> tuple[str, str]:
    """Return (group, layer) for one adjudicated point. group is KNOWN, CLEAN or UNSURE.

    label/event are the adjudication of the live run (RAW + rule); status/reason are what the live run did.
    """
    if label == "CHƯA CHẮC":
        return "UNSURE", "S5_UNSURE"
    if label == "SÓT":
        if "chặn nhầm" in event:
            return "KNOWN", "S3_GUARD_FALSE_BLOCK"
        if "bị chặn đúng" in event:
            return "KNOWN", "S2B_WRONG_PROPOSAL_BLOCKED_DRAFT_STILL_WRONG"
        if live_status == "FIXED":
            return "KNOWN", "S2P_PARTIAL_FIX"
        return "KNOWN", "S2_MODEL_KEPT"
    if label == "SAI":
        return "KNOWN", "S4_WRONG_PASSED"
    if label == "ĐÚNG":
        if event.startswith("sửa đúng"):
            return "CLEAN", "CLEAN_FIXED_OK"
        if "bị chặn đúng" in event:
            return "CLEAN", "CLEAN_WRONG_PROPOSAL_BLOCKED"
        return "CLEAN", "CLEAN_KEPT"
    raise ValueError(f"unknown label {label!r}")


def parse_literal(source: str, name: str) -> Any:
    """Value of a top-level literal assignment in a private score script, read with ast (the script is never executed)."""
    for node in ast.parse(source).body:
        if isinstance(node, ast.Assign) and any(isinstance(t, ast.Name) and t.id == name for t in node.targets):
            return ast.literal_eval(node.value)
    raise ValueError(f"{name} not found")


def label_006(point: dict[str, Any], sets: dict[str, set[str]]) -> tuple[str, str]:
    """Final adjudication of an 006 point in the C6E run (the logic of the private score_c6e.py)."""
    k, lab = point["key"], point["label"]
    fixed = point["status"] == "FIXED"
    if fixed:
        if k in sets["NEW_WRONG_PASSED"]:
            return "SAI", "sửa sai lọt qua"
        if k in sets["NEW_CORRECT"] or lab == "ĐÚNG":
            return "ĐÚNG", "sửa đúng"
        return lab, "sửa khác"
    if point["reason"].startswith("ADDRESS_"):
        if k in sets["WRONGLY_BLOCKED"]:
            return "SÓT", "sửa đúng bị chặn nhầm"
        if k in sets["WRONG_BLOCKED"]:
            return "ĐÚNG", "sửa sai bị chặn đúng"
        return lab, "bị chặn khác"
    if lab == "SÓT":
        return "SÓT", "bỏ sót"
    if lab == "CHƯA CHẮC":
        return "CHƯA CHẮC", "giữ"
    if lab == "SAI":
        return "ĐÚNG", "giữ đúng"
    if lab == "ĐÚNG" and point.get("old_status") == "FIXED":
        return "SÓT", "bỏ sót (C6B model sửa đúng, lần này giữ)"
    return "ĐÚNG", "giữ đúng"


def read_replay(path: Path) -> dict[str, dict[str, str]]:
    out: dict[str, dict[str, str]] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        a = line.split("\t")
        if len(a) < 7:
            continue
        out[a[0]] = {"types": a[1], "status": a[2], "reason": a[3], "before": base64.b64decode(a[5]).decode("utf-8"),
                     "after": base64.b64decode(a[6]).decode("utf-8")}
    return out


# run name -> (chapter, points file, score script, replay file, kind)
RUNS = {
    "006-C6E": ("006", "c6e-points.json", "score_c6e.py", "v15-replay-006-c6e.tsv", "006"),
    "008-C7A": ("008", "c7a-points.json", "score_c7a.py", "v15-replay-008.tsv", "KEY"),
    "011-C7D": ("011", "c7d-points.json", "score_c7d.py", "v15-replay-011-c7d.tsv", "KEY"),
}


def final_states(facts: dict[str, list[dict[str, Any]]], final_root: Path | None) -> dict[str, list[tuple[str, str | None]]]:
    """FINAL counterpart state of every DRAFT line, per chapter. A reference for reading the suite, never a label."""
    out: dict[str, list[tuple[str, str | None]]] = {}
    if final_root is None:
        return out
    for ch, fs in facts.items():
        path = final_root / f"{ch}_FINAL_QA_JAKUAKU_MONSTER_VOL1.txt"
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8-sig")
        final_lines = [l.strip() for l in text.replace(chr(13)+chr(10), chr(10)).replace(chr(13), chr(10)).split(chr(10)) if has_letters(l)]
        out[ch] = align_final([f["text"] for f in fs], final_lines)
    return out


def build_suite(guard_dir: Path, facts_path: Path | None, final_root: Path | None = None) -> dict[str, Any]:
    facts: dict[str, list[dict[str, Any]]] = read_facts(facts_path)[1] if facts_path else {}
    finals = final_states(facts, final_root)
    records: list[dict[str, Any]] = []
    for run, (ch, points_file, score_file, replay_file, kind) in RUNS.items():
        points = json.loads((guard_dir / points_file).read_text(encoding="utf-8"))
        source = (guard_dir / score_file).read_text(encoding="utf-8")
        replay = read_replay(guard_dir / replay_file)
        key_table = sets = None
        if kind == "KEY":
            key_table = parse_literal(source, "KEY")
        else:
            sets = {n: set(parse_literal(source, n)) for n in ("NEW_CORRECT", "NEW_WRONG_PASSED", "WRONGLY_BLOCKED", "WRONG_BLOCKED")}
        used: set[int] = set()
        for p in points:
            key = p.get("key") or f"{p['chunk']}:{p['id']}"
            if key_table is not None:
                label, event, *_ = key_table.get(key, ("ĐÚNG", "giữ đúng", ""))
            else:
                label, event = label_006(p, sets)
            group, layer = layer_and_group(label, event, p["status"], p["reason"])
            cur = replay.get(key)
            d_global = None
            for f in facts.get(ch, []):
                if str(f.get("chunk")) == str(p["chunk"]) and f["text"] == p["before"] and f["d"] not in used:
                    d_global = f["d"]
                    used.add(f["d"])
                    break
            final_ref = None
            if d_global is not None and ch in finals:
                final_ref = finals[ch][d_global][0]
            records.append({
                "id": f"{run}:{key}", "finalReference": final_ref, "run": run, "chapter": ch, "key": key, "chunk": int(p["chunk"]), "types": p["types"],
                "group": group, "layer": layer, "label": label, "event": event,
                "live": {"status": p["status"], "reason": p["reason"]},
                "current": ({"status": cur["status"], "reason": cur["reason"], "afterSha256": sha256(cur["after"])} if cur else {"status": "NO_POINT", "reason": "", "afterSha256": ""}),
                "draftLine": d_global, "rawSha256": sha256(p["raw"]), "draftSha256": sha256(p["before"]),
                "text": {"raw": p["raw"], "draft": p["before"], "currentAfter": cur["after"] if cur else ""},
            })
    return {"schema": SUITE_SCHEMA, "records": records}


RELABEL_SCHEMA = "pronoun-suite-relabel-v1"


def apply_relabel(suite: dict[str, Any], relabel: dict[str, Any]) -> list[str]:
    """Apply an owner rule (for example D-A) to adjudicated records, in place. Returns problems; nothing is applied then.

    Each entry names one record id, the label it must have now ("from"), the new label ("to", only ĐÚNG is allowed: a rule may
    move a point into the clean part, never invent an error) and the reason read from RAW plus the pronoun row. Entries with
    "keep" document a point the rule does not move. The file holds ids and reasons only, never book text.
    """
    problems: list[str] = []
    if relabel.get("schema") != RELABEL_SCHEMA:
        return [f"relabel schema must be {RELABEL_SCHEMA}"]
    rule = relabel.get("rule", "")
    by_id = {r["id"]: r for r in suite["records"]}
    planned: list[tuple[dict[str, Any], dict[str, Any]]] = []
    seen: set[str] = set()
    for entry in relabel.get("entries", []):
        rid = entry.get("id", "")
        if rid in seen:
            problems.append(f"{rid}: listed twice")
            continue
        seen.add(rid)
        rec = by_id.get(rid)
        if rec is None:
            problems.append(f"{rid}: not in the suite")
            continue
        if not entry.get("reason"):
            problems.append(f"{rid}: no reason")
        if entry.get("from") != rec["label"]:
            problems.append(f"{rid}: label is {rec['label']}, the relabel expects {entry.get('from')}")
        if "keep" in entry:
            continue
        if entry.get("to") != "ĐÚNG":
            problems.append(f"{rid}: a relabel may only move a point to ĐÚNG")
            continue
        planned.append((rec, entry))
    if problems:
        return problems
    for rec, entry in planned:
        previous = {"label": rec["label"], "layer": rec["layer"], "event": rec["event"]}
        rec["label"], rec["event"] = "ĐÚNG", f"giữ đúng ({rule})"
        rec["group"], rec["layer"] = layer_and_group(rec["label"], rec["event"], rec["live"]["status"], rec["live"]["reason"])
        rec["relabel"] = {"rule": rule, "reason": entry["reason"], "previous": previous}
    for entry in relabel.get("entries", []):
        if "keep" in entry:
            by_id[entry["id"]]["ruleNote"] = {"rule": rule, "keep": entry["keep"], "reason": entry["reason"]}
    return []


def apply_optional_choices(suite: dict[str, Any], entries: list[dict[str, Any]]) -> list[str]:
    """Pin complete allowed lines as hashes: only adjudicated second-person cậu spans may become the call name.

    RAW adjudication and the row's call are supplied by the owner-reviewed table, not inferred from FINAL.
    Replay stays strict; this extra contract is for grading a future live measurement.
    """
    by_id = {r["id"]: r for r in suite["records"]}
    problems, planned, seen = [], [], set()
    for entry in entries:
        rid, name = entry.get("id", ""), entry.get("callName", "")
        rec = by_id.get(rid)
        if rid in seen or rec is None or rec["group"] != "CLEAN" or not name or not entry.get("reason"):
            problems.append(f"{rid}: invalid or duplicate D-A optional clean entry")
            continue
        seen.add(rid)
        basis = entry.get("basis", "draft")
        if basis not in ("draft", "currentAfter"):
            problems.append(f"{rid}: invalid optional basis")
            continue
        draft = rec["text"][basis]
        spans = list(re.finditer(r"\bcậu\b", draft, re.IGNORECASE))
        if not spans or len(spans) > 8 or any(t in WORDS or t == "mình" for t in tokens(name)):
            problems.append(f"{rid}: D-A needs second-person cậu spans and a personal-name call")
            continue
        allowed = set()
        for choices in itertools.product((False, True), repeat=len(spans)):
            parts, offset = [], 0
            for match, replace in zip(spans, choices):
                parts.extend((draft[offset:match.start()], name if replace else match.group()))
                offset = match.end()
            parts.append(draft[offset:])
            allowed.add(sha256("".join(parts)))
        if rec["current"]["afterSha256"] not in allowed:
            problems.append(f"{rid}: current clean outcome is outside the D-A alternatives")
        planned.append((rec, {"rule": "D-A", "callName": name, "basis": basis, "reason": entry["reason"],
                              "allowedAfterSha256": sorted(allowed)}))
    if not problems:
        for rec, choice in planned:
            rec["optionalDa"] = choice
    return problems


def valid_da_choice(rec: dict[str, Any], after: str) -> bool:
    """Future measurement accepts exactly the adjudicated complete-line hashes, never a new pronoun or other edit."""
    return rec.get("group") == "CLEAN" and sha256(after) in rec.get("optionalDa", {}).get("allowedAfterSha256", [])


def split_suite(suite: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
    known = [r for r in suite["records"] if r["group"] in ("KNOWN", "UNSURE")]
    clean = [r for r in suite["records"] if r["group"] == "CLEAN"]
    return ({"schema": SUITE_SCHEMA, "part": "KNOWN", "records": known},
            {"schema": SUITE_SCHEMA, "part": "CLEAN", "records": clean})


def public_index(part: dict[str, Any]) -> dict[str, Any]:
    recs = []
    for r in part["records"]:
        recs.append({k: v for k, v in r.items() if k != "text"})
    layers = Counter(r["layer"] for r in part["records"])
    address = Counter(r["layer"] for r in part["records"] if "ADDRESS" in r["types"])
    ref: dict[str, Counter[str]] = {}
    for r in part["records"]:
        if "ADDRESS" in r["types"]:
            ref.setdefault(r["layer"], Counter())[r.get("finalReference") or "NONE"] += 1
    return {"schema": SUITE_SCHEMA, "part": part["part"], "count": len(recs), "byLayer": dict(sorted(layers.items())),
            "byLayerAddressOnly": dict(sorted(address.items())),
            "finalReferenceByLayerAddressOnly": {k: dict(sorted(v.items())) for k, v in sorted(ref.items())}, "records": recs}


def verify_index(index: dict[str, Any], private: dict[str, Any]) -> list[str]:
    """Problems found when the private sidecar and the public hash index do not describe the same records."""
    problems: list[str] = []
    pub = {r["id"]: r for r in index["records"]}
    prv = {r["id"]: r for r in private["records"]}
    for rid in pub.keys() - prv.keys():
        problems.append(f"{rid}: in the index but not in the private suite")
    for rid in prv.keys() - pub.keys():
        problems.append(f"{rid}: in the private suite but not in the index")
    for rid in pub.keys() & prv.keys():
        r = prv[rid]
        if sha256(r["text"]["raw"]) != pub[rid]["rawSha256"]:
            problems.append(f"{rid}: RAW text does not match its hash")
        if sha256(r["text"]["draft"]) != pub[rid]["draftSha256"]:
            problems.append(f"{rid}: DRAFT text does not match its hash")
        if r["layer"] != pub[rid]["layer"] or r["label"] != pub[rid]["label"]:
            problems.append(f"{rid}: layer/label differ between index and private suite")
    return problems


def check_separation(known: dict[str, Any], clean: dict[str, Any]) -> list[str]:
    """The known-error part and the clean part must never share a record, a DRAFT line or a clean layer."""
    problems: list[str] = []
    ids = {r["id"] for r in known["records"]} & {r["id"] for r in clean["records"]}
    for rid in sorted(ids):
        problems.append(f"{rid}: in both parts")
    for r in clean["records"]:
        if not r["layer"].startswith("CLEAN_") or r["label"] != "ĐÚNG":
            problems.append(f"{r['id']}: clean part holds {r['layer']}/{r['label']}")
    for r in known["records"]:
        if r["layer"].startswith("CLEAN_"):
            problems.append(f"{r['id']}: known part holds a clean layer")
    return problems


def expectation(rec: dict[str, Any]) -> str:
    """What a later code change must keep or may improve, judged on the replay of the stored answers."""
    cur = rec["current"]["status"]
    if rec["group"] == "CLEAN":
        return "KEEP_OUTCOME"
    if rec["layer"] == "S3_GUARD_FALSE_BLOCK":
        return "STAY_FIXED" if cur == "FIXED" else "SHOULD_BECOME_FIXED"
    if rec["layer"] == "S4_WRONG_PASSED":
        return "STAY_BLOCKED" if cur in ("KEPT", "NO_POINT") else "MUST_BECOME_BLOCKED"
    return "NO_REPLAY_CHANGE_EXPECTED"  # S2/S2B/S2P/S5: the model decided; only a new live run can move them


def verify_replay(records: list[dict[str, Any]], replays: dict[str, dict[str, dict[str, str]]], allow_da_choices: bool = False) -> dict[str, Any]:
    """Compare a fresh replay with the recorded current outcome of every suite record."""
    regressions: list[str] = []
    improvements: list[str] = []
    unchanged = 0
    optional_accepted = 0
    for rec in records:
        replay = replays.get(rec["run"])
        if replay is None:
            continue
        obs = replay.get(rec["key"])
        exp = rec["current"]
        o_status = obs["status"] if obs else "NO_POINT"
        o_reason = obs["reason"] if obs else ""
        o_after = sha256(obs["after"]) if obs else ""
        same = (o_status, o_reason, o_after) == (exp["status"], exp["reason"], exp.get("afterSha256", ""))
        if same:
            unchanged += 1
            continue
        if allow_da_choices and o_status in ("FIXED", "KEPT") and obs and valid_da_choice(rec, obs["after"]):
            optional_accepted += 1
            continue
        want = expectation(rec)
        if rec["group"] == "CLEAN":
            regressions.append(f"{rec['id']}: clean control changed {exp['status']}/{exp['reason']} -> {o_status}/{o_reason}")
        elif want == "SHOULD_BECOME_FIXED" and o_status == "FIXED":
            improvements.append(f"{rec['id']}: guard false block now fixed")
        elif want == "STAY_FIXED" and o_status != "FIXED":
            regressions.append(f"{rec['id']}: previously fixed point is {o_status}/{o_reason}")
        elif want in ("STAY_BLOCKED", "MUST_BECOME_BLOCKED") and o_status == "FIXED":
            regressions.append(f"{rec['id']}: wrong fix passes again")
        elif want == "STAY_BLOCKED":
            unchanged += 1  # still not applied, reason text may differ
        else:
            improvements.append(f"{rec['id']}: {exp['status']}/{exp['reason']} -> {o_status}/{o_reason} (review)")
    result = {"unchanged": unchanged, "regressions": regressions, "improvements": improvements}
    if allow_da_choices:
        result["optionalDaAccepted"] = optional_accepted
    return result


# ---------------------------------------------------------------------------------------------------------- CLI

def _write(path: Path, obj: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=1) + "\n", encoding="utf-8", newline="\n")


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("analyze", help="S1: why lines are not flagged, against the owner's FINAL as reference")
    a.add_argument("--facts", required=True, type=Path)
    a.add_argument("--final-root", type=Path)
    a.add_argument("--out-private", type=Path)
    a.add_argument("--out-public", type=Path)
    s = sub.add_parser("suite", help="build the known-error suite and the separate clean control set")
    s.add_argument("--guard-dir", required=True, type=Path)
    s.add_argument("--facts", type=Path)
    s.add_argument("--final-root", type=Path)
    s.add_argument("--out-private-dir", required=True, type=Path)
    s.add_argument("--out-public-dir", required=True, type=Path)
    s.add_argument("--relabel", type=Path, help="owner rule applied to adjudicated records (ids and reasons only)")
    v = sub.add_parser("verify-replay", help="compare replays of the stored answers with the recorded outcomes")
    v.add_argument("--known", required=True, type=Path)
    v.add_argument("--clean", required=True, type=Path)
    v.add_argument("--replay", action="append", required=True, metavar="RUN=TSV")
    v.add_argument("--allow-da-choices", action="store_true", help="future measurement only; strict replay must omit this flag")
    args = ap.parse_args(argv)

    if args.cmd == "analyze":
        analysis = analyze(args.facts, args.final_root)
        if args.out_private:
            _write(args.out_private, analysis)
        summary = public_summary(analysis)
        if args.out_public:
            _write(args.out_public, summary)
        print(json.dumps({"total": analysis["total"], "uncoveredByReason": analysis["addressDeltaUncoveredByReason"]}, ensure_ascii=False))
        return 0
    if args.cmd == "suite":
        suite = build_suite(args.guard_dir, args.facts, args.final_root)
        if args.relabel:
            bad = apply_relabel(suite, json.loads(args.relabel.read_text(encoding="utf-8")))
            if not bad:
                bad = apply_optional_choices(suite, json.loads(args.relabel.read_text(encoding="utf-8")).get("optionalChoices", []))
            if bad:
                print("\n".join(bad), file=sys.stderr)
                return 1
        known, clean = split_suite(suite)
        problems = check_separation(known, clean)
        idx_known, idx_clean = public_index(known), public_index(clean)
        problems += verify_index(idx_known, known) + verify_index(idx_clean, clean)
        if problems:
            print("\n".join(problems), file=sys.stderr)
            return 1
        _write(args.out_private_dir / "known-misses.json", known)
        _write(args.out_private_dir / "clean-controls.json", clean)
        _write(args.out_public_dir / "V419_PRONOUN_KNOWN_MISS_INDEX.json", idx_known)
        _write(args.out_public_dir / "V419_PRONOUN_CLEAN_CONTROL_INDEX.json", idx_clean)
        print(json.dumps({"known": idx_known["byLayer"], "clean": idx_clean["byLayer"]}, ensure_ascii=False))
        return 0
    known = json.loads(args.known.read_text(encoding="utf-8"))["records"]
    clean = json.loads(args.clean.read_text(encoding="utf-8"))["records"]
    replays = {}
    for item in args.replay:
        run, _, path = item.partition("=")
        replays[run] = read_replay(Path(path))
    result = verify_replay(known + clean, replays, args.allow_da_choices)
    print(json.dumps(result, ensure_ascii=False, indent=1))
    return 1 if result["regressions"] else 0


if __name__ == "__main__":
    sys.exit(main())
