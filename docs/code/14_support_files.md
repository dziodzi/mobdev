# Тесты, mock, конфигурация и платформенные точки входа

[К порядку чтения](../../CODE_WALKTHROUGH.md)

Ниже тоже приведён реальный код. Изучать сначала 12 файлов приложения; эти файлы объясняют проверку и запуск, а не добавляют ещё один слой бизнес-логики.

## test/state_test.dart

```dart
import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:channel_lab/data/api_client.dart';
import 'package:channel_lab/data/local_store.dart';
import 'package:channel_lab/models/models.dart';
import 'package:channel_lab/state/chat_state.dart';

// Исходная строка 12: Подмена LocalStore для теста без платформенного хранилища.
class MemoryStore extends LocalStore {
  Map<String, Map<String, String>> data = {};
  @override
  // Исходная строка 15: Переопределение метода: фиксируем копию Map в памяти, не пишем в KeyStore.
  Future<void> saveDrafts(String u, Map<String, String> d) async {
    // Исходная строка 16: Копия защищает сохранённое тестовое значение от следующих правок исходной Map.
    data[u] = Map.of(d);
  }

  @override
  Future<void> clear(String? u) async {
    data.remove(u);
  }
}

// Исходная строка 25: Помощник создаёт HTTP-ответ с JSON; это не вызов сервера.
http.Response json(Object body) => http.Response(jsonEncode(body), 200);
void main() {
  // Исходная строка 27: Фабрика тестового контроллера: настоящий ApiClient получает MockClient, поэтому проверяем ещё и преобразование сетевого ответа.
  ChatState make(Future<http.Response> Function(http.Request) handler) =>
      ChatState(
        ApiClient('http://localhost', client: MockClient(handler)),
        MemoryStore(),
      // Исходная строка 31: Каскад устанавливает тестовую сессию и возвращает сам созданный ChatState.
      )..session = const Session('alice', 'token');
  test('double send blocked; edits made during request survive', () async {
    // Исходная строка 33: Completer позволяет вручную завершить Future в нужный момент и воспроизвести порядок гонки.
    final reply = Completer<http.Response>();
    int calls = 0;
    final state = make((_) {
      calls++;
      return reply.future;
    });
    state.draft('1', 'first');
    final sending = state.send('1');
    // Исходная строка 41: Проверяем поведение реального метода, а не вручную выставляем ожидаемые поля.
    await state.send('1');
    state.draft('1', 'new draft');
    // Исходная строка 43: Именно здесь зависший тестовый запрос получает ответ. Код после await в контроллере теперь может продолжиться.
    reply.complete(http.Response('50', 200));
    await sending;
    // Исходная строка 45: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(calls, 1);
    // Исходная строка 46: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.drafts['1'], 'new draft');
    // Исходная строка 47: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
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
    // Исходная строка 57: Проверяем поведение реального метода, а не вручную выставляем ожидаемые поля.
    await state.send('1');
    final c = state.conversation('1');
    // Исходная строка 59: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(c.outgoing.single.status, Delivery.uncertain);
    // Исходная строка 60: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.drafts['1'], 'hello');
    // Исходная строка 61: Проверяем поведение реального метода, а не вручную выставляем ожидаемые поля.
    await state.send('1', retryId: c.outgoing.single.localId);
    // Исходная строка 62: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(c.outgoing.length, 1);
    // Исходная строка 63: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(c.messages.single.id, '51');
    // Исходная строка 64: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.drafts['1'], '');
  });
  test('late response after logout cannot restore data', () async {
    // Исходная строка 67: Completer позволяет вручную завершить Future в нужный момент и воспроизвести порядок гонки.
    final reply = Completer<http.Response>();
    final state = make((_) => reply.future);
    final load = state.loadChannels();
    // Исходная строка 70: Завершаем сессию ДО ответа старого запроса. expired:true исключает отдельный сетевой logout из этого сценария.
    await state.logout(expired: true);
    // Исходная строка 71: Именно здесь зависший тестовый запрос получает ответ. Код после await в контроллере теперь может продолжиться.
    reply.complete(
      json([
        {'id': '1', 'name': 'old'},
      ]),
    );
    await load;
    // Исходная строка 77: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.session, isNull);
    // Исходная строка 78: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.channels, isEmpty);
  });
  test('pagination merges IDs and failure preserves history', () async {
    int calls = 0;
    final state = make((request) async {
      // Исходная строка 83: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
      expect(request.url.queryParameters['limit'], '20');
      calls++;
      if (calls == 3) return http.Response('', 503);
      return json(
        // Исходная строка 87: Формируем 20 элементов страницы; страницы намеренно пересекаются по ID, проверяя дедупликацию.
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
    // Исходная строка 102: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.conversation('1').messages.length, 30);
    // Исходная строка 103: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.conversation('1').error, Failure.server);
  });
  test('late response belongs to source conversation', () async {
    // Исходная строка 106: Completer позволяет вручную завершить Future в нужный момент и воспроизвести порядок гонки.
    final reply = Completer<http.Response>();
    final state = make((_) => reply.future);
    final load = state.loadMessages('1');
    state.selected = const Channel('2', 'two');
    // Исходная строка 110: Именно здесь зависший тестовый запрос получает ответ. Код после await в контроллере теперь может продолжиться.
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
    // Исходная строка 122: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.conversation('2').messages, isEmpty);
    // Исходная строка 123: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.conversation('1').messages.length, 1);
  });
  test('concurrent 401 invalidates session once', () async {
    final state = make((_) async => http.Response('', 401));
    // Исходная строка 127: Начинаем две разные операции с 401 и дожидаемся обеих: только одна актуальная эпоха может завершить сессию.
    await Future.wait([state.loadChannels(), state.loadMessages('1')]);
    // Исходная строка 128: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.session, isNull);
    // Исходная строка 129: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.error, Failure.expired);
    // Исходная строка 130: Проверяем наблюдаемое поведение: число запросов, статус, оставшийся текст и принадлежность сообщений.
    expect(state.chats, isEmpty);
  });
}
```

## test/rotation_test.dart

```dart
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

// Исходная строка 13: Подмена стартового чтения: приложение сразу восстанавливает тестовую Session без KeyStore.
class Store extends LocalStore {
  @override
  Future<Session?> session() async => const Session('alice', 'token');
  @override
  Future<Map<String, String>> drafts(String user) async => {};
  @override
  Future<void> saveDrafts(String user, Map<String, String> data) async {}
}

void main() {
  // Исходная строка 23: Тест с Flutter WidgetTester: строим реальное дерево и взаимодействуем с ним.
  testWidgets('rotation keeps chat, draft, image and does not refetch', (
    tester,
  ) async {
    int requests = 0;
    // Исходная строка 27: Фиксируем масштаб тестового окна, чтобы physicalSize давал предсказуемые логические размеры.
    tester.view.devicePixelRatio = 1;
    // Исходная строка 28: Меняем доступную форму экрана: узкий высокий портрет или широкий низкий landscape.
    tester.view.physicalSize = const Size(500, 900);
    // Исходная строка 29: Возвращаем настройки после теста, чтобы они не влияли на следующие тесты.
    addTearDown(tester.view.resetPhysicalSize);
    // Исходная строка 30: Возвращаем настройки после теста, чтобы они не влияли на следующие тесты.
    addTearDown(tester.view.resetDevicePixelRatio);
    final state = ChatState(
      ApiClient(
        'http://localhost',
        client: MockClient((request) async {
          // Исходная строка 35: Счётчик всех HTTP-вызовов. Поворот должен перестроить UI без увеличения этого числа.
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
          // Исходная строка 53: Картинка берётся из локального PNG, но проходит через RemoteImage/Image.memory.
          return http.Response.bytes(
            File('tool/sample.png').readAsBytesSync(),
            200,
          );
        }),
      ),
      Store(),
    );
    // Исходная строка 61: Прикрепляем настоящий ChatApp к тестовому дереву.
    await tester.pumpWidget(ChatApp(state: state));
    // Исходная строка 62: Даём запланированным кадрам/анимациям завершиться. Это не реальная пауза пользователя.
    await tester.pumpAndSettle();
    // Исходная строка 63: Нажатие по найденному виджету, как пользовательский ввод.
    await tester.tap(find.text('1@channel'));
    // Исходная строка 64: Даём запланированным кадрам/анимациям завершиться. Это не реальная пауза пользователя.
    await tester.pumpAndSettle();
    // Исходная строка 65: Вводим текст через TextField, вызывая настоящий путь onChanged → draft.
    await tester.enterText(find.byType(TextField), 'draft');
    // Исходная строка 66: Даём запланированным кадрам/анимациям завершиться. Это не реальная пауза пользователя.
    await tester.pumpAndSettle();
    final count = requests;
    // Исходная строка 68: Меняем доступную форму экрана: узкий высокий портрет или широкий низкий landscape.
    tester.view.physicalSize = const Size(900, 500);
    // Исходная строка 69: Даём запланированным кадрам/анимациям завершиться. Это не реальная пауза пользователя.
    await tester.pumpAndSettle();
    // Исходная строка 70: Утверждение о состоянии/дереве/числе запросов. Тест не доказывает работу физического датчика или KeyStore.
    expect(state.selected?.id, '1@channel');
    // Исходная строка 71: Утверждение о состоянии/дереве/числе запросов. Тест не доказывает работу физического датчика или KeyStore.
    expect(find.text('draft'), findsOneWidget);
    // Исходная строка 72: Утверждение о состоянии/дереве/числе запросов. Тест не доказывает работу физического датчика или KeyStore.
    expect(requests, count);
    state.openImage('sample');
    // Исходная строка 74: Даём запланированным кадрам/анимациям завершиться. Это не реальная пауза пользователя.
    await tester.pumpAndSettle();
    final withImage = requests;
    // Исходная строка 76: Меняем доступную форму экрана: узкий высокий портрет или широкий низкий landscape.
    tester.view.physicalSize = const Size(500, 900);
    // Исходная строка 77: Даём запланированным кадрам/анимациям завершиться. Это не реальная пауза пользователя.
    await tester.pumpAndSettle();
    // Исходная строка 78: Утверждение о состоянии/дереве/числе запросов. Тест не доказывает работу физического датчика или KeyStore.
    expect(state.openedImage, 'sample');
    // Исходная строка 79: Утверждение о состоянии/дереве/числе запросов. Тест не доказывает работу физического датчика или KeyStore.
    expect(requests, withImage);
    // Исходная строка 80: Имитируем системный возврат, а не только нажатие нашей кнопки.
    await tester.binding.handlePopRoute();
    // Исходная строка 81: Даём запланированным кадрам/анимациям завершиться. Это не реальная пауза пользователя.
    await tester.pumpAndSettle();
    // Исходная строка 82: Утверждение о состоянии/дереве/числе запросов. Тест не доказывает работу физического датчика или KeyStore.
    expect(state.selected?.id, '1@channel');
    // Исходная строка 83: Утверждение о состоянии/дереве/числе запросов. Тест не доказывает работу физического датчика или KeyStore.
    expect(find.text('draft'), findsOneWidget);
  });
}
```

## tool/mock_server.py

```python
"""Local HTTP fixture. No external packages. Never use as production backend."""
import argparse, json, time, threading
# Исходная строка 3: HTTP-запросы обрабатываются потоками. Это тестовая инфраструктура на Python, не модель конкурентности Dart.
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from pathlib import Path

# Исходная строка 7: threading.Lock защищает общий список сообщений между потоками HTTP-сервера.
lock = threading.Lock()
# Исходная строка 8: Начальная история только в RAM; после перезапуска сервера начинается заново.
messages = {str(c): [dict(id=str(c*1000+i), **{"from":"demo", "data": {"Image":{"link":"sample"}} if i==43 else {"Text":{"text":f"Message {i}"}}}) for i in range(1, 46)] for c in (1,2)}
# Исходная строка 9: Один локальный PNG для проверки миниатюр, полноэкранного просмотра и жестов.
IMAGE = Path(__file__).with_name('sample.png').read_bytes()
class Handler(BaseHTTPRequestHandler):
    # Исходная строка 11: Не журналируем запросы и секреты.
    def log_message(self, *args): pass  # Never log passwords or bearer tokens.
    # Исходная строка 12: Сериализация ответа, код статуса и заголовки. Бинарные байты изображения не преобразуем в JSON.
    def respond(self, code, payload, image=False):
        data = payload if image else json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", ("image/png" if data.startswith(b"\x89PNG") else "text/plain") if image else "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        try: self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError): pass
    # Исходная строка 20: Стандартная точка входа BaseHTTPRequestHandler для GET.
    def do_GET(self): self.handle_api()
    # Исходная строка 21: Точка входа для POST; дальше общий роутинг handle_api.
    def do_POST(self): self.handle_api()
    def handle_api(self):
        global messages
        path = urlparse(self.path)
        parts = path.path.strip("/").split("/")
        # Исходная строка 26: Задержка только тестового сервера для воспроизведения гонок; UI-поток приложения она не блокирует.
        time.sleep(args.delay)
        # Исходная строка 27: Файл управления читается при каждом запросе, поэтому флаги можно менять без перезапуска.
        config = json.loads(Path(args.control).read_text()) if Path(args.control).exists() else {}
        # Исходная строка 28: Искусственный 503 для проверки ошибки сервиса.
        if config.get("server_error"): return self.respond(503,{})
        if path.path == "/login" and self.command == "POST":
            # Исходная строка 30: Читаем тело по Content-Length и превращаем JSON в Python dict.
            # Исходная строка 30: Читаем официальный X-Auth-Token; формат совпадает с клиентом приложения.
            body = json.loads(self.rfile.read(int(self.headers.get("Content-Length",0))))
            user = body.get("name")
            # Исходная строка 32: Тестовые alice/bob, а не реальная регистрация и не production-аутентификация.
            if user not in ("alice","bob") or body.get("pwd") != "password": return self.respond(401,{})
            return self.respond(200,("demo-"+user).encode(),True)
        # Исходная строка 34: Читаем официальный X-Auth-Token; формат совпадает с клиентом приложения.
        token = self.headers.get("X-Auth-Token","")
        # Исходная строка 35: Флаг управляемого 401 для проверки завершения сессии.
        if config.get("unauthorized") or token not in ("demo-alice","demo-bob"): return self.respond(401,{})
        if path.path == "/logout": return self.respond(200,{})
        if path.path == "/channels": return self.respond(200,[] if config.get("empty") else ["1@channel","2@channel"])
        if len(parts)==2 and parts[0] in ("img","thumb"): return self.respond(200,IMAGE,True)
        if path.path=="/messages" or (len(parts)==2 and parts[0]=="channel"):
            channel = parts[1].replace("%40","@").split("@")[0] if len(parts)==2 else None
            if self.command=="POST":
                # Исходная строка 42: Читаем официальный X-Auth-Token; формат совпадает с клиентом приложения.
                body=json.loads(self.rfile.read(int(self.headers.get("Content-Length",0))))
                channel=body["to"].split("@")[0]
                # Исходная строка 44: Критическая секция: вычисление ID и изменение общего списка не должны перемежаться с другим запросом.
                with lock:
                    m={"id":str(int(messages[channel][-1]["id"])+1),"from":body["from"],"data":body["data"]}
                    messages[channel].append(m)
                # Исходная строка 47: Важная проверка: запись уже добавлена сервером, а соединение закрывается ДО отправки ответа.
                if config.get("lose_response"):
                    self.connection.shutdown(2); self.connection.close(); return
                return self.respond(200,m["id"].encode(),True)
            q=parse_qs(path.query)
            # Исходная строка 51: Курсор, совместимый с официальным API; reverse=true клиента означает отбор меньших ID.
            before=int(q.get("lastKnownId",["281474976710655"])[0])
            if config.get("page_error") and before<281474976710655: return self.respond(503,{})
            # Исходная строка 53: Сервер тоже ограничивает выдачу максимум 20 элементами.
            limit=min(20,max(1,int(q.get("limit",[20])[0])))
            # Исходная строка 54: Критическая секция: вычисление ID и изменение общего списка не должны перемежаться с другим запросом.
            # Исходная строка 54: Берём записи старше курсора и сортируем от новых к старым.
            with lock: eligible=sorted([m for m in messages[channel] if int(m["id"])<before],key=lambda m:int(m["id"]),reverse=True)
            page=eligible[:limit]
            return self.respond(200,page)
        self.respond(404,{})
# Исходная строка 58: Запускаем сервер только при выполнении файла как программы, а не при импорте.
if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--port",type=int,default=8080)
    parser.add_argument("--delay",type=float,default=0.5)
    parser.add_argument("--control",default="tool/mock_control.json")
    args=parser.parse_args()
    print(f"Mock listening on 0.0.0.0:{args.port}; alice/bob, password")
    # Исходная строка 65: HTTP-запросы обрабатываются потоками. Это тестовая инфраструктура на Python, не модель конкурентности Dart.
    ThreadingHTTPServer(("0.0.0.0",args.port),Handler).serve_forever()
```

## ARB и генерируемая локализация

`app_ru.arb` редактируем вручную. Ключ `send` превращается в getter `send` класса AppLocalizations. Строки `@@locale` — метаданные, а не надпись экрана.

```json
{
  "@@locale": "ru",
  "appTitle": "Каналы",
  "login": "Логин",
  "password": "Пароль",
  "signIn": "Войти",
  "logout": "Выйти",
  "channels": "Каналы",
  "choose": "Выберите чат",
  "empty": "Здесь пока пусто",
  "retry": "Повторить",
  "refresh": "Обновить",
  "older": "Предыдущие сообщения",
  "message": "Сообщение",
  "send": "Отправить",
  "sending": "Отправляется",
  "sent": "Отправлено",
  "uncertain": "Не удалось подтвердить отправку",
  "restore": "Вернуть в редактор",
  "warning": "Сообщение могло быть доставлено. Повтор может создать дубликат. Повторить?",
  "yes": "Повторить отправку",
  "cancel": "Отмена",
  "back": "Назад",
  "close": "Закрыть",
  "image": "Изображение",
  "invalid": "Неверный логин или пароль",
  "connection": "Не удалось связаться с сервером",
  "server": "Сервис временно недоступен",
  "storage": "Не удалось сохранить данные на устройстве",
  "expired": "Сессия завершена. Войдите снова.",
  "protocol": "Неверный формат ответа сервера",
  "restoreWarning": "Текст будет добавлен к текущему черновику.",
  "loading": "Загрузка"
}
```

В сгенерированном `app_localizations.dart` объявлены getters и делегат. `AppLocalizations.of(context)` получает экземпляр переводов из Localizations. В `app_localizations_ru.dart` реализации getters возвращают русские строки. Это повторяющийся результат генератора: менять нужно ARB, иначе ручная правка исчезнет после gen-l10n.

## pubspec.yaml: что устанавливает проект

```yaml
name: channel_lab
description: Laboratory 3 - channel messaging.
publish_to: none
version: 1.0.0+1
environment:
  sdk: '>=3.10.0 <4.0.0'
dependencies:
  flutter:
    sdk: flutter
  flutter_localizations:
    sdk: flutter
  intl: any
  http: ^1.6.0
  flutter_secure_storage: ^9.2.4
dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^6.0.0
flutter:
  uses-material-design: true
  generate: true
```

- `environment.sdk`: совместимый Dart, а не версия Android.
- `dependencies`: runtime-зависимости приложения. `sdk: flutter` означает пакет из установленного SDK.
- `http`: сеть, `flutter_secure_storage`: платформенный секретный диск, `flutter_localizations`/`intl`: переводы.
- `dev_dependencies`: тесты и анализатор, не функции экрана.
- `flutter.generate: true`: включение кодогенерации локализации.
- `pubspec.lock`: конкретные разрешённые версии; yaml описывает ограничения, lock фиксирует решение.

## l10n.yaml

```yaml
arb-dir: lib/l10n
template-arb-file: app_ru.arb
output-localization-file: app_localizations.dart
```

`arb-dir` — каталог переводов; `template-arb-file` — основной набор ключей; `output-localization-file` — имя сгенерированного интерфейса.

## mock_control.json

```json
{"unauthorized":false,"server_error":false,"empty":false,"page_error":false,"lose_response":false}
```

Все флаги false — обычная работа. `unauthorized` даёт 401, `server_error` даёт 503, `empty` — пустые каналы, `page_error` ломает старые страницы, `lose_response` принимает отправку и обрывает ответ. Последний случай показывает, почему таймаут не равен недоставке.

## android/app/src/main/kotlin/ru/itmo/student/channel_lab/MainActivity.kt

package задаёт namespace Kotlin. import подключает FlutterActivity. MainActivity наследует её без переопределений: стандартная Android-точка входа размещает Flutter UI. Наши экраны написаны в Dart, а не в этой Activity.

```kotlin
package ru.itmo.student.channel_lab

import io.flutter.embedding.android.FlutterActivity

class MainActivity : FlutterActivity()
```

## ios/Runner/AppDelegate.swift

Платформенная точка входа iOS, созданная Flutter. Наследование FlutterAppDelegate подключает инфраструктуру приложения; регистрация плагинов обеспечивает вызовы платформенных возможностей из Dart. Пользовательские чаты и сеть здесь не реализованы.

```swift
import Flutter
import UIKit

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)
  }
}
```

## ios/Runner/SceneDelegate.swift

Стандартная интеграция жизненного цикла окна iOS через FlutterSceneDelegate; бизнес-состояние остаётся в Dart.

```swift
import Flutter
import UIKit

class SceneDelegate: FlutterSceneDelegate {

}
```

## android/app/build.gradle.kts

plugins подключает Android/Kotlin/Flutter сборку. namespace/applicationId определяют идентичность Android-приложения, а не адрес API. compileSdk — API компиляции, minSdk — минимальная поддерживаемая Android, targetSdk — целевой уровень поведения. versionCode/versionName приходят из Flutter-конфигурации. Debug APK не является production-релизом.

```kotlin
plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "ru.itmo.student.channel_lab"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "ru.itmo.student.channel_lab"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
```

## android/app/src/main/AndroidManifest.xml

INTERNET разрешает сеть. application/activity описывают платформенный контейнер. configChanges задаёт события, которые Activity обрабатывает без обычного пересоздания: Flutter получает обновлённые размеры и перестраивает UI. Это не сохранение данных после убийства процесса. Flutter embedding metadata подключает инфраструктуру движка.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET"/>
    <application
        android:label="channel_lab"
        android:name="${applicationName}"
        android:icon="@mipmap/ic_launcher">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:taskAffinity=""
            android:theme="@style/LaunchTheme"
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
            android:hardwareAccelerated="true"
            android:windowSoftInputMode="adjustResize">
            <!-- Specifies an Android theme to apply to this Activity as soon as
                 the Android process has started. This theme is visible to the user
                 while the Flutter UI initializes. After that, this theme continues
                 to determine the Window background behind the Flutter UI. -->
            <meta-data
              android:name="io.flutter.embedding.android.NormalTheme"
              android:resource="@style/NormalTheme"
              />
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
        <!-- Don't delete the meta-data below.
             This is used by the Flutter tool to generate GeneratedPluginRegistrant.java -->
        <meta-data
            android:name="flutterEmbedding"
            android:value="2" />
    </application>
    <!-- Required to query activities that can process text, see:
         https://developer.android.com/training/package-visibility and
         https://developer.android.com/reference/android/content/Intent#ACTION_PROCESS_TEXT.

         In particular, this is used by the Flutter engine in io.flutter.plugin.text.ProcessTextPlugin. -->
    <queries>
        <intent>
            <action android:name="android.intent.action.PROCESS_TEXT"/>
            <data android:mimeType="text/plain"/>
        </intent>
    </queries>
</manifest>
```

## android/app/src/debug/AndroidManifest.xml

Отладочная конфигурация разрешает cleartext HTTP для localhost/mock. Релизному серверу нужен HTTPS; исключение debug не означает глобального отключения сертификатов.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- The INTERNET permission is required for development. Specifically,
         the Flutter tool needs it to communicate with the running application
         to allow setting breakpoints, to provide hot reload, etc.
    -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <application android:usesCleartextTraffic="true"/>
</manifest>
```

## Остальная инфраструктура

`settings.gradle.kts` подключает Gradle-плагины и Flutter loader; корневой `build.gradle.kts` организует общую сборку; wrapper/gradlew закрепляет версию Gradle. Они не содержат логики чата. `analysis_options.yaml` подключает правила flutter_lints. `Info.plist` описывает свойства и ориентации iOS, а Xcode project/xcconfig — настройки платформенной сборки. `GeneratedPluginRegistrant` генерируется для подключения плагинов: вручную туда бизнес-логику не пишут. `build/`, `.dart_tool/`, `.gradle/` — кэши/результаты инструментов, не исходники, которые нужно учить на защите.
