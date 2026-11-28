# lib/main.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
import 'package:flutter/material.dart';

import 'app.dart';
import 'data/api_client.dart';
import 'data/local_store.dart';
import 'state/chat_state.dart';

// Исходная строка 8: Точка входа в программу Dart. Выполнение начинается с этой функции.
void main() {
  // Исходная строка 9: Создаёт/получает binding, связывающий framework, движок и платформенные службы. Это ещё не построение экрана.
  WidgetsFlutterBinding.ensureInitialized();
  // Исходная строка 10: Константа берётся из compile-time define --dart-define=API_URL=...; это не чтение окружения ОС во время работы приложения.
  const url = String.fromEnvironment(
    'API_URL',
    // Исходная строка 12: Если define не указан, выбирается официальный сервер.
    defaultValue: 'https://faerytea.name',
  );
  // Исходная строка 14: Сначала создаём ApiClient и LocalStore, передаём их ChatState, затем ChatApp. runApp прикрепляет корневой виджет к дереву Flutter. Это явное внедрение зависимостей.
  runApp(ChatApp(state: ChatState(ApiClient(url), LocalStore())));
}
```
