"""Tile out/sheets/<id>/t_*.png into labelled 4×2 contact sheets: out/sheets/<id>-beats-N.png"""
import glob
import os
import sys

from PIL import Image, ImageDraw

for comp_dir in sorted(glob.glob('out/sheets/*/')):
    cid = os.path.basename(os.path.normpath(comp_dir))
    if len(sys.argv) > 1 and cid not in sys.argv[1:]:
        continue
    files = sorted(glob.glob(os.path.join(comp_dir, 't_*.png')), key=lambda f: float(os.path.basename(f)[2:-4]))
    if not files:
        continue
    w, h = Image.open(files[0]).size
    scale = 480 / max(w, h)
    tw, th = int(w * scale), int(h * scale)
    cols = 4
    for n in range(0, len(files), 8):
        chunk = files[n:n + 8]
        rows = (len(chunk) + cols - 1) // cols
        sheet = Image.new('RGB', (cols * tw, rows * (th + 26)), (12, 12, 20))
        for i, f in enumerate(chunk):
            im = Image.open(f).convert('RGB').resize((tw, th))
            x, y = (i % cols) * tw, (i // cols) * (th + 26)
            sheet.paste(im, (x, y + 26))
            ImageDraw.Draw(sheet).text((x + 8, y + 6), f"{float(os.path.basename(f)[2:-4]):.2f}s", fill=(221, 248, 61))
        out = f'out/sheets/{cid}-beats-{n // 8 + 1}.png'
        sheet.save(out)
        print(out)
