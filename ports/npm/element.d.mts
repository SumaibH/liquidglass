/** Apache-2.0. Types for <liquid-glass>. */

export type GlassVariant = 'regular' | 'clear';
export type GlassShape = 'capsule' | 'rect';
export type GlassScheme = 'auto' | 'light' | 'dark';
export type GlassEngine = 'html' | 'canvas';
export type GlassLensing = 'auto' | 'off';
/** A source the canvas engine can draw: same-origin, or CORS-enabled. */
export type GlassSource = HTMLImageElement | HTMLCanvasElement | HTMLVideoElement | ImageBitmap | OffscreenCanvas;

export interface GlassRenderState {
  /** The engine drawing now (a canvas request falls back to html until it can draw). */
  engine: GlassEngine;
  /** 'shader' (canvas engine), 'lens' (SVG refraction, Chromium), 'frost' (blur), 'flat' (tint only). */
  optics: 'shader' | 'lens' | 'frost' | 'flat';
  /** Why a canvas request is drawing with html, or null. */
  reason: null | 'canvas-variant' | 'canvas-no-source' | 'canvas-loading' | 'canvas-source-pending' | 'canvas-unavailable';
}

export declare class LiquidGlassElement extends HTMLElement {
  static readonly observedAttributes: string[];
  /** Whether this browser refracts the live page (Chromium); elsewhere the HTML engine frosts. */
  static readonly lensingSupported: boolean;
  variant: GlassVariant;
  shape: GlassShape;
  /** Corner radius in CSS px for shape="rect" (default 16). */
  radius: number;
  /** Any CSS colour, as Glass.tint(color): replaces the tint; its alpha is the tint's opacity. */
  tint: string | null;
  scheme: GlassScheme;
  engine: GlassEngine;
  interactive: boolean;
  lensing: GlassLensing;
  /** What engine="canvas" refracts. */
  source: GlassSource | null;
  readonly renderState: GlassRenderState;
  /** Draw again after the pixels of a canvas source change. */
  refresh(): void;
}

export interface CanvasKitOptions {
  /** An initialized CanvasKit. */
  canvasKit?: unknown;
  /** CanvasKitInit, e.g. the default export of 'canvaskit-wasm'. */
  init?: (options?: {locateFile?: (file: string) => string}) => Promise<unknown>;
  /** Where canvaskit.wasm is served. */
  locateFile?: (file: string) => string;
  /** Any loader returning CanvasKit. */
  load?: () => Promise<unknown>;
}

/** How engine="canvas" obtains CanvasKit; call before the first canvas element renders. */
export declare function configureCanvasKit(options?: CanvasKitOptions): void;
/** Register the element (default `liquid-glass`); idempotent, a no-op outside a browser. */
export declare function define(name?: string): typeof LiquidGlassElement;

declare global {
  interface HTMLElementTagNameMap {
    'liquid-glass': LiquidGlassElement;
  }
}
