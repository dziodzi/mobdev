import 'package:devicecontacts/models/contact_access_state.dart';
import 'package:devicecontacts/models/contact_details.dart';
import 'package:devicecontacts/models/contact_summary.dart';
import 'package:devicecontacts/repositories/contacts_repository.dart';

/// Репозиторий для тестов. К системной адресной книге не обращается.
class FakeContactsRepository implements ContactsRepository {
  ContactAccessStatus permission = ContactAccessStatus.notDetermined;
  ContactAccessStatus? requestResult;
  List<ContactSummary> summaries = const [];
  Map<String, ContactDetails?> detailsById = {};

  bool throwOnCheck = false;
  bool throwOnRequest = false;
  bool throwOnGetAll = false;
  bool throwOnDetails = false;
  bool throwOnOpenSettings = false;

  int checkCount = 0;
  int requestCount = 0;
  int getAllCount = 0;
  int getDetailsCount = 0;
  int openSettingsCount = 0;

  @override
  Future<ContactAccessStatus> checkPermission() async {
    checkCount++;
    if (throwOnCheck) {
      throw StateError('check failed');
    }
    return permission;
  }

  @override
  Future<ContactAccessStatus> requestPermission() async {
    requestCount++;
    if (throwOnRequest) {
      throw StateError('request failed');
    }
    permission = requestResult ?? permission;
    return permission;
  }

  @override
  Future<void> openSettings() async {
    openSettingsCount++;
    if (throwOnOpenSettings) {
      throw StateError('settings failed');
    }
  }

  @override
  Future<List<ContactSummary>> getAllSummaries() async {
    getAllCount++;
    if (throwOnGetAll) {
      throw StateError('load failed');
    }
    return List<ContactSummary>.of(summaries);
  }

  @override
  Future<ContactDetails?> getDetails(String id) async {
    getDetailsCount++;
    if (throwOnDetails) {
      throw StateError('details failed');
    }
    return detailsById[id];
  }
}

ContactSummary testSummary(
  String id,
  String name, {
  String? phone,
  bool hasEmail = false,
}) {
  return ContactSummary(
    id: id,
    displayName: name,
    primaryPhone: phone,
    hasEmail: hasEmail,
  );
}
