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
  String get searchHint => 'Поиск по имени';

  @override
  String get clearSearch => 'Очистить поиск';

  @override
  String foundContacts(int count) {
    return 'Найдено контактов: $count';
  }

  @override
  String get sortAscending => 'По имени ↑';

  @override
  String get sortDescending => 'По имени ↓';

  @override
  String get sortTooltip => 'Сортировка';

  @override
  String get phoneNotSpecified => 'Номер не указан';

  @override
  String get unnamedContact => 'Без имени';

  @override
  String get hasEmail => 'Есть электронная почта';

  @override
  String get checkingPermission => 'Проверка доступа к контактам…';

  @override
  String get loadingContacts => 'Загрузка контактов…';

  @override
  String get loadingDetails => 'Загрузка сведений…';

  @override
  String get emptyContactsTitle => 'В адресной книге пока нет контактов';

  @override
  String get emptyContactsMessage =>
      'Когда в телефонной книге появятся контакты, они отобразятся здесь.';

  @override
  String get emptySearchTitle => 'Контакты не найдены';

  @override
  String get emptySearchMessage =>
      'По этому запросу в уже загруженном списке нет совпадений.';

  @override
  String get permissionTitle => 'Доступ к контактам';

  @override
  String get permissionExplanation =>
      'Приложение только читает адресную книгу: оно не создаёт, не изменяет и не удаляет контакты.';

  @override
  String get permissionDeniedMessage =>
      'Доступ не предоставлен. Можно запросить его ещё раз.';

  @override
  String get permissionPermanentlyDeniedMessage =>
      'Система больше не показывает окно запроса. Откройте настройки и разрешите чтение контактов.';

  @override
  String get permissionRestrictedMessage =>
      'Доступ ограничен системой, например родительским контролем, и не может быть запрошен из приложения.';

  @override
  String get permissionNotDeterminedMessage =>
      'Чтобы показать список, приложению нужен доступ на чтение адресной книги.';

  @override
  String get permissionUnknownMessage =>
      'Не удалось определить текущее разрешение.';

  @override
  String get limitedAccessBanner =>
      'Доступ ограничен: показаны только выбранные контакты.';

  @override
  String get allowAccess => 'Разрешить доступ';

  @override
  String get openSettings => 'Открыть настройки';

  @override
  String get retry => 'Повторить';

  @override
  String get refresh => 'Обновить';

  @override
  String get errorLoadContacts => 'Не удалось прочитать адресную книгу.';

  @override
  String get errorLoadDetails => 'Не удалось получить сведения о контакте.';

  @override
  String get errorContactNotFound =>
      'Контакт не найден. Возможно, он удалён из адресной книги.';

  @override
  String get errorPermissionCheck =>
      'Не удалось проверить разрешение на чтение контактов.';

  @override
  String get sectionPhones => 'Телефоны';

  @override
  String get sectionEmails => 'Электронная почта';

  @override
  String get sectionAddresses => 'Адреса';

  @override
  String get sectionOrganization => 'Организация';

  @override
  String get sectionWebsites => 'Сайты';

  @override
  String get sectionSocial => 'Социальные сети';

  @override
  String get sectionEvents => 'События';

  @override
  String get sectionRelations => 'Связи';

  @override
  String get sectionNotes => 'Заметки';

  @override
  String get sectionName => 'Имя';

  @override
  String get fieldFirstName => 'Имя';

  @override
  String get fieldMiddleName => 'Отчество';

  @override
  String get fieldLastName => 'Фамилия';

  @override
  String get fieldPrefix => 'Обращение';

  @override
  String get fieldSuffix => 'Суффикс';

  @override
  String get fieldNickname => 'Псевдоним';

  @override
  String get fieldPhonetic => 'Фонетическая запись';

  @override
  String get fieldPreviousFamilyName => 'Прежняя фамилия';

  @override
  String get labelICloud => 'iCloud';

  @override
  String get fieldCompany => 'Организация';

  @override
  String get fieldJobTitle => 'Должность';

  @override
  String get fieldDepartment => 'Отдел';

  @override
  String get fieldJobDescription => 'Описание должности';

  @override
  String get fieldOfficeLocation => 'Офис';

  @override
  String get fieldSymbol => 'Символ';

  @override
  String get favoriteContact => 'Избранный контакт';

  @override
  String get noExtraFields => 'Дополнительных сведений нет.';

  @override
  String get labelMobile => 'Мобильный';

  @override
  String get labelHome => 'Домашний';

  @override
  String get labelWork => 'Рабочий';

  @override
  String get labelMain => 'Основной';

  @override
  String get labelOther => 'Другой';

  @override
  String get labelCustom => 'Своя метка';

  @override
  String get labelSchool => 'Школа';

  @override
  String get labelIPhone => 'iPhone';

  @override
  String get labelPager => 'Пейджер';

  @override
  String get labelHomeFax => 'Домашний факс';

  @override
  String get labelWorkFax => 'Рабочий факс';

  @override
  String get labelOtherFax => 'Другой факс';

  @override
  String get labelWorkMobile => 'Рабочий мобильный';

  @override
  String get labelWorkPager => 'Рабочий пейджер';

  @override
  String get labelCompanyMain => 'Основной компании';

  @override
  String get labelAssistant => 'Ассистент';

  @override
  String get labelCar => 'Автомобиль';

  @override
  String get labelCallback => 'Обратный звонок';

  @override
  String get labelRadio => 'Рация';

  @override
  String get labelIsdn => 'ISDN';

  @override
  String get labelMms => 'MMS';

  @override
  String get labelTelex => 'Телекс';

  @override
  String get labelTtyTdd => 'TTY/TDD';

  @override
  String get labelAppleWatch => 'Apple Watch';

  @override
  String get labelBirthday => 'День рождения';

  @override
  String get labelAnniversary => 'Годовщина';

  @override
  String get labelHomepage => 'Домашняя страница';

  @override
  String get labelBlog => 'Блог';

  @override
  String get labelProfile => 'Профиль';

  @override
  String get labelFtp => 'FTP';

  @override
  String get labelBrother => 'Брат';

  @override
  String get labelSister => 'Сестра';

  @override
  String get labelMother => 'Мать';

  @override
  String get labelFather => 'Отец';

  @override
  String get labelParent => 'Родитель';

  @override
  String get labelChild => 'Ребёнок';

  @override
  String get labelSpouse => 'Супруг(а)';

  @override
  String get labelPartner => 'Партнёр';

  @override
  String get labelFriend => 'Друг';

  @override
  String get labelManager => 'Руководитель';

  @override
  String get labelRelative => 'Родственник';

  @override
  String get labelDomesticPartner => 'Гражданский партнёр';

  @override
  String get labelReferredBy => 'Рекомендован';

  @override
  String get labelAunt => 'Тётя';

  @override
  String get labelBoyfriend => 'Парень';

  @override
  String get labelBrotherInLaw => 'Шурин / деверь';

  @override
  String get labelChildInLaw => 'Зять / невестка';

  @override
  String get labelColleague => 'Коллега';

  @override
  String get labelCousin => 'Двоюродный родственник';

  @override
  String get labelDaughter => 'Дочь';

  @override
  String get labelFatherInLaw => 'Свёкор / тесть';

  @override
  String get labelGirlfriend => 'Девушка';

  @override
  String get labelGrandchild => 'Внук или внучка';

  @override
  String get labelGranddaughter => 'Внучка';

  @override
  String get labelGrandfather => 'Дедушка';

  @override
  String get labelGrandmother => 'Бабушка';

  @override
  String get labelGrandparent => 'Бабушка или дедушка';

  @override
  String get labelGrandson => 'Внук';

  @override
  String get labelHusband => 'Муж';

  @override
  String get labelMotherInLaw => 'Свекровь / тёща';

  @override
  String get labelNephew => 'Племянник';

  @override
  String get labelNiece => 'Племянница';

  @override
  String get labelParentInLaw => 'Родитель супруга';

  @override
  String get labelSibling => 'Брат или сестра';

  @override
  String get labelSiblingInLaw => 'Родственник супруга';

  @override
  String get labelSisterInLaw => 'Золовка / свояченица';

  @override
  String get labelSon => 'Сын';

  @override
  String get labelStepbrother => 'Сводный брат';

  @override
  String get labelStepchild => 'Пасынок или падчерица';

  @override
  String get labelStepdaughter => 'Падчерица';

  @override
  String get labelStepfather => 'Отчим';

  @override
  String get labelStepmother => 'Мачеха';

  @override
  String get labelStepparent => 'Отчим или мачеха';

  @override
  String get labelStepsister => 'Сводная сестра';

  @override
  String get labelStepson => 'Пасынок';

  @override
  String get labelTeacher => 'Учитель';

  @override
  String get labelUncle => 'Дядя';

  @override
  String get labelWife => 'Жена';

  @override
  String get socialAim => 'AIM';

  @override
  String get socialFacebook => 'Facebook';

  @override
  String get socialFlickr => 'Flickr';

  @override
  String get socialInstagram => 'Instagram';

  @override
  String get socialJabber => 'Jabber';

  @override
  String get socialLine => 'LINE';

  @override
  String get socialLinkedIn => 'LinkedIn';

  @override
  String get socialMessenger => 'Messenger';

  @override
  String get socialMsn => 'MSN';

  @override
  String get socialMySpace => 'MySpace';

  @override
  String get socialPinterest => 'Pinterest';

  @override
  String get socialQq => 'QQ';

  @override
  String get socialSkype => 'Skype';

  @override
  String get socialSnapchat => 'Snapchat';

  @override
  String get socialTelegram => 'Telegram';

  @override
  String get socialTumblr => 'Tumblr';

  @override
  String get socialTwitter => 'Twitter';

  @override
  String get socialViber => 'Viber';

  @override
  String get socialWechat => 'WeChat';

  @override
  String get socialWhatsapp => 'WhatsApp';

  @override
  String get socialYahoo => 'Yahoo';

  @override
  String get socialYoutube => 'YouTube';

  @override
  String get socialDiscord => 'Discord';

  @override
  String get socialTiktok => 'TikTok';

  @override
  String get socialGameCenter => 'Game Center';

  @override
  String get socialGoogleTalk => 'Google Talk';

  @override
  String get socialIcq => 'ICQ';

  @override
  String get socialSinaWeibo => 'Sina Weibo';

  @override
  String get socialTencentWeibo => 'Tencent Weibo';

  @override
  String get socialGaduGadu => 'Gadu-Gadu';

  @override
  String get socialBaiduTieba => 'Baidu Tieba';

  @override
  String get socialNetmeeting => 'NetMeeting';

  @override
  String get socialYelp => 'Yelp';

  @override
  String get socialMedium => 'Medium';
}
