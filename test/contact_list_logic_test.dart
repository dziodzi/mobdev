import 'package:devicecontacts/controllers/contact_list_logic.dart';
import 'package:devicecontacts/models/contact_sort_order.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fake_contacts_repository.dart';

void main() {
  final contacts = [
    testSummary('2', 'Алексей Петров', phone: '222'),
    testSummary('1', 'Александр Иванов', phone: '111', hasEmail: true),
    testSummary('3', 'Анна Смирнова'),
    testSummary('4', 'Иван Сидоров', phone: '444'),
    testSummary('5', 'иван сидоров', phone: '555'),
  ];

  group('filterContacts', () {
    test('находит полное имя', () {
      final result = filterContacts(contacts, 'Александр Иванов');
      expect(result.map((item) => item.id), ['1']);
    });

    test('находит часть имени', () {
      final result = filterContacts(contacts, 'ал');
      expect(result.map((item) => item.id), ['2', '1']);
    });

    test('не учитывает регистр', () {
      final result = filterContacts(contacts, 'АННА');
      expect(result.map((item) => item.id), ['3']);
    });

    test('считает ё и е одинаковыми', () {
      final result = filterContacts([
        testSummary('e', 'Ёлка'),
      ], 'елка');
      expect(result, hasLength(1));
    });

    test('пустой результат не путается с пустым списком источника', () {
      expect(filterContacts(contacts, 'нет такого'), isEmpty);
      expect(contacts, isNotEmpty);
    });

    test('очистка поиска возвращает исходный список', () {
      final result = filterContacts(contacts, '   ');
      expect(result.map((item) => item.id), ['2', '1', '3', '4', '5']);
    });
  });

  group('sortContacts', () {
    test('сортирует по возрастанию', () {
      final result = sortContacts(contacts, ContactSortOrder.ascending);
      expect(result.map((item) => item.id).toList(), ['1', '2', '3', '4', '5']);
    });

    test('сортирует по убыванию как полный разворот возрастания', () {
      final ascending = sortContacts(contacts, ContactSortOrder.ascending);
      final descending = sortContacts(contacts, ContactSortOrder.descending);
      expect(
        descending.map((item) => item.id),
        ascending.map((item) => item.id).toList().reversed,
      );
    });

    test('одинаковые имена различает по id', () {
      final same = [
        testSummary('b', 'Иван'),
        testSummary('a', 'Иван'),
      ];
      expect(
        sortContacts(same, ContactSortOrder.ascending).map((item) => item.id),
        ['a', 'b'],
      );
      expect(
        sortContacts(same, ContactSortOrder.descending).map((item) => item.id),
        ['b', 'a'],
      );
    });

    test('пустой список не вызывает ошибку', () {
      expect(sortContacts(const [], ContactSortOrder.ascending), isEmpty);
      expect(sortContacts(const [], ContactSortOrder.descending), isEmpty);
    });
  });
}
