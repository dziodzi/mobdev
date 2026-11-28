# lib/data/local_store.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/models.dart';

// Writes are serialized: a late draft write cannot overtake logout deletion.
class LocalStore {
  // Исходная строка 9: Платформенное защищённое хранилище скрыто за отдельным классом. UI не знает его ключей.
  final FlutterSecureStorage storage;
  // Исходная строка 10: Изначально завершённый хвост очереди. Новая операция цепляется после предыдущей.
  Future<void> _tail = Future.value();
  // Исходная строка 11: Внедрение зависимости: переданная storage либо стандартный экземпляр через ??.
  LocalStore({FlutterSecureStorage? storage})
    : storage = storage ?? const FlutterSecureStorage();
  // Исходная строка 13: Generic T сохраняет тип результата действия: Session?, Map или void.
  Future<T> _queue<T>(Future<T> Function() action) {
    // Исходная строка 14: action запустится после хвоста. Если возвращает Future, then ждёт её завершения, не создавая неудобную Future<Future<T>>.
    final next = _tail.then((_) => action());
    // Исходная строка 15: Отдельный хвост поглощает ошибку, чтобы очередь продолжилась. Но caller получит исходный next с его ошибкой — ошибка не потеряна.
    _tail = next.then<void>((_) {}, onError: (Object _, StackTrace _) {});
    // Исходная строка 16: Возвращаем результат именно этой операции.
    return next;
  }

  // Исходная строка 19: Чтение сессии также стоит в общей очереди. null означает отсутствие сохранённого входа.
  Future<Session?> session() => _queue(() async {
    // Исходная строка 20: Асинхронное чтение строки из платформенного хранилища.
    final raw = await storage.read(key: 'session');
    return raw == null
        ? null
        // Исходная строка 23: Строка → JSON Map → Session. Повреждённый JSON даст ошибку, которую обработает start.
        : Session.fromJson(jsonDecode(raw) as Map<String, dynamic>);
  });
  // Исходная строка 25: Модель преобразуется в JSON и записывается. Пароля в Session нет.
  Future<void> saveSession(Session session) => _queue(
    () => storage.write(key: 'session', value: jsonEncode(session.toJson())),
  );
  // Исходная строка 28: Ключ включает пользователя. encodeComponent делает специальные символы однозначными.
  String _key(String user) => 'drafts:${Uri.encodeComponent(user)}';
  // Исходная строка 29: Словарь черновиков одного пользователя, channelId → text. Если записи нет, возвращаем {}.
  Future<Map<String, String>> drafts(String user) => _queue(() async {
    final raw = await storage.read(key: _key(user));
    // Исходная строка 31: Проверяем и копируем декодированную Map в конкретный тип.
    return raw == null ? {} : Map<String, String>.from(jsonDecode(raw) as Map);
  });
  Future<void> saveDrafts(String user, Map<String, String> drafts) {
    // Исходная строка 34: Фиксируем строковый снимок сейчас, до ожидания очереди. Позднейшие изменения исходной Map его не изменят.
    final snapshot = jsonEncode(drafts);
    // Исходная строка 35: Closure захватывает snapshot, а не изменяемую drafts.
    return _queue(() => storage.write(key: _key(user), value: snapshot));
  }

  // Исходная строка 38: Удаление следует за прежними записями: старая запись не обгонит очистку при logout.
  Future<void> clear(String? user) => _queue(() async {
    // Исходная строка 39: Удаляем сессию, затем черновики только вышедшего пользователя.
    await storage.delete(key: 'session');
    if (user != null) await storage.delete(key: _key(user));
  });
}
```
