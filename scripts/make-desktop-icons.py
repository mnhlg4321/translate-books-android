"""Builds the PC icons from the phone logo: desktop/packaging/translate_books.ico (16..256) and the window PNGs under
desktop/src/main/resources/desktop/icons/. Run again only when the logo changes; the results are committed."""
import os
from PIL import Image

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
logo = Image.open(os.path.join(root, 'app', 'src', 'main', 'res', 'drawable-nodpi', 'translate_books_logo.png')).convert('RGBA')
side = max(logo.size)
square = Image.new('RGBA', (side, side), (0, 0, 0, 0))
square.paste(logo, ((side - logo.width) // 2, (side - logo.height) // 2))
sizes = [16, 24, 32, 48, 64, 128, 256]
icons = os.path.join(root, 'desktop', 'src', 'main', 'resources', 'desktop', 'icons')
os.makedirs(icons, exist_ok=True)
for s in sizes:
    square.resize((s, s), Image.LANCZOS).save(os.path.join(icons, f'logo-{s}.png'))
os.makedirs(os.path.join(root, 'desktop', 'packaging'), exist_ok=True)
square.save(os.path.join(root, 'desktop', 'packaging', 'translate_books.ico'), format='ICO', sizes=[(s, s) for s in sizes])
print('ok', sizes)
