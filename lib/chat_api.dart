import 'dart:convert';

import 'package:http/http.dart' as http;

class Session {
  const Session(this.user, this.token);
  final String user;
  final String token;
}

class Message {
  Message(Map<String, dynamic> json)
    : id = int.parse(json['id'].toString()),
      author = json['from'] as String,
      text = json['data']['Text']?['text'] as String?,
      image = json['data']['Image']?['link'] as String?;

  final int id;
  final String author;
  final String? text;
  final String? image;
}

class ApiError implements Exception {
  const ApiError(this.status);
  final int status;
}

class ChatApi {
  Uri _url(String route, [Map<String, String>? query]) =>
      Uri.https('faerytea.name', route, query);

  Future<String> _body(Future<http.Response> request) async {
    final response = await request.timeout(const Duration(seconds: 15));
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiError(response.statusCode);
    }
    return utf8.decode(response.bodyBytes);
  }

  Future<Session> login(String user, String password) async {
    final body = (await _body(
      http.post(
        _url('/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'name': user, 'pwd': password}),
      ),
    )).trim();
    if (body.isEmpty) throw const FormatException();
    if (body.startsWith('"') || body.startsWith('{')) {
      final value = jsonDecode(body);
      final token = value is String ? value : value['token'] as String?;
      if (token == null || token.isEmpty) throw const FormatException();
      return Session(user, token);
    }
    return Session(user, body);
  }

  Future<List<String>> channels() async =>
      List<String>.from(jsonDecode(await _body(http.get(_url('/channels')))));

  Future<List<Message>> history(String channel, {int? before}) async {
    final result = jsonDecode(
      await _body(
        http.get(
          _url('/channel/$channel', {
            'limit': '20',
            'reverse': 'true',
            'lastKnownId': '${before ?? 9223372036854775807}',
          }),
        ),
      ),
    );
    return (result as List)
        .map((row) => Message(Map<String, dynamic>.from(row as Map)))
        .toList();
  }

  Future<void> send(Session session, String channel, String text) async {
    await _body(
      http.post(
        _url('/messages'),
        headers: {
          'Content-Type': 'application/json',
          'X-Auth-Token': session.token,
        },
        body: jsonEncode({
          'from': session.user,
          'to': channel,
          'data': {
            'Text': {'text': text},
          },
        }),
      ),
    );
  }

  Uri image(String path, {bool full = false}) =>
      _url('/${full ? 'img' : 'thumb'}/$path');

  Future<void> logout(Session session) async {
    await _body(
      http.post(_url('/logout'), headers: {'X-Auth-Token': session.token}),
    );
  }
}
