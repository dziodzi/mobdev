import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_ru.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[Locale('ru')];

  /// No description provided for @appTitle.
  ///
  /// In ru, this message translates to:
  /// **'Каналы'**
  String get appTitle;

  /// No description provided for @login.
  ///
  /// In ru, this message translates to:
  /// **'Логин'**
  String get login;

  /// No description provided for @password.
  ///
  /// In ru, this message translates to:
  /// **'Пароль'**
  String get password;

  /// No description provided for @signIn.
  ///
  /// In ru, this message translates to:
  /// **'Войти'**
  String get signIn;

  /// No description provided for @logout.
  ///
  /// In ru, this message translates to:
  /// **'Выйти'**
  String get logout;

  /// No description provided for @channels.
  ///
  /// In ru, this message translates to:
  /// **'Каналы'**
  String get channels;

  /// No description provided for @choose.
  ///
  /// In ru, this message translates to:
  /// **'Выберите чат'**
  String get choose;

  /// No description provided for @empty.
  ///
  /// In ru, this message translates to:
  /// **'Здесь пока пусто'**
  String get empty;

  /// No description provided for @retry.
  ///
  /// In ru, this message translates to:
  /// **'Повторить'**
  String get retry;

  /// No description provided for @refresh.
  ///
  /// In ru, this message translates to:
  /// **'Обновить'**
  String get refresh;

  /// No description provided for @older.
  ///
  /// In ru, this message translates to:
  /// **'Предыдущие сообщения'**
  String get older;

  /// No description provided for @message.
  ///
  /// In ru, this message translates to:
  /// **'Сообщение'**
  String get message;

  /// No description provided for @send.
  ///
  /// In ru, this message translates to:
  /// **'Отправить'**
  String get send;

  /// No description provided for @sending.
  ///
  /// In ru, this message translates to:
  /// **'Отправляется'**
  String get sending;

  /// No description provided for @sent.
  ///
  /// In ru, this message translates to:
  /// **'Отправлено'**
  String get sent;

  /// No description provided for @uncertain.
  ///
  /// In ru, this message translates to:
  /// **'Не удалось подтвердить отправку'**
  String get uncertain;

  /// No description provided for @restore.
  ///
  /// In ru, this message translates to:
  /// **'Вернуть в редактор'**
  String get restore;

  /// No description provided for @warning.
  ///
  /// In ru, this message translates to:
  /// **'Сообщение могло быть доставлено. Повтор может создать дубликат. Повторить?'**
  String get warning;

  /// No description provided for @yes.
  ///
  /// In ru, this message translates to:
  /// **'Повторить отправку'**
  String get yes;

  /// No description provided for @cancel.
  ///
  /// In ru, this message translates to:
  /// **'Отмена'**
  String get cancel;

  /// No description provided for @back.
  ///
  /// In ru, this message translates to:
  /// **'Назад'**
  String get back;

  /// No description provided for @close.
  ///
  /// In ru, this message translates to:
  /// **'Закрыть'**
  String get close;

  /// No description provided for @image.
  ///
  /// In ru, this message translates to:
  /// **'Изображение'**
  String get image;

  /// No description provided for @invalid.
  ///
  /// In ru, this message translates to:
  /// **'Неверный логин или пароль'**
  String get invalid;

  /// No description provided for @connection.
  ///
  /// In ru, this message translates to:
  /// **'Не удалось связаться с сервером'**
  String get connection;

  /// No description provided for @server.
  ///
  /// In ru, this message translates to:
  /// **'Сервис временно недоступен'**
  String get server;

  /// No description provided for @storage.
  ///
  /// In ru, this message translates to:
  /// **'Не удалось сохранить данные на устройстве'**
  String get storage;

  /// No description provided for @expired.
  ///
  /// In ru, this message translates to:
  /// **'Сессия завершена. Войдите снова.'**
  String get expired;

  /// No description provided for @protocol.
  ///
  /// In ru, this message translates to:
  /// **'Неверный формат ответа сервера'**
  String get protocol;

  /// No description provided for @restoreWarning.
  ///
  /// In ru, this message translates to:
  /// **'Текст будет добавлен к текущему черновику.'**
  String get restoreWarning;

  /// No description provided for @loading.
  ///
  /// In ru, this message translates to:
  /// **'Загрузка'**
  String get loading;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['ru'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'ru':
      return AppLocalizationsRu();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
