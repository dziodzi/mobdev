import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lab2_contacts/main.dart';
import 'package:lab2_contacts/models/contact_record.dart';
import 'package:lab2_contacts/services/contact_repository.dart';

class PageTestRepository implements ContactRepository {
  int loadCount = 0;

  @override
  Future<ContactPermission> checkPermission() async =>
      ContactPermission.granted;

  @override
  Future<ContactPermission> requestPermission() async =>
      ContactPermission.granted;

  @override
  Future<bool> openSettings() async => true;

  @override
  Future<List<ContactRecord>> loadContacts() async {
    loadCount++;
    return const [
      ContactRecord(id: '1', name: 'Alice', phones: ['111']),
      ContactRecord(id: '2', name: 'Albert', emails: ['albert@test.dev']),
      ContactRecord(id: '3', name: 'Zoe'),
    ];
  }
}

void main() {
  testWidgets('search and sort survive rotation without reloading contacts', (
    tester,
  ) async {
    final repository = PageTestRepository();
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(ContactsApp(repository: repository));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), 'al');
    await tester.pump();
    await tester.tap(find.byType(DropdownButton<bool>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Z to A').last);
    await tester.pumpAndSettle();

    await tester.binding.setSurfaceSize(const Size(900, 500));
    await tester.pumpAndSettle();

    expect(find.text('al'), findsOneWidget);
    expect(find.text('Z to A'), findsOneWidget);
    expect(find.text('Zoe'), findsNothing);
    expect(repository.loadCount, 1);
  });
}
