import 'package:advanced_calculator/models/calculator_error.dart';
import 'package:advanced_calculator/services/expression_evaluator.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late ExpressionEvaluator evaluator;

  setUp(() {
    evaluator = ExpressionEvaluator();
  });

  double eval(String expression) => evaluator.evaluate(expression);

  group('priority and parentheses', () {
    test('12 + 5 * 3 = 27', () {
      expect(eval('12 + 5 * 3'), 27);
      expect(eval('12 + 5 × 3'), 27);
    });

    test('(12 + 5) * 3 = 51', () {
      expect(eval('(12 + 5) * 3'), 51);
      expect(eval('(12 + 5) × 3'), 51);
    });

    test('15.5 * 2 = 31', () {
      expect(eval('15.5 * 2'), 31);
    });

    test('20 / 4 + 7 = 12', () {
      expect(eval('20 / 4 + 7'), 12);
      expect(eval('20 ÷ 4 + 7'), 12);
    });

    test('2 + 3 * 4 = 14', () {
      expect(eval('2 + 3 * 4'), 14);
    });

    test('(2 + 3) * 4 = 20', () {
      expect(eval('(2 + 3) * 4'), 20);
    });

    test('10 / 2 / 5 = 1', () {
      expect(eval('10 / 2 / 5'), 1);
    });

    test('nested parentheses', () {
      expect(eval('((2 + 3) * 4)'), 20);
    });
  });

  group('percent and negatives', () {
    test('percent converts operand to /100', () {
      expect(eval('50%'), 0.5);
      expect(eval('15%'), 0.15);
      expect(eval('200 * 50%'), 100);
    });

    test('negative numbers and unary minus', () {
      expect(eval('-5'), -5);
      expect(eval('−25'), -25);
      expect(eval('2 * -3'), -6);
      expect(eval('-5 + 3'), -2);
      expect(eval('12 + -5'), 7);
    });
  });

  group('formatting', () {
    test('drops trailing .0', () {
      expect(evaluator.format(27.0), '27');
      expect(evaluator.format(31.0), '31');
      expect(evaluator.format(0.5), '0.5');
      expect(evaluator.format(0.15), '0.15');
    });
  });

  group('errors', () {
    test('division by zero', () {
      expect(
        () => eval('10 / 0'),
        throwsA(isA<EvaluationException>().having(
          (error) => error.message,
          'message',
          'Деление на ноль',
        )),
      );
    });

    test('unbalanced parentheses', () {
      expect(
        () => eval('(2 + 3'),
        throwsA(isA<EvaluationException>().having(
          (error) => error.message,
          'message',
          'Неправильные скобки',
        )),
      );
      expect(
        () => eval('2 + 3)'),
        throwsA(isA<EvaluationException>().having(
          (error) => error.message,
          'message',
          'Неправильные скобки',
        )),
      );
    });

    test('consecutive operators', () {
      expect(
        () => eval('2 ++ 3'),
        throwsA(isA<EvaluationException>().having(
          (error) => error.message,
          'message',
          'Два оператора подряд',
        )),
      );
      expect(
        () => eval('2 ** 3'),
        throwsA(isA<EvaluationException>().having(
          (error) => error.message,
          'message',
          'Два оператора подряд',
        )),
      );
    });

    test('multiple decimal points', () {
      expect(
        () => eval('1.2.3'),
        throwsA(isA<EvaluationException>().having(
          (error) => error.message,
          'message',
          'Некорректное число',
        )),
      );
    });

    test('empty expression', () {
      expect(
        () => eval(''),
        throwsA(isA<EvaluationException>().having(
          (error) => error.message,
          'message',
          'Пустое выражение',
        )),
      );
      expect(
        () => eval('   '),
        throwsA(isA<EvaluationException>().having(
          (error) => error.message,
          'message',
          'Пустое выражение',
        )),
      );
    });

    test('trailing operator', () {
      expect(
        () => eval('5 +'),
        throwsA(isA<EvaluationException>().having(
          (error) => error.message,
          'message',
          'Выражение заканчивается оператором',
        )),
      );
    });
  });
}
