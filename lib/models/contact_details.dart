import 'dart:typed_data';

import 'package:devicecontacts/models/contact_summary.dart';

/// Значение контакта с типом поля: телефон, email, сайт и т.д.
class LabeledValue {
  const LabeledValue({
    required this.value,
    required this.labelKey,
    this.customLabel,
  });

  final String value;

  /// Имя стандартной метки, например `mobile` или `work`.
  final String labelKey;

  /// Пользовательская подпись из адресной книги, если она задана.
  final String? customLabel;
}

/// Событие контакта: день рождения, годовщина или своя дата.
class ContactEvent {
  const ContactEvent({
    required this.labelKey,
    required this.month,
    required this.day,
    this.customLabel,
    this.year,
  });

  final String labelKey;
  final String? customLabel;
  final int? year;
  final int month;
  final int day;
}

/// Организация и должность. Пустые строки сюда не попадают.
class OrganizationInfo {
  const OrganizationInfo({
    this.name,
    this.jobTitle,
    this.department,
    this.jobDescription,
    this.officeLocation,
    this.phoneticName,
    this.symbol,
  });

  final String? name;
  final String? jobTitle;
  final String? department;
  final String? jobDescription;
  final String? officeLocation;
  final String? phoneticName;
  final String? symbol;

  bool get isEmpty =>
      _blank(name) &&
      _blank(jobTitle) &&
      _blank(department) &&
      _blank(jobDescription) &&
      _blank(officeLocation) &&
      _blank(phoneticName) &&
      _blank(symbol);
}

/// Части имени, которые можно показать отдельно от заголовка.
class ContactNameParts {
  const ContactNameParts({
    this.prefix,
    this.first,
    this.middle,
    this.last,
    this.suffix,
    this.nickname,
    this.phonetic,
    this.previousFamilyName,
  });

  final String? prefix;
  final String? first;
  final String? middle;
  final String? last;
  final String? suffix;
  final String? nickname;
  final String? phonetic;
  final String? previousFamilyName;

  bool get isEmpty =>
      _blank(prefix) &&
      _blank(first) &&
      _blank(middle) &&
      _blank(last) &&
      _blank(suffix) &&
      _blank(nickname) &&
      _blank(phonetic) &&
      _blank(previousFamilyName);
}

/// Подробная карточка одного контакта.
class ContactDetails {
  const ContactDetails({
    required this.id,
    required this.displayName,
    required this.name,
    required this.phones,
    required this.emails,
    required this.addresses,
    required this.organizations,
    required this.websites,
    required this.socialMedias,
    required this.events,
    required this.relations,
    required this.notes,
    required this.isFavorite,
    required this.photoBytes,
  });

  final String id;
  final String displayName;
  final ContactNameParts name;
  final List<LabeledValue> phones;
  final List<LabeledValue> emails;
  final List<LabeledValue> addresses;
  final List<OrganizationInfo> organizations;
  final List<LabeledValue> websites;
  final List<LabeledValue> socialMedias;
  final List<ContactEvent> events;
  final List<LabeledValue> relations;
  final List<String> notes;
  final bool? isFavorite;
  final Uint8List? photoBytes;

  bool get hasEmail => emails.isNotEmpty;

  String? get primaryPhone => phones.isEmpty ? null : phones.first.value;

  String get initial => ContactSummary(
    id: id,
    displayName: displayName,
    primaryPhone: primaryPhone,
    hasEmail: hasEmail,
  ).initial;

  bool get hasExtraFields =>
      !name.isEmpty ||
      phones.isNotEmpty ||
      emails.isNotEmpty ||
      addresses.isNotEmpty ||
      organizations.isNotEmpty ||
      websites.isNotEmpty ||
      socialMedias.isNotEmpty ||
      events.isNotEmpty ||
      relations.isNotEmpty ||
      notes.isNotEmpty ||
      isFavorite == true ||
      photoBytes != null;
}

bool _blank(String? value) => value == null || value.trim().isEmpty;
