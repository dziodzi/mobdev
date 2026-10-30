// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Contacts';

  @override
  String get searchHint => 'Search by name';

  @override
  String get clearSearch => 'Clear search';

  @override
  String foundContacts(int count) {
    return 'Contacts found: $count';
  }

  @override
  String get sortAscending => 'By name ↑';

  @override
  String get sortDescending => 'By name ↓';

  @override
  String get sortTooltip => 'Sort';

  @override
  String get phoneNotSpecified => 'No phone number';

  @override
  String get unnamedContact => 'Unnamed';

  @override
  String get hasEmail => 'Has an email address';

  @override
  String get checkingPermission => 'Checking contacts access…';

  @override
  String get loadingContacts => 'Loading contacts…';

  @override
  String get loadingDetails => 'Loading details…';

  @override
  String get emptyContactsTitle => 'The address book has no contacts yet';

  @override
  String get emptyContactsMessage =>
      'Contacts from the device address book will appear here.';

  @override
  String get emptySearchTitle => 'No contacts found';

  @override
  String get emptySearchMessage =>
      'Nothing in the loaded list matches this query.';

  @override
  String get permissionTitle => 'Contacts access';

  @override
  String get permissionExplanation =>
      'The app only reads the address book. It does not create, edit, or delete contacts.';

  @override
  String get permissionDeniedMessage =>
      'Access was not granted. You can request it again.';

  @override
  String get permissionPermanentlyDeniedMessage =>
      'The system will no longer show the permission dialog. Open settings and allow reading contacts.';

  @override
  String get permissionRestrictedMessage =>
      'Access is restricted by the system, for example by parental controls, and cannot be requested from the app.';

  @override
  String get permissionNotDeterminedMessage =>
      'Reading the address book requires permission before the list can be shown.';

  @override
  String get permissionUnknownMessage =>
      'The current permission could not be determined.';

  @override
  String get limitedAccessBanner =>
      'Access is limited: only selected contacts are shown.';

  @override
  String get allowAccess => 'Allow access';

  @override
  String get openSettings => 'Open settings';

  @override
  String get retry => 'Retry';

  @override
  String get refresh => 'Refresh';

  @override
  String get errorLoadContacts => 'The address book could not be read.';

  @override
  String get errorLoadDetails => 'Contact details could not be loaded.';

  @override
  String get errorContactNotFound =>
      'Contact not found. It may have been deleted from the address book.';

  @override
  String get errorPermissionCheck =>
      'Contacts permission could not be checked.';

  @override
  String get sectionPhones => 'Phones';

  @override
  String get sectionEmails => 'Email';

  @override
  String get sectionAddresses => 'Addresses';

  @override
  String get sectionOrganization => 'Organization';

  @override
  String get sectionWebsites => 'Websites';

  @override
  String get sectionSocial => 'Social networks';

  @override
  String get sectionEvents => 'Events';

  @override
  String get sectionRelations => 'Relations';

  @override
  String get sectionNotes => 'Notes';

  @override
  String get sectionName => 'Name';

  @override
  String get fieldFirstName => 'First name';

  @override
  String get fieldMiddleName => 'Middle name';

  @override
  String get fieldLastName => 'Last name';

  @override
  String get fieldPrefix => 'Prefix';

  @override
  String get fieldSuffix => 'Suffix';

  @override
  String get fieldNickname => 'Nickname';

  @override
  String get fieldPhonetic => 'Phonetic name';

  @override
  String get fieldPreviousFamilyName => 'Previous family name';

  @override
  String get labelICloud => 'iCloud';

  @override
  String get fieldCompany => 'Organization';

  @override
  String get fieldJobTitle => 'Job title';

  @override
  String get fieldDepartment => 'Department';

  @override
  String get fieldJobDescription => 'Job description';

  @override
  String get fieldOfficeLocation => 'Office';

  @override
  String get fieldSymbol => 'Symbol';

  @override
  String get favoriteContact => 'Favorite contact';

  @override
  String get noExtraFields => 'No additional details.';

  @override
  String get labelMobile => 'Mobile';

  @override
  String get labelHome => 'Home';

  @override
  String get labelWork => 'Work';

  @override
  String get labelMain => 'Main';

  @override
  String get labelOther => 'Other';

  @override
  String get labelCustom => 'Custom label';

  @override
  String get labelSchool => 'School';

  @override
  String get labelIPhone => 'iPhone';

  @override
  String get labelPager => 'Pager';

  @override
  String get labelHomeFax => 'Home fax';

  @override
  String get labelWorkFax => 'Work fax';

  @override
  String get labelOtherFax => 'Other fax';

  @override
  String get labelWorkMobile => 'Work mobile';

  @override
  String get labelWorkPager => 'Work pager';

  @override
  String get labelCompanyMain => 'Company main';

  @override
  String get labelAssistant => 'Assistant';

  @override
  String get labelCar => 'Car';

  @override
  String get labelCallback => 'Callback';

  @override
  String get labelRadio => 'Radio';

  @override
  String get labelIsdn => 'ISDN';

  @override
  String get labelMms => 'MMS';

  @override
  String get labelTelex => 'Telex';

  @override
  String get labelTtyTdd => 'TTY/TDD';

  @override
  String get labelAppleWatch => 'Apple Watch';

  @override
  String get labelBirthday => 'Birthday';

  @override
  String get labelAnniversary => 'Anniversary';

  @override
  String get labelHomepage => 'Homepage';

  @override
  String get labelBlog => 'Blog';

  @override
  String get labelProfile => 'Profile';

  @override
  String get labelFtp => 'FTP';

  @override
  String get labelBrother => 'Brother';

  @override
  String get labelSister => 'Sister';

  @override
  String get labelMother => 'Mother';

  @override
  String get labelFather => 'Father';

  @override
  String get labelParent => 'Parent';

  @override
  String get labelChild => 'Child';

  @override
  String get labelSpouse => 'Spouse';

  @override
  String get labelPartner => 'Partner';

  @override
  String get labelFriend => 'Friend';

  @override
  String get labelManager => 'Manager';

  @override
  String get labelRelative => 'Relative';

  @override
  String get labelDomesticPartner => 'Domestic partner';

  @override
  String get labelReferredBy => 'Referred by';

  @override
  String get labelAunt => 'Aunt';

  @override
  String get labelBoyfriend => 'Boyfriend';

  @override
  String get labelBrotherInLaw => 'Brother-in-law';

  @override
  String get labelChildInLaw => 'Child-in-law';

  @override
  String get labelColleague => 'Colleague';

  @override
  String get labelCousin => 'Cousin';

  @override
  String get labelDaughter => 'Daughter';

  @override
  String get labelFatherInLaw => 'Father-in-law';

  @override
  String get labelGirlfriend => 'Girlfriend';

  @override
  String get labelGrandchild => 'Grandchild';

  @override
  String get labelGranddaughter => 'Granddaughter';

  @override
  String get labelGrandfather => 'Grandfather';

  @override
  String get labelGrandmother => 'Grandmother';

  @override
  String get labelGrandparent => 'Grandparent';

  @override
  String get labelGrandson => 'Grandson';

  @override
  String get labelHusband => 'Husband';

  @override
  String get labelMotherInLaw => 'Mother-in-law';

  @override
  String get labelNephew => 'Nephew';

  @override
  String get labelNiece => 'Niece';

  @override
  String get labelParentInLaw => 'Parent-in-law';

  @override
  String get labelSibling => 'Sibling';

  @override
  String get labelSiblingInLaw => 'Sibling-in-law';

  @override
  String get labelSisterInLaw => 'Sister-in-law';

  @override
  String get labelSon => 'Son';

  @override
  String get labelStepbrother => 'Stepbrother';

  @override
  String get labelStepchild => 'Stepchild';

  @override
  String get labelStepdaughter => 'Stepdaughter';

  @override
  String get labelStepfather => 'Stepfather';

  @override
  String get labelStepmother => 'Stepmother';

  @override
  String get labelStepparent => 'Stepparent';

  @override
  String get labelStepsister => 'Stepsister';

  @override
  String get labelStepson => 'Stepson';

  @override
  String get labelTeacher => 'Teacher';

  @override
  String get labelUncle => 'Uncle';

  @override
  String get labelWife => 'Wife';

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
