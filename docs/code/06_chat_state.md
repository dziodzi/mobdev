# lib/state/chat_state.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
import 'package:flutter/foundation.dart';

import '../data/api_client.dart';
import '../data/local_store.dart';
import '../models/models.dart';

// Исходная строка 7: Состояние ОДНОГО канала вне UI: виджет может исчезнуть, а история и позиция останутся.
class Conversation {
  // Исходная строка 8: Список подтверждённых сообщений; его заменяем после объединения страниц.
  List<Message> messages = [];
  // Исходная строка 9: final фиксирует ссылку, но не содержимое List: add и замена элемента разрешены.
  final List<Outgoing> outgoing = [];
  // Исходная строка 10: Курсор предыдущей страницы; null — старых страниц больше нет.
  String? next;
  // Исходная строка 11: initialized отличает незагруженный чат от загруженного пустого; busy блокирует параллельные GET, sending — POST.
  bool initialized = false, busy = false, sending = false;
  Failure? error;
  // Исходная строка 13: Смещение просмотра канала в пикселях, независимо от виджета списка.
  double offset = 0;
}

// Исходная строка 16: Уведомляем подписчиков явно через notifyListeners. Само присваивание поля ничего не перерисовывает.
class ChatState extends ChangeNotifier {
  final ApiClient api;
  final LocalStore store;
  // Исходная строка 19: Зависимости переданы конструктором: в тесте подставляем другой клиент и хранилище.
  ChatState(this.api, this.store);
  // Исходная строка 20: null означает неавторизованную ветку приложения.
  Session? session;
  bool booting = true, signingIn = false, loadingChannels = false;
  Failure? error, channelsError, storageError;
  List<Channel> channels = [];
  Channel? selected;
  String? openedImage;
  // Исходная строка 26: Черновики активного пользователя: ID канала → текст.
  Map<String, String> drafts = {};
  // Исходная строка 27: Число изменений каждого черновика; защищает новый ввод от очистки после старой отправки.
  final Map<String, int> _revisions = {};
  // Исходная строка 28: Отдельный Conversation для каждого канала. Не смешиваем ответы разных чатов.
  final Map<String, Conversation> chats = {};
  // Исходная строка 29: Кэш Future байтов, отдельно от виджетов; общий запрос переживает смену раскладки.
  final Map<String, Future<Uint8List>> _images = {};
  // Исходная строка 30: _epoch — версия сессии для отбрасывания старых ответов; _local — счётчик локальных исходящих ID.
  int _epoch = 0, _local = 0;
  Conversation conversation(String id) =>
      // Исходная строка 32: Вернуть существующий Conversation либо создать через constructor tear-off Conversation.new и сохранить.
      chats.putIfAbsent(id, Conversation.new);
  // Исходная строка 33: Старт: восстановить сессию, затем черновики её пользователя, затем загрузить каналы.
  Future<void> start() async {
    try {
      // Исходная строка 35: На время ожидания booting=true. Ошибка восстановления будет показана как storage.
      session = await store.session();
      if (session != null) drafts = await store.drafts(session!.userId);
    } catch (_) {
      error = Failure.storage;
      session = null;
    }
    // Исходная строка 41: Разрешаем _Shell выбрать экран по результату восстановления.
    booting = false;
    notifyListeners();
    if (session != null) await loadChannels();
  }

  // Исходная строка 46: Попытка входа и установка подтверждённой сессии.
  Future<void> login(String login, String password) async {
    // Исходная строка 47: Защита от повторного вызова до await: работает даже если UI ещё не отключил кнопку.
    if (signingIn) return;
    // Исходная строка 48: Синхронно блокируем повтор до начала сети.
    signingIn = true;
    error = null;
    notifyListeners();
    // Исходная строка 51: Снимок версии сессии: после await состояние могло измениться.
    final epoch = _epoch;
    try {
      // Исходная строка 53: Ждём ответ; s пока локальная переменная, а не опубликованная session.
      final s = await api.login(login, password);
      // Исходная строка 54: Результат/ошибка старой сессии игнорируется. Второй одновременный 401 после первого logout тоже игнорируется.
      if (epoch != _epoch) return;
      // Исходная строка 55: Сохраняем токен до перехода к авторизованному экрану; сбой записи трактуем отдельно от неверного пароля.
      await store.saveSession(s);
      final saved = await store.drafts(s.userId);
      // Исходная строка 57: Результат/ошибка старой сессии игнорируется. Второй одновременный 401 после первого logout тоже игнорируется.
      if (epoch != _epoch) return;
      // Исходная строка 58: Публикуем успешную сессию и затем её черновики.
      session = s;
      drafts = saved;
    } on ApiException catch (e) {
      error = e.kind;
    } catch (_) {
      error = Failure.storage;
    // Исходная строка 64: Выполнится и при успехе, и при ошибке, и при return из try: индикатор не должен зависать.
    } finally {
      signingIn = false;
      notifyListeners();
    }
    if (session != null) await loadChannels();
  }

  // Исходная строка 71: Сначала сбрасываем экранные данные, потом очищаем диск и пытаемся уведомить сервер.
  Future<void> logout({bool expired = false}) async {
    // Исходная строка 72: Захватываем пользователя до обнуления session, чтобы удалить именно его черновики.
    final user = session?.userId;
    final oldToken = session?.token;
    // Исходная строка 74: Все запросы прежней эпохи становятся устаревшими. Это логическая отмена результатов, не закрытие сокета.
    _epoch++;
    session = null;
    selected = null;
    openedImage = null;
    drafts = {};
    // Исходная строка 79: История старого пользователя больше не доступна новому пользователю в памяти.
    chats.clear();
    _images.clear();
    channels = [];
    loadingChannels = false;
    channelsError = null;
    storageError = null;
    _revisions.clear();
    error = expired ? Failure.expired : null;
    notifyListeners();
    try {
      // Исходная строка 89: Удаление стоит в очереди после старых записей; черновик не воскреснет из-за запоздавшей записи.
      await store.clear(user);
      // Исходная строка 90: При ручном выходе забываем токен на сервере. При 401 он уже недействителен; повторный logout не нужен.
      if (!expired && oldToken != null) {
        try {
          await api.logout(oldToken);
        } on ApiException {
          /* Local logout remains effective offline. */
        }
      }
    } catch (_) {
      error = Failure.storage;
      notifyListeners();
    }
  }

  // Исходная строка 103: Центральное правило 401 и ошибок операций.
  void _failure(ApiException e, int epoch, void Function(Failure) set) {
    // Исходная строка 104: Результат/ошибка старой сессии игнорируется. Второй одновременный 401 после первого logout тоже игнорируется.
    if (epoch != _epoch) return;
    if (e.kind == Failure.expired) {
      // Исходная строка 106: Первый защищённый 401 завершает сессию один раз.
      logout(expired: true);
    } else {
      // Исходная строка 108: set — переданная callback-функция записи ошибки, например (f)=>c.error=f. Это не Flutter setState.
      set(e.kind);
    }
  }

  // Исходная строка 112: Обновление списка; прежний channels перед запросом не очищается, поэтому ошибка не стирает данные.
  Future<void> loadChannels() async {
    // Исходная строка 113: Не делаем запрос без входа и не запускаем такой же параллельный запрос.
    if (session == null || loadingChannels) return;
    final epoch = _epoch, token = session!.token;
    loadingChannels = true;
    channelsError = null;
    notifyListeners();
    try {
      final result = await api.channels(token);
      // Исходная строка 120: Ответ может изменить только ту сессию, которая его запросила.
      if (epoch == _epoch) channels = result;
    } on ApiException catch (e) {
      _failure(e, epoch, (f) => channelsError = f);
    // Исходная строка 123: Выполнится и при успехе, и при ошибке, и при return из try: индикатор не должен зависать.
    } finally {
      if (epoch == _epoch) {
        loadingChannels = false;
        notifyListeners();
      }
    }
  }

  // Исходная строка 131: Меняем выбранный канал и уведомляем UI; загружаем его при первом открытии.
  void select(Channel c) {
    selected = c;
    notifyListeners();
    // Исходная строка 134: Повторное открытие загруженного чата не должно автоматически повторять GET.
    if (!conversation(c.id).initialized) loadMessages(c.id);
  }

  // Исходная строка 137: Сначала закрываем картинку, иначе чат. Авторизацию этот метод не меняет.
  void back() {
    if (openedImage != null) {
      openedImage = null;
    } else {
      selected = null;
    }
    notifyListeners();
  }

  // Исходная строка 146: Выбранное изображение находится выше экранов, поэтому остаётся открытым при повороте.
  void openImage(String id) {
    openedImage = id;
    notifyListeners();
  }

  // Исходная строка 151: Вызывается пользовательским onChanged. Не перестраиваем весь экран на каждый символ: TextField уже показывает ввод через editor.
  void draft(String id, String text) {
    drafts[id] = text;
    // Исходная строка 153: Каждая правка увеличивает версию, даже если пользователь потом вернул прежний текст.
    _revisions[id] = (_revisions[id] ?? 0) + 1;
    final s = session;
    if (s == null) return;
    // Исходная строка 156: Сохранение асинхронное; catchError показывает ошибку записи, если этот пользователь ещё активен.
    store.saveDrafts(s.userId, drafts).catchError((Object _) {
      if (session?.userId == s.userId) {
        storageError = Failure.storage;
        notifyListeners();
      }
    });
  }

  // Исходная строка 164: Явный id — адресат результата. После await selected может стать другим, но ответ останется в своём Conversation.
  Future<void> loadMessages(String id, {bool older = false}) async {
    if (session == null) return;
    final c = conversation(id);
    // Исходная строка 167: Один GET на канал за раз. Если older=true, но next=null, идти дальше некуда.
    if (c.busy || (older && c.next == null)) return;
    final epoch = _epoch, token = session!.token;
    c.busy = true;
    c.error = null;
    notifyListeners();
    try {
      // Исходная строка 173: Для старой страницы используем курсор; для refresh берём верхнюю страницу заново.
      final page = await api.messages(token, id, older ? c.next : null);
      // Исходная строка 174: Результат/ошибка старой сессии игнорируется. Второй одновременный 401 после первого logout тоже игнорируется.
      if (epoch != _epoch) return;
      // Исходная строка 175: Collection for строит Map serverId → Message из текущей истории.
      final byId = {for (final m in c.messages) m.id: m};
      for (final m in page.messages) {
        // Исходная строка 177: Тот же ID заменяется, новый добавляется: это дедупликация по серверному ID, не по тексту.
        byId[m.id] = m;
      }
      // Исходная строка 179: Объединённые записи превращаем в новый List и сортируем по числовому ID от новых к старым.
      c.messages = byId.values.toList()
        ..sort((a, b) => int.parse(b.id).compareTo(int.parse(a.id)));
      // Исходная строка 181: Refresh верхней страницы не должен сбрасывать курсор глубокой уже загруженной истории.
      if (older || !c.initialized) c.next = page.next;
      c.initialized = true;
    } on ApiException catch (e) {
      _failure(e, epoch, (f) => c.error = f);
    // Исходная строка 185: Выполнится и при успехе, и при ошибке, и при return из try: индикатор не должен зависать.
    } finally {
      if (epoch == _epoch) {
        c.busy = false;
        notifyListeners();
      }
    }
  }

  // Исходная строка 193: Одна логическая попытка: локальный статус, один POST, подтверждение или неопределённость.
  Future<void> send(String id, {String? retryId}) async {
    if (session == null) return;
    final c = conversation(id);
    // Исходная строка 196: Повторный вызов останавливаем до создания второй попытки и запроса.
    if (c.sending) return;
    // Исходная строка 197: При новой отправке -1; при повторе ищем старую попытку по стабильному localId.
    final index = retryId == null
        ? -1
        : c.outgoing.indexWhere((m) => m.localId == retryId);
    // Исходная строка 200: Новая отправка берёт draft; повтор берёт неизменный текст старой попытки, а не текущий редактор.
    final text = index < 0 ? (drafts[id] ?? '') : c.outgoing[index].text;
    // Исходная строка 201: Пробелы/переносы не отправляем. trim только проверяет пустоту, исходный текст сохраняется.
    if (text.trim().isEmpty) return;
    // Исходная строка 202: Снимок версии редактирования перед отправкой.
    final revision = _revisions[id] ?? 0;
    // Исходная строка 203: Префиксный ++ увеличивает счётчик и использует новое значение; при повторе ID прежний.
    final localId = index < 0 ? 'local-${++_local}' : c.outgoing[index].localId;
    // Исходная строка 204: Локальная запись сразу показывает sending, ещё до ответа.
    final outgoing = Outgoing(localId, text, Delivery.sending);
    if (index < 0) {
      c.outgoing.add(outgoing);
    } else {
      // Исходная строка 208: Повтор заменяет элемент вместо добавления второй локальной копии.
      c.outgoing[index] = outgoing;
    }
    c.sending = true;
    final epoch = _epoch, token = session!.token;
    notifyListeners();
    try {
      // Исходная строка 214: Единственный POST этой попытки. Никакого автоматического повторения при сетевой ошибке.
      final result = await api.send(token, id, text, session!.userId);
      // Исходная строка 215: Результат/ошибка старой сессии игнорируется. Второй одновременный 401 после первого logout тоже игнорируется.
      if (epoch != _epoch) return;
      // Исходная строка 216: Находим ту же запись по localId и заменяем immutable-модель новым статусом.
      c.outgoing[c.outgoing.indexWhere((m) => m.localId == localId)] = Outgoing(
        localId,
        text,
        Delivery.sent,
        result.id,
      );
      // Исходная строка 222: Если serverId уже попал в историю, удаляем прежнее представление перед вставкой — дубля ID не будет.
      c.messages.removeWhere((m) => m.id == result.id);
      // Исходная строка 223: Подтверждённое сообщение становится первым в истории новых записей.
      c.messages.insert(0, result);
      // Исходная строка 224: Очищаем текст только если совпали И версия, И текст. Новый ввод во время отправки должен остаться.
      if ((_revisions[id] ?? 0) == revision && drafts[id] == text) {
        // Исходная строка 225: Очистка также сохраняется на диск через общий метод draft.
        draft(id, '');
      }
    } on ApiException catch (e) {
      // Исходная строка 228: Результат/ошибка старой сессии игнорируется. Второй одновременный 401 после первого logout тоже игнорируется.
      if (epoch != _epoch) return;
      // Исходная строка 229: Находим ту же запись по localId и заменяем immutable-модель новым статусом.
      c.outgoing[c.outgoing.indexWhere((m) => m.localId == localId)] = Outgoing(
        localId,
        text,
        // Исходная строка 232: Ошибка запроса не доказывает недоставку: сервер мог принять сообщение и потерять ответ.
        Delivery.uncertain,
      );
      _failure(e, epoch, (f) => c.error = f);
    // Исходная строка 235: Выполнится и при успехе, и при ошибке, и при return из try: индикатор не должен зависать.
    } finally {
      if (epoch == _epoch) {
        c.sending = false;
        notifyListeners();
      }
    }
  }

  // Исходная строка 243: Возвращаем текст попытки в редактор: тот же текст не дублируем; к другому непустому черновику добавляем через перенос строки.
  void restore(String id, Outgoing message) {
    final current = drafts[id] ?? '';
    draft(
      id,
      current == message.text
          ? current
          : current.isEmpty
          ? message.text
          : '$current\n${message.text}',
    );
    notifyListeners();
  }

  // Исходная строка 256: Возвращаем общую Future из кэша без async-обёртки.
  Future<Uint8List> image(String id, {bool thumb = false}) {
    // Исходная строка 257: Миниатюра и оригинал — разные ресурсы: thumb:ID и img:ID.
    final key = "${thumb ? 'thumb' : 'img'}:$id";
    // Исходная строка 258: Первый вызов создаёт Future, остальные получают её же, включая уже завершённую.
    return _images.putIfAbsent(
      key,
      () => _fetchImage(id, thumb: thumb).onError((error, stack) {
        // Исходная строка 261: Ошибочная Future удаляется, иначе Повторить снова показал бы ту же ошибку без запроса.
        _images.remove(key);
        // Исходная строка 262: Пробрасываем исходную ошибку и стек. ! утверждает ненулевое значение аргумента callback.
        Error.throwWithStackTrace(error!, stack);
      }),
    );
  }

  // Исходная строка 267: Реальная сеть для картинки с общей обработкой истёкшей сессии.
  Future<Uint8List> _fetchImage(String id, {bool thumb = false}) async {
    // Исходная строка 268: Снимок версии сессии: после await состояние могло измениться.
    final epoch = _epoch;
    final s = session;
    if (s == null) throw const ApiException(Failure.expired);
    try {
      return await api.image(s.token, id, thumb: thumb);
    } on ApiException catch (e) {
      _failure(e, epoch, (_) {});
      rethrow;
    }
  }

  @override
  void dispose() {
    // Исходная строка 281: Освобождаем клиент при уничтожении владельца.
    api.close();
    super.dispose();
  }
}
```
