import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/models.dart';

// Writes are serialized: a late draft write cannot overtake logout deletion.
class LocalStore {
  final FlutterSecureStorage storage;
  Future<void> _tail = Future.value();
  LocalStore({FlutterSecureStorage? storage})
    : storage = storage ?? const FlutterSecureStorage();
  Future<T> _queue<T>(Future<T> Function() action) {
    final next = _tail.then((_) => action());
    _tail = next.then<void>((_) {}, onError: (Object _, StackTrace _) {});
    return next;
  }

  Future<Session?> session() => _queue(() async {
    final raw = await storage.read(key: 'session');
    return raw == null
        ? null
        : Session.fromJson(jsonDecode(raw) as Map<String, dynamic>);
  });
  Future<void> saveSession(Session session) => _queue(
    () => storage.write(key: 'session', value: jsonEncode(session.toJson())),
  );
  String _key(String user) => 'drafts:${Uri.encodeComponent(user)}';
  Future<Map<String, String>> drafts(String user) => _queue(() async {
    final raw = await storage.read(key: _key(user));
    return raw == null ? {} : Map<String, String>.from(jsonDecode(raw) as Map);
  });
  Future<void> saveDrafts(String user, Map<String, String> drafts) {
    final snapshot = jsonEncode(drafts);
    return _queue(() => storage.write(key: _key(user), value: snapshot));
  }

  Future<void> clear(String? user) => _queue(() async {
    await storage.delete(key: 'session');
    if (user != null) await storage.delete(key: _key(user));
  });
}
