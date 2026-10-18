/** Read the Kotlin library's sources from the repository, for drift checks. A published copy of
 * this package has no Kotlin beside it; tests that need it are skipped there. */
import {readFile} from 'node:fs/promises';
import {KOTLIN_SOURCES} from '../material.mjs';

const root = new URL('../../../', import.meta.url);
export async function kotlin(key) {
  try { return (await readFile(new URL(KOTLIN_SOURCES[key], root), 'utf8')).replace(/\r\n/g, '\n'); }
  catch (error) { if (error.code === 'ENOENT') return null; throw error; }
}
export const haveKotlin = (await kotlin('material')) !== null;

/** A Kotlin literal (35f, 1.33.dp, 0.95f, 35f / 255f) as a number. */
export function literal(text) {
  const t = text.trim().replace(/\.dp$/, '').replace(/(\d)f\b/g, '$1');
  if (!/^[-0-9.\s/*+()]+$/.test(t)) throw new Error(`Not a numeric literal: ${text}`);
  return Function(`"use strict"; return (${t});`)();
}

/** The text between `start` and the next `end`. */
export function block(source, start, end) {
  const i = source.indexOf(start);
  if (i < 0) throw new Error(`Missing ${start}`);
  const j = source.indexOf(end, i + start.length);
  if (j < 0) throw new Error(`Missing ${end} after ${start}`);
  return source.slice(i, j);
}

/** `name = value,` pairs of a named-argument call, numeric values only. */
export function namedArgs(text) {
  const out = {};
  for (const m of text.matchAll(/^\s*(\w+)\s*=\s*(-?[0-9.]+(?:\.dp|f)?)\s*,/gm)) out[m[1]] = literal(m[2]);
  return out;
}

/** A floatArrayOf(...) assigned to `name`. */
export function floatArray(source, name) {
  const m = new RegExp(`${name}\\s*=\\s*floatArrayOf\\(([^)]*)\\)`).exec(source);
  if (!m) throw new Error(`Missing ${name}`);
  return m[1].split(',').map(literal);
}

/** One SkSL function, by brace matching, transpiled to a JavaScript function of the same name.
 * Handles the scalar subset these functions use (float locals, if/else, pow/clamp/min/max/sqrt). */
export function skslFunction(source, name) {
  const start = source.search(new RegExp(`float ${name}\\(`));
  if (start < 0) throw new Error(`Missing SkSL ${name}`);
  let depth = 0, i = source.indexOf('{', start);
  const open = i;
  for (; i < source.length; i++) {
    if (source[i] === '{') depth++;
    else if (source[i] === '}' && --depth === 0) break;
  }
  const signature = source.slice(start, open);
  const params = /\(([^)]*)\)/.exec(signature)[1].split(',').map(p => p.trim().split(/\s+/).pop());
  const body = source.slice(open + 1, i)
    .replace(/\/\/.*$/gm, '')
    .replace(/\bfloat\s+(\w+)\s*=/g, 'let $1 =');
  const helpers = {
    pow: Math.pow, sqrt: Math.sqrt, min: Math.min, max: Math.max, abs: Math.abs,
    clamp: (x, a, b) => Math.min(b, Math.max(a, x)), inversesqrt: x => 1 / Math.sqrt(x),
  };
  const fn = Function(...Object.keys(helpers), `return function ${name}(${params.join(', ')}) {${body}};`);
  return fn(...Object.values(helpers));
}
