// Checks the packed NuGet packages (CI, after `dotnet pack`): each has its readme, NOTICE, licence
// expression, the library's version and its assembly; LiquidGlass embeds the production shaders.
// Usage: node ports/dotnet/check-packages.mjs <directory with .nupkg files> [--winforms] [--wpf]
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { inflateRawSync } from 'node:zlib';

const [dir, ...flags] = process.argv.slice(2);
const version = /VERSION_NAME=([^\r\n]+)/.exec(readFileSync(new URL('../../gradle.properties', import.meta.url), 'utf8'))[1].trim();

/** Minimal zip reader: name -> Buffer, from the central directory. */
function unzip(buf) {
  const files = new Map();
  let eocd = buf.length - 22;
  while (buf.readUInt32LE(eocd) !== 0x06054b50) eocd--;
  let p = buf.readUInt32LE(eocd + 16);
  const count = buf.readUInt16LE(eocd + 10);
  for (let i = 0; i < count; i++) {
    const method = buf.readUInt16LE(p + 10), size = buf.readUInt32LE(p + 20);
    const nameLen = buf.readUInt16LE(p + 28), extra = buf.readUInt16LE(p + 30), comment = buf.readUInt16LE(p + 32);
    const local = buf.readUInt32LE(p + 42);
    const name = buf.toString('utf8', p + 46, p + 46 + nameLen);
    const start = local + 30 + buf.readUInt16LE(local + 26) + buf.readUInt16LE(local + 28);
    const raw = buf.subarray(start, start + size);
    files.set(name, method === 8 ? inflateRawSync(raw) : raw);
    p += 46 + nameLen + extra + comment;
  }
  return files;
}

const expected = [['LiquidGlass', 'lib/net8.0/LiquidGlass.Skia.dll']];
if (flags.includes('--winforms')) expected.push(['LiquidGlass.WinForms', 'lib/net10.0-windows10.0.19041/LiquidGlass.WinForms.dll']);
if (flags.includes('--wpf')) expected.push(['LiquidGlass.Wpf', 'lib/net10.0-windows10.0.19041/LiquidGlass.Wpf.dll']);
let failures = 0;
const fail = (m) => { console.error('FAIL ' + m); failures++; };
for (const [id, dll] of expected) {
  const file = `${id}.${version}.nupkg`;
  if (!readdirSync(dir).includes(file)) { fail(`${file} missing in ${dir}`); continue; }
  const zip = unzip(readFileSync(join(dir, file)));
  for (const name of ['PACKAGE.md', 'NOTICE', dll]) if (!zip.has(name)) fail(`${file}: no ${name}`);
  const nuspec = zip.get(`${id}.nuspec`)?.toString('utf8') ?? '';
  if (!nuspec.includes('<license type="expression">Apache-2.0</license>')) fail(`${file}: licence expression`);
  if (!nuspec.includes(`<version>${version}</version>`)) fail(`${file}: version is not ${version}`);
  if (!nuspec.includes('<repository type="git" url="https://github.com/shayann07/liquidglass"')) fail(`${file}: repository`);
  if (id === 'LiquidGlass') {
    const assembly = zip.get(dll) ?? Buffer.alloc(0);
    for (const s of ['LiquidGlass.Shaders.', 'LiquidGlass.SelectorMaterial.json'])
      if (!assembly.includes(Buffer.from(s))) fail(`${file}: assembly does not embed ${s}`);
  }
  console.log(`${failures ? 'checked' : 'ok'} ${file} (${zip.size} entries)`);
}
process.exit(failures ? 1 : 0);
