# lib/data/api_client.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
// Исходная строка 1: TimeoutException; convert даёт UTF-8/JSON, typed_data — массив байтов Uint8List.
import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;

import '../models/models.dart';

// Исходная строка 9: Категории ошибок приложения; UI не зависит от текста сетевых исключений.
enum Failure { invalid, connection, server, expired, protocol, storage }

// Исходная строка 11: Переносим Failure без паролей, токенов и сырых ответов.
class ApiException implements Exception {
  final Failure kind;
  const ApiException(this.kind);
}

class ApiClient {
  final String baseUrl;
  final http.Client client;
  // Исходная строка 19: http.Client внедряется: настоящий по умолчанию, MockClient в тестах.
  ApiClient(this.baseUrl, {http.Client? client})
    : client = client ?? http.Client();
  // Исходная строка 21: URL и query-параметры формирует Uri, а не ручная склейка ?/&.
  Uri uri(String path, [Map<String, String>? query]) =>
      Uri.parse('$baseUrl$path').replace(queryParameters: query);
  // Исходная строка 23: Общий транспорт. async возвращает Future и НЕ создаёт отдельный поток.
  Future<http.Response> request(
    String method,
    String path, {
    // Исходная строка 26: Необязательный именованный nullable-параметр: при login токена ещё нет.
    String? token,
    Object? body,
    Map<String, String>? query,
  }) async {
    try {
      final headers = {
        // Исходная строка 32: Формат отправляемого тела, не гарантия формата ответа: login/send возвращают plain text.
        'Content-Type': 'application/json',
        // Исходная строка 33: Null-aware элемент Map добавляет заголовок только при ненулевом token. ? перед значением — синтаксис Dart.
        'X-Auth-Token': ?token,
      };
      final response =
          // Исходная строка 36: Условие выбирает Future GET или POST. await приостанавливает продолжение этого метода, не блокируя event loop.
          await (method == 'GET'
                  ? client.get(uri(path, query), headers: headers)
                  : client.post(
                      uri(path, query),
                      headers: headers,
                      // Исходная строка 41: Map тела превращается в JSON-строку.
                      body: jsonEncode(body),
                    ))
              // Исходная строка 43: Ограничиваем ожидание 15 секундами. Это не гарантия отмены побочного эффекта на сервере.
              .timeout(const Duration(seconds: 15));
      // Исходная строка 44: Без токена — неверный вход; с токеном — истёкшая сессия.
      if (response.statusCode == 401) {
        throw ApiException(token == null ? Failure.invalid : Failure.expired);
      }
      // Исходная строка 47: Отдельно классифицируем ошибки сервиса.
      if (response.statusCode >= 500) throw const ApiException(Failure.server);
      // Исходная строка 48: Другие неуспешные статусы — protocol. Успех — диапазон 2xx.
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw const ApiException(Failure.protocol);
      }
      return response;
    // Исходная строка 52: Классифицированную ошибку пробрасываем с исходным стеком через rethrow.
    } on ApiException {
      rethrow;
    // Исходная строка 54: Таймаут становится connection.
    } on TimeoutException {
      throw const ApiException(Failure.connection);
    // Исходная строка 56: Транспортная ошибка библиотеки тоже становится connection.
    } on http.ClientException {
      throw const ApiException(Failure.connection);
    }
  }

  // Исходная строка 61: Generic-оболочка: переданная parse-функция превращает dynamic JSON в T.
  Future<T> decode<T>(
    Future<http.Response> response,
    T Function(dynamic) parse,
  ) async {
    // Исходная строка 65: Сеть ожидается до внутреннего try: её ошибка не будет ошибочно переклассифицирована как ошибка JSON.
    final r = await response;
    try {
      // Исходная строка 67: Байты → UTF-8 → JSON dynamic → типизированная модель.
      return parse(jsonDecode(utf8.decode(r.bodyBytes)));
    } catch (_) {
      throw const ApiException(Failure.protocol);
    }
  }

  // Исходная строка 73: Передаём официальные поля name/pwd, отличающиеся от локальных login/password.
  Future<Session> login(String login, String password) async {
    final response = await request(
      'POST',
      '/login',
      body: {'name': login, 'pwd': password},
    );
    // Исходная строка 79: Ответ — plain text. jsonDecode здесь был бы неверным.
    final token = utf8.decode(response.bodyBytes).trim();
    if (token.isEmpty) throw const ApiException(Failure.protocol);
    // Исходная строка 81: userId в ответе нет: используем имя, с которым успешно вошли.
    return Session(login, token);
  }

  // Исходная строка 84: Запрос забывает токен на сервере; локальное завершение сессии выполняет ChatState.
  Future<void> logout(String token) async {
    await request('POST', '/logout', token: token);
  }

  // Исходная строка 88: Массив строк превращаем в модели Channel. map ленивый, toList материализует результат.
  Future<List<Channel>> channels(String token) => decode(
    request('GET', '/channels', token: token),
    (j) => (j as List).map((e) => Channel(e as String, e)).toList(),
  );
  // Исходная строка 92: Серверный массив оборачиваем во внутреннюю MessagePage.
  Future<MessagePage> messages(String token, String channel, String? before) =>
      decode(
        request(
          'GET',
          // Исходная строка 96: Имя канала кодируем как один компонент URL.
          '/channel/${Uri.encodeComponent(channel)}',
          token: token,
          query: {
            // Исходная строка 99: Каждая страница запрашивает не более 20 сообщений.
            'limit': '20',
            // Исходная строка 100: Курсор старой страницы либо верхняя граница 0xffffffffffff из веб-примера для новой страницы.
            'lastKnownId': before ?? '281474976710655',
            // Исходная строка 101: Просим записи с ID меньше курсора — движение в прошлое.
            'reverse': 'true',
          },
        ),
        (j) {
          final messages =
              (j as List)
                  .map((e) => Message.fromJson(e as Map<String, dynamic>))
                  .toList()
                // Исходная строка 109: Каскад .. вызывает sort на List и оставляет результатом сам список. sort изменяет список на месте.
                ..sort((a, b) => int.parse(b.id).compareTo(int.parse(a.id)));
          return MessagePage(
            messages,
            // Исходная строка 112: Короткая страница означает конец истории; полная даёт курсор самого старого ID этой страницы.
            messages.length < 20 ? null : messages.last.id,
          );
        },
      );
  // Исходная строка 116: Ровно один POST. Автоматических повторов тут нет.
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
        // Исходная строка 127: Отправитель должен совпадать с авторизованным пользователем.
        'from': user,
        // Исходная строка 128: Адресат задаётся в теле /messages.
        'to': channel,
        'data': {
          // Исходная строка 130: Официальная вложенность data.Text.text.
          'Text': {'text': text},
        },
      },
    );
    // Исходная строка 134: Ответ содержит только ID. Допускаем числовой текст либо ID в кавычках.
    final id = utf8.decode(response.bodyBytes).trim().replaceAll('"', '');
    // Исходная строка 135: Ответ 200 без корректного ID нельзя считать подтверждённой доставкой.
    if (int.tryParse(id) == null) throw const ApiException(Failure.protocol);
    // Исходная строка 136: По ID и уже известным данным создаём подтверждённую модель сообщения.
    return Message(id, user, text, null);
  }

  // Исходная строка 139: Возвращаем байты картинки, без JSON-декодирования.
  Future<Uint8List> image(
    String token,
    String id, {
    bool thumb = false,
  }) async => (await request(
    'GET',
    // Исходная строка 145: Кодируем сегменты пути, сохраняя разделители /. thumb выбирает /thumb или /img.
    '/${thumb ? 'thumb' : 'img'}/${id.split('/').map(Uri.encodeComponent).join('/')}',
    token: token,
  )).bodyBytes;
  // Исходная строка 148: Освобождаем HTTP-клиент в конце жизни владельца.
  void close() => client.close();
}
```
