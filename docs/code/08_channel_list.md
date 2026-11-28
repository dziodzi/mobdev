# lib/ui/channel_list.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
import 'package:flutter/material.dart';

import '../state/chat_state.dart';
import 'strings.dart';

// Исходная строка 6: Собственного состояния нет; получает готовые данные и вызывает методы контроллера.
class ChannelList extends StatelessWidget {
  final ChatState state;
  const ChannelList({super.key, required this.state});
  @override
  Widget build(BuildContext context) {
    final s = strings(context);
    return Column(
      children: [
        // Исходная строка 14: Прогресс не скрывает старый список.
        if (state.loadingChannels) const LinearProgressIndicator(),
        if (state.channelsError != null)
          Text(failureText(context, state.channelsError!)),
        if (state.channelsError != null)
          // Исходная строка 18: Передаём ссылку на loadChannels без вызова во время build.
          TextButton(onPressed: state.loadChannels, child: Text(s.retry)),
        // Исходная строка 19: ListView получает конечную оставшуюся высоту Column; иначе возможна ошибка неограниченного viewport.
        Expanded(
          // Исходная строка 20: Жест обновления ждёт Future метода onRefresh и затем убирает индикатор.
          child: RefreshIndicator(
            onRefresh: state.loadChannels,
            child: ListView(
              // Исходная строка 23: Жест доступен даже у короткого/пустого списка.
              physics: const AlwaysScrollableScrollPhysics(),
              children: [
                // Исходная строка 25: Пустой результат показываем после загрузки, не выдаём ожидание за отсутствие каналов.
                if (state.channels.isEmpty && !state.loadingChannels)
                  ListTile(title: Text(s.empty)),
                // Исходная строка 27: Collection for создаёт строки UI, не делает запросы.
                for (final c in state.channels)
                  ListTile(
                    title: Text(c.name),
                    // Исходная строка 30: ?. безопасно читает selected; сравнение ID определяет подсветку.
                    selected: state.selected?.id == c.id,
                    // Исходная строка 31: Closure хранит конкретный канал этой строки до нажатия.
                    onTap: () => state.select(c),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
```
