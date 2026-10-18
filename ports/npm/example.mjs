/** Apache-2.0. The example page: scheme switching, frost toggle and a canvas source. */
import {configureCanvasKit, LiquidGlassElement} from './index.mjs';

// canvaskit.js is loaded by a <script> tag; tell it where its .wasm is served.
configureCanvasKit({locateFile: file => `./node_modules/canvaskit-wasm/bin/${file}`});

const params = new URLSearchParams(location.search);
const glass = () => [...document.querySelectorAll('liquid-glass')];

function setScheme(scheme) {
  if (scheme === 'auto') delete document.documentElement.dataset.scheme;
  else document.documentElement.dataset.scheme = scheme;
  for (const el of glass()) el.scheme = scheme;
  for (const button of document.querySelectorAll('[data-scheme-choice]'))
    button.setAttribute('aria-pressed', String(button.dataset.schemeChoice === scheme));
}
for (const button of document.querySelectorAll('[data-scheme-choice]'))
  button.addEventListener('click', () => setScheme(button.dataset.schemeChoice));

const frost = document.querySelector('#frost');
frost.addEventListener('change', () => { for (const el of glass()) el.lensing = frost.checked ? 'off' : 'auto'; });

// A colourful source for the canvas engine, drawn once (call refresh() on each glass if it changes).
const photo = document.querySelector('#photo');
function drawPhoto() {
  const g = photo.getContext('2d'), w = photo.width, h = photo.height;
  const sky = g.createLinearGradient(0, 0, w, h);
  sky.addColorStop(0, '#173a8a'); sky.addColorStop(0.45, '#7b2fbe'); sky.addColorStop(1, '#ff7a4d');
  g.fillStyle = sky; g.fillRect(0, 0, w, h);
  const blobs = [[160, 90, 90, '#ffd23d'], [520, 260, 120, '#2bd4a4'], [820, 110, 80, '#ff3d7f'], [380, 60, 50, '#3dd6ff']];
  for (const [x, y, r, c] of blobs) { g.fillStyle = c; g.beginPath(); g.arc(x, y, r, 0, Math.PI * 2); g.fill(); }
  g.fillStyle = 'rgba(255,255,255,.9)';
  for (let x = 0; x < w; x += 16) g.fillRect(x, 300, 2, 60);
  g.fillStyle = '#fff'; g.font = '800 64px system-ui, sans-serif'; g.fillText('Canvas source', 40, 210);
  g.font = '18px Georgia, serif'; g.fillText('Drawn once by the page; the glass refracts these pixels.', 44, 246);
}
drawPhoto();
for (const el of document.querySelectorAll('liquid-glass[engine=canvas]')) el.source = photo;

setScheme(['light', 'dark'].includes(params.get('scheme')) ? params.get('scheme') : 'auto');
if (params.get('lensing') === 'off') { frost.checked = true; frost.dispatchEvent(new Event('change')); }

const status = document.querySelector('#status');
function report() {
  const lines = glass().map(el => `#${el.id}: ${el.renderState.engine} / ${el.renderState.optics}` +
    (el.renderState.reason ? ` (${el.renderState.reason})` : ''));
  status.textContent = `lensing supported: ${LiquidGlassElement.lensingSupported}\n` + lines.join('  ');
}
setInterval(report, 500); report();

for (const id of ['back', 'share', 'edit', 'details', 'primary'])
  document.getElementById(id)?.addEventListener('click', () => { status.dataset.clicked = id; });
