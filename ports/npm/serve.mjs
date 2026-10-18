/** Apache-2.0. Loopback preview server for the example and the browser tests (no dependencies).
 * Serves this directory, plus the two CanvasKit runtime files the canvas engine needs. */
import {createServer} from 'node:http';
import {readFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {resolve, extname, relative, sep} from 'node:path';

const root = fileURLToPath(new URL('.', import.meta.url));
const runtime = new Set(['node_modules/canvaskit-wasm/bin/canvaskit.js', 'node_modules/canvaskit-wasm/bin/canvaskit.wasm']);
const types = {'.html': 'text/html', '.mjs': 'text/javascript', '.js': 'text/javascript', '.wasm': 'application/wasm',
  '.css': 'text/css', '.png': 'image/png', '.svg': 'image/svg+xml'};
const port = Number(process.env.PORT ?? 8767);

createServer(async (req, res) => {
  try {
    const requested = decodeURIComponent(new URL(req.url, 'http://127.0.0.1').pathname);
    const path = resolve(root, '.' + requested);
    const local = relative(root, path).split(sep).join('/');
    if ((path !== resolve(root) && !path.startsWith(resolve(root) + sep)) ||
        (local.startsWith('node_modules/') && !runtime.has(local)) || local.startsWith('test-results')) {
      res.writeHead(403); res.end(); return;
    }
    const file = path === resolve(root) ? resolve(root, 'index.html') : path;
    const type = types[extname(file)];
    if (!type) { res.writeHead(404); res.end(); return; }
    const body = await readFile(file);
    res.writeHead(200, {'Content-Type': type, 'Cache-Control': 'no-store'});
    res.end(body);
  } catch { res.writeHead(404); res.end(); }
}).listen(port, '127.0.0.1', () => console.log(`<liquid-glass> example http://127.0.0.1:${port}/`));
