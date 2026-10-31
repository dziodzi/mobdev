// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Russian (`ru`).
class AppLocalizationsRu extends AppLocalizations {
  AppLocalizationsRu([String locale = 'ru']) : super(locale);

  @override
  String get appTitle => 'Контакты';

  @override
  String get refresh => 'Обновить контакты';

  @override
  String get loading => 'Загрузка контактов';

  @override
  String get permissionTitle => 'Требуется доступ к контактам';

  @override
  String get permissionMessage =>
      'Разрешите доступ, чтобы показать контакты этого устройства.';

  @override
  String get permissionPermanentMessage =>
      'Повторный запрос недоступен. Откройте настройки приложения и разрешите доступ к контактам.';

  @override
  String get requestAccess => 'Запросить доступ';

  @override
  String get openSettings => 'Открыть настройки';

  @override
  String get retry => 'Повторить';

  @override
  String get loadError => 'Не удалось прочитать контакты';

  @override
  String get searchHint => 'Поиск по имени';

  @override
  String get sortAscending => 'От А до Я';

  @override
  String get sortDescending => 'От Я до А';

  @override
  String countLabel(int count) {
    return 'Найдено: $count';
  }

  @override
  String get noContacts => 'Адресная книга пуста';

  @override
  String get noResults => 'Поиск не дал результатов';

  @override
  String get noPhone => 'Номер телефона отсутствует';

  @override
  String get noEmail => 'Email отсутствует';

  @override
  String get unknownContact => 'Контакт без имени';

  @override
  String get detailsTitle => 'Информация о контакте';

  @override
  String get phones => 'Номера телефонов';

  @override
  String get emails => 'Email';

  @override
  String get organization => 'Организация';

  @override
  String get addresses => 'Адреса';

  @override
  String get notes => 'Заметки';
}
