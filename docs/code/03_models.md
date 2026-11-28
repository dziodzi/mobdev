# lib/models/models.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
// Исходная строка 1: Данные сессии: пользователь и токен, без пароля.
class Session {
  // Исходная строка 2: Обязательное ненулевое поле; final запрещает его переназначать после создания объекта.
  final String userId;
  final String token;
  // Исходная строка 4: Позиционные аргументы через this.field сразу заполняют поля.
  const Session(this.userId, this.token);
  // Исходная строка 5: Именованный factory-конструктор превращает Map JSON в модель; as String проверяет тип во время выполнения.
  factory Session.fromJson(Map<String, dynamic> j) =>
      Session(j['userId'] as String, j['token'] as String);
  // Исходная строка 7: Обратное преобразование для сохранения: Session → Map → jsonEncode.
  Map<String, dynamic> toJson() => {'userId': userId, 'token': token};
}

// Исходная строка 10: Внутренняя модель канала. Сервер возвращает имя; ApiClient использует его и как id, и как name.
class Channel {
  final String id;
  final String name;
  const Channel(this.id, this.name);
  // Исходная строка 14: Конструктор объектного JSON. В текущем официальном channels() не используется: там массив строк и Channel(e,e).
  factory Channel.fromJson(Map<String, dynamic> j) =>
      Channel(j['id'] as String, j['name'] as String);
}

// Исходная строка 18: Подтверждённая серверная запись, отдельно от ещё не подтверждённого Outgoing.
class Message {
  final String id;
  final String author;
  final String text;
  // Исходная строка 22: ? означает nullable: картинки может не быть.
  final String? imageId;
  const Message(this.id, this.author, this.text, this.imageId);
  // Исходная строка 24: Из официального формата from/data.Text/data.Image создаём удобные поля author/text/imageId.
  factory Message.fromJson(Map<String, dynamic> j) {
    // Исходная строка 25: Достаём вложенный объект data и проверяем, что это Map нужного вида.
    final data = j['data'] as Map<String, dynamic>;
    return Message(
      // Исходная строка 27: Приводим ID к строковому представлению. Для сортировки позднее преобразуем числовой ID в int.
      j['id'].toString(),
      j['from'] as String,
      // Исходная строка 29: as Map? разрешает null; ?[] не обращается к отсутствующей Map; as String? проверяет текст; ?? заменяет null пустой строкой.
      (data['Text'] as Map?)?['text'] as String? ?? '',
      // Исходная строка 30: Та же безопасная распаковка, но без ??, поэтому при отсутствии картинки сохраняется null.
      (data['Image'] as Map?)?['link'] as String?,
    );
  }
}

// Исходная строка 35: Список страницы и вычисленный клиентом курсор next. В серверном JSON поля next нет.
class MessagePage {
  final List<Message> messages;
  final String? next;
  const MessagePage(this.messages, this.next);
}

// Исходная строка 41: Конечный набор статусов без ошибок в строковых названиях.
enum Delivery { sending, sent, uncertain }

// Исходная строка 43: Локальная попытка: localId есть до ответа, serverId появляется после подтверждения.
class Outgoing {
  final String localId;
  final String text;
  final Delivery status;
  final String? serverId;
  // Исходная строка 48: [this.serverId] — необязательный позиционный аргумент с null по умолчанию. Для смены статуса создаём новый объект с final-полями.
  const Outgoing(this.localId, this.text, this.status, [this.serverId]);
}
```
