import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
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
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('ru'),
  ];

  /// No description provided for @appTitle.
  ///
  /// In ru, this message translates to:
  /// **'Контакты'**
  String get appTitle;

  /// No description provided for @searchHint.
  ///
  /// In ru, this message translates to:
  /// **'Поиск по имени'**
  String get searchHint;

  /// No description provided for @clearSearch.
  ///
  /// In ru, this message translates to:
  /// **'Очистить поиск'**
  String get clearSearch;

  /// No description provided for @foundContacts.
  ///
  /// In ru, this message translates to:
  /// **'Найдено контактов: {count}'**
  String foundContacts(int count);

  /// No description provided for @sortAscending.
  ///
  /// In ru, this message translates to:
  /// **'По имени ↑'**
  String get sortAscending;

  /// No description provided for @sortDescending.
  ///
  /// In ru, this message translates to:
  /// **'По имени ↓'**
  String get sortDescending;

  /// No description provided for @sortTooltip.
  ///
  /// In ru, this message translates to:
  /// **'Сортировка'**
  String get sortTooltip;

  /// No description provided for @phoneNotSpecified.
  ///
  /// In ru, this message translates to:
  /// **'Номер не указан'**
  String get phoneNotSpecified;

  /// No description provided for @unnamedContact.
  ///
  /// In ru, this message translates to:
  /// **'Без имени'**
  String get unnamedContact;

  /// No description provided for @hasEmail.
  ///
  /// In ru, this message translates to:
  /// **'Есть электронная почта'**
  String get hasEmail;

  /// No description provided for @checkingPermission.
  ///
  /// In ru, this message translates to:
  /// **'Проверка доступа к контактам…'**
  String get checkingPermission;

  /// No description provided for @loadingContacts.
  ///
  /// In ru, this message translates to:
  /// **'Загрузка контактов…'**
  String get loadingContacts;

  /// No description provided for @loadingDetails.
  ///
  /// In ru, this message translates to:
  /// **'Загрузка сведений…'**
  String get loadingDetails;

  /// No description provided for @emptyContactsTitle.
  ///
  /// In ru, this message translates to:
  /// **'В адресной книге пока нет контактов'**
  String get emptyContactsTitle;

  /// No description provided for @emptyContactsMessage.
  ///
  /// In ru, this message translates to:
  /// **'Когда в телефонной книге появятся контакты, они отобразятся здесь.'**
  String get emptyContactsMessage;

  /// No description provided for @emptySearchTitle.
  ///
  /// In ru, this message translates to:
  /// **'Контакты не найдены'**
  String get emptySearchTitle;

  /// No description provided for @emptySearchMessage.
  ///
  /// In ru, this message translates to:
  /// **'По этому запросу в уже загруженном списке нет совпадений.'**
  String get emptySearchMessage;

  /// No description provided for @permissionTitle.
  ///
  /// In ru, this message translates to:
  /// **'Доступ к контактам'**
  String get permissionTitle;

  /// No description provided for @permissionExplanation.
  ///
  /// In ru, this message translates to:
  /// **'Приложение только читает адресную книгу: оно не создаёт, не изменяет и не удаляет контакты.'**
  String get permissionExplanation;

  /// No description provided for @permissionDeniedMessage.
  ///
  /// In ru, this message translates to:
  /// **'Доступ не предоставлен. Можно запросить его ещё раз.'**
  String get permissionDeniedMessage;

  /// No description provided for @permissionPermanentlyDeniedMessage.
  ///
  /// In ru, this message translates to:
  /// **'Система больше не показывает окно запроса. Откройте настройки и разрешите чтение контактов.'**
  String get permissionPermanentlyDeniedMessage;

  /// No description provided for @permissionRestrictedMessage.
  ///
  /// In ru, this message translates to:
  /// **'Доступ ограничен системой, например родительским контролем, и не может быть запрошен из приложения.'**
  String get permissionRestrictedMessage;

  /// No description provided for @permissionNotDeterminedMessage.
  ///
  /// In ru, this message translates to:
  /// **'Чтобы показать список, приложению нужен доступ на чтение адресной книги.'**
  String get permissionNotDeterminedMessage;

  /// No description provided for @permissionUnknownMessage.
  ///
  /// In ru, this message translates to:
  /// **'Не удалось определить текущее разрешение.'**
  String get permissionUnknownMessage;

  /// No description provided for @limitedAccessBanner.
  ///
  /// In ru, this message translates to:
  /// **'Доступ ограничен: показаны только выбранные контакты.'**
  String get limitedAccessBanner;

  /// No description provided for @allowAccess.
  ///
  /// In ru, this message translates to:
  /// **'Разрешить доступ'**
  String get allowAccess;

  /// No description provided for @openSettings.
  ///
  /// In ru, this message translates to:
  /// **'Открыть настройки'**
  String get openSettings;

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

  /// No description provided for @errorLoadContacts.
  ///
  /// In ru, this message translates to:
  /// **'Не удалось прочитать адресную книгу.'**
  String get errorLoadContacts;

  /// No description provided for @errorLoadDetails.
  ///
  /// In ru, this message translates to:
  /// **'Не удалось получить сведения о контакте.'**
  String get errorLoadDetails;

  /// No description provided for @errorContactNotFound.
  ///
  /// In ru, this message translates to:
  /// **'Контакт не найден. Возможно, он удалён из адресной книги.'**
  String get errorContactNotFound;

  /// No description provided for @errorPermissionCheck.
  ///
  /// In ru, this message translates to:
  /// **'Не удалось проверить разрешение на чтение контактов.'**
  String get errorPermissionCheck;

  /// No description provided for @sectionPhones.
  ///
  /// In ru, this message translates to:
  /// **'Телефоны'**
  String get sectionPhones;

  /// No description provided for @sectionEmails.
  ///
  /// In ru, this message translates to:
  /// **'Электронная почта'**
  String get sectionEmails;

  /// No description provided for @sectionAddresses.
  ///
  /// In ru, this message translates to:
  /// **'Адреса'**
  String get sectionAddresses;

  /// No description provided for @sectionOrganization.
  ///
  /// In ru, this message translates to:
  /// **'Организация'**
  String get sectionOrganization;

  /// No description provided for @sectionWebsites.
  ///
  /// In ru, this message translates to:
  /// **'Сайты'**
  String get sectionWebsites;

  /// No description provided for @sectionSocial.
  ///
  /// In ru, this message translates to:
  /// **'Социальные сети'**
  String get sectionSocial;

  /// No description provided for @sectionEvents.
  ///
  /// In ru, this message translates to:
  /// **'События'**
  String get sectionEvents;

  /// No description provided for @sectionRelations.
  ///
  /// In ru, this message translates to:
  /// **'Связи'**
  String get sectionRelations;

  /// No description provided for @sectionNotes.
  ///
  /// In ru, this message translates to:
  /// **'Заметки'**
  String get sectionNotes;

  /// No description provided for @sectionName.
  ///
  /// In ru, this message translates to:
  /// **'Имя'**
  String get sectionName;

  /// No description provided for @fieldFirstName.
  ///
  /// In ru, this message translates to:
  /// **'Имя'**
  String get fieldFirstName;

  /// No description provided for @fieldMiddleName.
  ///
  /// In ru, this message translates to:
  /// **'Отчество'**
  String get fieldMiddleName;

  /// No description provided for @fieldLastName.
  ///
  /// In ru, this message translates to:
  /// **'Фамилия'**
  String get fieldLastName;

  /// No description provided for @fieldPrefix.
  ///
  /// In ru, this message translates to:
  /// **'Обращение'**
  String get fieldPrefix;

  /// No description provided for @fieldSuffix.
  ///
  /// In ru, this message translates to:
  /// **'Суффикс'**
  String get fieldSuffix;

  /// No description provided for @fieldNickname.
  ///
  /// In ru, this message translates to:
  /// **'Псевдоним'**
  String get fieldNickname;

  /// No description provided for @fieldPhonetic.
  ///
  /// In ru, this message translates to:
  /// **'Фонетическая запись'**
  String get fieldPhonetic;

  /// No description provided for @fieldPreviousFamilyName.
  ///
  /// In ru, this message translates to:
  /// **'Прежняя фамилия'**
  String get fieldPreviousFamilyName;

  /// No description provided for @labelICloud.
  ///
  /// In ru, this message translates to:
  /// **'iCloud'**
  String get labelICloud;

  /// No description provided for @fieldCompany.
  ///
  /// In ru, this message translates to:
  /// **'Организация'**
  String get fieldCompany;

  /// No description provided for @fieldJobTitle.
  ///
  /// In ru, this message translates to:
  /// **'Должность'**
  String get fieldJobTitle;

  /// No description provided for @fieldDepartment.
  ///
  /// In ru, this message translates to:
  /// **'Отдел'**
  String get fieldDepartment;

  /// No description provided for @fieldJobDescription.
  ///
  /// In ru, this message translates to:
  /// **'Описание должности'**
  String get fieldJobDescription;

  /// No description provided for @fieldOfficeLocation.
  ///
  /// In ru, this message translates to:
  /// **'Офис'**
  String get fieldOfficeLocation;

  /// No description provided for @fieldSymbol.
  ///
  /// In ru, this message translates to:
  /// **'Символ'**
  String get fieldSymbol;

  /// No description provided for @favoriteContact.
  ///
  /// In ru, this message translates to:
  /// **'Избранный контакт'**
  String get favoriteContact;

  /// No description provided for @noExtraFields.
  ///
  /// In ru, this message translates to:
  /// **'Дополнительных сведений нет.'**
  String get noExtraFields;

  /// No description provided for @labelMobile.
  ///
  /// In ru, this message translates to:
  /// **'Мобильный'**
  String get labelMobile;

  /// No description provided for @labelHome.
  ///
  /// In ru, this message translates to:
  /// **'Домашний'**
  String get labelHome;

  /// No description provided for @labelWork.
  ///
  /// In ru, this message translates to:
  /// **'Рабочий'**
  String get labelWork;

  /// No description provided for @labelMain.
  ///
  /// In ru, this message translates to:
  /// **'Основной'**
  String get labelMain;

  /// No description provided for @labelOther.
  ///
  /// In ru, this message translates to:
  /// **'Другой'**
  String get labelOther;

  /// No description provided for @labelCustom.
  ///
  /// In ru, this message translates to:
  /// **'Своя метка'**
  String get labelCustom;

  /// No description provided for @labelSchool.
  ///
  /// In ru, this message translates to:
  /// **'Школа'**
  String get labelSchool;

  /// No description provided for @labelIPhone.
  ///
  /// In ru, this message translates to:
  /// **'iPhone'**
  String get labelIPhone;

  /// No description provided for @labelPager.
  ///
  /// In ru, this message translates to:
  /// **'Пейджер'**
  String get labelPager;

  /// No description provided for @labelHomeFax.
  ///
  /// In ru, this message translates to:
  /// **'Домашний факс'**
  String get labelHomeFax;

  /// No description provided for @labelWorkFax.
  ///
  /// In ru, this message translates to:
  /// **'Рабочий факс'**
  String get labelWorkFax;

  /// No description provided for @labelOtherFax.
  ///
  /// In ru, this message translates to:
  /// **'Другой факс'**
  String get labelOtherFax;

  /// No description provided for @labelWorkMobile.
  ///
  /// In ru, this message translates to:
  /// **'Рабочий мобильный'**
  String get labelWorkMobile;

  /// No description provided for @labelWorkPager.
  ///
  /// In ru, this message translates to:
  /// **'Рабочий пейджер'**
  String get labelWorkPager;

  /// No description provided for @labelCompanyMain.
  ///
  /// In ru, this message translates to:
  /// **'Основной компании'**
  String get labelCompanyMain;

  /// No description provided for @labelAssistant.
  ///
  /// In ru, this message translates to:
  /// **'Ассистент'**
  String get labelAssistant;

  /// No description provided for @labelCar.
  ///
  /// In ru, this message translates to:
  /// **'Автомобиль'**
  String get labelCar;

  /// No description provided for @labelCallback.
  ///
  /// In ru, this message translates to:
  /// **'Обратный звонок'**
  String get labelCallback;

  /// No description provided for @labelRadio.
  ///
  /// In ru, this message translates to:
  /// **'Рация'**
  String get labelRadio;

  /// No description provided for @labelIsdn.
  ///
  /// In ru, this message translates to:
  /// **'ISDN'**
  String get labelIsdn;

  /// No description provided for @labelMms.
  ///
  /// In ru, this message translates to:
  /// **'MMS'**
  String get labelMms;

  /// No description provided for @labelTelex.
  ///
  /// In ru, this message translates to:
  /// **'Телекс'**
  String get labelTelex;

  /// No description provided for @labelTtyTdd.
  ///
  /// In ru, this message translates to:
  /// **'TTY/TDD'**
  String get labelTtyTdd;

  /// No description provided for @labelAppleWatch.
  ///
  /// In ru, this message translates to:
  /// **'Apple Watch'**
  String get labelAppleWatch;

  /// No description provided for @labelBirthday.
  ///
  /// In ru, this message translates to:
  /// **'День рождения'**
  String get labelBirthday;

  /// No description provided for @labelAnniversary.
  ///
  /// In ru, this message translates to:
  /// **'Годовщина'**
  String get labelAnniversary;

  /// No description provided for @labelHomepage.
  ///
  /// In ru, this message translates to:
  /// **'Домашняя страница'**
  String get labelHomepage;

  /// No description provided for @labelBlog.
  ///
  /// In ru, this message translates to:
  /// **'Блог'**
  String get labelBlog;

  /// No description provided for @labelProfile.
  ///
  /// In ru, this message translates to:
  /// **'Профиль'**
  String get labelProfile;

  /// No description provided for @labelFtp.
  ///
  /// In ru, this message translates to:
  /// **'FTP'**
  String get labelFtp;

  /// No description provided for @labelBrother.
  ///
  /// In ru, this message translates to:
  /// **'Брат'**
  String get labelBrother;

  /// No description provided for @labelSister.
  ///
  /// In ru, this message translates to:
  /// **'Сестра'**
  String get labelSister;

  /// No description provided for @labelMother.
  ///
  /// In ru, this message translates to:
  /// **'Мать'**
  String get labelMother;

  /// No description provided for @labelFather.
  ///
  /// In ru, this message translates to:
  /// **'Отец'**
  String get labelFather;

  /// No description provided for @labelParent.
  ///
  /// In ru, this message translates to:
  /// **'Родитель'**
  String get labelParent;

  /// No description provided for @labelChild.
  ///
  /// In ru, this message translates to:
  /// **'Ребёнок'**
  String get labelChild;

  /// No description provided for @labelSpouse.
  ///
  /// In ru, this message translates to:
  /// **'Супруг(а)'**
  String get labelSpouse;

  /// No description provided for @labelPartner.
  ///
  /// In ru, this message translates to:
  /// **'Партнёр'**
  String get labelPartner;

  /// No description provided for @labelFriend.
  ///
  /// In ru, this message translates to:
  /// **'Друг'**
  String get labelFriend;

  /// No description provided for @labelManager.
  ///
  /// In ru, this message translates to:
  /// **'Руководитель'**
  String get labelManager;

  /// No description provided for @labelRelative.
  ///
  /// In ru, this message translates to:
  /// **'Родственник'**
  String get labelRelative;

  /// No description provided for @labelDomesticPartner.
  ///
  /// In ru, this message translates to:
  /// **'Гражданский партнёр'**
  String get labelDomesticPartner;

  /// No description provided for @labelReferredBy.
  ///
  /// In ru, this message translates to:
  /// **'Рекомендован'**
  String get labelReferredBy;

  /// No description provided for @labelAunt.
  ///
  /// In ru, this message translates to:
  /// **'Тётя'**
  String get labelAunt;

  /// No description provided for @labelBoyfriend.
  ///
  /// In ru, this message translates to:
  /// **'Парень'**
  String get labelBoyfriend;

  /// No description provided for @labelBrotherInLaw.
  ///
  /// In ru, this message translates to:
  /// **'Шурин / деверь'**
  String get labelBrotherInLaw;

  /// No description provided for @labelChildInLaw.
  ///
  /// In ru, this message translates to:
  /// **'Зять / невестка'**
  String get labelChildInLaw;

  /// No description provided for @labelColleague.
  ///
  /// In ru, this message translates to:
  /// **'Коллега'**
  String get labelColleague;

  /// No description provided for @labelCousin.
  ///
  /// In ru, this message translates to:
  /// **'Двоюродный родственник'**
  String get labelCousin;

  /// No description provided for @labelDaughter.
  ///
  /// In ru, this message translates to:
  /// **'Дочь'**
  String get labelDaughter;

  /// No description provided for @labelFatherInLaw.
  ///
  /// In ru, this message translates to:
  /// **'Свёкор / тесть'**
  String get labelFatherInLaw;

  /// No description provided for @labelGirlfriend.
  ///
  /// In ru, this message translates to:
  /// **'Девушка'**
  String get labelGirlfriend;

  /// No description provided for @labelGrandchild.
  ///
  /// In ru, this message translates to:
  /// **'Внук или внучка'**
  String get labelGrandchild;

  /// No description provided for @labelGranddaughter.
  ///
  /// In ru, this message translates to:
  /// **'Внучка'**
  String get labelGranddaughter;

  /// No description provided for @labelGrandfather.
  ///
  /// In ru, this message translates to:
  /// **'Дедушка'**
  String get labelGrandfather;

  /// No description provided for @labelGrandmother.
  ///
  /// In ru, this message translates to:
  /// **'Бабушка'**
  String get labelGrandmother;

  /// No description provided for @labelGrandparent.
  ///
  /// In ru, this message translates to:
  /// **'Бабушка или дедушка'**
  String get labelGrandparent;

  /// No description provided for @labelGrandson.
  ///
  /// In ru, this message translates to:
  /// **'Внук'**
  String get labelGrandson;

  /// No description provided for @labelHusband.
  ///
  /// In ru, this message translates to:
  /// **'Муж'**
  String get labelHusband;

  /// No description provided for @labelMotherInLaw.
  ///
  /// In ru, this message translates to:
  /// **'Свекровь / тёща'**
  String get labelMotherInLaw;

  /// No description provided for @labelNephew.
  ///
  /// In ru, this message translates to:
  /// **'Племянник'**
  String get labelNephew;

  /// No description provided for @labelNiece.
  ///
  /// In ru, this message translates to:
  /// **'Племянница'**
  String get labelNiece;

  /// No description provided for @labelParentInLaw.
  ///
  /// In ru, this message translates to:
  /// **'Родитель супруга'**
  String get labelParentInLaw;

  /// No description provided for @labelSibling.
  ///
  /// In ru, this message translates to:
  /// **'Брат или сестра'**
  String get labelSibling;

  /// No description provided for @labelSiblingInLaw.
  ///
  /// In ru, this message translates to:
  /// **'Родственник супруга'**
  String get labelSiblingInLaw;

  /// No description provided for @labelSisterInLaw.
  ///
  /// In ru, this message translates to:
  /// **'Золовка / свояченица'**
  String get labelSisterInLaw;

  /// No description provided for @labelSon.
  ///
  /// In ru, this message translates to:
  /// **'Сын'**
  String get labelSon;

  /// No description provided for @labelStepbrother.
  ///
  /// In ru, this message translates to:
  /// **'Сводный брат'**
  String get labelStepbrother;

  /// No description provided for @labelStepchild.
  ///
  /// In ru, this message translates to:
  /// **'Пасынок или падчерица'**
  String get labelStepchild;

  /// No description provided for @labelStepdaughter.
  ///
  /// In ru, this message translates to:
  /// **'Падчерица'**
  String get labelStepdaughter;

  /// No description provided for @labelStepfather.
  ///
  /// In ru, this message translates to:
  /// **'Отчим'**
  String get labelStepfather;

  /// No description provided for @labelStepmother.
  ///
  /// In ru, this message translates to:
  /// **'Мачеха'**
  String get labelStepmother;

  /// No description provided for @labelStepparent.
  ///
  /// In ru, this message translates to:
  /// **'Отчим или мачеха'**
  String get labelStepparent;

  /// No description provided for @labelStepsister.
  ///
  /// In ru, this message translates to:
  /// **'Сводная сестра'**
  String get labelStepsister;

  /// No description provided for @labelStepson.
  ///
  /// In ru, this message translates to:
  /// **'Пасынок'**
  String get labelStepson;

  /// No description provided for @labelTeacher.
  ///
  /// In ru, this message translates to:
  /// **'Учитель'**
  String get labelTeacher;

  /// No description provided for @labelUncle.
  ///
  /// In ru, this message translates to:
  /// **'Дядя'**
  String get labelUncle;

  /// No description provided for @labelWife.
  ///
  /// In ru, this message translates to:
  /// **'Жена'**
  String get labelWife;

  /// No description provided for @socialAim.
  ///
  /// In ru, this message translates to:
  /// **'AIM'**
  String get socialAim;

  /// No description provided for @socialFacebook.
  ///
  /// In ru, this message translates to:
  /// **'Facebook'**
  String get socialFacebook;

  /// No description provided for @socialFlickr.
  ///
  /// In ru, this message translates to:
  /// **'Flickr'**
  String get socialFlickr;

  /// No description provided for @socialInstagram.
  ///
  /// In ru, this message translates to:
  /// **'Instagram'**
  String get socialInstagram;

  /// No description provided for @socialJabber.
  ///
  /// In ru, this message translates to:
  /// **'Jabber'**
  String get socialJabber;

  /// No description provided for @socialLine.
  ///
  /// In ru, this message translates to:
  /// **'LINE'**
  String get socialLine;

  /// No description provided for @socialLinkedIn.
  ///
  /// In ru, this message translates to:
  /// **'LinkedIn'**
  String get socialLinkedIn;

  /// No description provided for @socialMessenger.
  ///
  /// In ru, this message translates to:
  /// **'Messenger'**
  String get socialMessenger;

  /// No description provided for @socialMsn.
  ///
  /// In ru, this message translates to:
  /// **'MSN'**
  String get socialMsn;

  /// No description provided for @socialMySpace.
  ///
  /// In ru, this message translates to:
  /// **'MySpace'**
  String get socialMySpace;

  /// No description provided for @socialPinterest.
  ///
  /// In ru, this message translates to:
  /// **'Pinterest'**
  String get socialPinterest;

  /// No description provided for @socialQq.
  ///
  /// In ru, this message translates to:
  /// **'QQ'**
  String get socialQq;

  /// No description provided for @socialSkype.
  ///
  /// In ru, this message translates to:
  /// **'Skype'**
  String get socialSkype;

  /// No description provided for @socialSnapchat.
  ///
  /// In ru, this message translates to:
  /// **'Snapchat'**
  String get socialSnapchat;

  /// No description provided for @socialTelegram.
  ///
  /// In ru, this message translates to:
  /// **'Telegram'**
  String get socialTelegram;

  /// No description provided for @socialTumblr.
  ///
  /// In ru, this message translates to:
  /// **'Tumblr'**
  String get socialTumblr;

  /// No description provided for @socialTwitter.
  ///
  /// In ru, this message translates to:
  /// **'Twitter'**
  String get socialTwitter;

  /// No description provided for @socialViber.
  ///
  /// In ru, this message translates to:
  /// **'Viber'**
  String get socialViber;

  /// No description provided for @socialWechat.
  ///
  /// In ru, this message translates to:
  /// **'WeChat'**
  String get socialWechat;

  /// No description provided for @socialWhatsapp.
  ///
  /// In ru, this message translates to:
  /// **'WhatsApp'**
  String get socialWhatsapp;

  /// No description provided for @socialYahoo.
  ///
  /// In ru, this message translates to:
  /// **'Yahoo'**
  String get socialYahoo;

  /// No description provided for @socialYoutube.
  ///
  /// In ru, this message translates to:
  /// **'YouTube'**
  String get socialYoutube;

  /// No description provided for @socialDiscord.
  ///
  /// In ru, this message translates to:
  /// **'Discord'**
  String get socialDiscord;

  /// No description provided for @socialTiktok.
  ///
  /// In ru, this message translates to:
  /// **'TikTok'**
  String get socialTiktok;

  /// No description provided for @socialGameCenter.
  ///
  /// In ru, this message translates to:
  /// **'Game Center'**
  String get socialGameCenter;

  /// No description provided for @socialGoogleTalk.
  ///
  /// In ru, this message translates to:
  /// **'Google Talk'**
  String get socialGoogleTalk;

  /// No description provided for @socialIcq.
  ///
  /// In ru, this message translates to:
  /// **'ICQ'**
  String get socialIcq;

  /// No description provided for @socialSinaWeibo.
  ///
  /// In ru, this message translates to:
  /// **'Sina Weibo'**
  String get socialSinaWeibo;

  /// No description provided for @socialTencentWeibo.
  ///
  /// In ru, this message translates to:
  /// **'Tencent Weibo'**
  String get socialTencentWeibo;

  /// No description provided for @socialGaduGadu.
  ///
  /// In ru, this message translates to:
  /// **'Gadu-Gadu'**
  String get socialGaduGadu;

  /// No description provided for @socialBaiduTieba.
  ///
  /// In ru, this message translates to:
  /// **'Baidu Tieba'**
  String get socialBaiduTieba;

  /// No description provided for @socialNetmeeting.
  ///
  /// In ru, this message translates to:
  /// **'NetMeeting'**
  String get socialNetmeeting;

  /// No description provided for @socialYelp.
  ///
  /// In ru, this message translates to:
  /// **'Yelp'**
  String get socialYelp;

  /// No description provided for @socialMedium.
  ///
  /// In ru, this message translates to:
  /// **'Medium'**
  String get socialMedium;
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
      <String>['en', 'ru'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
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
