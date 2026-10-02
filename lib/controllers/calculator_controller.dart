import 'package:flutter/foundation.dart';

import '../models/calculation_history_item.dart';
import '../models/calculator_error.dart';
import '../models/calculator_persisted_state.dart';
import '../services/expression_evaluator.dart';
import '../services/storage_service.dart';

class CalculatorController extends ChangeNotifier {
  CalculatorController({
    required StorageService storage,
    ExpressionEvaluator? evaluator,
  })  : _storage = storage,
        _evaluator = evaluator ?? ExpressionEvaluator();

  static const maxHistoryLength = 10;
  static const operators = {'+', '−', '×', '÷', '-', '*', '/'};

  final StorageService _storage;
  final ExpressionEvaluator _evaluator;

  String _expression = '';
  String _lastResult = '';
  String _error = '';
  double _memory = 0;
  List<CalculationHistoryItem> _history = [];
  bool _justEvaluated = false;
  bool _loaded = false;

  String get expression => _expression;
  String get lastResult => _lastResult;
  String get error => _error;
  double get memory => _memory;
  bool get hasMemory => _memory != 0;
  List<CalculationHistoryItem> get history =>
      List<CalculationHistoryItem>.unmodifiable(_history);
  bool get hasError => _error.isNotEmpty;
  bool get isLoaded => _loaded;
  String get displayResult => hasError ? _error : _lastResult;

  Future<void> load() async {
    final state = await _storage.loadAll();
    _expression = state.expression;
    _lastResult = state.lastResult;
    _error = state.error;
    _memory = state.memory;
    _history = List<CalculationHistoryItem>.from(state.history);
    _justEvaluated = state.justEvaluated;
    _loaded = true;
    notifyListeners();
  }

  Future<void> onDigit(String digit) async {
    _clearError();
    if (_justEvaluated) {
      _expression = digit;
      _justEvaluated = false;
      _lastResult = '';
    } else {
      _expression += digit;
    }
    await _persist();
  }

  Future<void> onDecimal() async {
    _clearError();
    if (_justEvaluated) {
      _expression = '0.';
      _justEvaluated = false;
      _lastResult = '';
      await _persist();
      return;
    }

    if (_expression.isEmpty || _endsWithOperator() || _endsWith('(')) {
      _expression += '0.';
      await _persist();
      return;
    }

    if (_currentNumberHasDecimal()) {
      return;
    }

    if (_endsWith(')') || _endsWith('%')) {
      return;
    }

    _expression += '.';
    await _persist();
  }

  Future<void> onOperator(String operatorSymbol) async {
    _clearError();
    final symbol = _toUiOperator(operatorSymbol);

    if (_justEvaluated && _lastResult.isNotEmpty) {
      _expression = _lastResult + symbol;
      _justEvaluated = false;
      await _persist();
      return;
    }

    if (_expression.isEmpty) {
      if (symbol == '−') {
        _expression = '−';
        await _persist();
      }
      return;
    }

    if (_endsWith('.')) {
      _expression += '0';
    }

    if (_endsWithOperator()) {
      _expression = _expression.substring(0, _expression.length - 1) + symbol;
      await _persist();
      return;
    }

    if (_endsWith('(') && symbol != '−') {
      return;
    }

    _expression += symbol;
    _justEvaluated = false;
    await _persist();
  }

  Future<void> onOpenParen() async {
    _clearError();
    if (_justEvaluated) {
      _expression = '(';
      _justEvaluated = false;
      _lastResult = '';
      await _persist();
      return;
    }

    if (_expression.isNotEmpty &&
        (_endsWithDigit() ||
            _endsWith(')') ||
            _endsWith('%') ||
            _endsWith('.'))) {
      if (_endsWith('.')) {
        _expression += '0';
      }
      _expression += '×(';
    } else {
      _expression += '(';
    }
    await _persist();
  }

  Future<void> onCloseParen() async {
    _clearError();
    if (_justEvaluated) {
      return;
    }
    if (_openParenCount() <= _closeParenCount()) {
      return;
    }
    if (_expression.isEmpty ||
        _endsWithOperator() ||
        _endsWith('(') ||
        _endsWith('.')) {
      return;
    }
    _expression += ')';
    await _persist();
  }

  Future<void> onPercent() async {
    _clearError();
    if (_justEvaluated && _lastResult.isNotEmpty) {
      _expression = '$_lastResult%';
      _justEvaluated = false;
      await _persist();
      return;
    }
    if (_expression.isEmpty ||
        _endsWithOperator() ||
        _endsWith('(') ||
        _endsWith('%') ||
        _endsWith('.')) {
      return;
    }
    _expression += '%';
    await _persist();
  }

  Future<void> onPlusMinus() async {
    _clearError();
    if (_justEvaluated && _lastResult.isNotEmpty) {
      _expression = _negateOperand(_lastResult);
      _justEvaluated = false;
      await _persist();
      return;
    }
    if (_expression.isEmpty) {
      _expression = '−';
      await _persist();
      return;
    }

    final next = _toggleCurrentOperandSign(_expression);
    if (next != _expression) {
      _expression = next;
      await _persist();
    }
  }

  Future<void> onBackspace() async {
    if (_expression.isEmpty) {
      _clearError();
      await _persist();
      return;
    }
    _expression = _expression.substring(0, _expression.length - 1);
    _justEvaluated = false;
    _clearError();
    if (_expression.isEmpty) {
      _lastResult = '';
    }
    await _persist();
  }

  Future<void> onClear() async {
    _expression = '';
    _lastResult = '';
    _error = '';
    _justEvaluated = false;
    await _persist();
  }

  Future<void> onEquals() async {
    try {
      final value = _evaluator.evaluate(_expression);
      final formatted = _evaluator.format(value);
      _lastResult = formatted;
      _error = '';
      _justEvaluated = true;
      _addHistory(_expression, formatted);
      await _persist();
    } on EvaluationException catch (error) {
      _error = error.message;
      _justEvaluated = false;
      await _persist();
    } catch (_) {
      _error = EvaluationException.invalidExpression.message;
      _justEvaluated = false;
      await _persist();
    }
  }

  Future<void> memoryAdd() async {
    final value = _currentNumericValue();
    if (value == null) {
      return;
    }
    _memory += value;
    await _persist();
  }

  Future<void> memorySubtract() async {
    final value = _currentNumericValue();
    if (value == null) {
      return;
    }
    _memory -= value;
    await _persist();
  }

  Future<void> memoryRecall() async {
    _clearError();
    final formatted = _evaluator.format(_memory);
    if (_justEvaluated || _expression.isEmpty) {
      _expression = formatted;
      _justEvaluated = false;
      _lastResult = '';
      await _persist();
      return;
    }
    if (_endsWithDigit() || _endsWith(')') || _endsWith('%') || _endsWith('.')) {
      return;
    }
    _expression += formatted;
    await _persist();
  }

  Future<void> memoryClear() async {
    _memory = 0;
    await _persist();
  }

  Future<void> loadHistoryItem(CalculationHistoryItem item) async {
    _expression = item.expression;
    _lastResult = item.result;
    _error = '';
    _justEvaluated = false;
    await _persist();
  }

  double? _currentNumericValue() {
    if (_lastResult.isNotEmpty && !hasError) {
      try {
        return _evaluator.evaluate(_lastResult);
      } catch (_) {
        // Fall through to the current expression.
      }
    }
    if (_expression.isEmpty) {
      return null;
    }
    try {
      return _evaluator.evaluate(_expression);
    } catch (_) {
      return null;
    }
  }

  void _addHistory(String expression, String result) {
    _history.insert(
      0,
      CalculationHistoryItem(expression: expression, result: result),
    );
    if (_history.length > maxHistoryLength) {
      _history = _history.sublist(0, maxHistoryLength);
    }
  }

  void _clearError() {
    if (_error.isNotEmpty) {
      _error = '';
    }
  }

  Future<void> _persist() async {
    notifyListeners();
    await _storage.saveAll(
      CalculatorPersistedState(
        expression: _expression,
        lastResult: _lastResult,
        error: _error,
        memory: _memory,
        history: _history,
        justEvaluated: _justEvaluated,
      ),
    );
  }

  bool _endsWith(String suffix) => _expression.endsWith(suffix);

  bool _endsWithOperator() {
    if (_expression.isEmpty) {
      return false;
    }
    return operators.contains(_expression[_expression.length - 1]);
  }

  bool _endsWithDigit() {
    if (_expression.isEmpty) {
      return false;
    }
    final last = _expression[_expression.length - 1];
    return last.codeUnitAt(0) >= 48 && last.codeUnitAt(0) <= 57;
  }

  bool _currentNumberHasDecimal() {
    final number = _currentNumberLexeme(_expression);
    return number.contains('.');
  }

  String _currentNumberLexeme(String expression) {
    if (expression.isEmpty) {
      return '';
    }
    var i = expression.length - 1;
    while (i >= 0 &&
        ((expression[i].codeUnitAt(0) >= 48 &&
                expression[i].codeUnitAt(0) <= 57) ||
            expression[i] == '.')) {
      i--;
    }
    return expression.substring(i + 1);
  }

  int _openParenCount() => '('.allMatches(_expression).length;

  int _closeParenCount() => ')'.allMatches(_expression).length;

  String _toUiOperator(String operatorSymbol) {
    switch (operatorSymbol) {
      case '*':
      case '×':
        return '×';
      case '/':
      case '÷':
        return '÷';
      case '-':
      case '−':
        return '−';
      default:
        return '+';
    }
  }

  String _negateOperand(String operand) {
    if (operand.startsWith('−') || operand.startsWith('-')) {
      return operand.substring(1);
    }
    if (operand.startsWith('(') && operand.endsWith(')')) {
      return operand;
    }
    return '−$operand';
  }

  /// Toggles the sign of the last numeric operand without rewriting the whole expression.
  String _toggleCurrentOperandSign(String expression) {
    var end = expression.length;
    if (expression.endsWith('%')) {
      end -= 1;
    }

    if (end == 0) {
      return expression;
    }

    if (expression[end - 1] == ')') {
      final open = _findMatchingOpen(expression, end - 1);
      if (open == null) {
        return expression;
      }
      final inner = expression.substring(open + 1, end - 1);
      if (_isPlainNumber(inner)) {
        final toggled = _negatePlainNumber(inner);
        return expression.substring(0, open) +
            toggled +
            expression.substring(end);
      }
      if (open > 0 &&
          (expression[open - 1] == '−' || expression[open - 1] == '-')) {
        final beforeSign = expression.substring(0, open - 1);
        if (beforeSign.isEmpty ||
            operators.contains(beforeSign[beforeSign.length - 1]) ||
            beforeSign.endsWith('(')) {
          return beforeSign +
              expression.substring(open, end) +
              expression.substring(end);
        }
      }
      return '${expression.substring(0, open)}−${expression.substring(open, end)}${expression.substring(end)}';
    }

    var start = end - 1;
    while (start >= 0 &&
        ((expression[start].codeUnitAt(0) >= 48 &&
                expression[start].codeUnitAt(0) <= 57) ||
            expression[start] == '.')) {
      start--;
    }
    start += 1;
    if (start >= end) {
      if (expression.endsWith('−') || expression.endsWith('-')) {
        return expression.substring(0, expression.length - 1);
      }
      return expression;
    }

    final prefix = expression.substring(0, start);
    final number = expression.substring(start, end);
    final suffix = expression.substring(end);

    if (_hasUnaryMinusPrefix(prefix)) {
      return prefix.substring(0, prefix.length - 1) + number + suffix;
    }

    return '$prefix−$number$suffix';
  }

  bool _hasUnaryMinusPrefix(String prefix) {
    if (prefix.isEmpty) {
      return false;
    }
    final last = prefix[prefix.length - 1];
    if (last != '−' && last != '-') {
      return false;
    }
    if (prefix.length == 1) {
      return true;
    }
    final before = prefix[prefix.length - 2];
    return operators.contains(before) || before == '(';
  }

  bool _isPlainNumber(String value) {
    if (value.isEmpty) {
      return false;
    }
    final normalized = value.replaceAll('−', '-');
    return double.tryParse(normalized) != null;
  }

  String _negatePlainNumber(String value) {
    if (value.startsWith('−') || value.startsWith('-')) {
      return value.substring(1);
    }
    return '−$value';
  }

  int? _findMatchingOpen(String expression, int closeIndex) {
    var depth = 0;
    for (var i = closeIndex; i >= 0; i--) {
      if (expression[i] == ')') {
        depth++;
      } else if (expression[i] == '(') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return null;
  }
}
