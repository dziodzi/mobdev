import 'package:flutter/material.dart';

import '../models/models.dart';
import '../state/chat_state.dart';
import 'remote_image.dart';
import 'strings.dart';

class MessageTile extends StatelessWidget {
  final Message message;
  final ChatState state;
  final bool sent;
  const MessageTile({
    super.key,
    required this.message,
    required this.state,
    this.sent = false,
  });
  @override
  Widget build(BuildContext context) => ListTile(
    title: Text(message.author),
    subtitle: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (message.text.isNotEmpty) Text(message.text),
        if (message.imageId != null)
          Semantics(
            button: true,
            label: strings(context).image,
            child: InkWell(
              onTap: () => state.openImage(message.imageId!),
              child: RemoteImage(
                state: state,
                id: message.imageId!,
                thumb: true,
              ),
            ),
          ),
        if (sent) Text(strings(context).sent),
      ],
    ),
  );
}
