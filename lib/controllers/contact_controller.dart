import 'package:flutter/foundation.dart';

import '../models/contact_record.dart';
import '../services/contact_repository.dart';

enum ContactsState { loading, ready, denied, error }

class ContactController extends ChangeNotifier {
  ContactController(this.repository);

  final ContactRepository repository;
  ContactsState state = ContactsState.loading;
  ContactPermission permission = ContactPermission.denied;
  String query = '';
  bool ascending = true;

  List<ContactRecord> _contacts = [];
  bool _busy = false;
  bool _initialized = false;

  int get totalContacts => _contacts.length;
  bool get permanentlyDenied =>
      permission == ContactPermission.permanentlyDenied;

  List<ContactRecord> get visibleContacts {
    final search = query.trim().toLowerCase();
    final result = _contacts
        .where((contact) => contact.name.toLowerCase().contains(search))
        .toList();
    result.sort((first, second) {
      final comparison = first.name.toLowerCase().compareTo(
        second.name.toLowerCase(),
      );
      return ascending ? comparison : -comparison;
    });
    return result;
  }

  Future<void> initialize() async {
    if (_initialized) return;
    _initialized = true;
    await refresh(requestAccess: true);
  }

  Future<void> refresh({bool requestAccess = false}) async {
    if (_busy) return;
    _busy = true;
    state = ContactsState.loading;
    notifyListeners();

    try {
      permission = requestAccess
          ? await repository.requestPermission()
          : await repository.checkPermission();

      if (permission != ContactPermission.granted) {
        _contacts = [];
        state = ContactsState.denied;
      } else {
        _contacts = await repository.loadContacts();
        state = ContactsState.ready;
      }
    } catch (_) {
      state = ContactsState.error;
    } finally {
      _busy = false;
      notifyListeners();
    }
  }

  Future<void> checkAfterSettings() async {
    if (_busy) return;
    try {
      final current = await repository.checkPermission();
      if (current != permission || current != ContactPermission.granted) {
        await refresh();
      }
    } catch (_) {
      state = ContactsState.error;
      notifyListeners();
    }
  }

  void updateQuery(String value) {
    query = value;
    notifyListeners();
  }

  void updateSort(bool value) {
    ascending = value;
    notifyListeners();
  }
}
