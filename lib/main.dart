import 'package:flutter/material.dart';

import 'chat_api.dart';
import 'chat_screen.dart';
import 'chat_storage.dart';

void main() => runApp(
  const MaterialApp(debugShowCheckedModeBanner: false, home: ChatHome()),
);

class ChatHome extends StatefulWidget {
  const ChatHome({super.key});

  @override
  State<ChatHome> createState() => _ChatHomeState();
}

class _ChatHomeState extends State<ChatHome> {
  final _api = ChatApi();
  final _storage = ChatStorage();
  final _user = TextEditingController();
  final _password = TextEditingController();
  Session? _session;
  Future<List<String>>? _channels;
  bool _busy = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _restore();
  }

  Future<void> _restore() async {
    try {
      final session = await _storage.session();
      if (mounted && session != null) _start(session);
    } catch (_) {
      _error = 'Не удалось восстановить вход';
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _start(Session session) {
    _session = session;
    _channels = _api.channels();
  }

  Future<void> _login() async {
    final user = _user.text.trim();
    final password = _password.text;
    if (_busy) return;
    if (user.isEmpty || password.isEmpty) {
      setState(() => _error = 'Введите имя и пароль');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final session = await _api.login(user, password);
      await _storage.saveSession(session);
      if (!mounted) return;
      _password.clear();
      _start(session);
    } catch (error) {
      _error = error is ApiError
          ? ([400, 401, 403].contains(error.status)
                ? 'Проверьте имя и пароль'
                : 'Ошибка сервера (${error.status})')
          : 'Не удалось войти. Проверьте сеть и попробуйте снова.';
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _logout({bool expired = false}) async {
    final session = _session;
    if (session == null || _busy) return;
    setState(() {
      _busy = true;
      _session = null;
      _channels = null;
      _error = expired ? 'Сеанс закончился. Войдите снова.' : null;
    });
    try {
      await _storage.clearSession();
      if (!expired) await _storage.clearDrafts(session.user);
    } catch (_) {
      _error = 'Не удалось очистить локальные данные';
    }
    try {
      if (!expired) await _api.logout(session);
    } catch (_) {
      // The local session is already closed.
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  void dispose() {
    _user.dispose();
    _password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final session = _session;
    if (session == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Вход')),
        body: SafeArea(
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              TextField(
                controller: _user,
                enabled: !_busy,
                decoration: const InputDecoration(labelText: 'Имя'),
              ),
              TextField(
                controller: _password,
                enabled: !_busy,
                obscureText: true,
                decoration: const InputDecoration(labelText: 'Пароль'),
              ),
              if (_error != null) Text(_error!),
              if (_busy) const LinearProgressIndicator(),
              FilledButton(
                onPressed: _busy ? null : _login,
                child: const Text('Войти'),
              ),
            ],
          ),
        ),
      );
    }
    return Scaffold(
      appBar: AppBar(
        title: const Text('Каналы'),
        actions: [
          IconButton(
            tooltip: 'Обновить',
            onPressed: () => setState(() => _channels = _api.channels()),
            icon: const Icon(Icons.refresh),
          ),
          IconButton(
            tooltip: 'Выйти',
            onPressed: () => _logout(),
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: SafeArea(
        child: FutureBuilder<List<String>>(
          future: _channels,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              return const Center(
                child: Text('Не удалось загрузить каналы. Нажмите «Обновить».'),
              );
            }
            final channels = snapshot.data ?? [];
            if (channels.isEmpty) {
              return const Center(child: Text('Каналов пока нет'));
            }
            return ListView.builder(
              itemCount: channels.length,
              itemBuilder: (context, i) {
                final channel = channels[i];
                return ListTile(
                  title: Text(channel),
                  onTap: () {
                    final navigator = Navigator.of(context);
                    navigator.push(
                      MaterialPageRoute<void>(
                        builder: (_) =>
                            ChatScreen(_api, _storage, session, channel, () {
                              navigator.pop();
                              _logout(expired: true);
                            }),
                      ),
                    );
                  },
                );
              },
            );
          },
        ),
      ),
    );
  }
}
