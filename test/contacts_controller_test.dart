import 'package:devicecontacts/controllers/contacts_controller.dart';
import 'package:devicecontacts/models/contact_access_state.dart';
import 'package:devicecontacts/models/contact_details.dart';
import 'package:devicecontacts/models/contact_sort_order.dart';
import 'package:devicecontacts/models/contacts_error.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fake_contacts_repository.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeContactsRepository repository;
  late ContactsController controller;

  setUp(() {
    repository = FakeContactsRepository();
    controller = ContactsController(repository);
  });

  tearDown(() {
    controller.dispose();
  });

  Future<void> grantAndLoad() async {
    repository.permission = ContactAccessStatus.granted;
    repository.summaries = [
      testSummary('1', 'Анна', phone: '111', hasEmail: true),
      testSummary('2', 'Борис'),
    ];
    await controller.initialize();
  }

  group('разрешения', () {
    test('granted загружает контакты', () async {
      await grantAndLoad();
      expect(controller.permissionStatus, ContactAccessStatus.granted);
      expect(controller.hasLoadedContacts, isTrue);
      expect(controller.contacts, hasLength(2));
      expect(repository.getAllCount, 1);
      expect(controller.errorMessage, isNull);
    });

    test('denied не читает адресную книгу', () async {
      repository.permission = ContactAccessStatus.denied;
      await controller.initialize();
      expect(controller.permissionStatus, ContactAccessStatus.denied);
      expect(controller.hasLoadedContacts, isFalse);
      expect(controller.contacts, isEmpty);
      expect(repository.getAllCount, 0);
    });

    test('permanentlyDenied не читает адресную книгу', () async {
      repository.permission = ContactAccessStatus.permanentlyDenied;
      await controller.initialize();
      expect(controller.permissionStatus.requiresSettings, isTrue);
      expect(repository.getAllCount, 0);
      await controller.requestPermission();
      expect(repository.requestCount, 0);
    });

    test('restricted не читает адресную книгу', () async {
      repository.permission = ContactAccessStatus.restricted;
      await controller.initialize();
      expect(controller.permissionStatus, ContactAccessStatus.restricted);
      expect(repository.getAllCount, 0);
    });

    test('limited считается разрешённым ограниченным доступом', () async {
      repository.permission = ContactAccessStatus.limited;
      repository.summaries = [testSummary('1', 'Анна')];
      await controller.initialize();
      expect(controller.permissionStatus, ContactAccessStatus.limited);
      expect(controller.isLimitedAccess, isTrue);
      expect(controller.contacts, hasLength(1));
      expect(repository.getAllCount, 1);
    });

    test('notDetermined не читает адресную книгу', () async {
      repository.permission = ContactAccessStatus.notDetermined;
      await controller.initialize();
      expect(controller.permissionStatus, ContactAccessStatus.notDetermined);
      expect(repository.getAllCount, 0);
    });

    test('запрос после отказа загружает контакты один раз', () async {
      repository.permission = ContactAccessStatus.denied;
      await controller.initialize();
      repository.requestResult = ContactAccessStatus.granted;
      repository.summaries = [testSummary('1', 'Анна')];
      await controller.requestPermission();
      expect(controller.permissionStatus, ContactAccessStatus.granted);
      expect(controller.contacts, hasLength(1));
      expect(repository.getAllCount, 1);
    });
  });

  group('кэш', () {
    test('поиск не вызывает повторную загрузку', () async {
      await grantAndLoad();
      controller.searchContacts('ан');
      controller.searchContacts('бор');
      controller.searchContacts('');
      expect(repository.getAllCount, 1);
      expect(controller.fullLoadCount, 1);
      expect(controller.foundCount, 2);
    });

    test('сортировка не вызывает повторную загрузку', () async {
      await grantAndLoad();
      controller.changeSortOrder(ContactSortOrder.descending);
      controller.changeSortOrder(ContactSortOrder.ascending);
      expect(repository.getAllCount, 1);
    });

    test('детали не перечитывают всю книгу', () async {
      await grantAndLoad();
      repository.detailsById['1'] = const ContactDetails(
        id: '1',
        displayName: 'Анна',
        name: ContactNameParts(first: 'Анна'),
        phones: [],
        emails: [],
        addresses: [],
        organizations: [],
        websites: [],
        socialMedias: [],
        events: [],
        relations: [],
        notes: [],
        isFavorite: null,
        photoBytes: null,
      );
      final details = await controller.getContactDetails('1');
      expect(details?.displayName, 'Анна');
      expect(repository.getAllCount, 1);
      expect(repository.getDetailsCount, 1);
      expect(controller.isLoadingDetails, isFalse);
    });

    test('удалённый контакт не роняет приложение', () async {
      await grantAndLoad();
      final details = await controller.getContactDetails('missing');
      expect(details, isNull);
      expect(controller.detailsError, ContactsErrorKind.detailsNotFound);
      expect(controller.contacts, hasLength(2));
    });

    test('ошибка деталей не очищает список', () async {
      await grantAndLoad();
      repository.throwOnDetails = true;
      final details = await controller.getContactDetails('1');
      expect(details, isNull);
      expect(controller.detailsError, ContactsErrorKind.detailsFailed);
      expect(controller.hasLoadedContacts, isTrue);
    });

    test('возврат с тем же разрешением не загружает книгу снова', () async {
      await grantAndLoad();
      await controller.onAppResumed();
      expect(repository.getAllCount, 1);
      expect(repository.checkCount, 2);
    });

    test('восстановление разрешения загружает контакты', () async {
      repository.permission = ContactAccessStatus.denied;
      await controller.initialize();
      repository.permission = ContactAccessStatus.granted;
      repository.summaries = [testSummary('1', 'Анна')];
      await controller.onAppResumed();
      expect(controller.hasLoadedContacts, isTrue);
      expect(repository.getAllCount, 1);
    });

    test('отзыв разрешения очищает кэш', () async {
      await grantAndLoad();
      repository.permission = ContactAccessStatus.permanentlyDenied;
      await controller.onAppResumed();
      expect(controller.hasLoadedContacts, isFalse);
      expect(controller.contacts, isEmpty);
      expect(controller.permissionStatus, ContactAccessStatus.permanentlyDenied);
    });

    test('смена полного доступа на ограниченный обновляет данные', () async {
      await grantAndLoad();
      repository.permission = ContactAccessStatus.limited;
      repository.summaries = [testSummary('9', 'Только выбранный')];
      await controller.onAppResumed();
      expect(controller.contacts.map((item) => item.id), ['9']);
      expect(repository.getAllCount, 2);
    });

    test('ошибка чтения не роняет контроллер и даёт повторить', () async {
      repository.permission = ContactAccessStatus.granted;
      repository.throwOnGetAll = true;
      await controller.initialize();
      expect(controller.errorKind, ContactsErrorKind.loadFailed);
      expect(controller.hasLoadedContacts, isFalse);
      repository.throwOnGetAll = false;
      repository.summaries = [testSummary('1', 'Анна')];
      await controller.refreshContacts();
      expect(controller.hasLoadedContacts, isTrue);
      expect(controller.errorMessage, isNull);
    });
  });

  group('состояние поиска и сортировки', () {
    test('запрос и сортировка сохраняются независимо от загрузки', () async {
      await grantAndLoad();
      controller.searchContacts('ан');
      controller.changeSortOrder(ContactSortOrder.descending);
      expect(controller.searchQuery, 'ан');
      expect(controller.sortOrder, ContactSortOrder.descending);
      expect(controller.foundCount, 1);
      expect(controller.filteredContacts.single.displayName, 'Анна');
    });

    test('пустая книга отличается от отсутствия доступа', () async {
      repository.permission = ContactAccessStatus.granted;
      repository.summaries = const [];
      await controller.initialize();
      expect(controller.permissionStatus.canReadContacts, isTrue);
      expect(controller.hasLoadedContacts, isTrue);
      expect(controller.contacts, isEmpty);
      expect(controller.foundCount, 0);
    });

    test('пустой поиск отличается от пустой книги', () async {
      await grantAndLoad();
      controller.searchContacts('нет');
      expect(controller.contacts, isNotEmpty);
      expect(controller.filteredContacts, isEmpty);
      expect(controller.foundCount, 0);
    });

    test('restoreViewState не читает адресную книгу', () async {
      await grantAndLoad();
      controller.restoreViewState(
        searchQuery: 'бо',
        sortOrder: ContactSortOrder.descending,
      );
      expect(controller.searchQuery, 'бо');
      expect(controller.sortOrder, ContactSortOrder.descending);
      expect(repository.getAllCount, 1);
    });
  });

  test('openSettings вызывает репозиторий и не читает контакты', () async {
    repository.permission = ContactAccessStatus.permanentlyDenied;
    await controller.initialize();
    await controller.openSettings();
    expect(repository.openSettingsCount, 1);
    expect(repository.getAllCount, 0);
  });
}
