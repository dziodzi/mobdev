import 'package:flutter_test/flutter_test.dart';

import 'package:advanced_calculator/main.dart';

void main() {
  testWidgets('calculator screen renders core controls', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MyCalculatorApp());

    expect(find.text('ADVANCED CALCULATOR'), findsOneWidget);
    expect(find.text('7'), findsOneWidget);
    expect(find.text('M+'), findsOneWidget);
    expect(find.text('='), findsOneWidget);
    expect(find.text('HISTORY'), findsOneWidget);
  });
}
