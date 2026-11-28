import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;

import '../models/models.dart';

enum Failure { invalid, connection, server, expired, protocol, storage }

class ApiException implements Exception {
  final Failure kind;
  const ApiException(this.kind);
}

class ApiClient {
  final String baseUrl;
  final http.Client client;
  ApiClient(this.baseUrl, {http.Client? client})
    : client = client ?? http.Client();
  Uri uri(String path, [Map<String, String>? query]) =>
      Uri.parse('$baseUrl$path').replace(queryParameters: query);
  Future<http.Response> request(
    String method,
    String path, {
    String? token,
    Object? body,
    Map<String, String>? query,
  }) async {
    try {
      final headers = {
        'Content-Type': 'application/json',
        'X-Auth-Token': ?token,
      };
      final response =
          await (method == 'GET'
                  ? client.get(uri(path, query), headers: headers)
                  : client.post(
                      uri(path, query),
                      headers: headers,
                      body: jsonEncode(body),
                    ))
              .timeout(const Duration(seconds: 15));
      if (response.statusCode == 401) {
        throw ApiException(token == null ? Failure.invalid : Failure.expired);
      }
      if (response.statusCode >= 500) throw const ApiException(Failure.server);
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw const ApiException(Failure.protocol);
      }
      return response;
    } on ApiException {
      rethrow;
    } on TimeoutException {
      throw const ApiException(Failure.connection);
    } on http.ClientException {
      throw const ApiException(Failure.connection);
    }
  }

  Future<T> decode<T>(
    Future<http.Response> response,
    T Function(dynamic) parse,
  ) async {
    final r = await response;
    try {
      return parse(jsonDecode(utf8.decode(r.bodyBytes)));
    } catch (_) {
      throw const ApiException(Failure.protocol);
    }
  }

  Future<Session> login(String login, String password) async {
    final response = await request(
      'POST',
      '/login',
      body: {'name': login, 'pwd': password},
    );
    final token = utf8.decode(response.bodyBytes).trim();
    if (token.isEmpty) throw const ApiException(Failure.protocol);
    return Session(login, token);
  }

  Future<void> logout(String token) async {
    await request('POST', '/logout', token: token);
  }

  Future<List<Channel>> channels(String token) => decode(
    request('GET', '/channels', token: token),
    (j) => (j as List).map((e) => Channel(e as String, e)).toList(),
  );
  Future<MessagePage> messages(String token, String channel, String? before) =>
      decode(
        request(
          'GET',
          '/channel/${Uri.encodeComponent(channel)}',
          token: token,
          query: {
            'limit': '20',
            'lastKnownId': before ?? '281474976710655',
            'reverse': 'true',
          },
        ),
        (j) {
          final messages =
              (j as List)
                  .map((e) => Message.fromJson(e as Map<String, dynamic>))
                  .toList()
                ..sort((a, b) => int.parse(b.id).compareTo(int.parse(a.id)));
          return MessagePage(
            messages,
            messages.length < 20 ? null : messages.last.id,
          );
        },
      );
  Future<Message> send(
    String token,
    String channel,
    String text,
    String user,
  ) async {
    final response = await request(
      'POST',
      '/messages',
      token: token,
      body: {
        'from': user,
        'to': channel,
        'data': {
          'Text': {'text': text},
        },
      },
    );
    final id = utf8.decode(response.bodyBytes).trim().replaceAll('"', '');
    if (int.tryParse(id) == null) throw const ApiException(Failure.protocol);
    return Message(id, user, text, null);
  }

  Future<Uint8List> image(
    String token,
    String id, {
    bool thumb = false,
  }) async => (await request(
    'GET',
    '/${thumb ? 'thumb' : 'img'}/${id.split('/').map(Uri.encodeComponent).join('/')}',
    token: token,
  )).bodyBytes;
  void close() => client.close();
}
