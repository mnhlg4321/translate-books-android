#!/usr/bin/env python3
"""Verifies the R0 fixture set against docs/P6_R0_FIXTURE_MANIFEST.json (no book text is printed).

Checks: every file exists and matches its SHA-256; each seeded/holdout DRAFT differs from its base in exactly the
labelled lines (and nowhere else); a mutation leaves no accidental second CJK token; the holdout lock reproduces;
no runtime file contains a label, fixture id or target id (oracle leak); fixture directory names are opaque.
"""
import hashlib, json, os, re, sys

manifest = json.load(open(sys.argv[1] if len(sys.argv) > 1 else 'docs/P6_R0_FIXTURE_MANIFEST.json', encoding='utf-8'))
root = manifest['privateRoot'].replace('/', os.sep)
sha = lambda b: hashlib.sha256(b).hexdigest()
rd = lambda p: open(p, 'rb').read()
errors = []
by = {e['id']: e for e in manifest['fixtures']}
CJK = re.compile(r'[\u3040-\u30ff\u4e00-\u9fff]')


def lines(fid):
    return rd(os.path.join(root, fid, 'DRAFT.txt')).decode('utf-8-sig').split('\n')


for e in manifest['fixtures']:
    for name, f in e['files'].items():
        p = os.path.join(root, f['path'].replace('/', os.sep))
        if not os.path.exists(p) or sha(rd(p)) != f['sha256']:
            errors.append('%s/%s hash mismatch or missing' % (e['id'], name))
    lp = os.path.join(root, e['labels']['path'].replace('/', os.sep))
    if not os.path.exists(lp) or sha(rd(lp)) != e['labels']['sha256']:
        errors.append('%s labels hash mismatch' % e['id'])
    if not re.fullmatch(r'fx-[ah]\d\d', e['id']):
        errors.append('%s: directory name is not opaque' % e['id'])

# mutation scope: derived fixtures differ from their base only on the labelled lines
base_for = {'fx-a02': None}
for e in manifest['fixtures']:
    lab = json.loads(rd(os.path.join(root, e['labels']['path'].replace('/', os.sep))))
    targets = lab.get('targets', [])
    if e['kind'] in ('seeded', 'ambiguous', 'holdout'):
        if e['kind'] == 'holdout':
            base_lines = rd(os.path.join(r'D:\Ebooks\MERCEDES\VOL 5', '05_DRAFT_MERCEDES_VOL5', e['chapter'] + '_DRAFT_MERCEDES_VOL5.TXT')).decode('utf-8-sig').split('\n')
        else:
            base_lines = lines('fx-a02')
        cur = lines(e['id'])
        if len(cur) != len(base_lines):
            errors.append('%s: line count differs from base (%d vs %d)' % (e['id'], len(cur), len(base_lines)))
            continue
        changed = [i + 1 for i, (a, b) in enumerate(zip(base_lines, cur)) if a != b]
        expected = sorted({t['line'] for t in targets} | set(lab.get('paraphraseLines', [])))
        if changed != expected:
            errors.append('%s: changed lines %s != labelled lines %s' % (e['id'], changed, expected))
        for t in targets:
            line = cur[t['line'] - 1]
            if t['new'] and t['new'] not in line:
                errors.append('%s %s: new text missing' % (e['id'], t['id']))
            if t['old'] and t['old'] in line and t['old'] != '' and t['new'].find(t['old']) < 0:
                errors.append('%s %s: old text still present' % (e['id'], t['id']))
        # a seeded fixture must not gain CJK characters beyond the labelled untranslated-token class
        gained = sum(len(CJK.findall(cur[i - 1])) - len(CJK.findall(base_lines[i - 1])) for i in changed)
        allowed = any(t['class'] == 'OMITTED_WORD_UNTRANSLATED' for t in targets)
        if gained and not allowed:
            errors.append('%s: gained %d CJK chars without an untranslated-token target' % (e['id'], gained))

# holdout lock reproduces
hh = sorted(f['sha256'] for e in manifest['fixtures'] if e['kind'] == 'holdout' for f in e['files'].values()) + \
     sorted(e['labels']['sha256'] for e in manifest['fixtures'] if e['kind'] == 'holdout')
if sha('\n'.join(hh).encode('utf-8')) != manifest['holdout']['lockSha256']:
    errors.append('holdout lock does not reproduce')

# oracle leak: runtime files must not carry ids, class names or labels
leak_terms = ['fx-a', 'fx-h', 'T-S', 'T-H', 'SEEDED', 'seeded', 'MISSING_SENTENCE', 'ROLE_SWAP', 'NUMBER', 'NEGATION', 'mustContain', 'mustNotContain']
for e in manifest['fixtures']:
    for name in e['files']:
        text = rd(os.path.join(root, e['id'], name)).decode('utf-8-sig')
        for term in leak_terms:
            if term in text:
                errors.append('%s/%s contains leak term %r' % (e['id'], name, term))

print('fixtures checked:', len(manifest['fixtures']), 'errors:', len(errors))
for x in errors:
    print(' -', x)
sys.exit(1 if errors else 0)
