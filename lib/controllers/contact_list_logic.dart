import 'package:devicecontacts/models/contact_sort_order.dart';
import 'package:devicecontacts/models/contact_summary.dart';

/// Приводит имя к виду, удобному для поиска и сортировки.
///
/// Сравнение не зависит от регистра. Буква «ё» считается равной «е»,
/// чтобы запрос «елка» находил контакт «Ёлка».
String normalizeContactName(String value) {
  return value.trim().toLowerCase().replaceAll('ё', 'е');
}

/// Оставляет контакты, в имени которых есть [query].
///
/// Пустой запрос возвращает копию исходного списка. Функция не обращается
/// к адресной книге и работает только с уже загруженным кэшем.
List<ContactSummary> filterContacts(
  List<ContactSummary> contacts,
  String query,
) {
  final normalizedQuery = normalizeContactName(query);
  if (normalizedQuery.isEmpty) {
    return List<ContactSummary>.of(contacts);
  }
  return [
    for (final contact in contacts)
      if (normalizeContactName(contact.displayName).contains(normalizedQuery))
        contact,
  ];
}

/// Сортирует контакты по имени.
///
/// Одинаковые имена дополнительно сравниваются по id, поэтому порядок
/// всегда последовательный и не зависит от повторной загрузки.
List<ContactSummary> sortContacts(
  List<ContactSummary> contacts,
  ContactSortOrder order,
) {
  final sorted = List<ContactSummary>.of(contacts);
  sorted.sort((left, right) {
    final byName = normalizeContactName(left.displayName).compareTo(
      normalizeContactName(right.displayName),
    );
    final result = byName != 0 ? byName : left.id.compareTo(right.id);
    return order == ContactSortOrder.ascending ? result : -result;
  });
  return sorted;
}
