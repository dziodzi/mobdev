import 'package:flutter/widgets.dart';

import '../l10n/app_localizations.dart';
import '../data/api_client.dart';

AppLocalizations strings(BuildContext context) => AppLocalizations.of(context)!;
String failureText(BuildContext context, Failure failure) {
  final s = strings(context);
  return switch (failure) {
    Failure.invalid => s.invalid,
    Failure.connection => s.connection,
    Failure.server => s.server,
    Failure.expired => s.expired,
    Failure.protocol => s.protocol,
    Failure.storage => s.storage,
  };
}
