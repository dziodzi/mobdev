import 'dart:convert';

import 'package:flutter/material.dart';

import 'calculator_engine.dart';
import 'history_entry.dart';

class CalculatorPage extends StatefulWidget {
  const CalculatorPage({super.key});

  @override
  State<CalculatorPage> createState() => _CalculatorPageState();
}

class _CalculatorPageState extends State<CalculatorPage> with RestorationMixin {
  final _engine = CalculatorEngine();
  final _expression = RestorableString('');
  final _result = RestorableString('0');
  final _error = RestorableString('');
  final _memory = RestorableDouble(0);
  final _historyJson = RestorableString('[]');

  @override
  String get restorationId => 'calculator_page';

  @override
  void restoreState(RestorationBucket? oldBucket, bool initialRestore) {
    registerForRestoration(_expression, 'expression');
    registerForRestoration(_result, 'result');
    registerForRestoration(_error, 'error');
    registerForRestoration(_memory, 'memory');
    registerForRestoration(_historyJson, 'history');
  }

  List<HistoryEntry> get _history {
    try {
      return (jsonDecode(_historyJson.value) as List<dynamic>)
          .map((item) => HistoryEntry.fromJson(item as Map<String, dynamic>))
          .toList();
    } catch (_) {
      return [];
    }
  }

  void _setHistory(List<HistoryEntry> history) {
    _historyJson.value =
        jsonEncode(history.map((entry) => entry.toJson()).toList());
  }

  void _append(String value) {
    setState(() {
      _error.value = '';
      final expression = _expression.value;
      if (_isOperator(value) &&
          expression.isNotEmpty &&
          _isOperator(expression.characters.last)) {
        _expression.value =
            expression.substring(0, expression.length - 1) + value;
      } else if (value == '.' && _currentNumber(expression).contains('.')) {
        _error.value = 'A number can only have one decimal point';
      } else {
        _expression.value += value;
      }
    });
  }

  void _calculate() {
    setState(() {
      try {
        final original = _expression.value;
        final value = _engine.evaluate(original);
        final formatted = formatNumber(value);
        _result.value = formatted;
        _error.value = '';
        final history = _history;
        history.insert(0, HistoryEntry(original, formatted));
        _setHistory(history.take(10).toList());
      } on CalculatorException catch (exception) {
        _error.value = exception.message;
      } catch (_) {
        _error.value = 'Invalid expression';
      }
    });
  }

  void _clear() => setState(() {
        _expression.value = '';
        _result.value = '0';
        _error.value = '';
      });

  void _backspace() {
    if (_expression.value.isEmpty) return;
    setState(() {
      _expression.value =
          _expression.value.substring(0, _expression.value.length - 1);
      _error.value = '';
    });
  }

  void _toggleSign() {
    setState(() {
      final value = _expression.value;
      if (value.isEmpty) {
        _expression.value = '-';
      } else {
        final match = RegExp(r'(-?\d*\.?\d+)$').firstMatch(value);
        if (match == null) {
          _expression.value += '-';
        } else {
          final number = match.group(0)!;
          final replacement =
              number.startsWith('-') ? number.substring(1) : '-$number';
          _expression.value =
              value.replaceRange(match.start, match.end, replacement);
        }
      }
      _error.value = '';
    });
  }

  double? _currentValue() {
    try {
      if (_expression.value.isNotEmpty) {
        return _engine.evaluate(_expression.value);
      }
      return double.tryParse(_result.value);
    } catch (_) {
      setState(() => _error.value = 'Calculate a valid value first');
      return null;
    }
  }

  void _memoryAdd(double sign) {
    final value = _currentValue();
    if (value == null) return;
    setState(() {
      _memory.value += sign * value;
      _error.value = '';
    });
  }

  void _memoryRecall() => setState(() {
        _expression.value += formatNumber(_memory.value);
        _error.value = '';
      });

  void _loadHistory(HistoryEntry entry) => setState(() {
        _expression.value = entry.expression;
        _result.value = entry.result;
        _error.value = '';
      });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Advanced Calculator'),
        actions: [
          if (_memory.value != 0)
            Padding(
              padding: const EdgeInsets.only(right: 16),
              child: Center(child: Text('M: ${formatNumber(_memory.value)}')),
            ),
        ],
      ),
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            final calculator = _calculatorPanel();
            final history = _historyPanel();
            if (constraints.maxWidth >= 700) {
              return Row(children: [
                Expanded(flex: 3, child: calculator),
                const VerticalDivider(width: 1),
                Expanded(flex: 2, child: history)
              ]);
            }
            return Column(children: [
              Expanded(flex: 7, child: calculator),
              const Divider(height: 1),
              Expanded(flex: 3, child: history)
            ]);
          },
        ),
      ),
    );
  }

  Widget _calculatorPanel() => Column(
        children: [
          Expanded(
            flex: 2,
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              alignment: Alignment.bottomRight,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    reverse: true,
                    child: Text(
                        _expression.value.isEmpty
                            ? '0'
                            : _pretty(_expression.value),
                        style: const TextStyle(
                            fontSize: 30, color: Colors.white70)),
                  ),
                  const SizedBox(height: 8),
                  Text(_error.value.isEmpty ? _result.value : _error.value,
                      textAlign: TextAlign.right,
                      style: TextStyle(
                          fontSize: _error.value.isEmpty ? 44 : 18,
                          color: _error.value.isEmpty
                              ? Colors.white
                              : Theme.of(context).colorScheme.error)),
                ],
              ),
            ),
          ),
          _memoryRow(),
          Expanded(flex: 4, child: _keypad()),
        ],
      );

  Widget _memoryRow() => Row(
        children: [
          _smallButton('MC', () => setState(() => _memory.value = 0)),
          _smallButton('MR', _memoryRecall),
          _smallButton('M+', () => _memoryAdd(1)),
          _smallButton('M−', () => _memoryAdd(-1)),
        ],
      );

  Widget _smallButton(String text, VoidCallback action) => Expanded(
        child: TextButton(
            onPressed: action,
            child: Text(text,
                style: const TextStyle(fontWeight: FontWeight.bold))),
      );

  Widget _keypad() {
    const rows = [
      ['C', '⌫', '(', ')', '÷'],
      ['7', '8', '9', '%', '×'],
      ['4', '5', '6', '±', '−'],
      ['1', '2', '3', '.', '+'],
      ['0', '='],
    ];
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 0, 8, 8),
      child: Column(
        children: rows
            .map((row) => Expanded(
                child: Row(
                    children: row
                        .map((key) => Expanded(
                            flex: key == '0'
                                ? 4
                                : key == '='
                                    ? 6
                                    : 2,
                            child: _key(key)))
                        .toList())))
            .toList(),
      ),
    );
  }

  Widget _key(String label) {
    final special = const {'÷', '×', '−', '+', '='}.contains(label);
    return Padding(
      padding: const EdgeInsets.all(3),
      child: FilledButton.tonal(
        style: FilledButton.styleFrom(
          backgroundColor: special
              ? Theme.of(context).colorScheme.primaryContainer
              : const Color(0xFF24242A),
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
          padding: EdgeInsets.zero,
        ),
        onPressed: () {
          switch (label) {
            case 'C':
              _clear();
              break;
            case '⌫':
              _backspace();
              break;
            case '±':
              _toggleSign();
              break;
            case '=':
              _calculate();
              break;
            default:
              _append({'×': '*', '÷': '/', '−': '-'}[label] ?? label);
          }
        },
        child: Text(label,
            style: TextStyle(
                fontSize: special ? 25 : 21, fontWeight: FontWeight.w600)),
      ),
    );
  }

  Widget _historyPanel() {
    final entries = _history;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: const Text('History',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        ),
        Expanded(
          child: entries.isEmpty
              ? const Center(
                  child: Text('Your last 10 calculations appear here',
                      style: TextStyle(color: Colors.white54)))
              : ListView.builder(
                  itemCount: entries.length,
                  itemBuilder: (context, index) {
                    final entry = entries[index];
                    return ListTile(
                      onTap: () => _loadHistory(entry),
                      title: Text(_pretty(entry.expression),
                          maxLines: 1, overflow: TextOverflow.ellipsis),
                      trailing: Text('= ${entry.result}',
                          style: const TextStyle(
                              fontSize: 17, fontWeight: FontWeight.bold)),
                    );
                  },
                ),
        ),
      ],
    );
  }

  bool _isOperator(String value) => const {'+', '-', '*', '/'}.contains(value);
  String _currentNumber(String expression) =>
      expression.split(RegExp(r'[+\-*/()]')).last;
  String _pretty(String value) => value
      .replaceAll('*', ' × ')
      .replaceAll('/', ' ÷ ')
      .replaceAll('+', ' + ')
      .replaceAll('-', ' − ');

  @override
  void dispose() {
    _expression.dispose();
    _result.dispose();
    _error.dispose();
    _memory.dispose();
    _historyJson.dispose();
    super.dispose();
  }
}
