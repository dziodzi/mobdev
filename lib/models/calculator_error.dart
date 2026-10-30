/// User-facing evaluation errors. Never thrown uncaught from the evaluator.
class EvaluationException implements Exception {
  const EvaluationException(this.message);

  final String message;

  static const emptyExpression = EvaluationException('Пустое выражение');
  static const divisionByZero = EvaluationException('Деление на ноль');
  static const invalidParentheses = EvaluationException('Неправильные скобки');
  static const consecutiveOperators =
      EvaluationException('Два оператора подряд');
  static const invalidNumber = EvaluationException('Некорректное число');
  static const trailingOperator =
      EvaluationException('Выражение заканчивается оператором');
  static const invalidExpression =
      EvaluationException('Некорректное выражение');

  @override
  String toString() => message;
}
