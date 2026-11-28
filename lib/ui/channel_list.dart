import 'package:flutter/material.dart';

import '../state/chat_state.dart';
import 'strings.dart';

class ChannelList extends StatelessWidget {
  final ChatState state;
  const ChannelList({super.key, required this.state});
  @override
  Widget build(BuildContext context) {
    final s = strings(context);
    return Column(
      children: [
        if (state.loadingChannels) const LinearProgressIndicator(),
        if (state.channelsError != null)
          Text(failureText(context, state.channelsError!)),
        if (state.channelsError != null)
          TextButton(onPressed: state.loadChannels, child: Text(s.retry)),
        Expanded(
          child: RefreshIndicator(
            onRefresh: state.loadChannels,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              children: [
                if (state.channels.isEmpty && !state.loadingChannels)
                  ListTile(title: Text(s.empty)),
                for (final c in state.channels)
                  ListTile(
                    title: Text(c.name),
                    selected: state.selected?.id == c.id,
                    onTap: () => state.select(c),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
