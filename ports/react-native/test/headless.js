/** Apache-2.0. React Native Skia's headless build (its JSI API over CanvasKit), bound as an app binds it. */
import {createRequire} from 'node:module';
import React from 'react';
import CanvasKitInit from 'canvaskit-wasm';
import {bindLiquidGlass} from '../core.js';

const require = createRequire(import.meta.url);
globalThis.CanvasKit ??= await CanvasKitInit();
export const CanvasKit = globalThis.CanvasKit;
export const headless = require('@shopify/react-native-skia/lib/commonjs/headless');
export const {Skia} = headless.getSkiaExports();
const {TileMode, FilterMode, MipmapMode, BlendMode, ClipOp, Picture, Group} = headless;
export const glass = bindLiquidGlass({Skia, TileMode, FilterMode, MipmapMode, BlendMode, ClipOp, Picture, Group, React});

/** A 480 x 320 opaque test page: diagonal colour bands, a dark block and fine lines (RGBA bytes). */
export function pagePixels(width = 480, height = 320) {
  const pixels = new Uint8Array(width * height * 4);
  const bands = [[227, 93, 106], [76, 141, 246], [60, 196, 124], [242, 179, 61], [155, 108, 246]];
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    let c = bands[Math.floor((x + y) / 34) % bands.length];
    if (x > 300 && y > 180) c = [20, 24, 32];
    if (y % 40 < 2) c = [250, 250, 250];
    pixels.set([...c, 255], (y * width + x) * 4);
  }
  return {width, height, pixels};
}

/** The page as a React Native Skia image. */
export function pageImage(page = pagePixels()) {
  const data = Skia.Data.fromBytes(page.pixels);
  return Skia.Image.MakeImage({width: page.width, height: page.height, colorType: headless.ColorType.RGBA_8888,
    alphaType: headless.AlphaType.Premul}, data, page.width * 4);
}

/** The page as a CanvasKit image (the reference path, no adapter). */
export function canvasKitPage(page = pagePixels()) {
  return CanvasKit.MakeImage({width: page.width, height: page.height, colorType: CanvasKit.ColorType.RGBA_8888,
    alphaType: CanvasKit.AlphaType.Premul, colorSpace: CanvasKit.ColorSpace.SRGB}, page.pixels, page.width * 4);
}

/** RGBA bytes of an image of either kind. */
export function pixelsOf(image) {
  const width = image.width(), height = image.height();
  const native = typeof image.dispose === 'function';
  const info = native
    ? {width, height, colorType: headless.ColorType.RGBA_8888, alphaType: headless.AlphaType.Premul}
    : {width, height, colorType: CanvasKit.ColorType.RGBA_8888, alphaType: CanvasKit.AlphaType.Premul, colorSpace: CanvasKit.ColorSpace.SRGB};
  return image.readPixels(0, 0, info);
}
