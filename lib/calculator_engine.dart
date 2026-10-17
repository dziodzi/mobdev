class CalculatorException implements Exception {
  const CalculatorException(this.message);
  final String message;

  @override
  String toString() => message;
}

class CalculatorEngine {
  static const _operators = <String>{'+', '-', '*', '/'};

  double evaluate(String expression) {
    if (expression.trim().isEmpty) {
      throw const CalculatorException('Enter an expression');
    }
    final tokens = _tokenize(expression);
    final postfix = _toPostfix(tokens);
    final value = _evaluatePostfix(postfix);
    if (!value.isFinite) {
      throw const CalculatorException('Result is not finite');
    }
    return value;
  }

  List<String> _tokenize(String source) {
    final input =
        source.replaceAll('×', '*').replaceAll('÷', '/').replaceAll('−', '-');
    final tokens = <String>[];
    var index = 0;
    String? previous;

    while (index < input.length) {
      final char = input[index];
      if (char.trim().isEmpty) {
        index++;
        continue;
      }

      final unaryMinus = char == '-' &&
          (previous == null ||
              _operators.contains(previous) ||
              previous == '(');
      if (_isDigit(char) || char == '.' || unaryMinus) {
        final buffer = StringBuffer();
        if (unaryMinus) {
          buffer.write('-');
          index++;
          if (index >= input.length) {
            throw const CalculatorException(
                'Expression cannot end with an operator');
          }
          if (input[index] == '(') {
            tokens.add('-1');
            tokens.add('*');
            previous = '*';
            continue;
          }
        }
        var dots = 0;
        while (index < input.length &&
            (_isDigit(input[index]) || input[index] == '.')) {
          if (input[index] == '.' && ++dots > 1) {
            throw const CalculatorException(
                'A number cannot contain multiple decimal points');
          }
          buffer.write(input[index++]);
        }
        final number = buffer.toString();
        if (number == '-' || number == '.' || number == '-.') {
          throw const CalculatorException('Invalid number');
        }
        tokens.add(number);
        previous = number;
        continue;
      }

      if (_operators.contains(char) ||
          char == '(' ||
          char == ')' ||
          char == '%') {
        if (_operators.contains(char) &&
            (previous == null ||
                _operators.contains(previous) ||
                previous == '(')) {
          throw const CalculatorException('Two operators cannot be adjacent');
        }
        if (char == '%' &&
            (previous == null ||
                _operators.contains(previous) ||
                previous == '(')) {
          throw const CalculatorException('Percent must follow a number');
        }
        tokens.add(char);
        previous = char;
        index++;
        continue;
      }
      throw CalculatorException('Unsupported character: $char');
    }
    if (previous != null && _operators.contains(previous)) {
      throw const CalculatorException('Expression cannot end with an operator');
    }
    return tokens;
  }

  List<String> _toPostfix(List<String> tokens) {
    final output = <String>[];
    final stack = <String>[];
    for (final token in tokens) {
      if (double.tryParse(token) != null) {
        output.add(token);
      } else if (token == '%') {
        output.add(token);
      } else if (_operators.contains(token)) {
        while (stack.isNotEmpty &&
            _operators.contains(stack.last) &&
            _precedence(stack.last) >= _precedence(token)) {
          output.add(stack.removeLast());
        }
        stack.add(token);
      } else if (token == '(') {
        stack.add(token);
      } else if (token == ')') {
        var foundOpening = false;
        while (stack.isNotEmpty) {
          final top = stack.removeLast();
          if (top == '(') {
            foundOpening = true;
            break;
          }
          output.add(top);
        }
        if (!foundOpening) {
          throw const CalculatorException('Parentheses do not match');
        }
      }
    }
    while (stack.isNotEmpty) {
      final token = stack.removeLast();
      if (token == '(') {
        throw const CalculatorException('Parentheses do not match');
      }
      output.add(token);
    }
    return output;
  }

  double _evaluatePostfix(List<String> tokens) {
    final values = <double>[];
    for (final token in tokens) {
      final number = double.tryParse(token);
      if (number != null) {
        values.add(number);
      } else if (token == '%') {
        if (values.isEmpty) {
          throw const CalculatorException('Invalid percent operation');
        }
        values[values.length - 1] /= 100;
      } else {
        if (values.length < 2) {
          throw const CalculatorException('Incomplete expression');
        }
        final right = values.removeLast();
        final left = values.removeLast();
        switch (token) {
          case '+':
            values.add(left + right);
            break;
          case '-':
            values.add(left - right);
            break;
          case '*':
            values.add(left * right);
            break;
          case '/':
            if (right == 0) throw const CalculatorException('Division by zero');
            values.add(left / right);
            break;
        }
      }
    }
    if (values.length != 1) {
      throw const CalculatorException('Invalid expression');
    }
    return values.single;
  }

  int _precedence(String operator) =>
      (operator == '*' || operator == '/') ? 2 : 1;
  bool _isDigit(String value) =>
      value.codeUnitAt(0) >= 48 && value.codeUnitAt(0) <= 57;
}

String formatNumber(double value) {
  if (value == value.roundToDouble()) return value.toInt().toString();
  return value
      .toStringAsFixed(10)
      .replaceFirst(RegExp(r'0+$'), '')
      .replaceFirst(RegExp(r'\.$'), '');
}
