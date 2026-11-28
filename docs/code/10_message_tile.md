# lib/ui/message_tile.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
import 'package:flutter/material.dart';

import '../models/models.dart';
import '../state/chat_state.dart';
import 'remote_image.dart';
import 'strings.dart';

class MessageTile extends StatelessWidget {
  // Исходная строка 9: Модель уже разобрана сетевым слоем, UI не знает JSON.
  final Message message;
  final ChatState state;
  final bool sent;
  const MessageTile({
    super.key,
    required this.message,
    required this.state,
    // Исходная строка 16: Именованный необязательный аргумент со значением по умолчанию.
    this.sent = false,
  });
  @override
  // Исходная строка 19: Строка истории: автор и содержимое, без загрузки данных внутри build.
  Widget build(BuildContext context) => ListTile(
    title: Text(message.author),
    subtitle: Column(
      // Исходная строка 22: Выравниваем содержимое столбца по началу горизонтальной оси.
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Исходная строка 24: Пустой текст не создаёт лишний Text у сообщения-картинки.
        if (message.text.isNotEmpty) Text(message.text),
        if (message.imageId != null)
          // Исходная строка 26: Подпись и роль кнопки для средств доступности.
          Semantics(
            button: true,
            label: strings(context).image,
            // Исходная строка 29: Нажатие с Material-реакцией меняет openedImage через контроллер.
            child: InkWell(
              onTap: () => state.openImage(message.imageId!),
              child: RemoteImage(
                state: state,
                id: message.imageId!,
                // Исходная строка 34: Миниатюра использует /thumb; полный просмотр использует /img.
                thumb: true,
              ),
            ),
          ),
        // Исходная строка 38: Подтверждение показываем на самой записи, а не отдельным дублем.
        if (sent) Text(strings(context).sent),
      ],
    ),
  );
}
```
