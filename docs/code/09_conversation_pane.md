# lib/ui/conversation_pane.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
import 'package:flutter/material.dart';

import '../models/models.dart';
import '../state/chat_state.dart';
import 'message_tile.dart';
import 'strings.dart';

// Исходная строка 8: Локально нужны контроллеры текста и прокрутки; данные восстанавливаются из ChatState.
class ConversationPane extends StatefulWidget {
  final ChatState state;
  final String id;
  const ConversationPane({super.key, required this.state, required this.id});
  @override
  State<ConversationPane> createState() => _ConversationPaneState();
}

class _ConversationPaneState extends State<ConversationPane> {
  // Исходная строка 17: late — присвоим позже в initState, final — единожды. Чтение до присваивания вызовет ошибку.
  late final TextEditingController editor;
  late final ScrollController scroll;
  @override
  // Исходная строка 20: При создании State восстанавливаем editor и scroll из черновика/позиции.
  void initState() {
    super.initState();
    // Исходная строка 22: Нет сохранённого текста → пустая строка через ??.
    editor = TextEditingController(text: widget.state.drafts[widget.id] ?? '');
    scroll = ScrollController(
      // Исходная строка 24: Список начинает с сохранённого смещения текущего канала.
      initialScrollOffset: widget.state.conversation(widget.id).offset,
    );
    // Исходная строка 26: При прокрутке сохраняем позицию в Conversation, не перестраивая весь UI на каждый пиксель.
    scroll.addListener(
      () => widget.state.conversation(widget.id).offset = scroll.offset,
    );
  }

  @override
  // Исходная строка 32: Освобождаем контроллеры панели, но не удаляем Conversation из ChatState.
  void dispose() {
    editor.dispose();
    scroll.dispose();
    super.dispose();
  }

  // Исходная строка 38: UI отвечает за подтверждение ручного повтора; HTTP остаётся в контроллере/API.
  Future<void> retry(Outgoing m) async {
    final s = strings(context);
    // Исходная строка 40: Возвращает Future<bool?>: true/false от кнопки либо null при dismiss.
    final yes = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        content: Text(s.warning),
        actions: [
          TextButton(
            // Исходная строка 46: Закрываем диалог и возвращаем false ожидающему showDialog.
            onPressed: () => Navigator.pop(context, false),
            child: Text(s.cancel),
          ),
          FilledButton(
            // Исходная строка 50: Закрываем диалог с явным подтверждением.
            onPressed: () => Navigator.pop(context, true),
            child: Text(s.yes),
          ),
        ],
      ),
    );
    // Исходная строка 56: false/null ничего не отправляют; retryId связывает повтор с прежней локальной попыткой.
    if (yes == true) await widget.state.send(widget.id, retryId: m.localId);
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state,
        c = state.conversation(widget.id),
        s = strings(context);
    // Исходная строка 64: Черновик мог измениться снаружи TextField после send/restore.
    final draft = state.drafts[widget.id] ?? '';
    // Исходная строка 65: Обновляем контроллер только при различии, чтобы не сбрасывать курсор при каждом build.
    if (editor.text != draft) {
      // Исходная строка 66: Одновременно синхронизируем текст и selection. Это локальный побочный эффект текущей реализации, не сетевой запрос.
      editor.value = TextEditingValue(
        text: draft,
        // Исходная строка 68: Курсор в конце восстановленного текста без выделения.
        selection: TextSelection.collapsed(offset: draft.length),
      );
    }
    // Исходная строка 71: Выделяем исходящие, ещё не представленные подтверждённой записью истории.
    final pending = c.outgoing
        // Исходная строка 72: Фильтр исключает sent, чтобы не рисовать одно подтверждённое сообщение дважды.
        .where((m) => m.status != Delivery.sent)
        .toList()
        // Исходная строка 74: Новые локальные попытки должны оказаться у нижнего края reverse-списка.
        .reversed
        .toList();
    return Column(
      children: [
        if (c.busy) const LinearProgressIndicator(),
        // Исходная строка 79: Ошибка рисуется рядом с историей, прежние сообщения остаются.
        if (c.error != null)
          Wrap(
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              Text(failureText(context, c.error!)),
              TextButton(
                onPressed: c.busy ? null : () => state.loadMessages(widget.id),
                child: Text(s.retry),
              ),
            ],
          ),
        if (state.storageError != null) Text(s.storage),
        // Исходная строка 91: Список занимает оставшуюся высоту; редактор расположен отдельной строкой ниже.
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => state.loadMessages(widget.id),
            // Исходная строка 94: Создаём элементы лениво по индексу по мере необходимости, не всю историю заранее.
            child: ListView.builder(
              controller: scroll,
              // Исходная строка 96: Вертикальное направление снизу вверх: index=0 у нижнего края, offset=0 соответствует новым сообщениям.
              reverse: true,
              physics: const AlwaysScrollableScrollPhysics(),
              // Исходная строка 98: pending + серверная история + одна служебная строка для старой страницы/пустоты.
              itemCount: pending.length + c.messages.length + 1,
              itemBuilder: (context, index) {
                // Исходная строка 100: Первая зона индексов — исходящие локальные записи.
                if (index < pending.length) {
                  final m = pending[index];
                  return ListTile(
                    // Исходная строка 103: Стабильная идентичность строки при отправке/ошибке/повторе.
                    key: ValueKey(m.localId),
                    title: Text(m.text),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          // Исходная строка 109: Пользователь различает ожидание ответа и неопределённую доставку.
                          m.status == Delivery.sending
                              ? s.sending
                              : s.uncertain,
                        ),
                        // Исходная строка 113: Только после неудачной попытки доступны повтор и возврат текста. Builder ничего сам не отправляет.
                        if (m.status == Delivery.uncertain)
                          Wrap(
                            children: [
                              TextButton(
                                // Исходная строка 117: Блокируем кнопку, но не редактор: новый текст можно печатать, пока старый отправляется.
                                onPressed: c.sending ? null : () => retry(m),
                                child: Text(s.retry),
                              ),
                              TextButton(
                                onPressed: () => state.restore(widget.id, m),
                                child: Text(s.restore),
                              ),
                            ],
                          ),
                      ],
                    ),
                  );
                }
                // Исходная строка 130: Пересчитываем общий индекс в индекс серверной истории.
                final i = index - pending.length;
                // Исходная строка 131: Последний элемент — служебный: это не выход за границы массива, а кнопка/пустое состояние.
                if (i == c.messages.length) {
                  return Column(
                    children: [
                      if (c.messages.isEmpty && !c.busy) Text(s.empty),
                      if (c.next != null)
                        TextButton(
                          onPressed: c.busy
                              ? null
                              : () =>
                                    // Исходная строка 140: older:true означает пагинацию в прошлое. Без этого параметра был бы refresh верхней страницы.
                                    state.loadMessages(widget.id, older: true),
                          child: Text(s.older),
                        ),
                    ],
                  );
                }
                // Исходная строка 146: Здесь индекс относится к настоящей серверной записи.
                final m = c.messages[i];
                return MessageTile(
                  key: ValueKey(m.id),
                  message: m,
                  state: state,
                  // Исходная строка 151: По serverId узнаём подтверждённую нашу отправку и показываем статус на серверной строке.
                  sent: c.outgoing.any((o) => o.serverId == m.id),
                );
              },
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(8),
          child: Row(
            // Исходная строка 160: Кнопка выравнивается по низу выросшего многострочного поля.
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              // Исходная строка 162: Список занимает оставшуюся высоту; редактор расположен отдельной строкой ниже.
              Expanded(
                child: TextField(
                  controller: editor,
                  // Исходная строка 165: Каждый пользовательский ввод идёт в state.draft, обновляя память, ревизию и диск.
                  onChanged: (text) => state.draft(widget.id, text),
                  // Исходная строка 166: Поле растёт от 1 до 4 строк; дальше содержимое прокручивается внутри.
                  minLines: 1,
                  maxLines: 4,
                  decoration: InputDecoration(labelText: s.message),
                ),
              ),
              IconButton(
                tooltip: s.send,
                // Исходная строка 173: Блокируем кнопку, но не редактор: новый текст можно печатать, пока старый отправляется.
                onPressed: c.sending ? null : () => state.send(widget.id),
                icon: const Icon(Icons.send),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
```
