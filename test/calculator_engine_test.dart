import 'package:advanced_calculator/calculator_engine.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  final engine = CalculatorEngine();

  group('CalculatorEngine', () {
    test('respects operation precedence', () {
      expect(engine.evaluate('12 + 5 * 3'), 27);
    });

    test('respects parentheses', () {
      expect(engine.evaluate('(12 + 5) * 3'), 51);
    });

    test('supports decimals, unary minus and percent', () {
      expect(engine.evaluate('-15.5 * 2'), -31);
      expect(engine.evaluate('200 * 10%'), 20);
    });

    test('rejects division by zero', () {
      expect(
          () => engine.evaluate('10 / 0'), throwsA(isA<CalculatorException>()));
    });

    test('rejects malformed expressions', () {
      for (final expression in ['', '(2 + 3', '2 + * 3', '1.2.3', '2 +']) {
        expect(() => engine.evaluate(expression),
            throwsA(isA<CalculatorException>()));
      }
    });
  });
}
