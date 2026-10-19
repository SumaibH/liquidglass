// Compile-only checks of the published types (npm run test:types).
import type {SkImage} from '@shopify/react-native-skia';
import {GlassSurface, GlassLens, createGlassPainter, recordGlass} from './index.js';

declare const backdrop: SkImage;
const surface = <GlassSurface source={backdrop} x={16} y={600} width={360} height={84} radius={28} dark light={[0, -1]} />;
const lens = <GlassLens source={backdrop} x={40} y={40} width={120} magnification={1.5} />;
const painter = createGlassPainter();
const picture = recordGlass(painter, backdrop, (p, canvas) => {
  p.drawSurface(canvas, {x: 0, y: 0, width: 100, height: 40, radius: [20, 20, 8, 8]}, {dark: true, density: 3});
  p.drawLens(canvas, {x: 0, y: 0, width: 80});
});
// @ts-expect-error a surface needs its size
const broken = <GlassSurface source={backdrop} x={0} y={0} />;
void [surface, lens, picture, broken];
