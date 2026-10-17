// Apache-2.0. The Calm bar's fusion with a free button beside it (Compose: GlassTabBarAccessory.link,
// GlassFuse.toPx / toMaterialPx, accessoryExtents, glassInscribedStadium, glassAccessoryYield), checked
// against fixtures/accessory-fuse.json. iOS 26's Phone search button joins the bar through a neck while
// pressed and breaks away when pulled (IMG_6756, 15 October).

const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));

/** The largest stadium (half extents and corner radius) inside an axis-aligned ellipse with half axes a, b. */
export function inscribedStadium(a, b) {
  if (a <= 0 || b <= 0) return [a, b, Math.min(a, b)];
  const long = Math.max(a, b), short = Math.min(a, b);
  if (long - short < 1e-3) return [a, b, short];
  const inside = (c, r) => {
    // The cap circle centred on the long axis at c, radius r, sampled every 7.5 degrees.
    for (let i = 0; i <= 12; i++) {
      const t = i * (Math.PI / 24), x = c + r * Math.cos(t), y = r * Math.sin(t);
      if ((x * x) / (long * long) + (y * y) / (short * short) > 1.0001) return false;
    }
    return true;
  };
  let best = [short, short, 0], bestArea = -1;
  for (let i = 0; i <= 8; i++) {
    const r = short * (0.6 + 0.05 * i);
    let lo = 0, hi = long - r;
    if (!inside(0, r)) continue;
    for (let k = 0; k < 14; k++) { const mid = (lo + hi) / 2; if (inside(mid, r)) lo = mid; else hi = mid; }
    const area = 4 * lo * r + Math.PI * r * r;
    // 3% inside, for the slant the axis-aligned stadium cannot follow.
    if (area > bestArea) { bestArea = area; best = [(lo + r) * .97, r * .97, r * .97]; }
  }
  return a >= b ? best : [best[1], best[0], best[2]];
}

/**
 * The rect a Calm bar's outline fuses with when a free button sits beside it, as drawCalmBar's `fuse`:
 * [centre x, centre y (from the bar's centre), half width, half height, corner radius, smoothing], px in
 * the bar's material space. `bar` is the bar's size and its own press scale (the navigation's), `button`
 * the button's laid-out rect relative to the bar's top-left, `transform` the button's createFreeButton
 * sample (its press and pull move and reshape what the bar reaches for), `smoothing` the fusion width
 * (Compose's GlassTabBarAccessory.fusion, times accessoryYield where a lens arrives).
 */
export function accessoryFuse({bar, button, transform = {}, smoothing}) {
  const {width: W, height: H, pressX: bx = 1, pressY: by = 1} = bar;
  const {pressScale = 1, along = 1, across = 1, angle = 0, tx = 0, ty = 0} = transform;
  if (![W, H, bx, by, button.x, button.y, button.width, button.height, smoothing].every(Number.isFinite))
    throw new RangeError('accessoryFuse requires finite geometry');
  const hw = button.width / 2, hh = button.height / 2;
  // accessoryExtents: the pressed body stretched along the pull (half axis A) and thinned across it (B).
  const a = angle * Math.PI / 180, c = Math.cos(a), s = Math.sin(a);
  const r = (hw * pressScale + hh * pressScale) / 2, A = r * along, B = r * across;
  const reach = (cx, sx) => 1 / Math.sqrt((cx * cx) / (A * A) + (sx * sx) / (B * B));
  const stadium = inscribedStadium(reach(c, s), reach(s, c));
  const fx = button.x + hw + tx - W / 2, fy = button.y + hh + ty - H / 2;
  // toMaterialPx for an anchored fuse: undo the bar's own press about its centre.
  const ex = Math.max(bx, 1e-3), ey = Math.max(by, 1e-3), e = Math.sqrt(ex * ey);
  return [fx / ex, fy / ey, stadium[0] / ex, stadium[1] / ey, stadium[2] / e, smoothing / e];
}

/**
 * How much of the neck a formed lens pressing into the bar's end has taken over, 0 to 1 (Compose's
 * glassAccessoryYield): multiply the fusion by 1 minus it so bar and button part as the lens arrives.
 * `endEdge` is the lens body's edge on the button's side, bar-local px.
 */
export function accessoryYield(formation, endEdge, barWidth, barHeight) {
  const span = Math.max(.1 * barHeight, 1e-3);
  const reach = clamp((endEdge - (barWidth - span)) / (2 * span), 0, 1);
  return clamp(formation, 0, 1) * reach * reach * (3 - 2 * reach);
}
