import 'package:flutter_test/flutter_test.dart';
import 'package:lab2_contacts/controllers/contact_controller.dart';
import 'package:lab2_contacts/models/contact_record.dart';
import 'package:lab2_contacts/services/contact_repository.dart';

class FakeContactRepository implements ContactRepository {
  ContactPermission permission = ContactPermission.granted;
  bool fail = false;
  int loadCount = 0;
  List<ContactRecord> contacts = [
    const ContactRecord(id: '1', name: 'Zoe', phones: ['123']),
    const ContactRecord(id: '2', name: 'Alice', emails: ['alice@test.dev']),
    const ContactRecord(id: '3', name: 'Albert'),
  ];

  @override
  Future<ContactPermission> checkPermission() async => permission;

  @override
  Future<ContactPermission> requestPermission() async => permission;

  @override
  Future<bool> openSettings() async => true;

  @override
  Future<List<ContactRecord>> loadContacts() async {
    loadCount++;
    if (fail) throw Exception('Read error');
    return contacts;
  }
}

void main() {
  test('search and sort use the cached contact list', () async {
    final repository = FakeContactRepository();
    final controller = ContactController(repository);

    await controller.initialize();
    controller.updateQuery('al');
    expect(controller.visibleContacts.map((contact) => contact.name).toList(), [
      'Albert',
      'Alice',
    ]);

    controller.updateSort(false);
    expect(controller.visibleContacts.map((contact) => contact.name).toList(), [
      'Alice',
      'Albert',
    ]);
    expect(repository.loadCount, 1);
  });

  test('denied permission does not load contacts', () async {
    final repository = FakeContactRepository()
      ..permission = ContactPermission.denied;
    final controller = ContactController(repository);

    await controller.initialize();

    expect(controller.state, ContactsState.denied);
    expect(repository.loadCount, 0);
  });

  test(
    'returning from settings loads contacts after permission grant',
    () async {
      final repository = FakeContactRepository()
        ..permission = ContactPermission.denied;
      final controller = ContactController(repository);
      await controller.initialize();

      repository.permission = ContactPermission.granted;
      await controller.checkAfterSettings();

      expect(controller.state, ContactsState.ready);
      expect(repository.loadCount, 1);
    },
  );

  test('read failure changes state to error', () async {
    final repository = FakeContactRepository()..fail = true;
    final controller = ContactController(repository);

    await controller.initialize();

    expect(controller.state, ContactsState.error);
  });

  test('empty address book is different from denied permission', () async {
    final repository = FakeContactRepository()..contacts = [];
    final controller = ContactController(repository);

    await controller.initialize();

    expect(controller.state, ContactsState.ready);
    expect(controller.totalContacts, 0);
  });
}
