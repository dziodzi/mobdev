/// Краткая карточка контакта для главного списка.
///
/// Хранит только то, что нужно строке списка: имя, основной телефон и
/// признак email. Полные данные загружаются отдельно по [id].
class ContactSummary {
  const ContactSummary({
    required this.id,
    required this.displayName,
    required this.primaryPhone,
    required this.hasEmail,
  });

  final String id;
  final String displayName;
  final String? primaryPhone;
  final bool hasEmail;

  /// Первая буква имени для аватара. Пустая строка, если имени нет.
  String get initial {
    final trimmed = displayName.trim();
    if (trimmed.isEmpty) {
      return '';
    }
    return trimmed[0].toUpperCase();
  }
}
