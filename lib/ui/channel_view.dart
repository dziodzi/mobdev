import 'package:flutter/material.dart';

import '../data/models.dart';
import '../state/chat_state.dart';
import 'common.dart';
import 'photo.dart';

class ChannelView extends StatefulWidget {
  const ChannelView({
    super.key,
    required this.state,
    required this.chat,
    required this.showPhoto,
  });

  final ChatState state;
  final ChannelState chat;
  final ValueChanged<String> showPhoto;

  @override
  State<ChannelView> createState() => _ChannelViewState();
}

class _ChannelViewState extends State<ChannelView> {
  static const _anchor = ValueKey('history-anchor');
  late final TextEditingController _editor;
  late final ScrollController _scroll;
  final _editorFocus = FocusNode();
  DialogRoute<bool>? _retryDialog;

  ChannelState get _chat => widget.chat;

  @override
  void initState() {
    super.initState();
    _editor = TextEditingController(text: _chat.draft);
    _scroll = ScrollController(
      initialScrollOffset: _chat.scrollOffset,
      keepScrollOffset: false,
    )..addListener(_savePosition);
  }

  @override
  void didUpdateWidget(covariant ChannelView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (_editor.text != _chat.draft) {
      _editor.value = TextEditingValue(
        text: _chat.draft,
        selection: TextSelection.collapsed(offset: _chat.draft.length),
      );
    }
  }

  void _savePosition() {
    if (_scroll.hasClients) _chat.scrollOffset = _scroll.offset;
  }

  @override
  void dispose() {
    final dialog = _retryDialog;
    if (dialog != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (dialog.isActive) dialog.navigator?.removeRoute(dialog);
      });
    }
    _editor.dispose();
    _editorFocus.dispose();
    _scroll.dispose();
    super.dispose();
  }

  void _followLatest() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted ||
          !_scroll.hasClients ||
          !_scroll.position.hasContentDimensions) {
        return;
      }
      _scroll.jumpTo(_scroll.position.minScrollExtent);
    });
  }

  Future<void> _refresh() async {
    final atLatest =
        !_scroll.hasClients ||
        _scroll.offset <= _scroll.position.minScrollExtent + 28;
    await widget.state.loadHistory(_chat);
    if (mounted && atLatest) _followLatest();
  }

  Future<void> _send([Outgoing? retry]) async {
    if (_retryDialog != null) return;
    final existing = retry ?? widget.state.failedDraft(_chat);
    if (existing?.delivery == Delivery.uncertain) {
      final t = context.strings;
      final dialog = DialogRoute<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(t.retryTitle),
          content: Text(t.retryWarning),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: Text(t.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(context, true),
              child: Text(t.retry),
            ),
          ],
        ),
      );
      _retryDialog = dialog;
      final confirmed = await Navigator.of(context).push(dialog);
      _retryDialog = null;
      if (!mounted || confirmed != true) return;
    }
    final sending = widget.state.send(_chat, retry: existing);
    _followLatest();
    await sending;
    if (mounted &&
        _scroll.hasClients &&
        _scroll.offset <= _scroll.position.minScrollExtent + 28) {
      _followLatest();
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = context.strings;
    final state = widget.state;
    final pivot = _chat.anchorId;
    final messages = _chat.messages.values.toList()
      ..sort((a, b) => b.id.compareTo(a.id));
    final newer = messages
        .where((message) => pivot != null && message.id > pivot)
        .toList();
    final older = messages
        .where((message) => pivot == null || message.id <= pivot)
        .toList();
    final problem = _chat.problem;
    return LayoutBuilder(
      builder: (context, constraints) {
        final compact = constraints.maxHeight < 300;
        return Column(
          children: [
            if (_chat.loading)
              LinearProgressIndicator(semanticsLabel: t.loading),
            Expanded(
              child: CustomScrollView(
                controller: _scroll,
                reverse: true,
                center: _anchor,
                keyboardDismissBehavior:
                    ScrollViewKeyboardDismissBehavior.onDrag,
                slivers: [
                  SliverList.list(
                    children: [
                      for (final message in newer.reversed)
                        _message(context, message),
                      for (final item in _chat.outgoing)
                        _outgoing(context, item),
                    ],
                  ),
                  SliverList(
                    key: _anchor,
                    delegate: SliverChildBuilderDelegate(
                      (context, index) => _message(context, older[index]),
                      childCount: older.length,
                    ),
                  ),
                  if (problem != null && (!_chat.loaded || compact))
                    SliverToBoxAdapter(
                      child: ErrorNotice(
                        problemText(context, problem),
                        retry: _chat.olderFailed ? _older : _refresh,
                      ),
                    ),
                  if (_chat.loaded && messages.isEmpty && problem == null)
                    SliverToBoxAdapter(
                      child: Padding(
                        padding: const EdgeInsets.all(20),
                        child: Text(t.noMessages, textAlign: TextAlign.center),
                      ),
                    ),
                  if (_chat.hasOlder && (problem == null || !_chat.olderFailed))
                    SliverToBoxAdapter(
                      child: Center(
                        child: TextButton.icon(
                          onPressed: _chat.loading ? null : _older,
                          icon: const Icon(Icons.history),
                          label: Text(t.older),
                        ),
                      ),
                    ),
                ],
              ),
            ),
            if (problem != null && _chat.loaded && !compact)
              ErrorNotice(
                problemText(context, problem),
                retry: _chat.olderFailed ? _older : _refresh,
              ),
            SafeArea(
              top: false,
              child: Padding(
                padding: EdgeInsets.fromLTRB(
                  8,
                  compact ? 2 : 6,
                  8,
                  compact ? 2 : 8,
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    IconButton(
                      tooltip: t.refresh,
                      onPressed: _chat.loading ? null : _refresh,
                      icon: const Icon(Icons.refresh),
                    ),
                    Expanded(
                      child: TextField(
                        controller: _editor,
                        focusNode: _editorFocus,
                        minLines: 1,
                        maxLines: compact ? 1 : 4,
                        keyboardType: TextInputType.multiline,
                        textInputAction: TextInputAction.newline,
                        onChanged: (text) => state.editDraft(_chat, text),
                        decoration: InputDecoration(
                          labelText: t.messageInput,
                          isDense: compact,
                        ),
                      ),
                    ),
                    const SizedBox(width: 6),
                    IconButton.filled(
                      tooltip: t.send,
                      onPressed: _chat.sending || _chat.draft.trim().isEmpty
                          ? null
                          : _send,
                      icon: const Icon(Icons.send),
                    ),
                  ],
                ),
              ),
            ),
          ],
        );
      },
    );
  }

  Future<void> _older() => widget.state.loadHistory(_chat, older: true);

  Widget _message(BuildContext context, Message message) {
    final own = message.author == widget.state.session?.user;
    final child = switch (message.content) {
      TextContent(:final text) => SelectableText(text),
      PhotoContent(:final path) => ChatPhoto(
        url: widget.state.api.photo(path, thumbnail: true),
        open: () => widget.showPhoto(path),
      ),
    };
    return _Bubble(
      key: ValueKey(message.id),
      own: own,
      author: message.author,
      time: message.time,
      status: _chat.sentIds.contains(message.id) ? context.strings.sent : null,
      child: child,
    );
  }

  Widget _outgoing(BuildContext context, Outgoing item) {
    final t = context.strings;
    final status = switch (item.delivery) {
      Delivery.sending => t.sending,
      Delivery.uncertain => t.uncertain,
      Delivery.rejected => t.failed,
    };
    return _Bubble(
      key: ValueKey(item.id),
      own: true,
      author: widget.state.session!.user,
      time: item.time,
      status: status,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SelectableText(item.text),
          if (item.problem case final problem? when problem != Problem.rejected)
            Text(
              problemText(context, problem),
              style: Theme.of(context).textTheme.bodySmall,
            ),
          if (item.delivery != Delivery.sending)
            Wrap(
              spacing: 6,
              children: [
                TextButton(
                  onPressed: _chat.sending ? null : () => _send(item),
                  child: Text(t.retry),
                ),
                TextButton(
                  onPressed: () {
                    widget.state.returnToEditor(_chat, item);
                    _editorFocus.requestFocus();
                  },
                  child: Text(t.editAgain),
                ),
              ],
            ),
        ],
      ),
    );
  }
}

class _Bubble extends StatelessWidget {
  const _Bubble({
    super.key,
    required this.own,
    required this.author,
    required this.time,
    required this.child,
    this.status,
  });

  final bool own;
  final String author;
  final DateTime time;
  final Widget child;
  final String? status;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final palette = own ? colors.onPrimaryContainer : colors.onSurface;
    final local = MaterialLocalizations.of(context);

    final phoneWidth = MediaQuery.sizeOf(context).shortestSide;
    final date = local.formatShortDate(time);
    final clock = local.formatTimeOfDay(
      TimeOfDay.fromDateTime(time),
      alwaysUse24HourFormat: true,
    );
    return LayoutBuilder(
      builder: (context, constraints) {
        return Align(
          alignment: own ? Alignment.centerRight : Alignment.centerLeft,
          child: Container(
            constraints: BoxConstraints(
              maxWidth: (phoneWidth * 0.83).clamp(
                0.0,
                (constraints.maxWidth - 24).clamp(0.0, 520.0),
              ),
            ),
            margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              color: own
                  ? colors.primaryContainer
                  : colors.surfaceContainerHigh,
              borderRadius: BorderRadius.circular(16),
            ),
            child: DefaultTextStyle.merge(
              style: TextStyle(color: palette),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    author,
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: palette,
                    ),
                  ),
                  const SizedBox(height: 3),
                  child,
                  const SizedBox(height: 4),
                  Wrap(
                    spacing: 8,
                    children: [
                      Text(
                        '$date · $clock',
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: palette,
                        ),
                      ),
                      if (status != null)
                        Semantics(
                          liveRegion: true,
                          child: Text(
                            status!,
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: palette,
                            ),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
