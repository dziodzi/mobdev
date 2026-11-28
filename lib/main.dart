import 'package:flutter/material.dart';

import 'app.dart';
import 'data/api_client.dart';
import 'data/local_store.dart';
import 'state/chat_state.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  const url = String.fromEnvironment(
    'API_URL',
    defaultValue: 'https://faerytea.name',
  );
  runApp(ChatApp(state: ChatState(ApiClient(url), LocalStore())));
}
