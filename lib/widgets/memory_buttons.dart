import 'package:flutter/material.dart';

import 'calculator_button.dart';

class MemoryButtons extends StatelessWidget {
  const MemoryButtons({
    super.key,
    required this.onMemoryAdd,
    required this.onMemorySubtract,
    required this.onMemoryRecall,
    required this.onMemoryClear,
  });

  final VoidCallback onMemoryAdd;
  final VoidCallback onMemorySubtract;
  final VoidCallback onMemoryRecall;
  final VoidCallback onMemoryClear;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        CalculatorButton(
          label: 'M+',
          kind: CalculatorButtonKind.memory,
          onPressed: onMemoryAdd,
        ),
        CalculatorButton(
          label: 'M−',
          kind: CalculatorButtonKind.memory,
          onPressed: onMemorySubtract,
        ),
        CalculatorButton(
          label: 'MR',
          kind: CalculatorButtonKind.memory,
          onPressed: onMemoryRecall,
        ),
        CalculatorButton(
          label: 'MC',
          kind: CalculatorButtonKind.memory,
          onPressed: onMemoryClear,
        ),
      ],
    );
  }
}
