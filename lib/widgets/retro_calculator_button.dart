import 'package:flutter/material.dart';

import '../theme/retro_theme.dart';

enum RetroKeyType {
  number,
  operator,
  action,
  memory,
  clear,
  equals,
}

class RetroCalculatorButton extends StatefulWidget {
  final String label;
  final VoidCallback onPressed;
  final RetroKeyType type;

  const RetroCalculatorButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.type = RetroKeyType.number,
  });

  @override
  State<RetroCalculatorButton> createState() =>
      _RetroCalculatorButtonState();
}

class _RetroCalculatorButtonState extends State<RetroCalculatorButton> {
  bool _pressed = false;

  List<Color> get _gradientColors {
    switch (widget.type) {
      case RetroKeyType.number:
        return const [
          RetroPalette.silverTop,
          RetroPalette.silverBottom,
        ];
      case RetroKeyType.operator:
        return const [
          RetroPalette.operatorTop,
          RetroPalette.operatorBottom,
        ];
      case RetroKeyType.memory:
        return const [
          RetroPalette.memoryKeyTop,
          RetroPalette.memoryKeyBottom,
        ];
      case RetroKeyType.equals:
        return const [
          RetroPalette.equalsTop,
          RetroPalette.equalsBottom,
        ];
      case RetroKeyType.clear:
        return const [Color(0xFF704D50), Color(0xFF3A292B)];
      case RetroKeyType.action:
        return const [
          RetroPalette.darkKeyTop,
          RetroPalette.darkKeyBottom,
        ];
    }
  }

  Color get _textColor {
    switch (widget.type) {
      case RetroKeyType.number:
        return RetroPalette.silverText;
      case RetroKeyType.equals:
        return RetroPalette.equalsText;
      case RetroKeyType.operator:
        return const Color(0xFFD8F8FC);
      case RetroKeyType.memory:
        return const Color(0xFFE8F5F6);
      case RetroKeyType.clear:
        return const Color(0xFFFFE8E8);
      case RetroKeyType.action:
        return RetroPalette.darkKeyText;
    }
  }

  Color get _borderColor {
    switch (widget.type) {
      case RetroKeyType.number:
        return const Color(0xFF71767B);
      case RetroKeyType.operator:
        return RetroPalette.cyanDark;
      case RetroKeyType.equals:
        return const Color(0xFFBCEAF0);
      case RetroKeyType.clear:
        return const Color(0xFF9B666B);
      case RetroKeyType.memory:
        return const Color(0xFF7C8A94);
      case RetroKeyType.action:
        return const Color(0xFF666D75);
    }
  }

  void _setPressed(bool value) {
    if (_pressed == value) return;
    setState(() {
      _pressed = value;
    });
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox.expand(
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: (_) => _setPressed(true),
        onTapUp: (_) => _setPressed(false),
        onTapCancel: () => _setPressed(false),
        onTap: widget.onPressed,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 70),
          curve: Curves.easeOut,
          margin: EdgeInsets.only(
            top: _pressed ? 2 : 0,
            bottom: _pressed ? 0 : 2,
          ),
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: _pressed
                  ? _gradientColors.reversed.toList()
                  : _gradientColors,
            ),
            borderRadius: BorderRadius.circular(6),
            border: Border.all(
              color: _borderColor,
              width: 1,
            ),
            boxShadow: _pressed
                ? const []
                : const [
                    BoxShadow(
                      color: Color(0xA8000000),
                      offset: Offset(0, 2),
                      blurRadius: 1.5,
                    ),
                  ],
          ),
          child: Stack(
            fit: StackFit.expand,
            children: [
              Positioned(
                top: 1,
                left: 4,
                right: 4,
                child: Container(
                  height: 1,
                  color: Colors.white.withOpacity(0.26),
                ),
              ),
              Center(
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  child: Text(
                    widget.label,
                    style: TextStyle(
                      color: _textColor,
                      fontSize: 18,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 0.25,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
