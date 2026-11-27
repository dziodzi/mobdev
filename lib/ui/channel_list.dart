import 'package:flutter/material.dart';

import '../state/chat_state.dart';
import 'common.dart';

class ChannelList extends StatefulWidget {
  const ChannelList({super.key, required this.state});

  final ChatState state;

  @override
  State<ChannelList> createState() => _ChannelListState();
}

class _ChannelListState extends State<ChannelList> {
  String _filter = '';

  @override
  Widget build(BuildContext context) {
    final t = context.strings;
    final state = widget.state;
    final colors = Theme.of(context).colorScheme;
    final visible = state.channels
        .where((channel) => channel.toLowerCase().contains(_filter))
        .toList();
    return Column(
      children: [
        if (state.channelsLoaded && state.channels.isNotEmpty)
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
            child: TextField(
              onChanged: (text) =>
                  setState(() => _filter = text.trim().toLowerCase()),
              decoration: InputDecoration(
                labelText: t.search,
                prefixIcon: const Icon(Icons.search),
                isDense: true,
              ),
            ),
          ),
        if (state.channelsProblem case final problem?)
          ErrorNotice(problemText(context, problem), retry: state.loadChannels),
        if (state.loadingChannels)
          LinearProgressIndicator(semanticsLabel: t.loading),
        Expanded(
          child: RefreshIndicator(
            onRefresh: state.loadChannels,
            child: ListView.builder(
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(8, 4, 8, 12),
              itemCount: visible.isEmpty && state.channelsLoaded
                  ? 1
                  : visible.length,
              itemBuilder: (context, index) {
                if (visible.isEmpty) {
                  return Padding(
                    padding: const EdgeInsets.all(20),
                    child: Text(
                      state.channels.isEmpty ? t.noChannels : t.noMatches,
                    ),
                  );
                }
                final channel = visible[index];
                return ListTile(
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                  selected: state.selected?.name == channel,
                  selectedTileColor: colors.secondaryContainer,
                  selectedColor: colors.onSecondaryContainer,
                  leading: const Icon(Icons.tag),
                  title: Text(channel),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {
                    FocusManager.instance.primaryFocus?.unfocus();
                    state.select(channel);
                  },
                );
              },
            ),
          ),
        ),
      ],
    );
  }
}
