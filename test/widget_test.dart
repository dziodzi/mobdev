import 'package:advanced_calculator/main.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('calculator evaluates a full expression', (tester) async {
    await tester.pumpWidget(const CalculatorApp());
    for (final key in ['1', '2', '+', '5', '×', '3', '=']) {
      await tester.tap(find.text(key).first);
    }
    await tester.pump();
    expect(find.text('27'), findsOneWidget);
    expect(find.text('= 27'), findsOneWidget);
  });
}
