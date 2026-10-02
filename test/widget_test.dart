import 'package:advanced_calculator/controllers/calculator_controller.dart';
import 'package:advanced_calculator/main.dart';
import 'package:advanced_calculator/services/storage_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Future<CalculatorController> _controller() async {
  final controller = CalculatorController(storage: InMemoryStorageService());
  await controller.load();
  return controller;
}

void _setSurface(WidgetTester tester, Size size) {
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

void main() {
  testWidgets('calculator screen renders keypad and display', (tester) async {
    final controller = await _controller();

    await tester.pumpWidget(AdvancedCalculatorApp(controller: controller));
    await tester.pumpAndSettle();

    expect(find.text('Advanced Calculator'), findsOneWidget);
    expect(find.text('='), findsOneWidget);
    expect(find.text('C'), findsOneWidget);
    expect(find.text('M+'), findsOneWidget);
    expect(find.text('±'), findsOneWidget);
    expect(find.text('⌫'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('landscape and small portrait do not overflow', (tester) async {
    final controller = await _controller();
    await controller.onDigit('1');
    await controller.onDigit('2');
    await controller.onOperator('÷');
    await controller.onDigit('0');
    await controller.onEquals();

    for (final size in const [
      Size(823, 411),
      Size(640, 360),
      Size(320, 568),
    ]) {
      _setSurface(tester, size);
      await tester.pumpWidget(AdvancedCalculatorApp(controller: controller));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'overflow at $size');
      expect(find.text('Деление на ноль'), findsOneWidget);
      expect(find.text('M−'), findsOneWidget);
      expect(find.text('MC'), findsOneWidget);
    }
  });

  testWidgets('history sheet shows ten items without overflow', (tester) async {
    final controller = await _controller();
    for (var i = 1; i <= 10; i++) {
      await controller.onClear();
      await controller.onDigit('$i');
      await controller.onEquals();
    }

    _setSurface(tester, const Size(360, 640));
    await tester.pumpWidget(AdvancedCalculatorApp(controller: controller));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.history));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('10'), findsWidgets);
    expect(find.text('= 10'), findsOneWidget);
  });
}
