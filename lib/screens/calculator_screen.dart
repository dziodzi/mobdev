import 'package:flutter/material.dart';

import '../controllers/calculator_controller.dart';
import '../widgets/calculator_button.dart';
import '../widgets/calculator_display.dart';
import '../widgets/history_sheet.dart';
import '../widgets/memory_buttons.dart';

class CalculatorScreen extends StatelessWidget {
  const CalculatorScreen({super.key, required this.controller});

  final CalculatorController controller;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, _) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Advanced Calculator'),
            actions: [
              IconButton(
                tooltip: 'История',
                onPressed: () => HistorySheet.show(
                  context: context,
                  history: controller.history,
                  onSelect: controller.loadHistoryItem,
                ),
                icon: const Icon(Icons.history),
              ),
            ],
          ),
          body: SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final keypad = _Keypad(controller: controller);
                  final display = CalculatorDisplay(
                    expression: controller.expression,
                    result: controller.displayResult,
                    hasError: controller.hasError,
                    hasMemory: controller.hasMemory,
                  );
                  final isLandscape = constraints.maxWidth > constraints.maxHeight;

                  if (isLandscape) {
                    return Row(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Expanded(
                          flex: 4,
                          child: SingleChildScrollView(child: display),
                        ),
                        const SizedBox(width: 12),
                        Expanded(flex: 6, child: keypad),
                      ],
                    );
                  }

                  return Column(
                    children: [
                      display,
                      const SizedBox(height: 12),
                      Expanded(child: keypad),
                    ],
                  );
                },
              ),
            ),
          ),
        );
      },
    );
  }
}

class _Keypad extends StatelessWidget {
  const _Keypad({required this.controller});

  final CalculatorController controller;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Expanded(
          child: MemoryButtons(
            onMemoryAdd: controller.memoryAdd,
            onMemorySubtract: controller.memorySubtract,
            onMemoryRecall: controller.memoryRecall,
            onMemoryClear: controller.memoryClear,
          ),
        ),
        Expanded(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              CalculatorButton(
                label: 'C',
                kind: CalculatorButtonKind.action,
                onPressed: controller.onClear,
              ),
              CalculatorButton(
                label: '(',
                kind: CalculatorButtonKind.action,
                onPressed: controller.onOpenParen,
              ),
              CalculatorButton(
                label: ')',
                kind: CalculatorButtonKind.action,
                onPressed: controller.onCloseParen,
              ),
              CalculatorButton(
                label: '÷',
                kind: CalculatorButtonKind.operator,
                onPressed: () => controller.onOperator('÷'),
              ),
              CalculatorButton(
                label: '⌫',
                kind: CalculatorButtonKind.action,
                onPressed: controller.onBackspace,
              ),
            ],
          ),
        ),
        Expanded(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              CalculatorButton(
                label: '7',
                onPressed: () => controller.onDigit('7'),
              ),
              CalculatorButton(
                label: '8',
                onPressed: () => controller.onDigit('8'),
              ),
              CalculatorButton(
                label: '9',
                onPressed: () => controller.onDigit('9'),
              ),
              CalculatorButton(
                label: '×',
                kind: CalculatorButtonKind.operator,
                onPressed: () => controller.onOperator('×'),
              ),
              CalculatorButton(
                label: '%',
                kind: CalculatorButtonKind.operator,
                onPressed: controller.onPercent,
              ),
            ],
          ),
        ),
        Expanded(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              CalculatorButton(
                label: '4',
                onPressed: () => controller.onDigit('4'),
              ),
              CalculatorButton(
                label: '5',
                onPressed: () => controller.onDigit('5'),
              ),
              CalculatorButton(
                label: '6',
                onPressed: () => controller.onDigit('6'),
              ),
              CalculatorButton(
                label: '−',
                kind: CalculatorButtonKind.operator,
                onPressed: () => controller.onOperator('−'),
              ),
              CalculatorButton(
                label: '±',
                kind: CalculatorButtonKind.operator,
                onPressed: controller.onPlusMinus,
              ),
            ],
          ),
        ),
        Expanded(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              CalculatorButton(
                label: '1',
                onPressed: () => controller.onDigit('1'),
              ),
              CalculatorButton(
                label: '2',
                onPressed: () => controller.onDigit('2'),
              ),
              CalculatorButton(
                label: '3',
                onPressed: () => controller.onDigit('3'),
              ),
              CalculatorButton(
                label: '+',
                kind: CalculatorButtonKind.operator,
                onPressed: () => controller.onOperator('+'),
              ),
              CalculatorButton(
                label: '=',
                kind: CalculatorButtonKind.equals,
                onPressed: controller.onEquals,
              ),
            ],
          ),
        ),
        Expanded(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              CalculatorButton(
                label: '0',
                flex: 2,
                onPressed: () => controller.onDigit('0'),
              ),
              CalculatorButton(
                label: '.',
                onPressed: controller.onDecimal,
              ),
              const Spacer(flex: 2),
            ],
          ),
        ),
      ],
    );
  }
}
