import 'package:flutter/material.dart';

enum CalculatorButtonKind { number, operator, action, equals, memory }

class CalculatorButton extends StatelessWidget {
  const CalculatorButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.kind = CalculatorButtonKind.number,
    this.flex = 1,
  });

  final String label;
  final VoidCallback onPressed;
  final CalculatorButtonKind kind;
  final int flex;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final Color background;
    final Color foreground;

    switch (kind) {
      case CalculatorButtonKind.equals:
        background = scheme.primary;
        foreground = scheme.onPrimary;
      case CalculatorButtonKind.operator:
        background = scheme.secondaryContainer;
        foreground = scheme.onSecondaryContainer;
      case CalculatorButtonKind.action:
        background = scheme.tertiaryContainer;
        foreground = scheme.onTertiaryContainer;
      case CalculatorButtonKind.memory:
        background = scheme.surfaceContainerHighest;
        foreground = scheme.onSurface;
      case CalculatorButtonKind.number:
        background = scheme.surfaceContainerHigh;
        foreground = scheme.onSurface;
    }

    return Expanded(
      flex: flex,
      child: Padding(
        padding: const EdgeInsets.all(4),
        child: SizedBox.expand(
          child: FilledButton(
            onPressed: onPressed,
            style: FilledButton.styleFrom(
              backgroundColor: background,
              foregroundColor: foreground,
              minimumSize: Size.zero,
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              padding: const EdgeInsets.symmetric(horizontal: 4),
              textStyle: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.w600,
              ),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: Text(label),
            ),
          ),
        ),
      ),
    );
  }
}
