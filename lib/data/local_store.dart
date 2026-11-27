import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'models.dart';

class LocalStore {
  LocalStore(this._secrets, this._settings, Uri origin)
    : _prefix = 'channels.v1.$origin';

  final FlutterSecureStorage _secrets;
  final SharedPreferencesAsync _settings;
  final String _prefix;

  String get _sessionKey => '$_prefix.session';
  String _userKey(String user) => '$_prefix.data.${Uri.encodeComponent(user)}';

  Future<Session?> readSession() async {
    final raw = await _secrets.read(key: _sessionKey);
    if (raw == null) return null;
    final json = jsonDecode(raw) as Map<String, dynamic>;
    return Session(json['user'] as String, json['token'] as String);
  }

  Future<void> saveSession(Session session) => _secrets.write(
    key: _sessionKey,
    value: jsonEncode({'user': session.user, 'token': session.token}),
  );

  Future<void> deleteSession() => _secrets.delete(key: _sessionKey);

  Future<Map<String, dynamic>> readUser(String user) async {
    final raw = await _settings.getString(_userKey(user));
    return raw == null ? {} : jsonDecode(raw) as Map<String, dynamic>;
  }

  Future<void> saveUser(String user, String snapshot) =>
      _settings.setString(_userKey(user), snapshot);

  Future<void> deleteUser(String user) => _settings.remove(_userKey(user));
}
