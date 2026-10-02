#!/usr/bin/env python3
"""Builds the P6 R0 fixtures from the owner's Vol 5 sources into a private directory.

The fixtures contain copyrighted text, so they are written OUTSIDE Git (default D:/P5E-private/p6-fixtures).
Only the manifest (ids, kinds, hashes, mutation classes) goes to the repository. Labels (what is wrong, how it is
scored) are written to <out>/_labels/ and are never part of a runtime prompt; fixture directory names are opaque.

Usage: py -3 scripts/p6/build_fixtures.py --out D:/P5E-private/p6-fixtures --manifest docs/P6_R0_FIXTURE_MANIFEST.json
The script refuses to read anything under a '6.FINAL' directory except the one explicit clean-control base, and it
asserts every mutation hits exactly one place so a fixture never carries an accidental second error.
"""
import argparse, hashlib, json, os, random, re, sys, datetime

SRC = r'D:\Ebooks\MERCEDES\VOL 5'
OWNER_FINAL_001 = os.path.join(SRC, '6.FINAL', '001_FINAL_QA_MERCEDES_VOL5.txt')  # explicit, documented use as clean control base


def sha(b):
    return hashlib.sha256(b).hexdigest()


def read_bytes(p):
    return open(p, 'rb').read()


def dec(b):
    return b.decode('utf-8-sig')


def src(kind, n):
    d = {'RAW': ('02_RAW_MERCEDES_VOL5', '%s_RAW_MERCEDES_VOL5.TXT'),
         'DRAFT': ('05_DRAFT_MERCEDES_VOL5', '%s_DRAFT_MERCEDES_VOL5.TXT'),
         'GLOSSARY': ('03_GLOSSARY_MERCEDES_VOL5', '%s_GLOSSARY_MERCEDES_VOL5.CSV'),
         'PRONOUN': ('04_PRONOUN_MERCEDES_VOL5', '%s_PRONOUN_MERCEDES_VOL5.CSV')}[kind]
    return os.path.join(SRC, d[0], d[1] % n)


def encode_like(text, original_bytes):
    out = text.encode('utf-8')
    return (b'\xef\xbb\xbf' + out) if original_bytes.startswith(b'\xef\xbb\xbf') else out


class Mut:
    """One targeted mutation on one line of a base text, asserting exactly one hit."""
    def __init__(self, mid, cls, line, old, new, checks):
        self.mid, self.cls, self.line, self.old, self.new, self.checks = mid, cls, line, old, new, checks


def apply(lines, muts):
    lines = list(lines)
    for m in muts:
        l = lines[m.line - 1]
        assert l.count(m.old) == 1, 'mutation %s: expected exactly one hit, got %d' % (m.mid, l.count(m.old))
        lines[m.line - 1] = l.replace(m.old, m.new)
    return lines


# ---- development mutations on the clean control (manual FINAL of chapter 001) ----
DEV = {
    'S1': Mut('S1', 'OMITTED_WORD_UNTRANSLATED', 99, 'hộ vệ', '護衛',
              {'mustNotContain': ['護衛'], 'mustContain': ['hộ vệ']}),
    'S2': Mut('S2', 'ROLE_SWAP', 71, 'Nếu phải để Elfe giành mất thì thà để một ma cà rồng nào đó hoàn thành còn hơn.',
              'Nếu phải để ma cà rồng giành mất thì thà để một Elfe nào đó hoàn thành còn hơn.',
              {'mustContain': ['Nếu phải để Elfe giành mất'], 'mustNotContain': ['Nếu phải để ma cà rồng giành mất']}),
    'S3': Mut('S3', 'MISSING_SENTENCE', 327, ' Chẳng bao lâu sau, các Seeker cũng ngừng chú ý đến Mercedes.', '',
              {'mustContain': ['ngừng chú ý đến Mercedes']}),
    'S3B': Mut('S3B', 'EXTRA_SENTENCE', 53, 'cho dù có những hộ vệ tài giỏi đi cùng.',
               'cho dù có những hộ vệ tài giỏi đi cùng. Điều đó khiến mọi người vô cùng sợ hãi.',
               {'mustNotContain': ['vô cùng sợ hãi']}),
    'S4A': Mut('S4A', 'NUMBER', 5, 'khoảng năm trăm mét', 'khoảng bốn trăm mét',
               {'mustContain': ['khoảng năm trăm mét'], 'mustNotContain': ['khoảng bốn trăm mét']}),
    'S4B': Mut('S4B', 'NEGATION', 47, 'đương nhiên không thể đến đây', 'đương nhiên có thể đến đây',
               {'mustContain': ['đương nhiên không thể đến đây'], 'mustNotContain': ['đương nhiên có thể đến đây']}),
    'S5': Mut('S5', 'GLOSSARY_TERM', 13, 'đám ma cà rồng', 'đám huyết tộc',
              {'mustContain': ['đám ma cà rồng'], 'mustNotContain': ['huyết tộc']}),
    'S6': Mut('S6', 'ADDRESS_PROFILE', 91, 'Và bác biết chuyện đó', 'Và cô biết chuyện đó',
              {'mustContain': ['Và bác biết chuyện đó'], 'mustNotContain': ['Và cô biết chuyện đó']}),
}
AMBIG = [
    Mut('A1', 'PARAPHRASE_VALID', 13, 'Nhờ xếp chồng những khối đá ấy lên,', 'Nhờ chồng những khối đá đó lên nhau,', {}),
    Mut('A2', 'PARAPHRASE_VALID', 29, 'chỉ là cách phía ma cà rồng gọi mà thôi', 'chỉ là cách gọi của phía ma cà rồng mà thôi', {}),
    Mut('A3', 'PARAPHRASE_VALID', 53, 'luôn đi kèm nguy hiểm', 'lúc nào cũng đi kèm nguy hiểm', {}),
]

# ---- holdout rules (deterministic, applied to chapters not used elsewhere) ----
NUM = ['hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín']
KANJI = {'hai': '二', 'ba': '三', 'bốn': '四', 'năm': '五', 'sáu': '六', 'bảy': '七', 'tám': '八', 'chín': '九'}
CLASSIFIER = r'(người|ngày|lần|con|tầng|tháng|chiếc|cái|thanh|viên)'


def nonempty(lines):
    return [i for i, l in enumerate(lines) if l.strip()]


def holdout_mutations(ch, rng):
    raw = dec(read_bytes(src('RAW', ch))).split('\n')
    drf = dec(read_bytes(src('DRAFT', ch))).split('\n')
    gl = dec(read_bytes(src('GLOSSARY', ch))).split('\n')[1:]
    rn, dn = nonempty(raw), nonempty(drf)
    assert len(rn) == len(dn), 'chapter %s is not 1:1 aligned' % ch
    pair = {d + 1: r + 1 for r, d in zip(rn, dn)}  # DRAFT line -> RAW line
    muts, notes = [], []
    # (a) untranslated glossary term
    rows = []
    for g in gl:
        parts = g.split(',')
        if len(parts) >= 3 and parts[2] in ('place', 'race', 'world_concept', 'item', 'title', 'organization'):
            rows.append((parts[0], parts[1]))
    rng.shuffle(rows)
    done = False
    for srcterm, tgt in rows:
        if len(tgt) < 3 or done:
            continue
        for dl, rl in pair.items():
            if srcterm in raw[rl - 1] and drf[dl - 1].count(tgt) == 1 and len(drf[dl - 1]) < 400:
                muts.append(Mut('H-A', 'OMITTED_WORD_UNTRANSLATED', dl, tgt, srcterm,
                                {'mustNotContain': [srcterm], 'mustContain': [tgt]}))
                done = True
                break
    if not done:
        notes.append('no untranslated-term site found')
    # (b) number change
    done = False
    for dl, rl in sorted(pair.items()):
        m = re.search(r'\b(%s) %s\b' % ('|'.join(NUM), CLASSIFIER), drf[dl - 1])
        if m and drf[dl - 1].count(m.group(0)) == 1 and KANJI[m.group(1)] in raw[rl - 1] and not done:
            nxt = NUM[(NUM.index(m.group(1)) + 1) % len(NUM)]
            muts.append(Mut('H-B', 'NUMBER', dl, m.group(0), nxt + ' ' + m.group(2),
                            {'mustContain': [m.group(0)], 'mustNotContain': [nxt + ' ' + m.group(2)]}))
            done = True
    if not done:
        notes.append('no number site found')
    # (c) missing last sentence
    done = False
    for dl, rl in sorted(pair.items()):
        sents = re.split(r'(?<=[.!?…])\s+', drf[dl - 1])
        if len(sents) >= 3 and raw[rl - 1].count('。') >= 3 and not done and len(drf[dl - 1]) < 600 and drf[dl - 1][0] != '「':
            last = sents[-1]
            if drf[dl - 1].count(' ' + last) == 1:
                muts.append(Mut('H-C', 'MISSING_SENTENCE', dl, ' ' + last, '', {'mustContain': [last[:30]]}))
                done = True
    if not done:
        # fallback: two sentences on both sides
        for dl, rl in sorted(pair.items()):
            sents = re.split(r'(?<=[.!?…])\s+', drf[dl - 1])
            if len(sents) == 2 and raw[rl - 1].count('。') >= 2 and not done and len(drf[dl - 1]) < 400 and drf[dl - 1][0] != '「':
                last = sents[-1]
                if len(last) > 25 and drf[dl - 1].count(' ' + last) == 1:
                    muts.append(Mut('H-C', 'MISSING_SENTENCE', dl, ' ' + last, '', {'mustContain': [last[:30]]}))
                    done = True
    if not done:
        notes.append('no missing-sentence site found')
    return drf, muts, notes


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=r'D:\P5E-private\p6-fixtures')
    ap.add_argument('--manifest', default='docs/P6_R0_FIXTURE_MANIFEST.json')
    args = ap.parse_args()
    out = args.out
    os.makedirs(out, exist_ok=True)
    os.makedirs(os.path.join(out, '_labels'), exist_ok=True)

    raw1, gl1, pr1 = read_bytes(src('RAW', '001')), read_bytes(src('GLOSSARY', '001')), read_bytes(src('PRONOUN', '001'))
    draft1 = read_bytes(src('DRAFT', '001'))
    clean1 = read_bytes(OWNER_FINAL_001)
    entries = []

    def emit(fid, kind, desc, raw_b, draft_b, gl_b, pr_b, labels, derived=None, chapter='001'):
        d = os.path.join(out, fid)
        if os.path.exists(d):
            raise SystemExit('fixture directory exists, refusing to overwrite: ' + d)
        os.makedirs(d)
        files = {}
        for name, b in (('RAW.txt', raw_b), ('DRAFT.txt', draft_b), ('GLOSSARY.csv', gl_b), ('PRONOUN.csv', pr_b)):
            open(os.path.join(d, name), 'wb').write(b)
            files[name] = {'path': fid + '/' + name, 'sha256': sha(b), 'bytes': len(b)}
        lab = json.dumps(labels, ensure_ascii=False, indent=1, sort_keys=True).encode('utf-8')
        lp = os.path.join(out, '_labels', fid + '.json')
        open(lp, 'wb').write(lab)
        entries.append({'id': fid, 'kind': kind, 'chapter': chapter, 'description': desc, 'derivedFrom': derived,
                        'files': files, 'labels': {'path': '_labels/' + fid + '.json', 'sha256': sha(lab)},
                        'targets': [{'id': t['id'], 'class': t['class'], 'severity': t.get('severity', ''), 'basis': t.get('basis', '')}
                                    for t in labels.get('targets', [])]})

    # real chapter 001 with its natural defects (see docs/P6_R0_ISSUE_TABLE.md)
    emit('fx-a01', 'real', 'chapter 001 as translated (natural defects: I-001, I-002 x4, I-003; I-004/I-005 uncertain)',
         raw1, draft1, gl1, pr1,
         {'targets': [
             {'id': 'I-001', 'class': 'OMITTED_WORD_UNTRANSLATED', 'severity': 'minor', 'basis': 'RAW 237 DRAFT 237',
              'mustNotContain': ['今回'], 'scope': 'DRAFT line 237'},
             {'id': 'I-002', 'class': 'SENSE_LOSS_CONTRAST', 'severity': 'major', 'basis': 'RAW 105,147,175,215 DRAFT 107,148,176,216',
              'rule': 'tohha-must-differ-from-kouryaku', 'humanRubric': '踏破 means reaching/traversing to the deepest level; it must not be rendered with the same conquer-verb as 攻略 where the text contrasts them (RAW 175 踏破するも攻略ならず).'},
             {'id': 'I-003', 'class': 'ADDRESS_PROFILE', 'severity': 'minor', 'basis': 'RAW 319 DRAFT 321 PRONOUN row 3',
              'mustContain': ['cô bé'], 'scope': 'DRAFT line 321'}],
          'uncertain': ['I-004', 'I-005']})
    # clean control
    emit('fx-a02', 'clean', 'chapter 001 with the owner-corrected text as DRAFT (no known defect; control)',
         raw1, clean1, gl1, pr1, {'targets': [], 'expectNoSemanticEdit': True}, derived='owner-manual-final-001')
    clean_lines = dec(clean1).split('\n')

    ids = {}
    for k, key in enumerate(['S1', 'S2', 'S3', 'S3B', 'S4A', 'S4B', 'S5', 'S6'], start=3):
        m = DEV[key]
        fid = 'fx-a%02d' % k
        ids[key] = fid
        lines = apply(clean_lines, [m])
        text = '\n'.join(lines)
        emit(fid, 'seeded', 'single seeded error on the clean control (%s)' % m.cls,
             raw1, encode_like(text, clean1), gl1, pr1,
             {'targets': [{'id': 'T-' + m.mid, 'class': m.cls, 'severity': 'major', 'basis': 'DRAFT line %d' % m.line,
                           'line': m.line, 'old': m.old, 'new': m.new, **m.checks}]},
             derived='fx-a02')
    multi = [DEV[k] for k in ('S1', 'S2', 'S3', 'S4A', 'S5', 'S6')]
    text = '\n'.join(apply(clean_lines, multi))
    emit('fx-a11', 'seeded', 'six independent seeded errors on the clean control (more than four findings)', raw1,
         encode_like(text, clean1), gl1, pr1,
         {'targets': [{'id': 'T-' + m.mid, 'class': m.cls, 'severity': 'major', 'basis': 'DRAFT line %d' % m.line,
                       'line': m.line, 'old': m.old, 'new': m.new, **m.checks} for m in multi]}, derived='fx-a02')
    text = '\n'.join(apply(clean_lines, AMBIG))
    emit('fx-a12', 'ambiguous', 'three equivalent paraphrases on the clean control; romaji monster names stay untranslated by design',
         raw1, encode_like(text, clean1), gl1, pr1,
         {'targets': [], 'expectNoSemanticEdit': True, 'paraphraseLines': [m.line for m in AMBIG]}, derived='fx-a02')

    # holdouts: chapters 003 and 005, rule-based seeded errors on the translator's DRAFT, natural defects unknown
    rng = random.Random(20261002)
    for fid, ch in (('fx-h01', '003'), ('fx-h02', '005')):
        drf, muts, notes = holdout_mutations(ch, rng)
        text = '\n'.join(apply(drf, muts))
        draft_b = read_bytes(src('DRAFT', ch))
        emit(fid, 'holdout', 'chapter %s DRAFT with rule-based seeded errors; natural defects unknown, adjudicated after the run' % ch,
             read_bytes(src('RAW', ch)), encode_like(text, draft_b), read_bytes(src('GLOSSARY', ch)), read_bytes(src('PRONOUN', ch)),
             {'targets': [{'id': 'T-' + m.mid, 'class': m.cls, 'severity': 'major', 'basis': 'DRAFT line %d' % m.line,
                           'line': m.line, 'old': m.old, 'new': m.new, **m.checks} for m in muts],
              'notes': notes, 'naturalDefectsUnknown': True}, derived='source-draft-' + ch, chapter=ch)

    holdout_hashes = sorted(f['sha256'] for e in entries if e['kind'] == 'holdout' for f in e['files'].values()) + \
        sorted(e['labels']['sha256'] for e in entries if e['kind'] == 'holdout')
    manifest = {
        'schema': 'p6.r0.fixture-manifest.v1',
        'generatedUtc': datetime.datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ'),
        'privateRoot': 'D:/P5E-private/p6-fixtures',
        'sources': {'volume': 'MERCEDES VOL 5', 'cleanControlBase': 'owner-manual-final-001 (6.FINAL/001, used as DRAFT input only)',
                    'rawSha256': sha(raw1), 'draftSha256': sha(draft1), 'glossarySha256': sha(gl1), 'pronounSha256': sha(pr1),
                    'cleanControlSha256': sha(clean1)},
        'runtimeRules': ['labels live under _labels and never enter a prompt', 'fixture directory names are opaque',
                         'the runtime builder refuses any path containing 6.FINAL'],
        'fixtures': entries,
        'holdout': {'ids': [e['id'] for e in entries if e['kind'] == 'holdout'],
                    'lockSha256': sha('\n'.join(holdout_hashes).encode('utf-8')),
                    'lockedBeforeAnyNewModelOutput': True,
                    'note': 'mutation sites were chosen by rule from chapters 003/005; no prompt or contract has been tuned on them'},
    }
    mp = args.manifest
    json.dump(manifest, open(mp, 'w', encoding='utf-8'), ensure_ascii=False, indent=1, sort_keys=True)
    print('fixtures:', len(entries), 'holdout lock', manifest['holdout']['lockSha256'][:16])
    for e in entries:
        print(e['id'], e['kind'], len(e['targets']), 'targets', [t['class'] for t in e['targets']])


if __name__ == '__main__':
    main()
