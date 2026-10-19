// Compile-only checks of the public types (npm run test:types).
import {LiquidGlassElement, define, configureCanvasKit} from '@shayxo.dev/liquidglass';
import {define as defineOnly} from '@shayxo.dev/liquidglass/element';

const Ctor: typeof LiquidGlassElement = define();
void defineOnly('my-glass');
configureCanvasKit({locateFile: file => `/canvaskit/${file}`});
const glass = document.createElement('liquid-glass');
const typed: LiquidGlassElement = glass;
typed.variant = 'clear';
typed.shape = 'rect';
typed.radius = 24;
typed.tint = 'rgb(10 132 255 / 0.5)';
typed.engine = 'canvas';
typed.source = document.createElement('canvas');
const optics: 'shader' | 'lens' | 'frost' | 'flat' = typed.renderState.optics;
void optics; void Ctor; void LiquidGlassElement.lensingSupported;
// @ts-expect-error Only regular and clear are variants.
typed.variant = 'thick';
// @ts-expect-error The source must be drawable.
typed.source = 'image.png';
