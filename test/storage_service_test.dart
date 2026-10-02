import 'package:advanced_calculator/models/calculation_history_item.dart';
import 'package:advanced_calculator/models/calculator_persisted_state.dart';
import 'package:advanced_calculator/services/storage_service.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('in-memory storage saves and loads calculator state', () async {
    final storage = InMemoryStorageService();
    const state = CalculatorPersistedState(
      expression: '(12+5)×3',
      lastResult: '51',
      error: 'Неправильные скобки',
      memory: 8.5,
      history: [
        CalculationHistoryItem(expression: '20÷4+7', result: '12'),
      ],
      justEvaluated: true,
    );

    await storage.saveAll(state);
    await storage.saveExpression('(12+5)×3');
    await storage.saveLastResult('51');
    await storage.saveError('Неправильные скобки');
    await storage.saveMemory(8.5);
    await storage.saveHistory(state.history);
    await storage.saveJustEvaluated(true);

    final loaded = await storage.loadAll();
    expect(loaded.expression, '(12+5)×3');
    expect(loaded.lastResult, '51');
    expect(loaded.error, 'Неправильные скобки');
    expect(loaded.memory, 8.5);
    expect(loaded.justEvaluated, isTrue);
    expect(loaded.history.single.result, '12');
  });

  test('shared preferences restores expression, history, memory and error', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final storage = SharedPreferencesStorageService(preferences: prefs);
    const state = CalculatorPersistedState(
      expression: '12+5×3',
      lastResult: '27',
      error: 'Деление на ноль',
      memory: 15,
      history: [
        CalculationHistoryItem(expression: '(12 + 5) × 3', result: '51'),
      ],
      justEvaluated: true,
    );

    await storage.saveAll(state);

    final restored = SharedPreferencesStorageService(
      preferences: await SharedPreferences.getInstance(),
    );
    final loaded = await restored.loadAll();

    expect(loaded.expression, '12+5×3');
    expect(loaded.lastResult, '27');
    expect(loaded.error, 'Деление на ноль');
    expect(loaded.memory, 15);
    expect(loaded.justEvaluated, isTrue);
    expect(loaded.history.single.expression, '(12 + 5) × 3');
    expect(loaded.history.single.result, '51');
  });
}
