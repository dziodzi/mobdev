import 'package:flutter/material.dart';

import 'chat_api.dart';
import 'chat_storage.dart';

class ChatScreen extends StatefulWidget {
  const ChatScreen(
    this.api,
    this.storage,
    this.session,
    this.channel,
    this.onExpired, {
    super.key,
  });
  final ChatApi api;
  final ChatStorage storage;
  final Session session;
  final String channel;
  final VoidCallback onExpired;

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final _input = TextEditingController();
  final _messages = <int, Message>{};
  bool _loading = false;
  bool _sending = false;
  bool _hasOlder = false;
  bool _ready = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _restoreDraft();
    _load();
  }

  Future<void> _restoreDraft() async {
    try {
      final draft = await widget.storage.draft(
        widget.session.user,
        widget.channel,
      );
      if (mounted) _input.text = draft;
    } catch (_) {
      if (mounted) _notice('Не удалось открыть черновик');
    } finally {
      if (mounted) setState(() => _ready = true);
    }
  }

  void _notice(String text) =>
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));

  Future<void> _saveDraft(String text) async {
    try {
      await widget.storage.saveDraft(widget.session.user, widget.channel, text);
    } catch (_) {
      if (mounted) _notice('Не удалось сохранить черновик');
    }
  }

  Future<void> _load({bool older = false}) async {
    if (_loading) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final before = older && _messages.isNotEmpty
          ? _messages.keys.reduce((a, b) => a < b ? a : b)
          : null;
      final page = await widget.api.history(widget.channel, before: before);
      if (!mounted) return;
      setState(() {
        final firstPage = _messages.isEmpty;
        for (final message in page) {
          _messages[message.id] = message;
        }
        if (firstPage || older) _hasOlder = page.length == 20;
      });
    } catch (_) {
      if (mounted) setState(() => _error = 'Не удалось загрузить сообщения');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _send() async {
    final text = _input.text.trim();
    if (_sending || text.isEmpty) return;
    setState(() => _sending = true);
    try {
      await widget.api.send(widget.session, widget.channel, text);
      if (!mounted) return;
      _input.clear();
      await _saveDraft('');
      if (mounted) await _load();
    } catch (error) {
      if (!mounted) return;
      if (error is ApiError && (error.status == 401 || error.status == 403)) {
        widget.onExpired();
      } else {
        _notice(
          'Не удалось подтвердить отправку. Проверьте чат перед повтором.',
        );
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  void _showImage(String path) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (context) {
          return Scaffold(
            appBar: AppBar(title: const Text('Изображение')),
            body: Center(
              child: InteractiveViewer(
                child: Image.network(
                  widget.api.image(path, full: true).toString(),
                  errorBuilder: (_, _, _) =>
                      const Text('Не удалось открыть изображение'),
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  @override
  void dispose() {
    _input.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final messages = _messages.values.toList()
      ..sort((a, b) => b.id.compareTo(a.id));
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.channel),
        actions: [
          IconButton(
            tooltip: 'Обновить',
            onPressed: _loading ? null : () => _load(),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            if (_loading) const LinearProgressIndicator(),
            if (_error != null) Text(_error!),
            if (_hasOlder)
              TextButton(
                onPressed: _loading ? null : () => _load(older: true),
                child: const Text('Загрузить старые сообщения'),
              ),
            Expanded(
              child: messages.isEmpty
                  ? Center(
                      child: Text(
                        _loading || _error != null ? '' : 'Сообщений пока нет',
                      ),
                    )
                  : ListView.builder(
                      reverse: true,
                      itemCount: messages.length,
                      itemBuilder: (context, index) {
                        final message = messages[index];
                        final image = message.image;
                        return ListTile(
                          title: Text(message.author),
                          subtitle: image == null
                              ? Text(message.text ?? '')
                              : Align(
                                  alignment: Alignment.centerLeft,
                                  child: InkWell(
                                    onTap: () => _showImage(image),
                                    child: Image.network(
                                      widget.api.image(image).toString(),
                                      height: 96,
                                      errorBuilder: (_, _, _) =>
                                          const Icon(Icons.broken_image),
                                    ),
                                  ),
                                ),
                        );
                      },
                    ),
            ),
            Padding(
              padding: const EdgeInsets.all(8),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _input,
                      enabled: _ready && !_sending,
                      onChanged: _saveDraft,
                      decoration: const InputDecoration(labelText: 'Сообщение'),
                    ),
                  ),
                  IconButton(
                    tooltip: 'Отправить',
                    onPressed: _ready && !_sending && !_loading ? _send : null,
                    icon: const Icon(Icons.send),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
