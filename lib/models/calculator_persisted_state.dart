import 'calculation_history_item.dart';

class CalculatorPersistedState {
  const CalculatorPersistedState({
    this.expression = '',
    this.lastResult = '',
    this.error = '',
    this.memory = 0,
    this.history = const [],
    this.justEvaluated = false,
  });

  final String expression;
  final String lastResult;
  final String error;
  final double memory;
  final List<CalculationHistoryItem> history;
  final bool justEvaluated;
}
