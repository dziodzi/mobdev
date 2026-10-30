class CalculationHistoryItem {
  const CalculationHistoryItem({
    required this.expression,
    required this.result,
  });

  final String expression;
  final String result;

  Map<String, dynamic> toJson() => {
        'expression': expression,
        'result': result,
      };

  factory CalculationHistoryItem.fromJson(Map<String, dynamic> json) {
    return CalculationHistoryItem(
      expression: json['expression'] as String? ?? '',
      result: json['result'] as String? ?? '',
    );
  }
}
