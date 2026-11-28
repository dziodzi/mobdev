import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../state/chat_state.dart';
import 'strings.dart';

class RemoteImage extends StatefulWidget {
  final ChatState state;
  final String id;
  final bool thumb;
  const RemoteImage({
    super.key,
    required this.state,
    required this.id,
    this.thumb = false,
  });
  @override
  State<RemoteImage> createState() => _RemoteImageState();
}

class _RemoteImageState extends State<RemoteImage> {
  late Future<Uint8List> future;
  @override
  void initState() {
    super.initState();
    load();
  }

  void load() {
    future = widget.state.image(widget.id, thumb: widget.thumb);
  }

  @override
  void didUpdateWidget(covariant RemoteImage old) {
    super.didUpdateWidget(old);
    if (old.id != widget.id || old.thumb != widget.thumb) load();
  }

  @override
  Widget build(BuildContext context) => FutureBuilder<Uint8List>(
    future: future,
    builder: (context, snapshot) {
      if (snapshot.hasError) {
        return TextButton(
          onPressed: () => setState(load),
          child: Text(strings(context).retry),
        );
      }
      if (!snapshot.hasData) {
        return const Center(child: CircularProgressIndicator());
      }
      final image = Image.memory(
        snapshot.data!,
        fit: BoxFit.contain,
        semanticLabel: strings(context).image,
        errorBuilder: (_, _, _) => Text(strings(context).protocol),
      );
      return widget.thumb
          ? SizedBox(height: 140, child: image)
          : InteractiveViewer(
              minScale: 0.5,
              maxScale: 5,
              child: Center(child: image),
            );
    },
  );
}
