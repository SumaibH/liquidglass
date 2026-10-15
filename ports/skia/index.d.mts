import type {Canvas, CanvasKit, Image} from 'canvaskit-wasm';
import type {CalmSelectorPose, CalmSelectorState} from './selector.mjs';
export type {CalmSelectorPose, CalmSelectorState, CalmSelectorBar, CalmNavigation} from './selector.mjs';
export {CalmSelectorController, calmSelectorBar, createCalmNavigation} from './selector.mjs';
/** The Compose Calm selector material as shader uniforms (lengths in dp) and its band blend. */
export function calmSelectorMaterial(state?: {dark?: boolean; formation?: number; transit?: number; heldTravel?: number}):
  {values: Readonly<Record<string, number | readonly number[]>>; uFormation: number};
/** The Compose Calm bar material (lengths in dp), its held lift following `formation` (or `touch`, for any touch). */
export function calmBarMaterial(state?: {dark?: boolean; formation?: number; touch?: number}): Readonly<Record<string, number | readonly number[]>>;
export interface CalmSelectorDrawOptions {
  dark?: boolean; density?: number; materialize?: number; reducedTransparency?: number; increasedContrast?: number;
  /** The source-sized backdrop with the bar's glass drawn over it: the lens looks through the bar. */
  through?: Image | null;
}

export interface GlassShaderSources { material: string; content: string; endpoint: string }
export type CornerRadii = number | readonly [number, number, number, number];
export interface GlassRect { x: number; y: number; width: number; height: number }
export interface GlassSurfaceBounds extends GlassRect { radius?: CornerRadii }
export interface GlassLensBounds { x: number; y: number; width: number; height?: number; magnification?: number }
export interface GlassMaterial {
  dark?: boolean; tintAmount?: number; density?: number; materialize?: number;
  reducedTransparency?: number; increasedContrast?: number;
}
export interface GlassFeedback {
  matrix: [number, number, number, number]; offset: [number, number];
  press: number; held: boolean; active: boolean;
}
export interface CalmGeometry {
  width: number; height: number; radius?: number; x?: number; y?: number;
  viewportWidth?: number; viewportHeight?: number;
}
export interface CalmOptions extends CalmGeometry {
  reducedMotion?: boolean; pressScale?: number; pressGrowth?: number;
  pullShape?: 'adaptive' | 'area-preserving';
}
export interface CalmInteraction {
  /** Monotonic seconds and cumulative logical displacement from pointer-down. */
  press(time: number, dx?: number, dy?: number): GlassFeedback;
  release(time: number): GlassFeedback;
  sample(time: number): GlassFeedback;
  resize(geometry: Partial<CalmGeometry>, time: number): GlassFeedback;
  setReducedMotion(reduced: boolean, time: number): GlassFeedback;
}
export function createCalmInteraction(options: CalmOptions): CalmInteraction;

export interface GlassPainter {
  /** Borrows an opaque source. Replace it before deleting the previous image. */
  setSource(image: Image): void;
  /** Low-level painter coordinates are device pixels. */
  drawLens(canvas: Canvas, bounds: GlassLensBounds): void;
  drawSurface(canvas: Canvas, bounds: GlassSurfaceBounds, material?: GlassMaterial): void;
  /** A Calm navigation bar's glass in the Compose Calm bar material (device px). */
  drawCalmBar(canvas: Canvas, bar: GlassRect, options?: Omit<CalmSelectorDrawOptions, 'through'> & {formation?: number; touch?: number}): void;
  /** A navigation selector's pose body over the bar rect (device px), in the Compose Calm selector material. */
  drawSelector(canvas: Canvas, bar: GlassRect,
    selector: {pose: CalmSelectorPose; formation?: number; transit?: number; heldTravel?: number}, options?: CalmSelectorDrawOptions): void;
  /** The selected ink inside the selector (Compose's content pass): `ink` is the bar's items in their
   * selected appearance, transparent ground, device px, origin at the bar's top-left. */
  drawSelectorInk(canvas: Canvas, bar: GlassRect,
    selector: {pose: CalmSelectorPose; formation?: number; transit?: number; heldTravel?: number}, ink: Image,
    options?: Omit<CalmSelectorDrawOptions, 'through'>): void;
  dispose(): void;
}
export function createGlassPainter(kit: CanvasKit, sources?: GlassShaderSources): GlassPainter;

export interface GlassLayer<Bounds> {
  readonly bounds: Readonly<Bounds>;
  readonly feedback: GlassFeedback;
  press(time: number, dx?: number, dy?: number): GlassFeedback;
  release(time: number): GlassFeedback;
  /** Host-owned layout changes cancel the current gesture. */
  setBounds(bounds: Partial<Bounds>, time: number): void;
  remove(): void;
}
export interface GlassSurfaceLayer extends GlassLayer<GlassSurfaceBounds> {
  setMaterial(material: Partial<GlassMaterial>): void;
}
export interface GlassLensLayer extends GlassLayer<GlassRect & {magnification: number}> {}
export interface GlassNavigationOptions {
  count: number; selected?: number; inset?: number; pillWidth?: number; pillInset?: number;
  onSelect?: (index: number) => void; material?: GlassMaterial;
}
/** A Calm navigation bar: logical page coordinates and monotonic seconds. */
export interface GlassNavigationLayer {
  readonly bounds: Readonly<GlassRect>;
  readonly selected: number;
  readonly selector: CalmSelectorState;
  down(x: number, y: number, time: number): void;
  move(x: number, y: number, time: number): void;
  up(x: number, y: number, time: number): void;
  cancel(time: number): void;
  select(index: number): void;
  /** The bar's items in their selected appearance (transparent ground, device px, bar origin), shown through the selector. */
  setSelectedInk(image: Image | null): void;
  remove(): void;
}
export interface GlassViewport { width: number; height: number; pixelRatio?: number }
export interface GlassSceneOptions extends GlassViewport { reducedMotion?: boolean; sources?: GlassShaderSources }
export interface GlassScene {
  setSource(image: Image): void;
  /** Scene bounds and input displacements are logical pixels. */
  addSurface(bounds: GlassSurfaceBounds, material?: GlassMaterial): GlassSurfaceLayer;
  addLens(bounds: GlassLensBounds): GlassLensLayer;
  addNavigation(bounds: GlassRect, options: GlassNavigationOptions): GlassNavigationLayer;
  resize(viewport: Partial<GlassViewport>, time: number): void;
  setReducedMotion(reduced: boolean, time: number): void;
  /** True requests another animation frame. Draw labels after this, then flush the host surface. */
  draw(canvas: Canvas, time: number, options?: {drawBackdrop?: boolean}): boolean;
  dispose(): void;
}
export function createGlassScene(kit: CanvasKit, options: GlassSceneOptions): GlassScene;
