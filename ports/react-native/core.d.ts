import type {GlassSurface, GlassLens, createGlassPainter, recordGlass} from './index.js';

/** Bind the components to a React Native Skia build (`index.js` binds the app's). */
export function bindLiquidGlass(deps: {
  Skia: unknown; TileMode: object; FilterMode: object; MipmapMode: object; BlendMode: object; ClipOp: object;
  Picture: unknown; Group: unknown; React: unknown; pixelRatio?: () => number;
}): {
  kit: unknown;
  createPainter: typeof createGlassPainter;
  recordGlass: typeof recordGlass;
  GlassSurface: typeof GlassSurface;
  GlassLens: typeof GlassLens;
};
