// Apache-2.0. A scrolling page under live Liquid Glass: a toolbar card and a dark bar refract the list
// as it moves. `flutter run` on an Android device or emulator (Impeller).
import 'package:flutter/material.dart';
import 'package:liquiglass/liquiglass.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  LiquidGlass.ensureInitialized();
  runApp(const MaterialApp(debugShowCheckedModeBanner: false, home: GlassDemo()));
}

const _bands = [Color(0xFFE35D6A), Color(0xFF4C8DF6), Color(0xFF3CC47C), Color(0xFFF2B33D), Color(0xFF9B6CF6)];

class GlassDemo extends StatelessWidget {
  const GlassDemo({super.key, this.scroll});
  final ScrollController? scroll;

  @override
  Widget build(BuildContext context) {
    final padding = MediaQuery.paddingOf(context);
    return Scaffold(
      backgroundColor: const Color(0xFFF6F3F8),
      body: Stack(children: [
        ListView.builder(
          controller: scroll,
          padding: EdgeInsets.only(top: padding.top + 120, bottom: 140),
          itemCount: 40,
          itemBuilder: (context, i) => Container(
            height: 92,
            margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(18),
              gradient: LinearGradient(colors: [_bands[i % 5], _bands[(i + 2) % 5]]),
            ),
            padding: const EdgeInsets.all(16),
            child: Text('Row $i — content scrolling under the glass',
                style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w600)),
          ),
        ),
        Positioned(
          left: 16,
          right: 16,
          top: padding.top + 12,
          height: 92,
          child: const GlassSurface(
            radius: 28,
            light: Offset(0, -1),
            child: Center(
              child: Text('Liquid Glass for Flutter',
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: Color(0xFF111111))),
            ),
          ),
        ),
        Positioned(
          left: 24,
          right: 24,
          bottom: padding.bottom + 20,
          height: 72,
          child: const GlassSurface(
            radius: 36,
            dark: true,
            child: Row(mainAxisAlignment: MainAxisAlignment.spaceEvenly, children: [
              Icon(Icons.home_rounded, color: Colors.white, size: 30),
              Icon(Icons.search_rounded, color: Colors.white, size: 30),
              Icon(Icons.person_rounded, color: Colors.white, size: 30),
            ]),
          ),
        ),
      ]),
    );
  }
}
