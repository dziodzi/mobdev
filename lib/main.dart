import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'calculator_logic.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Калькулятор',
      theme: ThemeData(
        colorScheme: .fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const CalculatorPage(),
    );
  }
}

class CalculatorPage extends StatefulWidget {
  const CalculatorPage({super.key});

  @override
  State<CalculatorPage> createState() => _CalculatorPageState();
}

class _CalculatorPageState extends State<CalculatorPage> {
  static const List<String> _buttons = [
    'C', '⌫', '%', '÷',
    '7', '8', '9', '×',
    '4', '5', '6', '−',
    '1', '2', '3', '+',
    '(', '0', ')', '.',
    '±', '=',
  ];

  static const Set<String> _operatorSymbols = {'+', '−', '×', '÷'};

  static const _keyExpression = 'calc_expression';
  static const _keyHistory = 'calc_history';
  static const _keyMemory = 'calc_memory';
  static const _keyLastResult = 'calc_last_result';
  static const _keyError = 'calc_error';

  String _expression = '';
  String? _errorText;
  String _lastResult = '';
  double _memory = 0;
  final List<HistoryEntry> _history = [];

  @override
  void initState() {
    super.initState();
    _loadState();
  }

  Future<void> _loadState() async {
    final prefs = await SharedPreferences.getInstance();
    if (!mounted) return;
    setState(() {
      _expression = prefs.getString(_keyExpression) ?? '';
      _lastResult = prefs.getString(_keyLastResult) ?? '';
      _memory = prefs.getDouble(_keyMemory) ?? 0;
      final savedError = prefs.getString(_keyError) ?? '';
      _errorText = savedError.isEmpty ? null : savedError;
      final historyRaw = prefs.getStringList(_keyHistory) ?? <String>[];
      _history
        ..clear()
        ..addAll(historyRaw.map(HistoryEntry.decode));
    });
  }

  Future<void> _saveState() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyExpression, _expression);
    await prefs.setString(_keyLastResult, _lastResult);
    await prefs.setDouble(_keyMemory, _memory);
    await prefs.setString(_keyError, _errorText ?? '');
    await prefs.setStringList(
      _keyHistory,
      _history.map((entry) => entry.encode()).toList(),
    );
  }

  bool _isDigitOrDot(String char) {
    final code = char.codeUnitAt(0);
    return (code >= 48 && code <= 57) || char == '.';
  }

  bool _isDigit(String char) {
    final code = char.codeUnitAt(0);
    return code >= 48 && code <= 57;
  }

  bool _currentNumberHasDecimalPoint() {
    for (var index = _expression.length - 1; index >= 0; index--) {
      final char = _expression[index];
      if (char == '.') return true;
      if (!_isDigit(char)) return false;
    }
    return false;
  }

  void _toggleSign() {
    if (_expression.isEmpty) return;

    var end = _expression.length;
    var start = end;
    while (start > 0 && _isDigitOrDot(_expression[start - 1])) {
      start--;
    }
    if (start == end) return;

    if (start > 0 && _expression[start - 1] == '-') {
      _expression = _expression.substring(0, start - 1) + _expression.substring(start);
    } else {
      _expression = '${_expression.substring(0, start)}-${_expression.substring(start)}';
    }
  }

  void _calculate() {
    try {
      final value = evaluateExpression(_expression);
      final formatted = formatResult(value);

      _history.insert(0, HistoryEntry(_expression, formatted));
      if (_history.length > 10) {
        _history.removeLast();
      }

      _lastResult = formatted;
      _expression = formatted;
      _errorText = null;
    } on CalculatorException catch (e) {
      _errorText = e.message;
    } catch (_) {
      _errorText = 'Некорректное выражение';
    }
  }

  void _onButtonPressed(String label) {
    setState(() {
      switch (label) {
        case 'C':
          _expression = '';
          _errorText = null;
          break;
        case '⌫':
          if (_errorText != null) {
            _errorText = null;
          } else if (_expression.isNotEmpty) {
            _expression = _expression.substring(0, _expression.length - 1);
          }
          break;
        case '±':
          _errorText = null;
          _toggleSign();
          break;
        case '=':
          _calculate();
          break;
        default:
          if (_errorText != null) {
            _expression = '';
            _errorText = null;
          }
          if (label == '.') {
            if (_currentNumberHasDecimalPoint()) return;
            final lastChar = _expression.isNotEmpty ? _expression[_expression.length - 1] : null;
            final needsLeadingZero = lastChar == null || !_isDigit(lastChar);
            _expression += needsLeadingZero ? '0.' : '.';
          } else if (_operatorSymbols.contains(label) &&
              _expression.isNotEmpty &&
              _operatorSymbols.contains(_expression[_expression.length - 1])) {
            _expression = _expression.substring(0, _expression.length - 1) + label;
          } else {
            _expression += label;
          }
      }
    });
    _saveState();
  }

  double? _currentValueForMemory() {
    if (_expression.isNotEmpty) {
      try {
        return evaluateExpression(_expression);
      } catch (_) {
      }
    }
    if (_lastResult.isNotEmpty) {
      return double.tryParse(_lastResult);
    }
    return null;
  }

  void _memoryAdd() {
    final value = _currentValueForMemory();
    if (value == null) return;
    setState(() => _memory += value);
    _saveState();
  }

  void _memorySubtract() {
    final value = _currentValueForMemory();
    if (value == null) return;
    setState(() => _memory -= value);
    _saveState();
  }

  void _memoryRecall() {
    setState(() {
      _expression = formatResult(_memory);
      _errorText = null;
    });
    _saveState();
  }

  void _memoryClear() {
    setState(() => _memory = 0);
    _saveState();
  }

  void _loadFromHistory(HistoryEntry entry) {
    setState(() {
      _expression = entry.expression;
      _errorText = null;
    });
    _saveState();
    Navigator.of(context).pop();
  }

  void _showHistory() {
    showModalBottomSheet(
      context: context,
      builder: (context) {
        if (_history.isEmpty) {
          return const Padding(
            padding: EdgeInsets.all(24),
            child: Text('История пуста'),
          );
        }
        return ListView.builder(
          shrinkWrap: true,
          itemCount: _history.length,
          itemBuilder: (context, index) {
            final entry = _history[index];
            return ListTile(
              title: Text('${entry.expression} = ${entry.result}'),
              onTap: () => _loadFromHistory(entry),
            );
          },
        );
      },
    );
  }

  Widget _buildMemoryRow() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        TextButton(onPressed: _memoryAdd, child: const Text('M+')),
        TextButton(onPressed: _memorySubtract, child: const Text('M-')),
        TextButton(onPressed: _memoryRecall, child: const Text('MR')),
        TextButton(onPressed: _memoryClear, child: const Text('MC')),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Калькулятор'),
        actions: [
          IconButton(
            icon: const Icon(Icons.history),
            onPressed: _showHistory,
          ),
        ],
      ),
      body: Column(
        children: [
          _buildMemoryRow(),
          Expanded(
            child: Container(
              width: double.infinity,
              alignment: Alignment.bottomRight,
              padding: const EdgeInsets.all(24),
              child: FittedBox(
                fit: BoxFit.scaleDown,
                alignment: Alignment.bottomRight,
                child: Text(
                  _errorText ?? (_expression.isEmpty ? '0' : _expression),
                  maxLines: 1,
                  softWrap: false,
                  style: const TextStyle(
                    fontSize: 36,
                    color: Colors.deepPurple,
                  ),
                ),
              ),
            ),
          ),
          Expanded(
            flex: 3,
            child: LayoutBuilder(
              builder: (context, constraints) {
                const crossAxisCount = 4;
                final rowCount = (_buttons.length / crossAxisCount).ceil();
                final cellWidth = constraints.maxWidth / crossAxisCount;
                final cellHeight = constraints.maxHeight / rowCount;

                return GridView.count(
                  crossAxisCount: crossAxisCount,
                  childAspectRatio: cellWidth / cellHeight,
                  physics: const NeverScrollableScrollPhysics(),
                  children: _buttons.map((label) {
                    return Padding(
                      padding: const EdgeInsets.all(4),
                      child: ElevatedButton(
                        onPressed: () => _onButtonPressed(label),
                        child: label == '⌫'
                            ? const Icon(
                                Icons.backspace_outlined,
                                size: 20,
                                color: Colors.deepPurple,
                              )
                            : Text(
                                label,
                                style: const TextStyle(
                                  fontSize: 20,
                                  color: Colors.deepPurple,
                                ),
                              ),
                      ),
                    );
                  }).toList(),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
