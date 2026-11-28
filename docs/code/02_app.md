# lib/app.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
import 'package:flutter/material.dart';

import 'l10n/app_localizations.dart';
import 'state/chat_state.dart';
import 'ui/login_screen.dart';
import 'ui/channel_list.dart';
import 'ui/conversation_pane.dart';
import 'ui/remote_image.dart';
import 'ui/strings.dart';

// Исходная строка 11: Widget — неизменяемая конфигурация. Изменяемая часть жизненного цикла находится в отдельном объекте State.
class ChatApp extends StatefulWidget {
  // Исходная строка 12: final запрещает заменить ссылку, но объект ChatState по этой ссылке может менять свои поля.
  final ChatState state;
  // Исходная строка 13: required требует аргумент state. super.key передаёт ключ базовому Widget. const-конструктор разрешает константное создание, но наш ChatState создаётся во время запуска.
  const ChatApp({super.key, required this.state});
  @override
  // Исходная строка 15: Framework создаёт отдельный объект _ChatAppState. Подчёркивание означает приватность внутри Dart-библиотеки.
  State<ChatApp> createState() => _ChatAppState();
}

class _ChatAppState extends State<ChatApp> {
  @override
  // Исходная строка 20: Вызывается один раз на объект State. Метод синхронный, его нельзя делать async.
  void initState() {
    super.initState();
    // Исходная строка 22: widget — текущая конфигурация ChatApp. Запускаем Future восстановления без await в initState; start сам управляет booting и обработкой ошибок.
    widget.state.start();
  }

  @override
  // Исходная строка 26: Конец жизни State. Владелец контроллера освобождает его ресурсы, в том числе HTTP-клиент.
  void dispose() {
    widget.state.dispose();
    // Исходная строка 28: Завершаем очистку базового класса. После dispose нельзя вызывать setState этого State.
    super.dispose();
  }

  @override
  // Исходная строка 32: Возвращаем новое описание поддерева. build может вызываться многократно; здесь не запускаем HTTP.
  Widget build(BuildContext context) => MaterialApp(
    // Исходная строка 33: Подключаем загрузчики переводов приложения и стандартных компонентов Flutter.
    localizationsDelegates: AppLocalizations.localizationsDelegates,
    // Исходная строка 34: Языки, которые поддерживают ARB-ресурсы.
    supportedLocales: AppLocalizations.supportedLocales,
    // Исходная строка 35: Выбираем русский язык явно.
    locale: const Locale('ru'),
    // Исходная строка 36: Этот callback получает context под Localizations и читает перевод заголовка.
    onGenerateTitle: (context) => strings(context).appTitle,
    // Исходная строка 37: Общая тема доступна потомкам через Theme.of(context).
    theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
    // Исходная строка 38: Подписка на ChangeNotifier. После notifyListeners builder будет вызван для обновления этого поддерева.
    home: ListenableBuilder(
      listenable: widget.state,
      // Исходная строка 40: Передаём функцию построения; _ — неиспользуемый child. _Shell создаётся при вызове builder.
      builder: (context, _) => _Shell(state: widget.state),
    ),
  );
}

// Исходная строка 45: Собственного State нет, но экран может перестраиваться от родителя и показывать новые данные. Stateless не означает статичный экран.
class _Shell extends StatelessWidget {
  // Исходная строка 46: final запрещает заменить ссылку, но объект ChatState по этой ссылке может менять свои поля.
  final ChatState state;
  const _Shell({required this.state});
  @override
  // Исходная строка 49: Возвращаем новое описание поддерева. build может вызываться многократно; здесь не запускаем HTTP.
  Widget build(BuildContext context) {
    // Исходная строка 50: Получаем локализованные надписи по текущему BuildContext.
    final s = strings(context);
    // Исходная строка 51: Пока хранилище отвечает, показываем индикатор. return завершает эту ветку build.
    if (state.booting) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    // Исходная строка 54: Нет сессии → в дереве экран входа. Навигация зависит от состояния, а не от цепочки ручных push логина.
    if (state.session == null) return LoginScreen(state: state);
    final landscape =
        // Исходная строка 56: Регистрируем зависимость от ориентации доступной области. При изменении этой зависимости Flutter обновит виджет.
        MediaQuery.orientationOf(context) == Orientation.landscape;
    // Исходная строка 57: Локальная функция возвращает правую панель или приглашение выбрать чат. Создание Widget само по себе не делает запрос.
    Widget pane() => state.selected == null
        ? Center(child: Text(s.choose))
        : ConversationPane(
            // Исходная строка 60: Ключ по ID отличает редакторы разных каналов. Он не хранит черновик на диске и не переносит State автоматически между любыми родителями.
            key: ValueKey(state.selected!.id),
            state: state,
            id: state.selected!.id,
          );
    // Исходная строка 64: Перехватываем системный Back корневого маршрута, потому что внутренние экраны находятся во вложенном Navigator.
    return PopScope(
      // Исходная строка 65: Корневой маршрут может закрываться стандартно только без выбранного чата и картинки.
      canPop: state.selected == null && state.openedImage == null,
      // Исходная строка 66: didPop сообщает, удалён ли маршрут. Если нет, выполняем нашу операцию back.
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop) state.back();
      },
      // Исходная строка 69: Вложенный Navigator управляет внутренним стеком страниц.
      child: Navigator(
        // Исходная строка 70: Декларативное описание стека: нижняя страница списка всегда есть; чат и картинка добавляются условиями.
        pages: [
          MaterialPage(
            // Исходная строка 72: Постоянный ключ сохраняет идентичность базовой страницы между обновлениями.
            key: const ValueKey('channels'),
            // Исходная строка 73: Правило возврата базовой страницы внутреннего Navigator, в том числе выбранного чата в ландшафтной панели.
            child: PopScope(
              canPop: state.selected == null,
              // Исходная строка 75: didPop сообщает, удалён ли маршрут. Если нет, выполняем нашу операцию back.
              onPopInvokedWithResult: (didPop, result) {
                if (!didPop) state.back();
              },
              // Исходная строка 78: Каркас Material-экрана: appBar, body и работа с доступной областью при клавиатуре.
              child: Scaffold(
                appBar: AppBar(
                  title: Text(s.channels),
                  // Исходная строка 81: В ландшафте чат справа, поэтому кнопку Back рисует общий экран.
                  leading: landscape && state.selected != null
                      ? IconButton(
                          tooltip: s.back,
                          onPressed: state.back,
                          icon: const Icon(Icons.arrow_back),
                        )
                      : null,
                  actions: [
                    IconButton(
                      tooltip: s.refresh,
                      // Исходная строка 91: Передаём ссылку на метод без скобок. Он выполнится по нажатию; со скобками вызов произошёл бы во время build.
                      onPressed: state.loadChannels,
                      icon: const Icon(Icons.refresh),
                    ),
                    IconButton(
                      tooltip: s.logout,
                      // Исходная строка 96: Выход — отдельное действие. Обычный Back не должен удалять сессию.
                      onPressed: state.logout,
                      icon: const Icon(Icons.logout),
                    ),
                  ],
                ),
                // Исходная строка 101: Отступы от системных перекрытий экрана. Не имеет отношения к сохранению данных.
                body: SafeArea(
                  child: landscape
                      // Исходная строка 103: Горизонтальная раскладка ландшафта.
                      ? Row(
                          children: [
                            // Исходная строка 105: Оставшаяся ширина Row делится в отношении 2:3. Expanded даёт панели ограниченные размеры.
                            Expanded(flex: 2, child: ChannelList(state: state)),
                            const VerticalDivider(width: 1),
                            Expanded(flex: 3, child: pane()),
                          ],
                        )
                      : ChannelList(state: state),
                ),
              ),
            ),
          ),
          // Исходная строка 115: Collection if добавляет отдельную страницу чата только в портретной ориентации.
          if (!landscape && state.selected != null)
            MaterialPage(
              // Исходная строка 117: Navigator различает страницы каналов по ключу с ID.
              key: ValueKey('chat:${state.selected!.id}'),
              // Исходная строка 118: Каркас Material-экрана: appBar, body и работа с доступной областью при клавиатуре.
              child: Scaffold(
                appBar: AppBar(
                  title: Text(state.selected!.name),
                  // Исходная строка 121: UI-кнопка использует ту же операцию state.back, что и системный возврат.
                  leading: BackButton(onPressed: state.back),
                  actions: [
                    IconButton(
                      tooltip: s.refresh,
                      onPressed: () => state.loadMessages(state.selected!.id),
                      icon: const Icon(Icons.refresh),
                    ),
                  ],
                ),
                // Исходная строка 130: Отступы от системных перекрытий экрана. Не имеет отношения к сохранению данных.
                body: SafeArea(child: pane()),
              ),
            ),
          // Исходная строка 133: Картинка — последняя, верхняя страница стека в обеих ориентациях.
          if (state.openedImage != null)
            MaterialPage(
              key: ValueKey('image:${state.openedImage}'),
              // Исходная строка 136: Каркас Material-экрана: appBar, body и работа с доступной областью при клавиатуре.
              child: Scaffold(
                appBar: AppBar(
                  title: Text(s.image),
                  leading: IconButton(
                    tooltip: s.close,
                    onPressed: state.back,
                    icon: const Icon(Icons.close),
                  ),
                ),
                // Исходная строка 145: Отступы от системных перекрытий экрана. Не имеет отношения к сохранению данных.
                body: SafeArea(
                  child: RemoteImage(state: state, id: state.openedImage!),
                ),
              ),
            ),
        ],
        // Исходная строка 151: Согласуем состояние с удалением страницы, например после платформенного жеста возврата.
        onDidRemovePage: (page) {
          if (state.openedImage != null &&
              page.key == ValueKey('image:${state.openedImage}')) {
            state.back();
          } else if (state.selected != null &&
              page.key == ValueKey('chat:${state.selected!.id}') &&
              // Исходная строка 157: Удаление портретной страницы при переходе в landscape не должно закрыть выбранный чат.
              !landscape) {
            state.back();
          }
        },
      ),
    );
  }
}
```
