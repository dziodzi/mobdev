import 'package:devicecontacts/app.dart';
import 'package:devicecontacts/models/contact_access_state.dart';
import 'package:devicecontacts/models/contact_details.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fake_contacts_repository.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<void> pumpApp(
    WidgetTester tester,
    FakeContactsRepository repository,
  ) async {
    await tester.pumpWidget(
      ContactsApp(repository: repository, locale: const Locale('ru')),
    );
    await tester.pump();
  }

  testWidgets('отказ показывает запрос и не открывает настройки', (tester) async {
    final repository = FakeContactsRepository()
      ..permission = ContactAccessStatus.denied;
    await pumpApp(tester, repository);

    expect(find.byKey(const Key('permission-view')), findsOneWidget);
    expect(find.text('Разрешить доступ'), findsOneWidget);
    expect(find.text('Открыть настройки'), findsNothing);
    expect(find.byKey(const Key('empty-contacts')), findsNothing);
    expect(repository.getAllCount, 0);
  });

  testWidgets('окончательный отказ предлагает настройки', (tester) async {
    final repository = FakeContactsRepository()
      ..permission = ContactAccessStatus.permanentlyDenied;
    await pumpApp(tester, repository);

    expect(find.text('Открыть настройки'), findsOneWidget);
    expect(find.text('Разрешить доступ'), findsNothing);
    expect(repository.getAllCount, 0);
  });

  testWidgets('пустая книга не выглядит как отказ в доступе', (tester) async {
    final repository = FakeContactsRepository()
      ..permission = ContactAccessStatus.granted
      ..summaries = const [];
    await pumpApp(tester, repository);

    expect(find.byKey(const Key('empty-contacts')), findsOneWidget);
    expect(find.text('В адресной книге пока нет контактов'), findsOneWidget);
    expect(find.byKey(const Key('permission-view')), findsNothing);
    expect(find.text('Найдено контактов: 0'), findsOneWidget);
  });

  testWidgets('список показывает имя, телефон, email и строится лениво', (
    tester,
  ) async {
    final repository = FakeContactsRepository()
      ..permission = ContactAccessStatus.granted
      ..summaries = [
        testSummary('1', 'Анна Смирнова', phone: '+7 900', hasEmail: true),
        testSummary('2', 'Борис', hasEmail: false),
      ];
    await pumpApp(tester, repository);

    expect(find.text('Анна Смирнова'), findsOneWidget);
    expect(find.text('+7 900'), findsOneWidget);
    expect(find.text('Номер не указан'), findsOneWidget);
    expect(find.byIcon(Icons.alternate_email), findsOneWidget);
    expect(find.text('Найдено контактов: 2'), findsOneWidget);

    final list = tester.widget<ListView>(find.byKey(const Key('contacts-list')));
    expect(list.childrenDelegate, isA<SliverChildBuilderDelegate>());
    expect(repository.getAllCount, 1);
  });

  testWidgets('поиск без совпадений отличается от пустой книги', (tester) async {
    final repository = FakeContactsRepository()
      ..permission = ContactAccessStatus.granted
      ..summaries = [testSummary('1', 'Анна')];
    await pumpApp(tester, repository);

    await tester.enterText(find.byKey(const Key('search-field')), 'нет');
    await tester.pump();

    expect(find.byKey(const Key('empty-search')), findsOneWidget);
    expect(find.text('Контакты не найдены'), findsOneWidget);
    expect(find.byKey(const Key('empty-contacts')), findsNothing);
    expect(find.text('Найдено контактов: 0'), findsOneWidget);
    expect(repository.getAllCount, 1);
  });

  testWidgets('поворот сохраняет поиск и сортировку', (tester) async {
    final repository = FakeContactsRepository()
      ..permission = ContactAccessStatus.granted
      ..summaries = [
        testSummary('1', 'Анна'),
        testSummary('2', 'Борис'),
      ];
    await pumpApp(tester, repository);
    addTearDown(() => tester.binding.setSurfaceSize(null));

    await tester.enterText(find.byKey(const Key('search-field')), 'ан');
    await tester.pump();
    await tester.tap(find.byKey(const Key('sort-control')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('По имени ↓').last);
    await tester.pumpAndSettle();

    await tester.binding.setSurfaceSize(const Size(800, 360));
    await tester.pump();

    expect(find.text('ан'), findsOneWidget);
    expect(find.text('По имени ↓'), findsWidgets);
    expect(find.text('Анна'), findsOneWidget);
    expect(find.text('Борис'), findsNothing);
    expect(find.text('Найдено контактов: 1'), findsOneWidget);
    expect(repository.getAllCount, 1);
  });

  testWidgets('возврат с деталей не перечитывает книгу', (tester) async {
    final repository = FakeContactsRepository()
      ..permission = ContactAccessStatus.granted
      ..summaries = [testSummary('1', 'Анна', phone: '111')]
      ..detailsById = {
        '1': const ContactDetails(
          id: '1',
          displayName: 'Анна',
          name: ContactNameParts(first: 'Анна'),
          phones: [LabeledValue(value: '111', labelKey: 'mobile')],
          emails: [LabeledValue(value: 'a@example.com', labelKey: 'work')],
          addresses: [],
          organizations: [],
          websites: [],
          socialMedias: [],
          events: [],
          relations: [],
          notes: [],
          isFavorite: false,
          photoBytes: null,
        ),
      };
    await pumpApp(tester, repository);

    await tester.tap(find.text('Анна'));
    await tester.pump();
    await tester.pump();

    expect(find.text('111'), findsWidgets);
    expect(find.text('a@example.com'), findsOneWidget);
    expect(find.text('Мобильный'), findsOneWidget);
    expect(repository.getAllCount, 1);

    await tester.tap(find.byType(BackButton));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
    expect(find.byKey(const Key('contacts-list')), findsOneWidget);
    expect(repository.getAllCount, 1);
  });
}
