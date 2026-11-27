import 'package:flutter/material.dart';

import '../state/chat_state.dart';
import 'channel_list.dart';
import 'channel_view.dart';
import 'common.dart';
import 'photo.dart';

class Workspace extends StatefulWidget {
  const Workspace({super.key, required this.state});

  final ChatState state;

  @override
  State<Workspace> createState() => _WorkspaceState();
}

class _WorkspaceState extends State<Workspace> {
  Uri? _photo;

  void _back() {
    if (_photo != null) {
      setState(() => _photo = null);
    } else {
      FocusManager.instance.primaryFocus?.unfocus();
      widget.state.closeChat();
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    final t = context.strings;
    final chat = state.selected;
    final wide = MediaQuery.orientationOf(context) == Orientation.landscape;
    final photo = _photo;
    final base = Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        leading: chat == null
            ? null
            : IconButton(
                tooltip: t.backToChannels,
                onPressed: _back,
                icon: Icon(wide ? Icons.close : Icons.arrow_back),
              ),
        title: Text(
          chat?.name ?? t.channelList,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        actions: [
          if (chat == null || wide)
            IconButton(
              tooltip: t.refresh,
              onPressed: state.loadingChannels ? null : state.loadChannels,
              icon: const Icon(Icons.list_alt),
            ),
          IconButton(
            tooltip: t.signOut,
            onPressed: state.signOut,
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: SafeArea(
        top: false,
        child: Column(
          children: [
            if (state.storageProblem case final problem?)
              ErrorNotice(problemText(context, problem)),
            Expanded(
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final showList = wide || chat == null;
                  final listWidth = wide
                      ? (constraints.maxWidth * 0.34).clamp(150.0, 320.0)
                      : constraints.maxWidth;
                  return Row(
                    children: [
                      Offstage(
                        offstage: !showList,
                        child: SizedBox(
                          width: listWidth,
                          child: ChannelList(state: state),
                        ),
                      ),
                      VerticalDivider(width: wide ? 1 : 0),
                      Expanded(
                        child: chat == null
                            ? Center(child: Text(t.chooseChannel))
                            : ChannelView(
                                key: ObjectKey(chat),
                                state: state,
                                chat: chat,
                                showPhoto: (path) {
                                  FocusManager.instance.primaryFocus?.unfocus();
                                  setState(
                                    () => _photo = state.api.photo(
                                      path,
                                      thumbnail: false,
                                    ),
                                  );
                                },
                              ),
                      ),
                    ],
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
    return PopScope(
      canPop: photo == null && chat == null,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop && (_photo != null || state.selected != null)) _back();
      },
      child: Stack(
        fit: StackFit.expand,
        children: [
          ExcludeFocus(
            excluding: photo != null,
            child: ExcludeSemantics(excluding: photo != null, child: base),
          ),
          if (photo != null)
            Scaffold(
              backgroundColor: Colors.black,
              appBar: AppBar(
                backgroundColor: Colors.black,
                foregroundColor: Colors.white,
                title: Text(t.photo),
                leading: IconButton(
                  onPressed: _back,
                  icon: const Icon(Icons.close),
                  tooltip: t.closePhoto,
                ),
              ),
              body: SafeArea(
                child: Theme(
                  data: ThemeData.dark(),
                  child: ChatPhoto(url: photo, full: true),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
