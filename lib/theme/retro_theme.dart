import 'package:flutter/material.dart';

class RetroPalette {
  const RetroPalette._();

  static const Color background = Color(0xFF171A1F);
  static const Color shellTop = Color(0xFF4A5057);
  static const Color shellBottom = Color(0xFF24282D);
  static const Color panel = Color(0xFF2E3339);
  static const Color panelDark = Color(0xFF1F2328);

  static const Color lcdTop = Color(0xFFF2F6F3);
  static const Color lcdBottom = Color(0xFFC9D8D1);
  static const Color lcdBorder = Color(0xFF7F908A);
  static const Color lcdText = Color(0xFF17211D);
  static const Color lcdMuted = Color(0xFF52635C);

  static const Color cyan = Color(0xFF73C5D2);
  static const Color cyanDark = Color(0xFF327C89);
  static const Color red = Color(0xFFC95A5A);
  static const Color green = Color(0xFF70A86F);

  static const Color silverTop = Color(0xFFF4F5F4);
  static const Color silverBottom = Color(0xFFABB0B4);
  static const Color silverText = Color(0xFF1A1C20);

  static const Color darkKeyTop = Color(0xFF50565D);
  static const Color darkKeyBottom = Color(0xFF23272C);
  static const Color darkKeyText = Color(0xFFF6F7F7);

  static const Color memoryKeyTop = Color(0xFF66717A);
  static const Color memoryKeyBottom = Color(0xFF343A40);

  static const Color operatorTop = Color(0xFF465B66);
  static const Color operatorBottom = Color(0xFF22333A);

  static const Color equalsTop = Color(0xFF9AD9E1);
  static const Color equalsBottom = Color(0xFF4A9DAA);
  static const Color equalsText = Color(0xFF102327);

  static const LinearGradient shellGradient = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [shellTop, shellBottom],
  );

  static const LinearGradient bodyGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF30353B), Color(0xFF171A1F)],
  );

  static const LinearGradient lcdGradient = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [lcdTop, lcdBottom],
  );
}

class RetroTheme {
  const RetroTheme._();

  static ThemeData get data {
    return ThemeData(
      useMaterial3: false,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: RetroPalette.background,
      colorScheme: const ColorScheme.dark(
        primary: RetroPalette.cyan,
        secondary: RetroPalette.green,
        surface: RetroPalette.panel,
        error: RetroPalette.red,
      ),
      splashFactory: NoSplash.splashFactory,
      highlightColor: Colors.transparent,
      visualDensity: VisualDensity.compact,
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: Colors.transparent,
      ),
    );
  }
}
