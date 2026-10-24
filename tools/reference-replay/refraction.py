# refraction.py: how far the glass selector bends what lies beneath it, iOS against our replay of the
# same recording (GitHub issue #77). Measurement only; it changes nothing.
#
#   python tools/reference-replay/refraction.py <name> <ours-subdir> [<name> <ours-subdir> ...] --csv out.csv [--no-icons]
#   python tools/reference-replay/refraction_summary.py out.csv [...]   (the tables of the record)
#
# Run from the repository root of the main checkout. The recordings and the replay catalogues are
# private and stay out of git: docs/1.MP4, docs/2.MP4, research/datasets/..., and
# .local/sxs/ref/<name>/{catalogue3.txt, <ours-subdir>/fNNNNN.png} (frames rendered by
# `:desktop:phoneReplay` at 60 fps). Both sources go through the same code; positions are scene pixels
# (3 px/pt, the bottom 702 rows of the screen). iOS frames are HDR10 decoded to display sRGB (the chain
# of .local/sxs/hdr.py, research/README.md II.4).
#
# Two measures, each against an unrefracted copy of the same feature in the same video:
#
#  RIM  The bar's top and bottom edges are straight with a bright hairline. Where a held lens overhangs
#       the bar, each lens column shows, going inward: the lens rim, a band of the page beyond the bar,
#       then the bar's hairline. Its apparent row y_A is compared with the true hairline y_B of the same
#       frame, read at bar columns at least 230 px from the lens (straight run).
#         d = y_A - y_B toward the lens centre (px; + = the exterior is pulled inward, i.e. sampled from
#             further out, the iOS behaviour);
#         h = y_B's depth inside the lens rim (px).
#       Only columns whose page just outside the bar is dark (black pages, gaps between rows) count.
#  ICON Each tab's icon and label box is matched (normalised cross-correlation over scale 0.90-1.50 and
#       shift) against the same tab at rest with the selector elsewhere: apparent scale s and centre
#       shift (dx, dy), against u = tab centre minus selector centre. The selector centre is the
#       selected (blue) ink's centroid in each source. Under the selector the icon is the selected ink,
#       so s includes any ink scale as well as the lens's magnification.
#
# States: rest (no touch for 0.6 s), tap (0-0.4 s after a tap's onset), held (inside a hold or held
# drag, ink speed < 1.5 px/frame) and moving (inside a held drag, faster).
import sys, os, re, csv, subprocess
import numpy as np
import cv2
from PIL import Image

ROOT = os.getcwd()
REF = os.path.join(ROOT, '.local', 'sxs', 'ref')
VIDEO = {'docs1': 'docs/1.MP4', 'docs2': 'docs/2.MP4',
         'IMG_6756': 'research/datasets/ios27-phone/bubble/IMG_6756.MP4',
         'JGEY2190': 'research/datasets/ios27-phone/bubble/JGEY2190.MP4',
         'LOLL8185': 'research/datasets/ios27-phone/LOLL8185.MP4'}
CX = [217, 479, 742]          # tab centres at rest (scene px)
Y0, Y1 = 380, 700             # scene rows decoded / read
SDR = ("zscale=t=linear:npl=100,format=gbrpf32le,"
       "zscale=p=bt709:t=iec61966-2-1:m=bt709:r=full,format=rgb24")


def luma(a):
    return a.astype(np.float32) @ np.array([.299, .587, .114], np.float32)


def vsize(path):
    w, h = subprocess.run(['ffprobe', '-v', 'error', '-select_streams', 'v:0', '-show_entries',
                           'stream=width,height', '-of', 'csv=p=0', path],
                          capture_output=True, text=True).stdout.strip().split(',')[:2]
    return int(w), int(h)


def ios_frames(path, t0, dur):
    """Yield (t, rgb scene rows Y0..Y1) for every source frame in [t0, t0 + dur], display sRGB."""
    w, h = vsize(path)
    cw, ch, cy = w, Y1 - Y0, h - 702 + Y0
    vf = f"crop={cw}:{ch}:0:{cy}," + SDR
    args = ['-ss', f'{t0:.3f}', '-t', f'{dur:.3f}']
    log = subprocess.run(['ffmpeg', '-v', 'info'] + args + ['-i', path, '-fps_mode', 'passthrough',
                          '-vf', 'showinfo', '-f', 'null', '-'], capture_output=True, text=True).stderr
    pts = [t0 + float(x) for x in re.findall(r'pts_time:([\d.]+)', log)]
    p = subprocess.Popen(['ffmpeg', '-v', 'error'] + args + ['-i', path, '-fps_mode', 'passthrough',
                          '-vf', vf, '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-'], stdout=subprocess.PIPE)
    n = cw * ch * 3
    for t in pts:
        b = p.stdout.read(n)
        if len(b) < n:
            break
        yield t, np.frombuffer(b, np.uint8).reshape(ch, cw, 3)
    p.stdout.close()
    p.wait()


def our_frame(d, t):
    p = os.path.join(d, f'f{round(t * 60):05d}.png')
    return np.array(Image.open(p).convert('RGB'))[Y0:Y1] if os.path.exists(p) else None


def ink_cx(rgb):
    """Centroid x of the selected (blue) ink in scene rows 480-600 (track.py's mask); None without ink."""
    a = rgb[480 - Y0:600 - Y0, 60:900].astype(int)
    m = (a[..., 2] - a[..., 0] > 30) & (a[..., 2] > 110) & (a[..., 2] - a[..., 1] > 15)
    if m.sum() < 300:
        return None
    return float(np.nonzero(m)[1].mean() + 60)


# ---------------------------------------------------------------- RIM
def _cross(p, lo, hi):
    """Sub-pixel index where profile p first rises through the middle of [lo, hi]."""
    mid = (lo + hi) / 2
    for i in range(1, len(p)):
        if p[i - 1] < mid <= p[i]:
            return i - 1 + (mid - p[i - 1]) / max(p[i] - p[i - 1], 1e-3)
    return None


def _inward(g, x, top, start, n):
    """Column x (3 px wide) as a profile running inward from scene row `start`, with its rows."""
    col = g[:, x - 1:x + 2].mean(axis=1)
    i = start - Y0
    if top:
        return col[i:i + n], start + np.arange(n, dtype=float)
    return col[i - n + 1:i + 1][::-1], start - np.arange(n, dtype=float)


def _first_peak(c):
    i = next((i for i in range(len(c)) if c[i] > 18), None)
    if i is None:
        return None, None
    j = i
    while j + 1 < len(c) and c[j + 1] >= c[j]:
        j += 1
    return i, j


def bar_edges(g, cx):
    """True hairline rows (top, bottom) of the bar's straight run at columns >= 230 px from cx."""
    out = {True: [], False: []}
    for x in range(200, 800, 8):
        if cx is not None and abs(x - cx) < 230:
            continue
        for top in (True, False):
            c, ys = _inward(g, x, top, 405 if top else 675, 75)
            if c[0] > 12:
                continue
            i, j = _first_peak(c)
            if i is None or j > 60:
                continue
            e = _cross(c[:j + 1], 0, c[j])
            if e is not None:
                out[top].append(ys[0] + e if top else ys[0] - e)
    f = lambda v: float(np.median(v)) if len(v) >= 4 else None
    return f(out[True]), f(out[False])


def _side(g, x, top):
    """('lens', rim row, apparent hairline row) or None for one column."""
    c, ys = _inward(g, x, top, 405 if top else 675, 120)
    if c[0] > 12:
        return None              # the page just outside is not dark here
    i, j = _first_peak(c)
    if i is None or i > 70:
        return None
    seg = c[j:j + 60]
    if len(seg) < 20:
        return None
    k = int(np.argmin(seg[:45])); band = seg[k]
    after = seg[k:k + 16]
    kk = int(np.argmax(after)); peak2 = after[kk]
    if k < 2 or peak2 - band < 18:
        return None              # no band: no lens over this column
    e = _cross(after[:kk + 1], band, peak2)
    if e is None:
        return None
    pos = j + k + e
    return 'lens', float(ys[i]), float(ys[0] + pos if top else ys[0] - pos)


def rim(g, cx):
    """[(side, x - cx, h, d)] for lens columns within 200 px of cx."""
    bt, bb = bar_edges(g, cx)
    res = []
    for x in range(int(cx) - 200, int(cx) + 201, 3):
        if x < 30 or x > 1140:
            continue
        for top, yb in ((True, bt), (False, bb)):
            if yb is None:
                continue
            r = _side(g, x, top)
            if not r:
                continue
            _, yr, ya = r
            h = (yb - yr) if top else (yr - yb)
            d = (ya - yb) if top else (yb - ya)
            if 4 <= h <= 60:          # the lens rim overhangs the bar
                res.append(('top' if top else 'bottom', x - cx, h, d, yb, yr, ya))
    return res


# ---------------------------------------------------------------- ICON
IY0, IY1, IHW = 474, 612, 82    # icon + label box (scene rows, half width)


def icon_template(g, k):
    return np.ascontiguousarray(g[IY0 - Y0:IY1 - Y0, CX[k] - IHW:CX[k] + IHW])


def icon_fit(g, tmpl, k, scales=np.arange(0.90, 1.505, 0.02)):
    """(scale, dx, dy, ncc) of tab k's icon box in frame g against its resting template."""
    best = (-2, None)
    h0, w0 = tmpl.shape
    cy0 = (IY0 + IY1) / 2 - Y0
    for s in scales:
        t = cv2.resize(tmpl, (int(round(w0 * s)), int(round(h0 * s))), interpolation=cv2.INTER_LINEAR)
        h, w = t.shape
        y0 = int(max(0, cy0 - h / 2 - 22)); y1 = int(min(g.shape[0], cy0 + h / 2 + 22))
        x0 = int(max(0, CX[k] - w / 2 - 45)); x1 = int(min(g.shape[1], CX[k] + w / 2 + 45))
        reg = np.ascontiguousarray(g[y0:y1, x0:x1])
        if reg.shape[0] < h or reg.shape[1] < w:
            continue
        r = cv2.matchTemplate(reg, t, cv2.TM_CCOEFF_NORMED)
        _, mx, _, loc = cv2.minMaxLoc(r)
        if mx > best[0]:
            best = (mx, (s, x0 + loc[0] + w / 2 - CX[k], y0 + loc[1] + h / 2 - cy0))
    if best[1] is None:
        return None
    s, dx, dy = best[1]
    return s, dx, dy, best[0]


# ---------------------------------------------------------------- episodes
def moments(name):
    out = []
    for w in (l.split() for l in open(os.path.join(REF, name, 'catalogue3.txt')) if l.strip()):
        a, b = w[0].split('-')
        t0, t1 = (float(a), float(b)) if b else (float(a), float(w[1]))
        rest = ' '.join(w)
        kind = 'tap' if ' tap ' in f' {rest} ' else ('hold' if 'stationary' in rest else 'drag')
        ab = [p for p in w if '->' in p][0].split('->')
        if 'None' in ab:
            continue
        out.append((kind, t0, max(t1, t0), int(ab[0]), int(ab[1])))
    return out


def state(ms, t, speed):
    for kind, t0, t1, a, b in ms:
        if kind == 'tap' and t0 - .02 <= t <= t0 + .4:
            return 'tap'
        if kind in ('hold', 'drag') and t0 + .15 <= t <= t1:
            return 'held' if speed < 1.5 else 'moving'
    prev = max([t1 for _, t0, t1, _, _ in ms if t1 < t] + [-1e9])
    nxt = min([t0 for _, t0, t1, _, _ in ms if t0 > t] + [1e9])
    return 'rest' if t - prev > .6 and nxt - t > .1 else None


def windows(ms):
    """Merged sampling windows: each touch from 0.35 s before to 0.6 s after."""
    out = []
    for a, b in sorted((max(t0 - .35, 0), t1 + .6) for _, t0, t1, _, _ in ms):
        if out and a <= out[-1][1]:
            out[-1][1] = max(out[-1][1], b)
        else:
            out.append([a, b])
    return out


def templates(ms, get):
    """Resting template per tab: 0.25 s before a press while the selector sits >= 200 px away."""
    tm = {}
    for kind, t0, t1, a, b in ms:
        for k in range(3):
            if k in tm or a == k:
                continue
            g = get(t0 - .25)
            if g is None:
                continue
            c = ink_cx(g)
            if c is None or abs(c - CX[k]) < 200:
                continue
            tm[k] = icon_template(luma(g), k)
    return tm


def measure(name, sub, step=2, icons=True):
    ms = moments(name)
    video = os.path.join(ROOT, VIDEO[name])
    ours_dir = os.path.join(REF, name, sub)
    rows = []
    for src in ('ios', 'ours'):
        if src == 'ios':
            def get(t):
                fr = list(ios_frames(video, max(t - .03, 0), .07))
                return min(fr, key=lambda f: abs(f[0] - t))[1] if fr else None
        else:
            def get(t):
                return our_frame(ours_dir, t)
        tm = templates(ms, get) if icons else {}
        for a, b in windows(ms):
            if src == 'ios':
                seq = (f for i, f in enumerate(ios_frames(video, a, b - a)) if i % step == 0)
            else:
                seq = ((n / 60, our_frame(ours_dir, n / 60)) for n in range(int(np.ceil(a * 60)), int(b * 60), step))
                seq = ((t, f) for t, f in seq if f is not None)
            prev = None
            for t, f in seq:
                cx = ink_cx(f)
                if cx is None:
                    prev = None
                    continue
                sp = abs(cx - prev[1]) / max((t - prev[0]) * 60, 1e-3) if prev else 99
                prev = (t, cx)
                st = state(ms, t, sp)
                if st is None:
                    continue
                g = luma(f)
                if st in ('held', 'moving'):
                    for side, dx, h, d, yb, yr, ya in rim(g, cx):
                        rows.append((name, src, st, round(t, 3), round(cx, 1), 'rim', side, dx, round(h, 1), round(d, 2), round(yb, 1), round(ya, 1)))
                for k in range(3):
                    u = CX[k] - cx
                    if not icons or abs(u) > 330 or k not in tm:
                        continue
                    r = icon_fit(g, tm[k], k)
                    if r is None or r[3] < .5:
                        continue
                    s, ddx, ddy, ncc = r
                    rows.append((name, src, st, round(t, 3), round(cx, 1), 'icon', k, round(u), round(s, 2), round(ddx, 1), round(ddy, 1), round(ncc, 2)))
        print(f'  {name} {src}: {len(rows)} rows so far', flush=True)
    return rows


HEAD = ('rec', 'src', 'state', 't', 'cx', 'kind', 'side_or_tab', 'x_or_u', 'h_or_scale', 'd_or_dx', 'yb_or_dy', 'ya_or_ncc')

if __name__ == '__main__':
    args = sys.argv[1:]
    icons = '--no-icons' not in args
    args = [a for a in args if a != '--no-icons']
    out = 'refraction.csv'
    if '--csv' in args:
        out = args[args.index('--csv') + 1]
        del args[args.index('--csv'):args.index('--csv') + 2]
    for name, sub in zip(args[::2], args[1::2]):
        r = measure(name, sub, icons=icons)
        new = not os.path.exists(out)
        with open(out, 'a', newline='') as f:
            w = csv.writer(f)
            if new:
                w.writerow(HEAD)
            w.writerows(r)
        print(name, sub, len(r), 'rows ->', out, flush=True)
