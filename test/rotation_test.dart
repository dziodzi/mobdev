import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:channel_lab/app.dart';
import 'package:channel_lab/data/api_client.dart';
import 'package:channel_lab/data/local_store.dart';
import 'package:channel_lab/models/models.dart';
import 'package:channel_lab/state/chat_state.dart';

class Store extends LocalStore {
  @override
  Future<Session?> session() async => const Session('alice', 'token');
  @override
  Future<Map<String, String>> drafts(String user) async => {};
  @override
  Future<void> saveDrafts(String user, Map<String, String> data) async {}
}

void main() {
  testWidgets('rotation keeps chat, draft, image and does not refetch', (
    tester,
  ) async {
    int requests = 0;
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(500, 900);
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final state = ChatState(
      ApiClient(
        'http://localhost',
        client: MockClient((request) async {
          requests++;
          if (request.url.path == '/channels') {
            return http.Response(jsonEncode(['1@channel']), 200);
          }
          if (request.url.path.startsWith('/channel/')) {
            return http.Response(
              jsonEncode([
                {
                  'id': '1',
                  'from': 'alice',
                  'data': {
                    'Image': {'link': 'sample'},
                  },
                },
              ]),
              200,
            );
          }
          return http.Response.bytes(
            File('tool/sample.png').readAsBytesSync(),
            200,
          );
        }),
      ),
      Store(),
    );
    await tester.pumpWidget(ChatApp(state: state));
    await tester.pumpAndSettle();
    await tester.tap(find.text('1@channel'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'draft');
    await tester.pumpAndSettle();
    final count = requests;
    tester.view.physicalSize = const Size(900, 500);
    await tester.pumpAndSettle();
    expect(state.selected?.id, '1@channel');
    expect(find.text('draft'), findsOneWidget);
    expect(requests, count);
    state.openImage('sample');
    await tester.pumpAndSettle();
    final withImage = requests;
    tester.view.physicalSize = const Size(500, 900);
    await tester.pumpAndSettle();
    expect(state.openedImage, 'sample');
    expect(requests, withImage);
    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();
    expect(state.selected?.id, '1@channel');
    expect(find.text('draft'), findsOneWidget);
  });
}
