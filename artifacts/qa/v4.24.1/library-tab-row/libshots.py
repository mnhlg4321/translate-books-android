"""Library tab row screenshots on emulator-5554 at the owner phone geometry (1264 px wide, 560 dpi = 361 dp).
usage: py -I libshots.py LABEL OUTDIR [fontPercents] [palettes]
Writes OUTDIR/LABEL-font{pct}-{palette}-{tab}.png for the four tabs. Only QA/empty data; no provider call."""
import os, re, subprocess, sys, time
import xml.etree.ElementTree as ET

ADB = os.path.expandvars(r'%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe')
SER = 'emulator-5554'
PKG = 'com.ml.tblandroidtxt'
env = dict(os.environ, MSYS_NO_PATHCONV='1')


def adb(*a, text=True, check=False):
    r = subprocess.run([ADB, '-s', SER] + list(a), capture_output=True, env=env)
    if check and r.returncode:
        raise RuntimeError(r.stderr.decode(errors='replace'))
    return r.stdout.decode('utf-8', errors='replace') if text else r.stdout


def dump():
    adb('shell', 'uiautomator', 'dump', '/sdcard/u.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/u.xml').strip())


def find(root, pred):
    for n in root.iter('node'):
        if pred(n):
            m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n.get('bounds'))
            x1, y1, x2, y2 = map(int, m.groups())
            return (x1 + x2) // 2, (y1 + y2) // 2
    return None


def tap(x, y):
    adb('shell', 'input', 'tap', str(x), str(y))


def shot(path):
    open(path, 'wb').write(adb('exec-out', 'screencap', '-p', text=False))


def set_appearance(font, palette):
    xml = ("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
           "  <string name=\"mode\">fixed</string>\n  <string name=\"palette\">%s</string>\n  <string name=\"paletteLight\">light</string>\n"
           "  <int name=\"fontPercent\" value=\"%d\" />\n  <string name=\"density\">comfortable</string>\n  <string name=\"corners\">medium</string>\n</map>\n") % (palette, font)
    open('appearance.tmp.xml', 'w', encoding='utf-8', newline='\n').write(xml)
    adb('push', 'appearance.tmp.xml', '/data/local/tmp/appearance.xml')
    adb('shell', 'am', 'force-stop', PKG)
    adb('shell', 'run-as', PKG, 'cp', '/data/local/tmp/appearance.xml', 'shared_prefs/appearance.xml')


def main():
    label, out = sys.argv[1], sys.argv[2]
    fonts = [int(x) for x in (sys.argv[3] if len(sys.argv) > 3 else '100,115,130').split(',')]
    pals = (sys.argv[4] if len(sys.argv) > 4 else 'default,light').split(',')
    os.makedirs(out, exist_ok=True)
    for pal in pals:
        for font in fonts:
            set_appearance(font, pal)
            adb('shell', 'am', 'start', '-n', PKG + '/.MainActivity')
            time.sleep(4)
            # open the drawer and the Library destination
            root = dump()
            p = find(root, lambda n: n.get('text') == '☰')
            if not p:
                print('no menu button', font, pal); continue
            tap(*p); time.sleep(1.2)
            root = dump()
            p = find(root, lambda n: 'Thư viện' in (n.get('text') or ''))
            if not p:
                print('no library entry', font, pal); continue
            tap(*p); time.sleep(2)
            for tabname in ('RAW', 'Glossary', 'Pronoun', 'Tập'):
                root = dump()
                p = find(root, lambda n: n.get('text') == tabname and n.get('clickable') == 'true')
                if p:
                    tap(*p); time.sleep(1.5)
                else:
                    print('tab missing', tabname, font, pal)
                shot(os.path.join(out, '%s-font%d-%s-%s.png' % (label, font, pal, 'Tap' if tabname == 'Tập' else tabname)))
            print('done', font, pal)
    adb('shell', 'am', 'force-stop', PKG)


main()
