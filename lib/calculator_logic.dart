enum CalcErrorType {
  emptyExpression,
  divisionByZero,
  unbalancedParentheses,
  consecutiveOperators,
  multipleDecimalPoints,
  trailingOperator,
  invalidExpression,
}

class CalculatorException implements Exception {
  final CalcErrorType type;
  final String message;

  const CalculatorException(this.type, this.message);

  @override
  String toString() => message;
}

enum _TokenType {
  number,
  plus,
  minus, 
  sign,
  times,
  divide,
  lparen,
  rparen,
  percent,
}

class _Token {
  final _TokenType type;
  final double? value;

  const _Token(this.type, [this.value]);
}

bool _isBinaryOperator(_TokenType? type) {
  return type == _TokenType.plus ||
      type == _TokenType.minus ||
      type == _TokenType.times ||
      type == _TokenType.divide;
}

bool _isDigit(String char) {
  final code = char.codeUnitAt(0);
  return code >= 48 && code <= 57;
}

List<_Token> _tokenize(String input) {
  final tokens = <_Token>[];
  var i = 0;

  while (i < input.length) {
    final char = input[i];

    if (char == ' ') {
      i++;
    } else if (char == '+') {
      tokens.add(const _Token(_TokenType.plus));
      i++;
    } else if (char == '−') {
      tokens.add(const _Token(_TokenType.minus));
      i++;
    } else if (char == '-') {
      tokens.add(const _Token(_TokenType.sign));
      i++;
    } else if (char == '×') {
      tokens.add(const _Token(_TokenType.times));
      i++;
    } else if (char == '÷') {
      tokens.add(const _Token(_TokenType.divide));
      i++;
    } else if (char == '(') {
      tokens.add(const _Token(_TokenType.lparen));
      i++;
    } else if (char == ')') {
      tokens.add(const _Token(_TokenType.rparen));
      i++;
    } else if (char == '%') {
      tokens.add(const _Token(_TokenType.percent));
      i++;
    } else if (_isDigit(char) || char == '.') {
      final start = i;
      var dotCount = 0;
      while (i < input.length && (_isDigit(input[i]) || input[i] == '.')) {
        if (input[i] == '.') {
          dotCount++;
          if (dotCount > 1) {
            throw const CalculatorException(
              CalcErrorType.multipleDecimalPoints,
              'В числе несколько десятичных точек',
            );
          }
        }
        i++;
      }
      final numberText = input.substring(start, i);
      final value = double.tryParse(numberText);
      if (value == null) {
        throw const CalculatorException(
          CalcErrorType.invalidExpression,
          'Некорректное число в выражении',
        );
      }
      tokens.add(_Token(_TokenType.number, value));
    } else {
      throw const CalculatorException(
        CalcErrorType.invalidExpression,
        'Недопустимый символ в выражении',
      );
    }
  }

  return tokens;
}

void _validateStructure(List<_Token> tokens) {
  if (tokens.isEmpty) {
    throw const CalculatorException(
      CalcErrorType.emptyExpression,
      'Пустое выражение',
    );
  }

  var balance = 0;
  _TokenType? previous;

  for (final token in tokens) {
    if (token.type == _TokenType.lparen) {
      balance++;
      previous = token.type;
      continue;
    }
    if (token.type == _TokenType.rparen) {
      balance--;
      if (balance < 0) {
        throw const CalculatorException(
          CalcErrorType.unbalancedParentheses,
          'Неправильная расстановка скобок',
        );
      }
      previous = token.type;
      continue;
    }
    if (token.type == _TokenType.sign) {
      previous = token.type;
      continue;
    }

    if (_isBinaryOperator(token.type)) {
      final atStartOrAfterParen = previous == null || previous == _TokenType.lparen;
      final canBeUnary = token.type == _TokenType.plus || token.type == _TokenType.minus;

      if (atStartOrAfterParen) {
        if (!canBeUnary) {
          throw const CalculatorException(
            CalcErrorType.invalidExpression,
            'Некорректное выражение',
          );
        }
      } else if (_isBinaryOperator(previous)) {
        throw const CalculatorException(
          CalcErrorType.consecutiveOperators,
          'Два оператора подряд',
        );
      }
    }

    previous = token.type;
  }

  if (balance != 0) {
    throw const CalculatorException(
      CalcErrorType.unbalancedParentheses,
      'Неправильная расстановка скобок',
    );
  }

  final last = tokens.last.type;
  if (_isBinaryOperator(last) || last == _TokenType.sign) {
    throw const CalculatorException(
      CalcErrorType.trailingOperator,
      'Выражение не может заканчиваться оператором',
    );
  }
}

class _Parser {
  _Parser(this._tokens);

  final List<_Token> _tokens;
  int _pos = 0;

  _Token? get _current => _pos < _tokens.length ? _tokens[_pos] : null;

  void _advance() => _pos++;

  double parse() {
    final result = _parseExpression();
    if (_current != null) {
      throw const CalculatorException(
        CalcErrorType.invalidExpression,
        'Некорректное выражение',
      );
    }
    return result;
  }

  double _parseExpression() {
    var value = _parseTerm();
    while (_current != null &&
        (_current!.type == _TokenType.plus || _current!.type == _TokenType.minus)) {
      final op = _current!.type;
      _advance();
      final right = _parseTerm();
      value = op == _TokenType.plus ? value + right : value - right;
    }
    return value;
  }

  double _parseTerm() {
    var value = _parseFactor();
    while (_current != null &&
        (_current!.type == _TokenType.times || _current!.type == _TokenType.divide)) {
      final op = _current!.type;
      _advance();
      final right = _parseFactor();
      if (op == _TokenType.times) {
        value = value * right;
      } else {
        if (right == 0) {
          throw const CalculatorException(
            CalcErrorType.divisionByZero,
            'Деление на ноль',
          );
        }
        value = value / right;
      }
    }
    return value;
  }

  double _parseFactor() {
    final token = _current;
    if (token == null) {
      throw const CalculatorException(
        CalcErrorType.invalidExpression,
        'Некорректное выражение',
      );
    }

    double value;

    if (token.type == _TokenType.minus || token.type == _TokenType.sign) {
      _advance();
      value = -_parseFactor();
    } else if (token.type == _TokenType.plus) {
      _advance();
      value = _parseFactor();
    } else if (token.type == _TokenType.number) {
      value = token.value!;
      _advance();
    } else if (token.type == _TokenType.lparen) {
      _advance();
      value = _parseExpression();
      if (_current == null || _current!.type != _TokenType.rparen) {
        throw const CalculatorException(
          CalcErrorType.unbalancedParentheses,
          'Неправильная расстановка скобок',
        );
      }
      _advance();
    } else {
      throw const CalculatorException(
        CalcErrorType.invalidExpression,
        'Некорректное выражение',
      );
    }

    while (_current != null && _current!.type == _TokenType.percent) {
      value = value / 100;
      _advance();
    }

    return value;
  }
}

double evaluateExpression(String rawInput) {
  final input = rawInput.trim();

  try {
    final tokens = _tokenize(input);
    _validateStructure(tokens);
    final result = _Parser(tokens).parse();
    if (!result.isFinite) {
      throw const CalculatorException(
        CalcErrorType.invalidExpression,
        'Слишком большое число',
      );
    }
    return result;
  } on CalculatorException {
    rethrow;
  } catch (_) {
    throw const CalculatorException(
      CalcErrorType.invalidExpression,
      'Некорректное выражение',
    );
  }
}

String formatResult(double value) {
  if (value.isNaN || value.isInfinite) {
    return 'Ошибка';
  }
  if (value == value.roundToDouble() && value.abs() < 1e15) {
    return value.toStringAsFixed(0);
  }
  var text = value.toStringAsFixed(6);
  text = text.replaceFirst(RegExp(r'0+$'), '');
  text = text.replaceFirst(RegExp(r'\.$'), '');
  return text;
}

class HistoryEntry {
  final String expression;
  final String result;

  const HistoryEntry(this.expression, this.result);

  String encode() => '$expression|$result';

  static HistoryEntry decode(String raw) {
    final separatorIndex = raw.lastIndexOf('|');
    if (separatorIndex == -1) {
      return HistoryEntry(raw, '');
    }
    return HistoryEntry(
      raw.substring(0, separatorIndex),
      raw.substring(separatorIndex + 1),
    );
  }
}
