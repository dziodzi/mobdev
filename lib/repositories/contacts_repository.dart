import 'package:devicecontacts/models/contact_access_state.dart';
import 'package:devicecontacts/models/contact_details.dart';
import 'package:devicecontacts/models/contact_summary.dart';

/// Доступ к системной адресной книге.
///
/// Контроллер зависит от этого интерфейса, а не от плагина напрямую.
/// В тестах вместо устройства подставляется поддельная реализация.
abstract class ContactsRepository {
  /// Проверяет текущее разрешение, не показывая системное окно.
  Future<ContactAccessStatus> checkPermission();

  /// Запрашивает разрешение только на чтение.
  Future<ContactAccessStatus> requestPermission();

  /// Открывает системные настройки приложения.
  Future<void> openSettings();

  /// Читает краткий список контактов.
  Future<List<ContactSummary>> getAllSummaries();

  /// Читает подробности одного контакта. null, если контакт уже удалён.
  Future<ContactDetails?> getDetails(String id);
}
