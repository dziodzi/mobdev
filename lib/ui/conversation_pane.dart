import 'package:flutter/material.dart';

import '../models/models.dart';
import '../state/chat_state.dart';
import 'message_tile.dart';
import 'strings.dart';

class ConversationPane extends StatefulWidget {
  final ChatState state;
  final String id;
  const ConversationPane({super.key, required this.state, required this.id});
  @override
  State<ConversationPane> createState() => _ConversationPaneState();
}

class _ConversationPaneState extends State<ConversationPane> {
  late final TextEditingController editor;
  late final ScrollController scroll;
  @override
  void initState() {
    super.initState();
    editor = TextEditingController(text: widget.state.drafts[widget.id] ?? '');
    scroll = ScrollController(
      initialScrollOffset: widget.state.conversation(widget.id).offset,
    );
    scroll.addListener(
      () => widget.state.conversation(widget.id).offset = scroll.offset,
    );
  }

  @override
  void dispose() {
    editor.dispose();
    scroll.dispose();
    super.dispose();
  }

  Future<void> retry(Outgoing m) async {
    final s = strings(context);
    final yes = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        content: Text(s.warning),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(s.cancel),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(s.yes),
          ),
        ],
      ),
    );
    if (yes == true) await widget.state.send(widget.id, retryId: m.localId);
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state,
        c = state.conversation(widget.id),
        s = strings(context);
    final draft = state.drafts[widget.id] ?? '';
    if (editor.text != draft) {
      editor.value = TextEditingValue(
        text: draft,
        selection: TextSelection.collapsed(offset: draft.length),
      );
    }
    final pending = c.outgoing
        .where((m) => m.status != Delivery.sent)
        .toList()
        .reversed
        .toList();
    return Column(
      children: [
        if (c.busy) const LinearProgressIndicator(),
        if (c.error != null)
          Wrap(
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              Text(failureText(context, c.error!)),
              TextButton(
                onPressed: c.busy ? null : () => state.loadMessages(widget.id),
                child: Text(s.retry),
              ),
            ],
          ),
        if (state.storageError != null) Text(s.storage),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => state.loadMessages(widget.id),
            child: ListView.builder(
              controller: scroll,
              reverse: true,
              physics: const AlwaysScrollableScrollPhysics(),
              itemCount: pending.length + c.messages.length + 1,
              itemBuilder: (context, index) {
                if (index < pending.length) {
                  final m = pending[index];
                  return ListTile(
                    key: ValueKey(m.localId),
                    title: Text(m.text),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          m.status == Delivery.sending
                              ? s.sending
                              : s.uncertain,
                        ),
                        if (m.status == Delivery.uncertain)
                          Wrap(
                            children: [
                              TextButton(
                                onPressed: c.sending ? null : () => retry(m),
                                child: Text(s.retry),
                              ),
                              TextButton(
                                onPressed: () => state.restore(widget.id, m),
                                child: Text(s.restore),
                              ),
                            ],
                          ),
                      ],
                    ),
                  );
                }
                final i = index - pending.length;
                if (i == c.messages.length) {
                  return Column(
                    children: [
                      if (c.messages.isEmpty && !c.busy) Text(s.empty),
                      if (c.next != null)
                        TextButton(
                          onPressed: c.busy
                              ? null
                              : () =>
                                    state.loadMessages(widget.id, older: true),
                          child: Text(s.older),
                        ),
                    ],
                  );
                }
                final m = c.messages[i];
                return MessageTile(
                  key: ValueKey(m.id),
                  message: m,
                  state: state,
                  sent: c.outgoing.any((o) => o.serverId == m.id),
                );
              },
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(8),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: TextField(
                  controller: editor,
                  onChanged: (text) => state.draft(widget.id, text),
                  minLines: 1,
                  maxLines: 4,
                  decoration: InputDecoration(labelText: s.message),
                ),
              ),
              IconButton(
                tooltip: s.send,
                onPressed: c.sending ? null : () => state.send(widget.id),
                icon: const Icon(Icons.send),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
