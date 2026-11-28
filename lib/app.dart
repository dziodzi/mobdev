import 'package:flutter/material.dart';

import 'l10n/app_localizations.dart';
import 'state/chat_state.dart';
import 'ui/login_screen.dart';
import 'ui/channel_list.dart';
import 'ui/conversation_pane.dart';
import 'ui/remote_image.dart';
import 'ui/strings.dart';

class ChatApp extends StatefulWidget {
  final ChatState state;
  const ChatApp({super.key, required this.state});
  @override
  State<ChatApp> createState() => _ChatAppState();
}

class _ChatAppState extends State<ChatApp> {
  @override
  void initState() {
    super.initState();
    widget.state.start();
  }

  @override
  void dispose() {
    widget.state.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => MaterialApp(
    localizationsDelegates: AppLocalizations.localizationsDelegates,
    supportedLocales: AppLocalizations.supportedLocales,
    locale: const Locale('ru'),
    onGenerateTitle: (context) => strings(context).appTitle,
    theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
    home: ListenableBuilder(
      listenable: widget.state,
      builder: (context, _) => _Shell(state: widget.state),
    ),
  );
}

class _Shell extends StatelessWidget {
  final ChatState state;
  const _Shell({required this.state});
  @override
  Widget build(BuildContext context) {
    final s = strings(context);
    if (state.booting) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (state.session == null) return LoginScreen(state: state);
    final landscape =
        MediaQuery.orientationOf(context) == Orientation.landscape;
    Widget pane() => state.selected == null
        ? Center(child: Text(s.choose))
        : ConversationPane(
            key: ValueKey(state.selected!.id),
            state: state,
            id: state.selected!.id,
          );
    return PopScope(
      canPop: state.selected == null && state.openedImage == null,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop) state.back();
      },
      child: Navigator(
        pages: [
          MaterialPage(
            key: const ValueKey('channels'),
            child: PopScope(
              canPop: state.selected == null,
              onPopInvokedWithResult: (didPop, result) {
                if (!didPop) state.back();
              },
              child: Scaffold(
                appBar: AppBar(
                  title: Text(s.channels),
                  leading: landscape && state.selected != null
                      ? IconButton(
                          tooltip: s.back,
                          onPressed: state.back,
                          icon: const Icon(Icons.arrow_back),
                        )
                      : null,
                  actions: [
                    IconButton(
                      tooltip: s.refresh,
                      onPressed: state.loadChannels,
                      icon: const Icon(Icons.refresh),
                    ),
                    IconButton(
                      tooltip: s.logout,
                      onPressed: state.logout,
                      icon: const Icon(Icons.logout),
                    ),
                  ],
                ),
                body: SafeArea(
                  child: landscape
                      ? Row(
                          children: [
                            Expanded(flex: 2, child: ChannelList(state: state)),
                            const VerticalDivider(width: 1),
                            Expanded(flex: 3, child: pane()),
                          ],
                        )
                      : ChannelList(state: state),
                ),
              ),
            ),
          ),
          if (!landscape && state.selected != null)
            MaterialPage(
              key: ValueKey('chat:${state.selected!.id}'),
              child: Scaffold(
                appBar: AppBar(
                  title: Text(state.selected!.name),
                  leading: BackButton(onPressed: state.back),
                  actions: [
                    IconButton(
                      tooltip: s.refresh,
                      onPressed: () => state.loadMessages(state.selected!.id),
                      icon: const Icon(Icons.refresh),
                    ),
                  ],
                ),
                body: SafeArea(child: pane()),
              ),
            ),
          if (state.openedImage != null)
            MaterialPage(
              key: ValueKey('image:${state.openedImage}'),
              child: Scaffold(
                appBar: AppBar(
                  title: Text(s.image),
                  leading: IconButton(
                    tooltip: s.close,
                    onPressed: state.back,
                    icon: const Icon(Icons.close),
                  ),
                ),
                body: SafeArea(
                  child: RemoteImage(state: state, id: state.openedImage!),
                ),
              ),
            ),
        ],
        onDidRemovePage: (page) {
          if (state.openedImage != null &&
              page.key == ValueKey('image:${state.openedImage}')) {
            state.back();
          } else if (state.selected != null &&
              page.key == ValueKey('chat:${state.selected!.id}') &&
              !landscape) {
            state.back();
          }
        },
      ),
    );
  }
}
