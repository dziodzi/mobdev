import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:permission_handler/permission_handler.dart';

import '../models/contact_record.dart';

enum ContactPermission { granted, denied, permanentlyDenied }

abstract class ContactRepository {
  Future<ContactPermission> checkPermission();
  Future<ContactPermission> requestPermission();
  Future<bool> openSettings();
  Future<List<ContactRecord>> loadContacts();
}

class DeviceContactRepository implements ContactRepository {
  Future<ContactPermission> _permission() async {
    final status = await Permission.contacts.status;
    if (status.isGranted || status.isLimited) {
      return ContactPermission.granted;
    }
    if (status.isPermanentlyDenied || status.isRestricted) {
      return ContactPermission.permanentlyDenied;
    }
    return ContactPermission.denied;
  }

  @override
  Future<ContactPermission> checkPermission() => _permission();

  @override
  Future<ContactPermission> requestPermission() async {
    await Permission.contacts.request();
    return _permission();
  }

  @override
  Future<bool> openSettings() => openAppSettings();

  @override
  Future<List<ContactRecord>> loadContacts() async {
    final contacts = await FlutterContacts.getAll(
      properties: ContactProperties.all,
    );
    return contacts
        .map(
          (contact) => ContactRecord(
            id: contact.id ?? '',
            name: (contact.displayName ?? '').trim(),
            phones: contact.phones
                .map((phone) => phone.number.trim())
                .where((phone) => phone.isNotEmpty)
                .toList(),
            emails: contact.emails
                .map((email) => email.address.trim())
                .where((email) => email.isNotEmpty)
                .toList(),
            organization: contact.organizations
                .map((item) => (item.name ?? '').trim())
                .where((item) => item.isNotEmpty)
                .join(', '),
            addresses: contact.addresses
                .map((address) => (address.formatted ?? '').trim())
                .where((address) => address.isNotEmpty)
                .toList(),
            notes: contact.notes
                .map((note) => note.note.trim())
                .where((note) => note.isNotEmpty)
                .toList(),
          ),
        )
        .toList();
  }
}
