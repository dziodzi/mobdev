import 'dart:async';

import 'package:channel_chat/data/chat_api.dart';
import 'package:channel_chat/data/models.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;

class LoginResponse extends http.BaseClient {
  LoginResponse(this.body);

  final Stream<List<int>> body;

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async =>
      http.StreamedResponse(body, 401);
}

void main() {
  test('login rejects credentials as soon as 401 headers arrive', () async {
    var canceled = false;
    final body = StreamController<List<int>>(onCancel: () => canceled = true);
    final api = ChatApi(LoginResponse(body.stream), Uri.https('example.test'));
    addTearDown(api.close);
    addTearDown(body.close);

    await expectLater(
      api.login('test', 'invalid').timeout(const Duration(seconds: 1)),
      throwsA(
        isA<ApiException>().having(
          (error) => error.problem,
          'problem',
          Problem.credentials,
        ),
      ),
    );
    expect(canceled, isTrue);
  });
}
