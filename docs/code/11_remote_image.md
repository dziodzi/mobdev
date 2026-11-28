# lib/ui/remote_image.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../state/chat_state.dart';
import 'strings.dart';

class RemoteImage extends StatefulWidget {
  final ChatState state;
  final String id;
  // Исходная строка 11: Конфигурация выбирает миниатюру либо оригинал.
  final bool thumb;
  const RemoteImage({
    super.key,
    required this.state,
    required this.id,
    this.thumb = false,
  });
  @override
  State<RemoteImage> createState() => _RemoteImageState();
}

class _RemoteImageState extends State<RemoteImage> {
  // Исходная строка 23: Future хранится между build, а Uint8List содержит байты, не путь файла.
  late Future<Uint8List> future;
  @override
  // Исходная строка 25: Первая Future получается при создании State, вне build.
  void initState() {
    super.initState();
    load();
  }

  // Исходная строка 30: Берём Future из кэша контроллера. Это может быть уже идущая или завершённая операция.
  void load() {
    future = widget.state.image(widget.id, thumb: widget.thumb);
  }

  @override
  // Исходная строка 35: Родитель передал новую конфигурацию существующему совместимому State. covariant уточняет тип старого widget.
  void didUpdateWidget(covariant RemoteImage old) {
    super.didUpdateWidget(old);
    // Исходная строка 37: Только другой ID/режим thumb требует другой Future. Обычная перерисовка — нет.
    if (old.id != widget.id || old.thumb != widget.thumb) load();
  }

  @override
  // Исходная строка 41: Подписывается на Future и строит UI по AsyncSnapshot. Сам HTTP-запрос не создаёт.
  Widget build(BuildContext context) => FutureBuilder<Uint8List>(
    // Исходная строка 42: Передаём сохранённую Future, не вызываем load в build.
    future: future,
    builder: (context, snapshot) {
      // Исходная строка 44: Ошибка проверяется до ожидания data, иначе ошибка без данных выглядела бы бесконечной загрузкой.
      if (snapshot.hasError) {
        return TextButton(
          // Исходная строка 46: setState синхронно вызывает load и планирует rebuild. load присваивает Future; ожидание сети не выполняется внутри callback setState.
          onPressed: () => setState(load),
          child: Text(strings(context).retry),
        );
      }
      // Исходная строка 50: Пока нет байтов, показываем индикатор.
      if (!snapshot.hasData) {
        return const Center(child: CircularProgressIndicator());
      }
      // Исходная строка 53: Декодируем байты. Это не дополнительный HTTP GET.
      final image = Image.memory(
        // Исходная строка 54: ! утверждает не-null после hasData. При ложном предположении ! бросил бы ошибку, а не вылечил null.
        snapshot.data!,
        // Исходная строка 55: Вписываем картинку целиком без искажения пропорций.
        fit: BoxFit.contain,
        semanticLabel: strings(context).image,
        // Исходная строка 57: Ошибка декодирования самих байтов изображения — отдельный этап после успешного HTTP.
        errorBuilder: (_, _, _) => Text(strings(context).protocol),
      );
      return widget.thumb
          // Исходная строка 60: Миниатюре ограничиваем высоту, оригиналу даём InteractiveViewer.
          ? SizedBox(height: 140, child: image)
          : InteractiveViewer(
              // Исходная строка 62: Жестом меняем масштаб от 0.5 до 5, не скачивая новые версии изображения.
              minScale: 0.5,
              maxScale: 5,
              child: Center(child: image),
            );
    },
  );
}
```
