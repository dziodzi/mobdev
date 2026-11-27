import 'dart:async';
import 'dart:convert';

import 'package:channel_chat/data/chat_api.dart';
import 'package:channel_chat/data/local_store.dart';
import 'package:channel_chat/data/models.dart';
import 'package:channel_chat/state/chat_state.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

class MemoryStore implements LocalStore {
  Session? session;
  final users = <String, Map<String, dynamic>>{};

  @override
  Future<Session?> readSession() async => session;

  @override
  Future<void> saveSession(Session value) async => session = value;

  @override
  Future<void> deleteSession() async => session = null;

  @override
  Future<Map<String, dynamic>> readUser(String user) async => users[user] ?? {};

  @override
  Future<void> saveUser(String user, String snapshot) async {
    users[user] = jsonDecode(snapshot) as Map<String, dynamic>;
  }

  @override
  Future<void> deleteUser(String user) async => users.remove(user);
}

void main() {
  const channel = 'test@channel';
  final origin = Uri.parse('https://example.test');
  const serverMessage =
      '[{"id":7,"from":"demo","to":"test@channel","time":1700000000000,'
      '"data":{"Text":{"text":"first"}}}]';

  test(
    'confirmed send preserves a newer draft and merges the same server ID',
    () async {
      final sending = Completer<String?>();
      final response = Completer<http.Response>();
      final store = MemoryStore();
      final api = ChatApi(
        MockClient((request) async {
          switch (request.url.path) {
            case '/login':
              return http.Response('token', 200);
            case '/channels':
              return http.Response('["test@channel"]', 200);
            case '/messages':
              sending.complete(request.headers['x-auth-token']);
              return response.future;
            default:
              return http.Response(serverMessage, 200);
          }
        }),
        origin,
      );
      final state = ChatState(api, store);
      addTearDown(state.dispose);
      await state.restore();
      await state.signIn('demo', 'password');
      expect(state.session, isNotNull, reason: state.loginProblem.toString());
      state.select(channel);
      final chat = state.selected!;
      state.editDraft(chat, 'first');
      final send = state.send(chat);
      expect(await sending.future.timeout(const Duration(seconds: 3)), 'token');

      await state.send(chat);
      state.editDraft(chat, 'second');
      response.complete(http.Response('7', 200));
      await send;
      await state.loadHistory(chat);

      expect(chat.draft, 'second');
      expect((await store.readUser('demo'))[channel]['draft'], 'second');
      expect(chat.outgoing, isEmpty);
      expect(chat.messages.keys, [7]);
      expect(chat.sentIds, {7});
    },
  );

  test(
    'ambiguous delivery retries the same item only on a manual request',
    () async {
      var attempts = 0;
      final retryReachedServer = Completer<void>();
      final retryResult = Completer<http.Response>();
      final store = MemoryStore();
      final api = ChatApi(
        MockClient((request) async {
          switch (request.url.path) {
            case '/login':
              return http.Response('token', 200);
            case '/channels':
              return http.Response('[]', 200);
            case '/messages':
              if (++attempts == 1) {
                throw http.ClientException('connection lost');
              }
              retryReachedServer.complete();
              return retryResult.future;
            default:
              return http.Response('[]', 200);
          }
        }),
        origin,
      );
      final state = ChatState(api, store);
      addTearDown(state.dispose);
      await state.restore();
      await state.signIn('demo', 'password');
      expect(state.session, isNotNull, reason: state.loginProblem.toString());
      state.select(channel);
      final chat = state.selected!;
      state.editDraft(chat, 'first');
      await state.send(chat);

      final failed = chat.outgoing.single;
      expect(failed.delivery, Delivery.uncertain);
      expect(chat.draft, 'first');
      expect(attempts, 1);

      final send = state.send(chat, retry: failed);
      await retryReachedServer.future.timeout(const Duration(seconds: 3));
      expect(chat.outgoing.single.id, failed.id);
      expect(chat.outgoing.single.delivery, Delivery.sending);
      retryResult.complete(http.Response('7', 200));
      await send;
      expect(chat.outgoing, isEmpty);
      expect(chat.draft, isEmpty);
      expect(chat.messages.keys, [7]);
      expect(attempts, 2);
    },
  );
}
