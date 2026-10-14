// Apache-2.0. The ported Calm selector against trajectories exported by the Kotlin controller.
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {CalmSelectorController, calmSelectorBar, createCalmNavigation} from './selector.mjs';

const fixture = JSON.parse(await readFile(new URL('./fixtures/selector-calm.json', import.meta.url), 'utf8'));
const MODES = {Rest: 'rest', TapTransit: 'tap', PressPending: 'pending', Held: 'held', Released: 'released'};
const field = name => fixture.frameFields.indexOf(name);
const bar = () => {
  const b = fixture.bar;
  return calmSelectorBar({width: b.width, height: b.height, count: b.count, inset: b.inset,
    pillWidth: b.pillWidth, pillInset: b.pillInset, density: b.density});
};

/** Positions in px; shape and optics in their own units. Float32 (Kotlin) against float64 here. */
const TOLERANCE = {cx: .05, cy: .05, left: .05, right: .05, top: .05, bottom: .05, formation: 1e-4,
  transit: 1e-3, heldTravel: 1e-4, a0: 1e-4, a1: 1e-4, a2: 1e-4, a3: 1e-4, spine: .05, radius: .05, band: .05};

for (const scenario of fixture.scenarios) {
  test(`Calm selector matches Kotlin: ${scenario.name}`, () => {
    const c = new CalmSelectorController();
    c.attach(bar());
    const rest = scenario.calls.find(call => call[1] === 'rest');
    c.snapToRest(rest ? rest[2] : 0);
    let next = 0;
    const worst = {};
    for (const frame of scenario.frames) {
      const tf = frame[field('t')];
      while (next < scenario.calls.length && scenario.calls[next][0] <= tf) {
        const [t, op, ...a] = scenario.calls[next++];
        if (op === 'down') c.pointerDown(a[0], a[1], t, true);
        else if (op === 'move') c.pointerMove(a[0], a[1], t);
        else if (op === 'drag') c.beginDrag(a[0], a[1]);
        else if (op === 'retarget') c.retarget(a[0]);
        else if (op === 'up') c.pointerUp(a[0]);
      }
      c.advanceFrameTo(tf);
      const e = c.extents(), f = c.frame;
      const ours = {cx: c.centreX, cy: c.centreY, formation: c.formation, transit: c.transitAppearance,
        heldTravel: c.heldTravel, left: e.left, right: e.right, top: e.top, bottom: e.bottom,
        a0: f.a[0], a1: f.a[1], a2: f.a[2], a3: f.a[3], spine: f.spine, radius: f.radius, band: f.band()};
      assert.equal(c.mode, MODES[frame[field('mode')]], `${scenario.name} t=${tf} mode`);
      for (const [key, tol] of Object.entries(TOLERANCE)) {
        const error = Math.abs(ours[key] - frame[field(key)]);
        worst[key] = Math.max(worst[key] ?? 0, error);
        assert.ok(error <= tol, `${scenario.name} t=${tf} ${key}: ${ours[key]} vs ${frame[field(key)]}`);
      }
    }
  });
}

test('navigation: a tap selects the tab under the finger and settles there', () => {
  const selected = [];
  const nav = createCalmNavigation({bar: bar(), onSelect: i => selected.push(i)});
  nav.down(700, 93, .1); nav.up(700, 93, .16);
  assert.deepEqual(selected, [2]);
  let t = .16;
  while (nav.frame(t) && t < 3) t += 1 / 60;
  assert.ok(t < 2, 'tap never settled');
  assert.ok(Math.abs(nav.state().centreX - nav.controller.bar.inset - 2.5 * nav.controller.bar.slotWidth) < .5);
});

test('navigation: a hold-and-drag commits where the finger ends, a fling carries one further', () => {
  const b = bar(), selected = [];
  const nav = createCalmNavigation({bar: b, onSelect: i => selected.push(i)});
  nav.down(155, 93, 0);
  for (let t = .01; t <= .3; t += .01) { nav.move(155, 93, t); nav.frame(t); }
  assert.ok(nav.state().held, 'hold did not form');
  // Slow drag to the middle tab: commits the middle.
  for (let i = 1; i <= 50; i++) { const t = .3 + i * .02; nav.move(155 + 262 * i / 50, 93, t); nav.frame(t); }
  for (let i = 1; i <= 10; i++) { const t = 1.3 + i * .02; nav.move(417, 93, t); nav.frame(t); }
  nav.up(417, 93, 1.52);
  assert.deepEqual(selected, [1]);
  // A fast flick from the middle carries to the last tab.
  nav.down(417, 93, 3);
  for (let i = 1; i <= 15; i++) { const t = 3 + i * .02; nav.move(417, 93, t); nav.frame(t); }
  for (let i = 1; i <= 10; i++) { const t = 3.3 + i * .01; nav.move(417 + 25 * i, 93, t); nav.frame(t); }
  nav.up(667, 93, 3.4);
  assert.deepEqual(selected, [1, 2]);
});

test('navigation: a vertical scroll before the hold cancels without selecting', () => {
  const selected = [];
  const nav = createCalmNavigation({bar: bar(), onSelect: i => selected.push(i)});
  nav.down(417, 93, 0); nav.move(417, 140, .05); nav.up(417, 140, .08);
  assert.deepEqual(selected, []);
  assert.equal(nav.selected, 0);
});

test('reduced motion snaps to rest and never holds', () => {
  const nav = createCalmNavigation({bar: bar(), motion: false});
  nav.down(700, 93, 0);
  for (let t = .01; t <= .5; t += .01) { nav.move(700, 93, t); nav.frame(t); }
  assert.equal(nav.state().held, false);
  nav.up(700, 93, .5); nav.frame(.52);
  assert.equal(nav.state().formation, 0);
});
