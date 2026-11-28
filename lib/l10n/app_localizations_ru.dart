// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Russian (`ru`).
class AppLocalizationsRu extends AppLocalizations {
  AppLocalizationsRu([String locale = 'ru']) : super(locale);

  @override
  String get appTitle => 'Каналы';

  @override
  String get login => 'Логин';

  @override
  String get password => 'Пароль';

  @override
  String get signIn => 'Войти';

  @override
  String get logout => 'Выйти';

  @override
  String get channels => 'Каналы';

  @override
  String get choose => 'Выберите чат';

  @override
  String get empty => 'Здесь пока пусто';

  @override
  String get retry => 'Повторить';

  @override
  String get refresh => 'Обновить';

  @override
  String get older => 'Предыдущие сообщения';

  @override
  String get message => 'Сообщение';

  @override
  String get send => 'Отправить';

  @override
  String get sending => 'Отправляется';

  @override
  String get sent => 'Отправлено';

  @override
  String get uncertain => 'Не удалось подтвердить отправку';

  @override
  String get restore => 'Вернуть в редактор';

  @override
  String get warning =>
      'Сообщение могло быть доставлено. Повтор может создать дубликат. Повторить?';

  @override
  String get yes => 'Повторить отправку';

  @override
  String get cancel => 'Отмена';

  @override
  String get back => 'Назад';

  @override
  String get close => 'Закрыть';

  @override
  String get image => 'Изображение';

  @override
  String get invalid => 'Неверный логин или пароль';

  @override
  String get connection => 'Не удалось связаться с сервером';

  @override
  String get server => 'Сервис временно недоступен';

  @override
  String get storage => 'Не удалось сохранить данные на устройстве';

  @override
  String get expired => 'Сессия завершена. Войдите снова.';

  @override
  String get protocol => 'Неверный формат ответа сервера';

  @override
  String get restoreWarning => 'Текст будет добавлен к текущему черновику.';

  @override
  String get loading => 'Загрузка';
}
