// Apache-2.0. The Dart material uniforms equal ports/skia's (test/material_fixture.json, generated from
// ports/skia/material.mjs), and the widget lays out and paints where shader filters are unavailable.
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:liquidglass/liquid_glass.dart';

void main() {
  final fixture = jsonDecode(File('test/material_fixture.json').readAsStringSync()) as List<dynamic>;

  // First: the load must start outside a widget test's fake clock, or it never completes.
  testWidgets('the generated shader compiles and loads', (tester) async {
    await tester.runAsync(LiquidGlass.ensureInitialized);
    expect(LiquidGlass.loadError, isNull);
  });

  for (final (i, entry) in fixture.indexed) {
    test('case $i matches the CanvasKit port\'s material', () {
      final input = entry['input'] as Map<String, dynamic>;
      double n(String key, double fallback) => (input[key] as num?)?.toDouble() ?? fallback;
      final light = (input['light'] as List).cast<num>();
      final tint = (input['tint'] as List?)?.cast<num>();
      final density = n('density', 1);
      final material = GlassMaterial(
        radius: n('radius', 20),
        dark: input['dark'] as bool,
        tintAmount: n('tintAmount', 50),
        light: Offset(light[0].toDouble(), light[1].toDouble()),
        tint: tint == null
            ? null
            : Color.from(alpha: tint[3].toDouble(), red: tint[0].toDouble(), green: tint[1].toDouble(), blue: tint[2].toDouble()),
        materialize: n('materialize', 1),
        reducedTransparency: n('reducedTransparency', 0),
        increasedContrast: n('increasedContrast', 0),
      );
      final ours = glassMaterialUniforms(material, Size(n('width', 0) * density, n('height', 0) * density), density);
      final theirs = (entry['uniforms'] as Map<String, dynamic>).map((k, v) => MapEntry(k, (v as List).cast<num>()));
      // Flutter-only: the backdrop is the live texture (unbounded here) and the wide tone is always on.
      const flutterOnly = {'uBackdrop', 'uWideStrip'};
      expect(ours.keys.toSet(), theirs.keys.toSet());
      for (final name in theirs.keys.where((k) => !flutterOnly.contains(k))) {
        final a = ours[name]!, b = theirs[name]!;
        expect(a.length, b.length, reason: name);
        for (var j = 0; j < a.length; j++) {
          expect(a[j], closeTo(b[j].toDouble(), 1e-6), reason: '$name[$j]');
        }
      }
    });
  }

  testWidgets('GlassSurface lays out its child and paints without Impeller', (tester) async {
    await tester.pumpWidget(const Directionality(
      textDirection: TextDirection.ltr,
      child: Center(
        child: SizedBox(width: 200, height: 80, child: GlassSurface(radius: 24, child: Text('On glass'))),
      ),
    ));
    expect(find.text('On glass'), findsOneWidget);
    expect(tester.getSize(find.byType(GlassSurface)), const Size(200, 80));
    expect(tester.takeException(), isNull);
  });

}
