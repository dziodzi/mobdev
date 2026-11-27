import 'package:flutter/material.dart';

import 'common.dart';

class ChatPhoto extends StatefulWidget {
  const ChatPhoto({super.key, required this.url, this.open, this.full = false});

  final Uri url;
  final VoidCallback? open;
  final bool full;

  @override
  State<ChatPhoto> createState() => _ChatPhotoState();
}

class _ChatPhotoState extends State<ChatPhoto> {
  int _attempt = 0;

  Future<void> _retry() async {
    await NetworkImage(widget.url.toString()).evict();
    if (mounted) setState(() => _attempt++);
  }

  @override
  Widget build(BuildContext context) {
    final t = context.strings;
    final image = Image.network(
      widget.url.toString(),
      key: ValueKey(_attempt),
      width: widget.full ? null : 192,
      height: widget.full ? null : 124,
      fit: widget.full ? BoxFit.contain : BoxFit.cover,
      semanticLabel: t.photo,
      frameBuilder: (context, child, frame, synchronous) {
        if (synchronous || frame != null) return child;
        return SizedBox(
          width: widget.full ? null : 192,
          height: widget.full ? null : 124,
          child: Center(
            child: CircularProgressIndicator(semanticsLabel: t.loading),
          ),
        );
      },
      loadingBuilder: (context, child, progress) {
        if (progress == null) return child;
        return SizedBox(
          width: widget.full ? null : 192,
          height: widget.full ? null : 124,
          child: Center(
            child: CircularProgressIndicator(semanticsLabel: t.loading),
          ),
        );
      },
      errorBuilder: (context, _, _) => Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(t.photoFailed),
            TextButton(onPressed: _retry, child: Text(t.retry)),
          ],
        ),
      ),
    );
    if (widget.full) {
      return InteractiveViewer(maxScale: 5, child: Center(child: image));
    }
    return Semantics(
      button: true,
      label: t.openPhoto,
      child: InkWell(
        onTap: widget.open,
        child: ClipRRect(borderRadius: BorderRadius.circular(12), child: image),
      ),
    );
  }
}
