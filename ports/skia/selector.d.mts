/** The Calm navigation selector's motion and geometry, ported from the Compose library. */
export interface CalmSelectorBar {
  readonly width: number; readonly height: number; readonly inset: number; readonly count: number;
  readonly cornerRadius: number; readonly baseHalfWidth: number; readonly baseHalfHeight: number;
  readonly allowedHalfHeight: number; readonly density: number; readonly slotWidth: number;
}
export interface CalmSelectorBarOptions {
  width: number; height: number; count: number; inset?: number; pillWidth?: number;
  pillInset?: number; density?: number;
}
/** Shader pose uniforms: A, A^-1, (cx, cy, bx, by) bar-local, (spine, radius, scaleX, band). */
export interface CalmSelectorPose {
  a: [number, number, number, number]; aInv: [number, number, number, number];
  c: [number, number, number, number]; d: [number, number, number, number];
}
export interface CalmSelectorExtents { left: number; right: number; top: number; bottom: number; width: number; height: number }
export interface CalmSelectorState {
  centreX: number; centreY: number; extents: CalmSelectorExtents; formation: number;
  transitAppearance: number; heldTravel: number; held: boolean; protrusion: number; pose: CalmSelectorPose;
}
export type CalmSelectorSpec = Readonly<Record<string, number | boolean>>;
export const CALM_SELECTOR_SPEC: CalmSelectorSpec;
export function calmSelectorBar(options: CalmSelectorBarOptions): CalmSelectorBar;
export class CalmSelectorController {
  constructor(spec?: Partial<CalmSelectorSpec>);
  motionEnabled: boolean;
  readonly mode: 'rest' | 'tap' | 'pending' | 'held' | 'released';
  readonly formation: number; readonly transitAppearance: number; readonly heldTravel: number;
  readonly isHeld: boolean; readonly isIdle: boolean; readonly centreX: number; readonly centreY: number;
  attach(bar: CalmSelectorBar, itemBounds?: ArrayLike<number> | null, itemCentres?: ArrayLike<number> | null, inkHalfHeight?: number): void;
  snapToRest(index: number): void;
  retarget(index: number): void;
  pointerDown(x: number, y: number, seconds: number, eligible: boolean): void;
  pointerMove(x: number, y: number, seconds: number): void;
  beginDrag(x: number, y: number): void;
  pointerUp(restIndex: number): void;
  cancel(restIndex: number): void;
  advanceFrameTo(seconds: number): void;
  extents(): CalmSelectorExtents;
  protrusion(): number;
  render(originX?: number, originY?: number): CalmSelectorPose;
}
export interface CalmNavigationOptions {
  bar: CalmSelectorBar; selected?: number; onSelect?: (index: number) => void;
  spec?: Partial<CalmSelectorSpec>; touchSlop?: number; flingVelocity?: number; motion?: boolean;
}
export interface CalmNavigation {
  readonly controller: CalmSelectorController;
  readonly selected: number;
  select(index: number): void;
  setMotion(enabled: boolean): void;
  /** Bar-local px and monotonic seconds. */
  down(x: number, y: number, seconds: number): void;
  move(x: number, y: number, seconds: number): void;
  up(x: number, y: number, seconds: number): void;
  cancel(seconds: number): void;
  /** True while another frame is needed. */
  frame(seconds: number): boolean;
  state(originX?: number, originY?: number): CalmSelectorState;
}
export function createCalmNavigation(options: CalmNavigationOptions): CalmNavigation;
