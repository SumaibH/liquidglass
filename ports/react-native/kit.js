/** Apache-2.0. The slice of the CanvasKit API the production painter uses, over React Native Skia.
 *
 * The painter in ports/skia (vendored here) is written against CanvasKit. React Native Skia's `Skia`
 * object is nearly the same API, with three differences this adapter absorbs, so the painter and its
 * shaders run unchanged:
 *
 * - objects are released with `dispose()`, not `delete()`;
 * - enums are numbers exported beside `Skia`, matrices are `SkMatrix` objects, and a few factories
 *   (`Paint`, `Color4f`, `Matrix.translated`) have other names;
 * - a canvas cannot make a surface; offscreen surfaces come from `Skia.Surface`.
 *
 * Objects cross the adapter wrapped in a Proxy whose `delete()` calls `dispose()`. Every argument is
 * unwrapped before it reaches React Native Skia, so native (JSI) objects only ever see their own kind.
 */

/**
 * @param {object} Skia React Native Skia's `Skia` object.
 * @param {{TileMode: object, FilterMode: object, MipmapMode: object, BlendMode: object, ClipOp: object}} enums
 *   the enums React Native Skia exports beside it.
 */
export function createSkiaKit(Skia, {TileMode, FilterMode, MipmapMode, BlendMode, ClipOp}) {
  const targets = new WeakMap();
  const unwrap = value => {
    if (Array.isArray(value)) return value.map(unwrap);
    if (value !== null && typeof value === 'object') return targets.get(value) ?? value;
    return value;
  };
  const handler = {
    get(target, key) {
      if (key === 'delete') return () => target.dispose?.();
      // The tone-buffer cache compares canvases; two wrappers of one canvas are the same canvas.
      if (key === 'isAliasOf') return other => unwrap(other) === target;
      if (key === 'makeSurface' && typeof target.makeSurface !== 'function') return makeSurface;
      const value = target[key];
      if (typeof value !== 'function') return value;
      return (...args) => {
        const plain = args.map(unwrap);
        // Native RuntimeEffect bindings take uniforms as a plain array.
        if (key === 'makeShaderWithChildren' || key === 'makeShader')
          plain[0] = ArrayBuffer.isView(plain[0]) ? Array.from(plain[0]) : plain[0];
        return wrap(value.apply(target, plain));
      };
    },
  };
  function wrap(value) {
    if (value === null || typeof value !== 'object' || ArrayBuffer.isView(value) || Array.isArray(value)) return value;
    if (typeof value.dispose !== 'function' && !('getCanvas' in value) && !('drawRect' in value)) return value;
    const proxy = new Proxy(value, handler);
    targets.set(proxy, value);
    return proxy;
  }
  /** The painter's offscreen buffers (the tone buffer, a composed backdrop): raster, on purpose. A GPU
   * surface made on the JS thread yields a texture image that a picture replayed on React Native
   * Skia's render thread cannot draw; on Android the glass then drew nothing (19 October, emulator). */
  function makeSurface({width, height}) {
    return wrap(Skia.Surface.Make(width, height));
  }
  const namespace = object => new Proxy(object, {
    get(target, key) {
      const value = target[key];
      return typeof value === 'function' ? (...args) => wrap(value.apply(target, args.map(unwrap))) : value;
    },
  });
  const color = (r, g, b, a) => Skia.Color([r, g, b, a]);

  return {
    RuntimeEffect: {
      Make(source, onError) {
        let effect = null, message = 'React Native Skia could not compile the shader';
        try { effect = Skia.RuntimeEffect.Make(source); }
        catch (error) { message = String(error?.message ?? error); }
        if (!effect) { onError?.(message); return null; }
        return wrap(effect);
      },
    },
    ImageFilter: namespace(Skia.ImageFilter),
    // `new kit.Paint()` returns the wrapped paint: a constructor that returns an object yields it.
    Paint: function Paint() { return wrap(Skia.Paint()); },
    Color4f: color,
    Color: (r, g, b, a = 1) => color(r / 255, g / 255, b / 255, a),
    BLACK: color(0, 0, 0, 1),
    TRANSPARENT: color(0, 0, 0, 0),
    XYWHRect: (x, y, width, height) => wrap(Skia.XYWHRect(x, y, width, height)),
    RRectXY: (rect, rx, ry) => wrap(Skia.RRectXY(unwrap(rect), rx, ry)),
    Matrix: {
      translated: (x, y) => wrap(Skia.Matrix([1, 0, x, 0, 1, y, 0, 0, 1])),
      scaled: (sx, sy = sx) => wrap(Skia.Matrix([sx, 0, 0, 0, sy, 0, 0, 0, 1])),
    },
    TileMode: {Clamp: TileMode.Clamp, Decal: TileMode.Decal, Repeat: TileMode.Repeat, Mirror: TileMode.Mirror},
    FilterMode: {Linear: FilterMode.Linear, Nearest: FilterMode.Nearest},
    MipmapMode: {None: MipmapMode.None, Nearest: MipmapMode.Nearest, Linear: MipmapMode.Linear},
    BlendMode: {Plus: BlendMode.Plus, SrcOver: BlendMode.SrcOver},
    ClipOp: {Difference: ClipOp.Difference, Intersect: ClipOp.Intersect},
    // Surface descriptors only: offscreen surfaces are always RGBA, premultiplied, sRGB.
    ColorType: {RGBA_8888: 'RGBA_8888'},
    AlphaType: {Premul: 'Premul'},
    ColorSpace: {SRGB: 'SRGB'},
    /** A host canvas (a `PictureRecorder`'s, a surface's) as the painter expects one. */
    canvas: canvas => {
      if (targets.has(canvas)) return canvas;
      const proxy = new Proxy(canvas, handler);
      targets.set(proxy, canvas);
      return proxy;
    },
    /** A React Native Skia object (an image, a shader) as the painter expects one. */
    wrap: value => targets.has(value) ? value : wrap(value),
    /** The React Native Skia object behind an adapter object. */
    unwrap,
  };
}
