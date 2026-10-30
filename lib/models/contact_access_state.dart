/// Состояние доступа к адресной книге.
///
/// Повторяет статусы [PermissionStatus] пакета flutter_contacts, но не
/// зависит от плагина. Так контроллер и тесты работают с одной моделью.
enum ContactAccessStatus {
  /// Разрешение ещё не проверялось.
  unknown,

  /// Полный доступ на чтение.
  granted,

  /// Ограниченный доступ: на iOS 18+ видны только выбранные контакты.
  limited,

  /// Пользователь отказал, системный запрос можно показать снова.
  denied,

  /// Повторный системный запрос недоступен, нужно открыть настройки.
  permanentlyDenied,

  /// Доступ запрещён политикой системы, например родительским контролем.
  restricted,

  /// Пользователь ещё не отвечал на запрос.
  notDetermined,
}

extension ContactAccessStatusX on ContactAccessStatus {
  /// Можно ли читать адресную книгу в этом состоянии.
  bool get canReadContacts =>
      this == ContactAccessStatus.granted ||
      this == ContactAccessStatus.limited;

  /// Можно ли ещё показать системное окно запроса.
  bool get canRequestPermission =>
      this == ContactAccessStatus.denied ||
      this == ContactAccessStatus.notDetermined ||
      this == ContactAccessStatus.unknown;

  /// Системное окно больше не появится, пользователю нужны настройки.
  bool get requiresSettings =>
      this == ContactAccessStatus.permanentlyDenied ||
      this == ContactAccessStatus.restricted;
}
