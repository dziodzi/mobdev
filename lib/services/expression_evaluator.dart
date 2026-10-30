import '../models/calculator_error.dart';

enum _TokenType {
  number,
  plus,
  minus,
  star,
  slash,
  lparen,
  rparen,
  percent,
  eof,
}

class _Token {
  const _Token(this.type, this.lexeme, {this.value});

  final _TokenType type;
  final String lexeme;
  final double? value;

  bool get isBinaryOperator =>
      type == _TokenType.plus ||
      type == _TokenType.minus ||
      type == _TokenType.star ||
      type == _TokenType.slash;
}

/// Recursive-descent evaluator with operator precedence.
///
/// expression -> term ((+ | -) term)*
/// term       -> unary ((* | /) unary)*
/// unary      -> - unary | postfix
/// postfix    -> primary (%)*
/// primary    -> number | ( expression )
class ExpressionEvaluator {
  ExpressionEvaluator();

  late List<_Token> _tokens;
  int _current = 0;

  /// Evaluates an arithmetic expression and returns a [double].
  /// UI symbols × ÷ − are normalized to * / -.
  double evaluate(String rawExpression) {
    final source = _normalize(rawExpression).trim();
    if (source.isEmpty) {
      throw EvaluationException.emptyExpression;
    }

    _tokens = _tokenize(source);
    _current = 0;

    final value = _parseExpression();
    if (!_isAtEnd) {
      final leftover = _peek();
      if (leftover.type == _TokenType.rparen) {
        throw EvaluationException.invalidParentheses;
      }
      throw EvaluationException.invalidExpression;
    }
    if (value.isInfinite || value.isNaN) {
      throw EvaluationException.divisionByZero;
    }
    return value;
  }

  String format(double value) {
    if (value.isNaN) {
      return 'Некорректное выражение';
    }
    if (value.isInfinite) {
      return 'Деление на ноль';
    }

    final rounded = _roundForDisplay(value);
    if (rounded == 0) {
      return '0';
    }

    final absValue = rounded.abs();
    // Avoid scientific notation for ordinary calculator results.
    if (absValue >= 1e12 || (absValue < 1e-8 && absValue != 0)) {
      return rounded.toString();
    }

    if (_isInteger(rounded)) {
      return rounded.toStringAsFixed(0);
    }

    var text = rounded.toStringAsFixed(10);
    text = text.replaceFirst(RegExp(r'0+$'), '');
    text = text.replaceFirst(RegExp(r'\.$'), '');
    return text;
  }

  double _roundForDisplay(double value) {
    return (value * 1e12).roundToDouble() / 1e12;
  }

  bool _isInteger(double value) {
    return (value - value.roundToDouble()).abs() < 1e-10;
  }

  String _normalize(String input) {
    return input
        .replaceAll('×', '*')
        .replaceAll('÷', '/')
        .replaceAll('−', '-')
        .replaceAll('–', '-')
        .replaceAll('—', '-')
        .replaceAll(' ', '');
  }

  List<_Token> _tokenize(String source) {
    final tokens = <_Token>[];
    var i = 0;

    while (i < source.length) {
      final char = source[i];

      if (_isDigit(char) || char == '.') {
        final start = i;
        var dotCount = char == '.' ? 1 : 0;
        i++;
        while (i < source.length &&
            (_isDigit(source[i]) || source[i] == '.')) {
          if (source[i] == '.') {
            dotCount++;
            if (dotCount > 1) {
              throw EvaluationException.invalidNumber;
            }
          }
          i++;
        }
        final lexeme = source.substring(start, i);
        if (lexeme == '.' || lexeme.isEmpty) {
          throw EvaluationException.invalidNumber;
        }
        final value = double.tryParse(lexeme);
        if (value == null) {
          throw EvaluationException.invalidNumber;
        }
        tokens.add(_Token(_TokenType.number, lexeme, value: value));
        continue;
      }

      switch (char) {
        case '+':
          tokens.add(const _Token(_TokenType.plus, '+'));
        case '-':
          tokens.add(const _Token(_TokenType.minus, '-'));
        case '*':
          tokens.add(const _Token(_TokenType.star, '*'));
        case '/':
          tokens.add(const _Token(_TokenType.slash, '/'));
        case '(':
          tokens.add(const _Token(_TokenType.lparen, '('));
        case ')':
          tokens.add(const _Token(_TokenType.rparen, ')'));
        case '%':
          tokens.add(const _Token(_TokenType.percent, '%'));
        default:
          throw EvaluationException.invalidExpression;
      }
      i++;
    }

    _validateTokenSequence(tokens);
    tokens.add(const _Token(_TokenType.eof, ''));
    return tokens;
  }

  void _validateTokenSequence(List<_Token> tokens) {
    if (tokens.isEmpty) {
      throw EvaluationException.emptyExpression;
    }

    var depth = 0;
    for (var i = 0; i < tokens.length; i++) {
      final token = tokens[i];
      final previous = i > 0 ? tokens[i - 1] : null;

      if (token.type == _TokenType.lparen) {
        depth++;
      } else if (token.type == _TokenType.rparen) {
        depth--;
        if (depth < 0) {
          throw EvaluationException.invalidParentheses;
        }
      }

      if (token.isBinaryOperator &&
          previous != null &&
          previous.isBinaryOperator) {
        // Allow unary minus after another operator: 2 * -3
        final isUnaryMinus = token.type == _TokenType.minus;
        if (!isUnaryMinus) {
          throw EvaluationException.consecutiveOperators;
        }
      }

      if (token.type == _TokenType.percent &&
          previous != null &&
          previous.type == _TokenType.percent) {
        throw EvaluationException.invalidExpression;
      }
    }

    if (depth != 0) {
      throw EvaluationException.invalidParentheses;
    }

    final last = tokens.last;
    if (last.isBinaryOperator) {
      throw EvaluationException.trailingOperator;
    }
    if (last.type == _TokenType.lparen) {
      throw EvaluationException.invalidParentheses;
    }
  }

  bool _isDigit(String char) =>
      char.codeUnitAt(0) >= 48 && char.codeUnitAt(0) <= 57;

  double _parseExpression() {
    var value = _parseTerm();
    while (_match(_TokenType.plus) || _match(_TokenType.minus)) {
      final op = _previous();
      final right = _parseTerm();
      value = op.type == _TokenType.plus ? value + right : value - right;
    }
    return value;
  }

  double _parseTerm() {
    var value = _parseUnary();
    while (_match(_TokenType.star) || _match(_TokenType.slash)) {
      final op = _previous();
      final right = _parseUnary();
      if (op.type == _TokenType.star) {
        value *= right;
      } else {
        if (right == 0) {
          throw EvaluationException.divisionByZero;
        }
        value /= right;
      }
    }
    return value;
  }

  double _parseUnary() {
    if (_match(_TokenType.minus)) {
      return -_parseUnary();
    }
    if (_match(_TokenType.plus)) {
      // Unary plus is treated as a consecutive-operator error.
      throw EvaluationException.consecutiveOperators;
    }
    return _parsePostfix();
  }

  double _parsePostfix() {
    var value = _parsePrimary();
    while (_match(_TokenType.percent)) {
      value /= 100;
    }
    return value;
  }

  double _parsePrimary() {
    if (_match(_TokenType.number)) {
      return _previous().value!;
    }
    if (_match(_TokenType.lparen)) {
      final value = _parseExpression();
      if (!_match(_TokenType.rparen)) {
        throw EvaluationException.invalidParentheses;
      }
      return value;
    }
    throw EvaluationException.invalidExpression;
  }

  bool get _isAtEnd => _peek().type == _TokenType.eof;

  _Token _peek() => _tokens[_current];

  _Token _previous() => _tokens[_current - 1];

  bool _match(_TokenType type) {
    if (_isAtEnd) {
      return false;
    }
    if (_peek().type != type) {
      return false;
    }
    _current++;
    return true;
  }
}
