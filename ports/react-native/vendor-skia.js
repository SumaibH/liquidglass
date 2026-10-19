/** Apache-2.0. Copy the CanvasKit painter from ports/skia so this package needs no sibling at runtime.
 *
 * The files are copied byte for byte (line endings normalized) into vendor/skia/, renamed from .mjs to
 * .js with their relative imports rewritten to match: Metro, React Native's bundler, resolves `.js`
 * everywhere and `.mjs` only when an app configures it. `node vendor-skia.js --check` (part of
 * `npm test`) fails when a copy drifts from its source; run `npm run vendor` after changing ports/skia.
 */
import {mkdir, readFile, writeFile, readdir} from 'node:fs/promises';

// painter.mjs and everything it imports.
export const VENDORED = [
  'painter.mjs', 'renderer.mjs', 'material.mjs', 'backdrop.mjs', 'shaders.mjs',
  'selector-material.mjs', 'selector-material-data.mjs', 'LICENSE', 'NOTICE',
];
const target = name => name.replace(/\.mjs$/, '.js');
const from = name => new URL(`../skia/${name}`, import.meta.url);
const to = name => new URL(`vendor/skia/${target(name)}`, import.meta.url);
const transform = (name, text) => {
  const normalized = text.replace(/\r\n/g, '\n');
  return name.endsWith('.mjs') ? normalized.replace(/(from\s+'\.\/[\w-]+)\.mjs'/g, "$1.js'") : normalized;
};

const check = process.argv.includes('--check');
const stale = [];
let sourceMissing = false;
for (const name of VENDORED) {
  let source;
  try { source = transform(name, await readFile(from(name), 'utf8')); }
  catch (error) { if (error.code !== 'ENOENT') throw error; sourceMissing = true; continue; }
  if (check) {
    const actual = await readFile(to(name), 'utf8').catch(error => { if (error.code === 'ENOENT') return null; throw error; });
    if (actual === null || actual.replace(/\r\n/g, '\n') !== source) stale.push(name);
  } else {
    await mkdir(new URL('vendor/skia/', import.meta.url), {recursive: true});
    await writeFile(to(name), source);
  }
}
if (sourceMissing) {
  console.log('ports/skia is not beside this package; vendored files left as they are.');
} else if (check) {
  const present = await readdir(new URL('vendor/skia/', import.meta.url)).catch(() => []);
  const extra = present.filter(name => !VENDORED.map(target).includes(name));
  if (stale.length || extra.length)
    throw new Error(`vendor/skia is stale (${[...stale, ...extra].join(', ')}). Run npm run vendor in ports/react-native.`);
  console.log(`vendor/skia matches ports/skia (${VENDORED.length} files).`);
} else {
  console.log(`Copied ${VENDORED.length} files from ports/skia into vendor/skia.`);
}
