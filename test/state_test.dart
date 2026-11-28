import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:channel_lab/data/api_client.dart';
import 'package:channel_lab/data/local_store.dart';
import 'package:channel_lab/models/models.dart';
import 'package:channel_lab/state/chat_state.dart';

class MemoryStore extends LocalStore {
  Map<String, Map<String, String>> data = {};
  @override
  Future<void> saveDrafts(String u, Map<String, String> d) async {
    data[u] = Map.of(d);
  }

  @override
  Future<void> clear(String? u) async {
    data.remove(u);
  }
}

http.Response json(Object body) => http.Response(jsonEncode(body), 200);
void main() {
  ChatState make(Future<http.Response> Function(http.Request) handler) =>
      ChatState(
        ApiClient('http://localhost', client: MockClient(handler)),
        MemoryStore(),
      )..session = const Session('alice', 'token');
  test('double send blocked; edits made during request survive', () async {
    final reply = Completer<http.Response>();
    int calls = 0;
    final state = make((_) {
      calls++;
      return reply.future;
    });
    state.draft('1', 'first');
    final sending = state.send('1');
    await state.send('1');
    state.draft('1', 'new draft');
    reply.complete(http.Response('50', 200));
    await sending;
    expect(calls, 1);
    expect(state.drafts['1'], 'new draft');
    expect(state.conversation('1').outgoing.single.status, Delivery.sent);
  });
  test('uncertain result keeps one outgoing item on manual retry', () async {
    int calls = 0;
    final state = make((_) async {
      calls++;
      if (calls == 1) throw http.ClientException('offline');
      return http.Response('51', 200);
    });
    state.draft('1', 'hello');
    await state.send('1');
    final c = state.conversation('1');
    expect(c.outgoing.single.status, Delivery.uncertain);
    expect(state.drafts['1'], 'hello');
    await state.send('1', retryId: c.outgoing.single.localId);
    expect(c.outgoing.length, 1);
    expect(c.messages.single.id, '51');
    expect(state.drafts['1'], '');
  });
  test('late response after logout cannot restore data', () async {
    final reply = Completer<http.Response>();
    final state = make((_) => reply.future);
    final load = state.loadChannels();
    await state.logout(expired: true);
    reply.complete(
      json([
        {'id': '1', 'name': 'old'},
      ]),
    );
    await load;
    expect(state.session, isNull);
    expect(state.channels, isEmpty);
  });
  test('pagination merges IDs and failure preserves history', () async {
    int calls = 0;
    final state = make((request) async {
      expect(request.url.queryParameters['limit'], '20');
      calls++;
      if (calls == 3) return http.Response('', 503);
      return json(
        List.generate(
          20,
          (i) => {
            'id': '${40 - i - (calls == 2 ? 10 : 0)}',
            'from': 'a',
            'data': {
              'Text': {'text': 'hello'},
            },
          },
        ),
      );
    });
    await state.loadMessages('1');
    await state.loadMessages('1', older: true);
    await state.loadMessages('1', older: true);
    expect(state.conversation('1').messages.length, 30);
    expect(state.conversation('1').error, Failure.server);
  });
  test('late response belongs to source conversation', () async {
    final reply = Completer<http.Response>();
    final state = make((_) => reply.future);
    final load = state.loadMessages('1');
    state.selected = const Channel('2', 'two');
    reply.complete(
      json([
        {
          'id': '1',
          'from': 'a',
          'data': {
            'Text': {'text': 'one'},
          },
        },
      ]),
    );
    await load;
    expect(state.conversation('2').messages, isEmpty);
    expect(state.conversation('1').messages.length, 1);
  });
  test('concurrent 401 invalidates session once', () async {
    final state = make((_) async => http.Response('', 401));
    await Future.wait([state.loadChannels(), state.loadMessages('1')]);
    expect(state.session, isNull);
    expect(state.error, Failure.expired);
    expect(state.chats, isEmpty);
  });
}
