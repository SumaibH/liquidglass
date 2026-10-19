// Apache-2.0. The in-app material's uniforms: a line-for-line port of ports/skia's
// clearMaterialUniforms (renderer.mjs) and inAppMaterialUniforms (material.mjs), which carry
// Compose's GlassStyle.inApp. Lengths are device pixels; the density converts the dp constants.
part of '../liquiglass.dart';

const List<(double, double)> _lightOpacity = [(0, .435), (31, .514), (50, .565), (62, .600), (100, .733)];
const List<(double, double)> _darkOpacity = [(0, .416), (29, .439), (50, .451), (62, .510), (86, .655), (100, .737)];

double _interpolate(List<(double, double)> table, double t) {
  var i = 0;
  while (i < table.length - 2 && table[i + 1].$1 <= t) {
    i++;
  }
  final (x, a) = table[i];
  final (y, b) = table[i + 1];
  return a + (b - a) * (t - x) / (y - x);
}

/// Every material uniform for a surface of [size] device pixels at [density] device pixels per
/// logical pixel, keyed by its name in liquid_glass.frag.
Map<String, List<double>> glassMaterialUniforms(GlassMaterial m, Size size, double density) {
  if (m.tintAmount < 0 || m.tintAmount > 100) throw RangeError.range(m.tintAmount, 0, 100, 'tintAmount');
  const pad = 2.0;
  final width = size.width, height = size.height;
  final values = <String, List<double>>{
    for (final (name, length) in _materialUniforms) name: List<double>.filled(length, 0),
  };
  void set(String name, List<double> v) {
    if (!values.containsKey(name)) throw StateError('liquidglass: no uniform $name in the shader');
    values[name] = v;
  }

  // clearMaterialUniforms (magnification 1).
  set('uSize', [width, height]);
  set('uPad', [pad]);
  // The backdrop texture is sampled clamped at its edges, so the backdrop region is unbounded here.
  set('uBackdrop', [-1e5, -1e5, 1e5, 1e5]);
  set('uRefractBand', [24]);
  set('uRefractDepth', [12]);
  set('uIor', [1.5]);
  set('uBevelPower', [2]);
  set('uCornerPower', [2]);
  set('uFieldRange', [1]);
  set('uFieldScale', [1]);
  set('uBevel', [2]);
  set('uSpecularPow', [4]);
  set('uCounterLight', [1]);
  set('uEdgeLight', [1]);
  set('uTint', [1, 1, 1, 0]);
  set('uFineShare', [1]);
  set('uWideScale', [.25]);
  set('uTouch', [width / 2, height / 2]);
  set('uPoseA', [1, 0, 0, 1]);
  set('uPoseAInv', [1, 0, 0, 1]);
  set('uPoseD', [1, 1, 1, 0]);

  // inAppMaterialUniforms.
  final t = m.tintAmount / 100;
  final limit = math.min(width, height) / 2;
  final r = math.min(m.radius * density, limit);
  final grey = m.dark ? 0.0 : 241 / 255;
  final opacity = (_interpolate(m.dark ? _darkOpacity : _lightOpacity, m.tintAmount) * 255).roundToDouble() / 255;
  set('uRadii', [r, r, r, r]);
  set('uRefractBand', [.6 * r]);
  set('uRefractDepth', [14 * density]);
  set('uProfile', [1]);
  set('uFormation', [0]);
  set('uHeldLens', [0]);
  set('uHeldMagnification', [0]);
  set('uHeldGlow', [1]);
  set('uAberration', [.07]);
  set('uBlur', [(.5 + 1.5 * t * t) / .6 * density]);
  set('uWideKernel', [10 * density]);
  set('uFineShare', [.95 * math.pow(1 - t, 1.5).toDouble()]);
  set('uTint', [grey, grey, grey, opacity]);
  set('uLift', [m.dark ? 35 / 255 : 0]);
  set('uBevel', [1.33 * density]);
  set('uBevelPeak', [.25]);
  set('uSpecular', [.25]);
  set('uSpecularPow', [6]);
  set('uCounterLight', [1]);
  set('uEdgeLight', [0]);
  set('uLight', [m.light.dx, m.light.dy]);
  set('uLegibility', [.6]);
  set('uScale', [((math.min(width, height) / density - 56) / 264).clamp(0.0, 1.0)]);
  set('uMaterialize', [m.materialize.clamp(0.0, 1.0)]);
  set('uFrost', [m.reducedTransparency.clamp(0.0, 1.0)]);
  set('uContrast', [m.increasedContrast.clamp(0.0, 1.0)]);
  final tint = m.tint;
  if (tint != null) {
    double q(double c) => (c * 255).roundToDouble() / 255;
    set('uTint', [q(tint.r), q(tint.g), q(tint.b), q(tint.a)]);
  }
  // Flutter reads the wide tone from the backdrop directly (see generate_shader.mjs): switch it on.
  set('uWideStrip', [1]);
  return values;
}
