class _ParsedValue {
  final double value;

  final bool isPercent;

  const _ParsedValue(this.value, {this.isPercent = false});
}

class CalculatorEngine {
  String _expression = '';
  int _position = 0;

  double calculate(String expression) {
    _expression = expression;
    _position = 0;

    if (_expression.isEmpty) {
      throw FormatException('Expression is empty!');
    }

    _validateOperators();

    if (_isOperator(_expression[_expression.length - 1])) {
      throw FormatException('Expression cannot end with an operator');
    }

    final result = _parseExpression();

    if (_position != _expression.length) {
      throw FormatException('Unexpected symbol at position $_position');
    }

    return result.value;
  }

  double _parseNumber() {
    final start = _position;

    while (_position < _expression.length &&
        (_isDigit(_expression[_position]) ||
            _expression[_position] == '.')) {
      _position++;
    }

    if (start == _position) {
      throw FormatException('Expected a number');
    }

    final numberText = _expression.substring(start, _position);
    final dotCount = '.'.allMatches(numberText).length;

    if (dotCount > 1 || numberText == '.') {
      throw FormatException('Invalid decimal number');
    }

    return double.parse(numberText);
  }

  _ParsedValue _parseFactor() {
    if (_position >= _expression.length) {
      throw FormatException('Expected a number');
    }

    // Unary minus: −5, −(2+3), (−9)
    if (_expression[_position] == '−') {
      _position++;
      final factor = _parseFactor();

      return _ParsedValue(
        -factor.value,
        isPercent: factor.isPercent,
      );
    }

    _ParsedValue result;

    if (_expression[_position] == '(') {
      _position++;

      result = _parseExpression();

      if (_position >= _expression.length ||
          _expression[_position] != ')') {
        throw FormatException('Missing closing parenthesis!');
      }

      _position++;
    } else {
      result = _ParsedValue(_parseNumber());
    }

    // Postfix percent.
    // 10% becomes 0.10.
    // The isPercent flag is kept so that 100 + 10% can become 110.
    if (_position < _expression.length &&
        _expression[_position] == '%') {
      _position++;

      if (_position < _expression.length &&
          _expression[_position] == '%') {
        throw FormatException('Unexpected second percent sign');
      }

      result = _ParsedValue(
        result.value / 100,
        isPercent: true,
      );
    }

    return result;
  }

  _ParsedValue _parseTerm() {
    var result = _parseFactor();

    while (_position < _expression.length) {
      final current = _expression[_position];

      if (current == '×' || current == '÷') {
        _position++;

        final nextFactor = _parseFactor();

        if (current == '×') {
          result = _ParsedValue(
            result.value * nextFactor.value,
          );
        } else {
          if (nextFactor.value == 0) {
            throw FormatException('Division by zero!');
          }

          result = _ParsedValue(
            result.value / nextFactor.value,
          );
        }
      } else if (_startsFactor(current)) {
        // Implicit multiplication:
        // 2(3+4), (2+3)4, (2+3)(4+5)
        final nextFactor = _parseFactor();

        result = _ParsedValue(
          result.value * nextFactor.value,
        );
      } else {
        break;
      }
    }

    return result;
  }

  _ParsedValue _parseExpression() {
    var result = _parseTerm();

    while (_position < _expression.length) {
      final operator = _expression[_position];

      if (operator != '+' && operator != '−') {
        break;
      }

      _position++;

      final nextTerm = _parseTerm();

      if (operator == '+') {
        if (nextTerm.isPercent) {
          // BlackBerry/classic phone-calculator style:
          // 100 + 10% = 110
          result = _ParsedValue(
            result.value + result.value * nextTerm.value,
          );
        } else {
          result = _ParsedValue(
            result.value + nextTerm.value,
          );
        }
      } else {
        if (nextTerm.isPercent) {
          // 100 - 10% = 90
          result = _ParsedValue(
            result.value - result.value * nextTerm.value,
          );
        } else {
          result = _ParsedValue(
            result.value - nextTerm.value,
          );
        }
      }
    }

    return result;
  }

  bool _isDigit(String character) {
    return character.codeUnitAt(0) >= '0'.codeUnitAt(0) &&
        character.codeUnitAt(0) <= '9'.codeUnitAt(0);
  }

  bool _isOperator(String character) {
    return character == '+' ||
        character == '−' ||
        character == '×' ||
        character == '÷';
  }

  bool _startsFactor(String character) {
    return _isDigit(character) ||
        character == '.' ||
        character == '(';
  }

  void _validateOperators() {
    for (var i = 1; i < _expression.length; i++) {
      if (_isOperator(_expression[i - 1]) &&
          _isOperator(_expression[i])) {
        throw FormatException('Two operators in a row!');
      }
    }
  }
}
