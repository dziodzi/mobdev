import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import 'data/chat_api.dart';
import 'data/local_store.dart';
import 'l10n/generated/app_localizations.dart';
import 'state/chat_state.dart';
import 'ui/login_page.dart';
import 'ui/workspace.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  const configured = String.fromEnvironment(
    'CHAT_ORIGIN',
    defaultValue: 'https://faerytea.name',
  );
  final origin = Uri.parse(configured);
  runApp(
    ChannelsApp(
      state: ChatState(
        ChatApi(http.Client(), origin),
        LocalStore(
          const FlutterSecureStorage(),
          SharedPreferencesAsync(),
          origin,
        ),
      ),
    ),
  );
}

class ChannelsApp extends StatefulWidget {
  const ChannelsApp({super.key, required this.state});

  final ChatState state;

  @override
  State<ChannelsApp> createState() => _ChannelsAppState();
}

class _ChannelsAppState extends State<ChannelsApp> {
  @override
  void initState() {
    super.initState();
    unawaited(widget.state.restore());
  }

  @override
  void dispose() {
    widget.state.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => MaterialApp(
    onGenerateTitle: (context) => AppLocalizations.of(context)!.appName,
    debugShowCheckedModeBanner: false,
    localizationsDelegates: AppLocalizations.localizationsDelegates,
    supportedLocales: AppLocalizations.supportedLocales,
    theme: ThemeData(
      colorSchemeSeed: const Color(0xFF166760),
      brightness: Brightness.light,
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(),
      ),
    ),
    darkTheme: ThemeData(
      colorSchemeSeed: const Color(0xFF66C2B4),
      brightness: Brightness.dark,
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(),
      ),
    ),
    home: ListenableBuilder(
      listenable: widget.state,
      builder: (context, _) {
        final state = widget.state;
        if (state.starting) {
          return Scaffold(
            body: Center(
              child: CircularProgressIndicator(
                semanticsLabel: AppLocalizations.of(context)!.loading,
              ),
            ),
          );
        }
        final session = state.session;
        return session == null
            ? LoginPage(state: state)
            : Workspace(key: ObjectKey(session), state: state);
      },
    ),
  );
}
