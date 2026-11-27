import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:sqflite/sqflite.dart';

import 'chat_api.dart';

class ChatStorage {
  final _secure = const FlutterSecureStorage();
  late final _database = _open();

  Future<Database> _open() async => openDatabase(
    '${await getDatabasesPath()}/chat.db',
    version: 1,
    onCreate: (db, _) => db.execute('''
      CREATE TABLE drafts (
        username TEXT NOT NULL,
        channel TEXT NOT NULL,
        body TEXT NOT NULL,
        PRIMARY KEY (username, channel)
      )
    '''),
  );

  Future<Session?> session() async {
    final raw = await _secure.read(key: 'session');
    if (raw == null) return null;
    final data = jsonDecode(raw);
    return Session(data['user'] as String, data['token'] as String);
  }

  Future<void> saveSession(Session value) => _secure.write(
    key: 'session',
    value: jsonEncode({'user': value.user, 'token': value.token}),
  );

  Future<void> clearSession() => _secure.delete(key: 'session');

  Future<String> draft(String user, String channel) async {
    final rows = await (await _database).query(
      'drafts',
      where: 'username = ? AND channel = ?',
      whereArgs: [user, channel],
    );
    return rows.isEmpty ? '' : rows.first['body'] as String;
  }

  Future<void> saveDraft(String user, String channel, String body) async {
    final db = await _database;
    if (body.isEmpty) {
      await db.delete(
        'drafts',
        where: 'username = ? AND channel = ?',
        whereArgs: [user, channel],
      );
    } else {
      await db.insert('drafts', {
        'username': user,
        'channel': channel,
        'body': body,
      }, conflictAlgorithm: ConflictAlgorithm.replace);
    }
  }

  Future<void> clearDrafts(String user) async {
    await (await _database).delete(
      'drafts',
      where: 'username = ?',
      whereArgs: [user],
    );
  }
}
