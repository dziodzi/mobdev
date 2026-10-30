import 'package:devicecontacts/controllers/contacts_controller.dart';
import 'package:devicecontacts/controllers/contacts_scope.dart';
import 'package:devicecontacts/l10n/app_localizations.dart';
import 'package:devicecontacts/repositories/contacts_repository.dart';
import 'package:devicecontacts/repositories/device_contacts_repository.dart';
import 'package:devicecontacts/screens/contacts_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

/// Корень приложения. Контроллер создаётся здесь один раз, не в build().
class ContactsApp extends StatefulWidget {
  const ContactsApp({super.key, this.repository, this.locale});

  /// Подмена репозитория нужна тестам. В обычном запуске читается устройство.
  final ContactsRepository? repository;

  /// Если не задан, язык берётся из системы. Русский остаётся запасным.
  final Locale? locale;

  @override
  State<ContactsApp> createState() => _ContactsAppState();
}

class _ContactsAppState extends State<ContactsApp> {
  late final ContactsController _controller;

  @override
  void initState() {
    super.initState();
    _controller = ContactsController(
      widget.repository ?? DeviceContactsRepository(),
    );
    _controller.bindLifecycle();
    _controller.initialize();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ContactsScope(
      controller: _controller,
      child: MaterialApp(
        onGenerateTitle: (context) => AppLocalizations.of(context)!.appTitle,
        locale: widget.locale,
        restorationScopeId: 'contacts_app',
        localeResolutionCallback: (locale, supported) {
          if (locale == null) {
            return const Locale('ru');
          }
          for (final item in supported) {
            if (item.languageCode == locale.languageCode) {
              return item;
            }
          }
          return const Locale('ru');
        },
        localizationsDelegates: const [
          AppLocalizations.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        supportedLocales: AppLocalizations.supportedLocales,
        theme: _theme(Brightness.light),
        darkTheme: _theme(Brightness.dark),
        home: const ContactsScreen(),
      ),
    );
  }
}

ThemeData _theme(Brightness brightness) {
  final scheme = ColorScheme.fromSeed(
    seedColor: const Color(0xFF1F6FEB),
    brightness: brightness,
  );
  return ThemeData(
    colorScheme: scheme,
    useMaterial3: true,
    appBarTheme: AppBarTheme(
      centerTitle: false,
      backgroundColor: scheme.surface,
      foregroundColor: scheme.onSurface,
      elevation: 0,
      scrolledUnderElevation: 1,
    ),
    cardTheme: CardThemeData(
      elevation: 0,
      color: scheme.surfaceContainerLow,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: scheme.surfaceContainerHighest,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: BorderSide.none,
      ),
    ),
  );
}
