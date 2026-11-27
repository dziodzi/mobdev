import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import 'models.dart';

class ApiException implements Exception {
  const ApiException(this.problem);
  final Problem problem;
}

class ChatApi {
  ChatApi(this._client, this.origin);

  static const pageSize = 20;
  static const _requestTimeout = Duration(seconds: 10);
  final http.Client _client;
  final Uri origin;

  Uri _url(List<String> segments, [Map<String, String>? query]) =>
      origin.replace(pathSegments: segments, queryParameters: query);

  Uri photo(String path, {required bool thumbnail}) =>
      _url([thumbnail ? 'thumb' : 'img', ...path.split('/')]);

  Future<Session> login(String user, String password) async {
    final body = await _request(
      _url(['login']),
      body: {'name': user, 'pwd': password},
      login: true,
    );
    final token = body.trim();
    if (token.isEmpty) throw const ApiException(Problem.server);
    // The service responds with a plain token. A JSON string is also accepted.
    final value = token.startsWith('"')
        ? _parse(() => jsonDecode(token) as String)
        : token;
    if (value.isEmpty) throw const ApiException(Problem.server);
    return Session(user, value);
  }

  Future<List<String>> channels() async {
    final body = await _request(_url(['channels']));
    return _parse(() => (jsonDecode(body) as List).cast<String>().toList());
  }

  Future<List<Message>> history(
    String channel, {
    int? cursor,
    bool reverse = true,
  }) async {
    final body = await _request(
      _url(
        ['channel', channel],
        {
          'limit': '$pageSize',
          'reverse': '$reverse',
          if (cursor != null) 'lastKnownId': '$cursor',
        },
      ),
    );
    return _parse(
      () => (jsonDecode(body) as List)
          .map((item) => Message.fromJson(item as Map<String, dynamic>))
          .where((message) => message.channel == channel)
          .toList(),
    );
  }

  Future<int> send(Session session, String channel, String text) async {
    final answer = await _request(
      _url(['messages']),
      token: session.token,
      body: {
        'from': session.user,
        'to': channel,
        'data': {
          'Text': {'text': text},
        },
      },
    );
    return _parse(() {
      final id = jsonDecode(answer);
      if (id is Map) return int.parse(id['id'].toString());
      return int.parse(id.toString());
    });
  }

  Future<void> logout(Session session) async {
    await _request(_url(['logout']), token: session.token, body: {});
  }

  Future<String> _request(
    Uri url, {
    String? token,
    Map<String, dynamic>? body,
    bool login = false,
  }) async {
    try {
      final request = http.Request(body == null ? 'GET' : 'POST', url)
        ..followRedirects = false
        ..headers['Accept'] = 'application/json, text/plain';
      if (token != null) request.headers['X-Auth-Token'] = token;
      if (body != null) {
        request.headers['Content-Type'] = 'application/json; charset=utf-8';
        request.body = jsonEncode(body);
      }
      final response = await _client.send(request).timeout(_requestTimeout);
      final status = response.statusCode;
      if (status >= 200 && status < 300) {
        final bytes = await response.stream.toBytes().timeout(_requestTimeout);
        final payload = _parse(() => utf8.decode(bytes));
        return payload;
      }
      // The status is sufficient. Error bodies can be slow and may echo a password.
      unawaited(
        response.stream.listen(null).cancel().catchError((Object _) {}),
      );
      if (status == 401 && login) throw const ApiException(Problem.credentials);
      if (status == 401 && token != null) {
        throw const ApiException(Problem.unauthorized);
      }
      if (status >= 500 || status < 400 || status == 408) {
        throw const ApiException(Problem.server);
      }
      throw const ApiException(Problem.rejected);
    } on TimeoutException {
      throw const ApiException(Problem.connection);
    } on http.ClientException {
      throw const ApiException(Problem.connection);
    } on IOException {
      throw const ApiException(Problem.connection);
    }
  }

  T _parse<T>(T Function() parse) {
    try {
      return parse();
    } on FormatException {
      throw const ApiException(Problem.server);
    } on TypeError {
      throw const ApiException(Problem.server);
    } on ArgumentError {
      throw const ApiException(Problem.server);
    }
  }

  void close() => _client.close();
}
