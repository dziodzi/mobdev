import 'package:flutter/material.dart';

import 'calculator_engine.dart';
import 'theme/retro_theme.dart';
import 'widgets/retro_calculator_button.dart';
import 'widgets/retro_display.dart';
import 'widgets/retro_header.dart';

void main() {
  runApp(const MyCalculatorApp());
}

class MyCalculatorApp extends StatelessWidget {
  const MyCalculatorApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: RetroTheme.data,
      home: const CalculatorScreen(),
    );
  }
}

class CalculationHistoryItem {
  final String expression;
  final String result;

  const CalculationHistoryItem({
    required this.expression,
    required this.result,
  });
}

class CalculatorScreen extends StatefulWidget {
  const CalculatorScreen({super.key});

  @override
  State<CalculatorScreen> createState() {
    return _CalculatorScreenState();
  }
}

class _CalculatorScreenState extends State<CalculatorScreen> {
  final CalculatorEngine engine = CalculatorEngine();

  String expression = '';
  String result = '';
  String? errorMessage;
  double memory = 0;

  final List<CalculationHistoryItem> history = [];

  void _append(String value) {
    setState(() {
      expression += value;
      result = '';
      errorMessage = null;
    });
  }

  void _calculate() {
    try {
      final value = engine.calculate(expression);
      final formattedResult = _formatNumber(value);

      setState(() {
        result = formattedResult;
        errorMessage = null;

        history.insert(
          0,
          CalculationHistoryItem(
            expression: expression,
            result: formattedResult,
          ),
        );

        if (history.length > 10) {
          history.removeLast();
        }
      });
    } on FormatException catch (error) {
      setState(() {
        result = '';
        errorMessage = error.message;
      });
    }
  }

  String _formatNumber(double value) {
    if (value == value.truncateToDouble()) {
      return value.toInt().toString();
    }

    return value
        .toStringAsFixed(10)
        .replaceFirst(RegExp(r'0+$'), '')
        .replaceFirst(RegExp(r'\.$'), '');
  }

  void _clear() {
    setState(() {
      expression = '';
      result = '';
      errorMessage = null;
    });
  }

  void _memoryClear() {
    setState(() {
      memory = 0;
    });
  }

  void _memoryRecall() {
    setState(() {
      expression = _formatNumber(memory);
      result = '';
      errorMessage = null;
    });
  }

  void _changeMemory(double direction) {
    try {
      final value = _getCurrentValue();

      setState(() {
        memory += direction * value;
        errorMessage = null;
      });
    } on FormatException catch (error) {
      setState(() {
        errorMessage = error.message;
      });
    }
  }

  double _getCurrentValue() {
    return engine.calculate(expression);
  }

  void _backspace() {
    if (expression.isEmpty) {
      return;
    }

    setState(() {
      expression = expression.substring(0, expression.length - 1);
      result = '';
      errorMessage = null;
    });
  }

  bool _isNumberCharacter(String character) {
    final code = character.codeUnitAt(0);

    return (code >= '0'.codeUnitAt(0) && code <= '9'.codeUnitAt(0)) ||
        character == '.';
  }

  void _toggleSign() {
    if (expression.isEmpty) {
      return;
    }

    setState(() {
      if (expression.endsWith(')')) {
        var numberStart = expression.length - 1;

        while (numberStart > 0 &&
            _isNumberCharacter(expression[numberStart - 1])) {
          numberStart--;
        }

        if (numberStart >= 2 &&
            expression[numberStart - 2] == '(' &&
            expression[numberStart - 1] == '−') {
          final number = expression.substring(
            numberStart,
            expression.length - 1,
          );

          expression = expression.substring(0, numberStart - 2) + number;
          result = '';
          errorMessage = null;
          return;
        }
      }

      var numberStart = expression.length;

      while (numberStart > 0 &&
          _isNumberCharacter(expression[numberStart - 1])) {
        numberStart--;
      }

      if (numberStart == expression.length) {
        return;
      }

      final number = expression.substring(numberStart);
      expression = '${expression.substring(0, numberStart)}(−$number)';
      result = '';
      errorMessage = null;
    });
  }

  void _showHistory() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        return SafeArea(
          top: false,
          child: Container(
            constraints: const BoxConstraints(maxHeight: 430),
            decoration: const BoxDecoration(
              color: RetroPalette.panelDark,
              borderRadius: BorderRadius.vertical(
                top: Radius.circular(8),
              ),
              border: Border(
                top: BorderSide(
                  color: RetroPalette.cyanDark,
                  width: 2,
                ),
              ),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 9,
                  ),
                  decoration: const BoxDecoration(
                    gradient: RetroPalette.shellGradient,
                  ),
                  child: const Text(
                    'HISTORY / LAST 10',
                    style: TextStyle(
                      color: Color(0xFFF1F4F5),
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 0.8,
                      fontFamily: 'monospace',
                    ),
                  ),
                ),
                if (history.isEmpty)
                  const SizedBox(
                    height: 120,
                    child: Center(
                      child: Text(
                        'NO CALCULATIONS YET',
                        style: TextStyle(
                          color: Color(0xFFAAB1B6),
                          fontSize: 11,
                          fontFamily: 'monospace',
                        ),
                      ),
                    ),
                  )
                else
                  Flexible(
                    child: ListView.separated(
                      shrinkWrap: true,
                      itemCount: history.length,
                      separatorBuilder: (context, index) {
                        return const Divider(
                          height: 1,
                          thickness: 1,
                          color: Color(0xFF41474D),
                        );
                      },
                      itemBuilder: (context, index) {
                        final item = history[index];

                        return GestureDetector(
                          behavior: HitTestBehavior.opaque,
                          onTap: () {
                            Navigator.pop(sheetContext);

                            setState(() {
                              expression = item.expression;
                              result = item.result;
                              errorMessage = null;
                            });
                          },
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 14,
                              vertical: 10,
                            ),
                            child: Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    item.expression,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(
                                      color: Color(0xFFF0F2F3),
                                      fontSize: 13,
                                      fontFamily: 'monospace',
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 12),
                                Text(
                                  '= ${item.result}',
                                  style: const TextStyle(
                                    color: RetroPalette.cyan,
                                    fontSize: 13,
                                    fontWeight: FontWeight.w700,
                                    fontFamily: 'monospace',
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildKey(
    String text, {
    VoidCallback? onPressed,
    RetroKeyType type = RetroKeyType.number,
  }) {
    return Expanded(
      child: Padding(
        padding: const EdgeInsets.all(3),
        child: RetroCalculatorButton(
          label: text,
          type: type,
          onPressed: onPressed ?? () => _append(text),
        ),
      ),
    );
  }

  Widget _buildDisplay() {
    return RetroDisplay(
      expression: expression,
      result: result,
      errorMessage: errorMessage,
      memoryText: memory == 0 ? null : 'M ${_formatNumber(memory)}',
    );
  }

  Widget _buildKeyboard() {
    return Container(
      margin: const EdgeInsets.fromLTRB(8, 2, 8, 8),
      padding: const EdgeInsets.all(5),
      decoration: BoxDecoration(
        gradient: RetroPalette.shellGradient,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(
          color: const Color(0xFF656D74),
          width: 1,
        ),
        boxShadow: const [
          BoxShadow(
            color: Color(0x88000000),
            offset: Offset(0, 3),
            blurRadius: 4,
          ),
        ],
      ),
      child: Column(
        children: [
          Expanded(
            child: Row(
              children: [
                _buildKey(
                  'MC',
                  onPressed: _memoryClear,
                  type: RetroKeyType.memory,
                ),
                _buildKey(
                  'MR',
                  onPressed: _memoryRecall,
                  type: RetroKeyType.memory,
                ),
                _buildKey(
                  'M+',
                  onPressed: () => _changeMemory(1),
                  type: RetroKeyType.memory,
                ),
                _buildKey(
                  'M−',
                  onPressed: () => _changeMemory(-1),
                  type: RetroKeyType.memory,
                ),
              ],
            ),
          ),
          Expanded(
            child: Row(
              children: [
                _buildKey(
                  'C',
                  onPressed: _clear,
                  type: RetroKeyType.clear,
                ),
                _buildKey('(', type: RetroKeyType.action),
                _buildKey(')', type: RetroKeyType.action),
                _buildKey(
                  '⌫',
                  onPressed: _backspace,
                  type: RetroKeyType.action,
                ),
              ],
            ),
          ),
          Expanded(
            child: Row(
              children: [
                _buildKey('7'),
                _buildKey('8'),
                _buildKey('9'),
                _buildKey('÷', type: RetroKeyType.operator),
              ],
            ),
          ),
          Expanded(
            child: Row(
              children: [
                _buildKey('4'),
                _buildKey('5'),
                _buildKey('6'),
                _buildKey('×', type: RetroKeyType.operator),
              ],
            ),
          ),
          Expanded(
            child: Row(
              children: [
                _buildKey('1'),
                _buildKey('2'),
                _buildKey('3'),
                _buildKey('−', type: RetroKeyType.operator),
              ],
            ),
          ),
          Expanded(
            child: Row(
              children: [
                _buildKey(
                  '±',
                  onPressed: _toggleSign,
                  type: RetroKeyType.action,
                ),
                _buildKey('0'),
                _buildKey('.', type: RetroKeyType.action),
                _buildKey('+', type: RetroKeyType.operator),
              ],
            ),
          ),
          Expanded(
            child: Row(
              children: [
                _buildKey('%', type: RetroKeyType.action),
                _buildKey(
                  '=',
                  onPressed: _calculate,
                  type: RetroKeyType.equals,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCalculatorBody(Orientation orientation) {
    if (orientation == Orientation.landscape) {
      return Row(
        children: [
          Expanded(
            flex: 2,
            child: _buildDisplay(),
          ),
          Expanded(
            flex: 3,
            child: _buildKeyboard(),
          ),
        ],
      );
    }

    return Column(
      children: [
        Expanded(
          flex: 2,
          child: _buildDisplay(),
        ),
        Expanded(
          flex: 5,
          child: _buildKeyboard(),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: DecoratedBox(
          decoration: const BoxDecoration(
            gradient: RetroPalette.bodyGradient,
          ),
          child: Column(
            children: [
              RetroHeader(onHistory: _showHistory),
              Expanded(
                child: OrientationBuilder(
                  builder: (context, orientation) {
                    return _buildCalculatorBody(orientation);
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
