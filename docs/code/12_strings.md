# lib/ui/strings.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
import 'package:flutter/widgets.dart';

import '../l10n/app_localizations.dart';
import '../data/api_client.dart';

// Исходная строка 6: Обёртка над AppLocalizations.of(context). Нужен context ниже MaterialApp с делегатами.
AppLocalizations strings(BuildContext context) => AppLocalizations.of(context)!;
// Исходная строка 7: Перевод технического Failure в пользовательскую строку ARB.
String failureText(BuildContext context, Failure failure) {
  final s = strings(context);
  // Исходная строка 9: Switch-expression возвращает значение; все варианты enum перечислены явно.
  return switch (failure) {
    Failure.invalid => s.invalid,
    Failure.connection => s.connection,
    Failure.server => s.server,
    Failure.expired => s.expired,
    Failure.protocol => s.protocol,
    Failure.storage => s.storage,
  };
}
```
