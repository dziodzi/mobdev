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
  String get refresh => 'Refresh contacts';

  @override
  String get loading => 'Loading contacts';

  @override
  String get permissionTitle => 'Contacts permission is required';

  @override
  String get permissionMessage =>
      'Allow access to display contacts stored on this device.';

  @override
  String get permissionPermanentMessage =>
      'Access cannot be requested again. Open application settings and enable Contacts permission.';

  @override
  String get requestAccess => 'Request access';

  @override
  String get openSettings => 'Open settings';

  @override
  String get retry => 'Try again';

  @override
  String get loadError => 'Could not read contacts';

  @override
  String get searchHint => 'Search by name';

  @override
  String get sortAscending => 'A to Z';

  @override
  String get sortDescending => 'Z to A';

  @override
  String countLabel(int count) {
    return 'Found: $count';
  }

  @override
  String get noContacts => 'The address book is empty';

  @override
  String get noResults => 'No contacts match the search';

  @override
  String get noPhone => 'No phone number';

  @override
  String get noEmail => 'No email';

  @override
  String get unknownContact => 'Unnamed contact';

  @override
  String get detailsTitle => 'Contact details';

  @override
  String get phones => 'Phone numbers';

  @override
  String get emails => 'Email';

  @override
  String get organization => 'Organization';

  @override
  String get addresses => 'Addresses';

  @override
  String get notes => 'Notes';
}
