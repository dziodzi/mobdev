import 'dart:typed_data';

import 'package:devicecontacts/models/contact_details.dart';
import 'package:devicecontacts/models/contact_summary.dart';
import 'package:flutter_contacts/flutter_contacts.dart';

/// Собирает имя, которое можно показать в списке.
String displayNameOf(Contact contact) {
  final display = contact.displayName?.trim();
  if (display != null && display.isNotEmpty) {
    return display;
  }
  final name = contact.name;
  if (name == null) {
    return '';
  }
  final parts = [
    name.prefix,
    name.first,
    name.middle,
    name.last,
    name.suffix,
  ].where(_notBlank).map((part) => part!.trim());
  final joined = parts.join(' ');
  if (joined.isNotEmpty) {
    return joined;
  }
  return name.nickname?.trim() ?? '';
}

/// Основной номер: помеченный isPrimary либо первый непустой.
String? primaryPhoneOf(List<Phone> phones) {
  String? first;
  for (final phone in phones) {
    final number = phone.number.trim();
    if (number.isEmpty) {
      continue;
    }
    if (phone.isPrimary == true) {
      return number;
    }
    first ??= number;
  }
  return first;
}

/// Превращает контакт плагина в строку списка. null, если нет id.
ContactSummary? mapContactSummary(Contact contact) {
  final id = contact.id;
  if (id == null || id.isEmpty) {
    return null;
  }
  return ContactSummary(
    id: id,
    displayName: displayNameOf(contact),
    primaryPhone: primaryPhoneOf(contact.phones),
    hasEmail: contact.emails.any((email) => email.address.trim().isNotEmpty),
  );
}

/// Превращает контакт плагина в подробную карточку.
ContactDetails mapContactDetails(Contact contact) {
  final id = contact.id;
  if (id == null || id.isEmpty) {
    throw StateError('Contact details require an id');
  }
  final name = contact.name;
  return ContactDetails(
    id: id,
    displayName: displayNameOf(contact),
    name: ContactNameParts(
      prefix: _clean(name?.prefix),
      first: _clean(name?.first),
      middle: _clean(name?.middle),
      last: _clean(name?.last),
      suffix: _clean(name?.suffix),
      nickname: _clean(name?.nickname),
      phonetic: _join([
        name?.phoneticFirst,
        name?.phoneticMiddle,
        name?.phoneticLast,
      ]),
      previousFamilyName: _clean(name?.previousFamilyName),
    ),
    phones: [
      for (final phone in _phonesWithPrimaryFirst(contact.phones))
        if (phone.number.trim().isNotEmpty)
          LabeledValue(
            value: phone.number.trim(),
            labelKey: phone.label.label.name,
            customLabel: phone.label.customLabel,
          ),
    ],
    emails: [
      for (final email in contact.emails)
        if (email.address.trim().isNotEmpty)
          LabeledValue(
            value: email.address.trim(),
            labelKey: email.label.label.name,
            customLabel: email.label.customLabel,
          ),
    ],
    addresses: [
      for (final address in contact.addresses)
        if (_addressText(address) != null)
          LabeledValue(
            value: _addressText(address)!,
            labelKey: address.label.label.name,
            customLabel: address.label.customLabel,
          ),
    ],
    organizations: [
      for (final organization in contact.organizations)
        if (!_organization(organization).isEmpty) _organization(organization),
    ],
    websites: [
      for (final website in contact.websites)
        if (website.url.trim().isNotEmpty)
          LabeledValue(
            value: website.url.trim(),
            labelKey: website.label.label.name,
            customLabel: website.label.customLabel,
          ),
    ],
    socialMedias: [
      for (final social in contact.socialMedias)
        if (social.username.trim().isNotEmpty)
          LabeledValue(
            value: social.username.trim(),
            labelKey: social.label.label.name,
            customLabel: social.label.customLabel,
          ),
    ],
    events: [
      for (final event in contact.events)
        if (event.month >= 1 &&
            event.month <= 12 &&
            event.day >= 1 &&
            event.day <= 31)
          ContactEvent(
            labelKey: event.label.label.name,
            customLabel: event.label.customLabel,
            year: event.year,
            month: event.month,
            day: event.day,
          ),
    ],
    relations: [
      for (final relation in contact.relations)
        if (relation.name.trim().isNotEmpty)
          LabeledValue(
            value: relation.name.trim(),
            labelKey: relation.label.label.name,
            customLabel: relation.label.customLabel,
          ),
    ],
    notes: [
      for (final note in contact.notes)
        if (note.note.trim().isNotEmpty) note.note.trim(),
    ],
    isFavorite: contact.android?.isFavorite,
    photoBytes: _photoBytes(contact),
  );
}

List<Phone> _phonesWithPrimaryFirst(List<Phone> phones) {
  final primary = <Phone>[];
  final rest = <Phone>[];
  for (final phone in phones) {
    if (phone.isPrimary == true) {
      primary.add(phone);
    } else {
      rest.add(phone);
    }
  }
  return [...primary, ...rest];
}

String? _addressText(Address address) {
  final formatted = _clean(address.formatted);
  if (formatted != null) {
    return formatted;
  }
  return _join([
    address.street,
    address.poBox,
    address.neighborhood,
    address.city,
    address.subLocality,
    address.state,
    address.subAdministrativeArea,
    address.postalCode,
    address.country,
    address.isoCountryCode,
  ]);
}

OrganizationInfo _organization(Organization organization) {
  return OrganizationInfo(
    name: _clean(organization.name),
    jobTitle: _clean(organization.jobTitle),
    department: _clean(organization.departmentName),
    jobDescription: _clean(organization.jobDescription),
    officeLocation: _clean(organization.officeLocation),
    phoneticName: _clean(organization.phoneticName),
    symbol: _clean(organization.symbol),
  );
}

Uint8List? _photoBytes(Contact contact) {
  final bytes = contact.photo?.thumbnail ?? contact.photo?.fullSize;
  if (bytes == null || bytes.isEmpty) {
    return null;
  }
  return bytes;
}

String? _join(List<String?> parts) {
  final values = parts.where(_notBlank).map((part) => part!.trim());
  final joined = values.join(' ');
  return joined.isEmpty ? null : joined;
}

String? _clean(String? value) {
  final trimmed = value?.trim();
  if (trimmed == null || trimmed.isEmpty) {
    return null;
  }
  return trimmed;
}

bool _notBlank(String? value) => value != null && value.trim().isNotEmpty;
