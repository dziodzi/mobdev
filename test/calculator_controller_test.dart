import 'package:advanced_calculator/controllers/calculator_controller.dart';
import 'package:advanced_calculator/models/calculation_history_item.dart';
import 'package:advanced_calculator/models/calculator_persisted_state.dart';
import 'package:advanced_calculator/services/storage_service.dart';
import 'package:flutter_test/flutter_test.dart';

Future<CalculatorController> buildController({
  CalculatorPersistedState? initial,
}) async {
  final storage = InMemoryStorageService();
  if (initial != null) {
    await storage.saveAll(initial);
  }
  final controller = CalculatorController(storage: storage);
  await controller.load();
  return controller;
}

void main() {
  test('plus-minus toggles the current operand', () async {
    final controller = await buildController();
    await controller.onDigit('2');
    await controller.onDigit('5');
    await controller.onPlusMinus();
    expect(controller.expression, '−25');
    await controller.onPlusMinus();
    expect(controller.expression, '25');
  });

  test('plus-minus changes only the last operand', () async {
    final controller = await buildController();
    await controller.onDigit('1');
    await controller.onDigit('2');
    await controller.onOperator('+');
    await controller.onDigit('3');
    await controller.onDigit('5');
    await controller.onPlusMinus();
    expect(controller.expression, '12+−35');
    await controller.onEquals();
    expect(controller.lastResult, '-23');
  });

  test('history keeps only 10 latest items', () async {
    final controller = await buildController();
    for (var i = 1; i <= 12; i++) {
      await controller.onClear();
      await controller.onDigit('$i');
      await controller.onEquals();
    }
    expect(controller.history.length, 10);
    expect(controller.history.first.expression, '12');
    expect(controller.history.last.expression, '3');
  });

  test('history item can be loaded and recalculated', () async {
    final controller = await buildController();
    await controller.onDigit('2');
    await controller.onOperator('+');
    await controller.onDigit('2');
    await controller.onEquals();
    await controller.onClear();
    await controller.loadHistoryItem(controller.history.first);
    expect(controller.expression, '2+2');
    await controller.onEquals();
    expect(controller.lastResult, '4');
  });

  test('memory add subtract recall and clear', () async {
    final controller = await buildController();
    await controller.onDigit('1');
    await controller.onDigit('0');
    await controller.onEquals();
    await controller.memoryAdd();
    expect(controller.memory, 10);

    await controller.onClear();
    await controller.onDigit('5');
    await controller.onEquals();
    await controller.memoryAdd();
    expect(controller.memory, 15);

    await controller.memorySubtract();
    expect(controller.memory, 10);

    await controller.onClear();
    await controller.memoryRecall();
    expect(controller.expression, '10');

    await controller.memoryClear();
    expect(controller.memory, 0);
  });

  test('memory operations do not crash without a value', () async {
    final controller = await buildController();
    await controller.memoryAdd();
    await controller.memorySubtract();
    expect(controller.memory, 0);
  });

  test('clear resets input but keeps memory and history', () async {
    final controller = await buildController();
    await controller.onDigit('8');
    await controller.onEquals();
    await controller.memoryAdd();
    await controller.onClear();
    expect(controller.expression, isEmpty);
    expect(controller.lastResult, isEmpty);
    expect(controller.error, isEmpty);
    expect(controller.memory, 8);
    expect(controller.history, isNotEmpty);
  });

  test('backspace removes the last character', () async {
    final controller = await buildController();
    await controller.onDigit('1');
    await controller.onDigit('2');
    await controller.onOperator('+');
    await controller.onDigit('3');
    await controller.onDigit('5');
    await controller.onBackspace();
    expect(controller.expression, '12+3');
    await controller.onClear();
    await controller.onBackspace();
    expect(controller.expression, isEmpty);
  });

  test('equals stores error messages without throwing', () async {
    final controller = await buildController();
    await controller.onDigit('1');
    await controller.onDigit('2');
    await controller.onOperator('÷');
    await controller.onDigit('0');
    await controller.onEquals();
    expect(controller.error, 'Деление на ноль');
    expect(controller.hasError, isTrue);
  });

  test('restores persisted state', () async {
    final storage = InMemoryStorageService();
    await storage.saveAll(
      const CalculatorPersistedState(
        expression: '12+5×3',
        lastResult: '27',
        error: 'Деление на ноль',
        memory: 42,
        history: [
          CalculationHistoryItem(expression: '1+1', result: '2'),
        ],
      ),
    );

    final controller = CalculatorController(storage: storage);
    await controller.load();

    expect(controller.expression, '12+5×3');
    expect(controller.lastResult, '27');
    expect(controller.error, 'Деление на ноль');
    expect(controller.memory, 42);
    expect(controller.history.single.expression, '1+1');
  });

  test('digit after result starts a new expression', () async {
    final controller = await buildController();
    await controller.onDigit('2');
    await controller.onOperator('+');
    await controller.onDigit('2');
    await controller.onEquals();
    await controller.onDigit('9');
    expect(controller.expression, '9');
  });

  test('operator after result continues from last result', () async {
    final controller = await buildController();
    await controller.onDigit('2');
    await controller.onOperator('+');
    await controller.onDigit('2');
    await controller.onEquals();
    await controller.onOperator('+');
    expect(controller.expression, '4+');
  });
}
