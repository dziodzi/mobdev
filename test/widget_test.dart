import 'package:flutter_test/flutter_test.dart';

import 'package:lab1_calculator/calculator_logic.dart';

void main() {
  group('Вычисление выражений', () {
    test('учитывает приоритет операций', () {
      expect(evaluateExpression('12+5×3'), 27);
    });

    test('учитывает скобки', () {
      expect(evaluateExpression('(12+5)×3'), 51);
    });

    test('работает с вещественными числами, процентами и сменой знака', () {
      expect(evaluateExpression('15.5×2'), 31);
      expect(evaluateExpression('50%'), 0.5);
      expect(evaluateExpression('2×-3'), -6);
    });
  });

  group('Ошибки ввода', () {
    void expectError(String input, CalcErrorType type) {
      expect(
        () => evaluateExpression(input),
        throwsA(
          isA<CalculatorException>().having((error) => error.type, 'type', type),
        ),
      );
    }

    test('пустое выражение', () => expectError('', CalcErrorType.emptyExpression));
    test('деление на ноль', () => expectError('5÷0', CalcErrorType.divisionByZero));
    test('деление на нулевой результат в скобках', () {
      expectError('5÷(2−2)', CalcErrorType.divisionByZero);
    });
    test('непарные скобки', () => expectError('(2+3', CalcErrorType.unbalancedParentheses));
    test('два оператора подряд', () => expectError('2×÷3', CalcErrorType.consecutiveOperators));
    test('две точки в числе', () => expectError('1.2.3', CalcErrorType.multipleDecimalPoints));
    test('оператор в конце', () => expectError('2+', CalcErrorType.trailingOperator));
    test('пустые скобки', () => expectError('()', CalcErrorType.invalidExpression));
    test('скобки сразу после числа', () => expectError('2(3)', CalcErrorType.invalidExpression));
    test('одна точка без цифры', () => expectError('.', CalcErrorType.invalidExpression));
  });
}
