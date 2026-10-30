import 'package:devicecontacts/models/contact_access_state.dart';
import 'package:devicecontacts/models/contact_details.dart';
import 'package:devicecontacts/models/contact_summary.dart';
import 'package:devicecontacts/repositories/contact_mapper.dart';
import 'package:devicecontacts/repositories/contacts_repository.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_contacts/flutter_contacts.dart';

/// Чтение системной адресной книги через flutter_contacts 2.x.
///
/// Запрашивается только [PermissionType.read]. Методы создания, изменения
/// и удаления контактов здесь намеренно не вызываются.
class DeviceContactsRepository implements ContactsRepository {
  static const listProperties = <ContactProperty>{
    ContactProperty.name,
    ContactProperty.phone,
    ContactProperty.email,
  };

  /// Свойства экрана деталей.
  ///
  /// Заметки на iOS требуют отдельный entitlement, поэтому их запрашиваем
  /// только на Android. Неподдерживаемые платформой свойства плагин пропускает.
  Set<ContactProperty> get detailProperties => {
    ContactProperty.name,
    ContactProperty.phone,
    ContactProperty.email,
    ContactProperty.address,
    ContactProperty.organization,
    ContactProperty.website,
    ContactProperty.socialMedia,
    ContactProperty.event,
    ContactProperty.relation,
    ContactProperty.favorite,
    ContactProperty.photoThumbnail,
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android)
      ContactProperty.note,
  };

  @override
  Future<ContactAccessStatus> checkPermission() async {
    final status = await FlutterContacts.permissions.check(PermissionType.read);
    return mapPermissionStatus(status);
  }

  @override
  Future<ContactAccessStatus> requestPermission() async {
    final status = await FlutterContacts.permissions.request(
      PermissionType.read,
    );
    return mapPermissionStatus(status);
  }

  @override
  Future<void> openSettings() async {
    await FlutterContacts.permissions.openSettings();
  }

  @override
  Future<List<ContactSummary>> getAllSummaries() async {
    final contacts = await FlutterContacts.getAll(properties: listProperties);
    return [
      for (final contact in contacts) ?mapContactSummary(contact),
    ];
  }

  @override
  Future<ContactDetails?> getDetails(String id) async {
    final contact = await FlutterContacts.get(id, properties: detailProperties);
    if (contact == null) {
      return null;
    }
    return mapContactDetails(contact);
  }
}

/// Переводит статус плагина в модель приложения.
ContactAccessStatus mapPermissionStatus(PermissionStatus status) {
  return switch (status) {
    PermissionStatus.granted => ContactAccessStatus.granted,
    PermissionStatus.limited => ContactAccessStatus.limited,
    PermissionStatus.denied => ContactAccessStatus.denied,
    PermissionStatus.permanentlyDenied => ContactAccessStatus.permanentlyDenied,
    PermissionStatus.restricted => ContactAccessStatus.restricted,
    PermissionStatus.notDetermined => ContactAccessStatus.notDetermined,
  };
}
