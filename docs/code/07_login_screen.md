# lib/ui/login_screen.dart

[К порядку чтения](../../CODE_WALKTHROUGH.md)

```dart
import 'package:flutter/material.dart';

import '../state/chat_state.dart';
import 'strings.dart';

// Исходная строка 6: State нужен для контроллеров ввода; сессия хранится в отдельном ChatState.
class LoginScreen extends StatefulWidget {
  final ChatState state;
  const LoginScreen({super.key, required this.state});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  // Исходная строка 14: Контроллеры живут между build: ввод не сбрасывается при появлении индикатора.
  final login = TextEditingController(), password = TextEditingController();
  @override
  // Исходная строка 16: Освобождаем оба созданных контроллера; это не операция logout.
  void dispose() {
    login.dispose();
    password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Исходная строка 24: s — переводы, state — внешний контроллер.
    final s = strings(context), state = widget.state;
    return Scaffold(
      appBar: AppBar(title: Text(s.signIn)),
      body: Center(
        // Исходная строка 28: Прокрутка формы при клавиатуре и крупном шрифте.
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            // Исходная строка 31: Ограничиваем ширину формы на широком экране.
            constraints: const BoxConstraints(maxWidth: 420),
            child: Column(
              // Исходная строка 33: Column занимает высоту содержимого, а не пытается растянуться внутри прокрутки.
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  // Исходная строка 36: Связываем поле с его TextEditingController. labelText даёт доступную подпись.
                  controller: login,
                  decoration: InputDecoration(labelText: s.login),
                  // Исходная строка 38: Подсказка платформе для автозаполнения. Не означает запись пароля в наш LocalStore.
                  autofillHints: const [AutofillHints.username],
                ),
                TextField(
                  controller: password,
                  decoration: InputDecoration(labelText: s.password),
                  // Исходная строка 43: Только визуально скрывает пароль; не шифрует сетевой запрос/диск.
                  obscureText: true,
                  // Исходная строка 44: Подсказка платформе для автозаполнения. Не означает запись пароля в наш LocalStore.
                  autofillHints: const [AutofillHints.password],
                ),
                // Исходная строка 46: Collection if добавляет виджет ошибки только при её наличии.
                if (state.error != null)
                  Text(
                    failureText(context, state.error!),
                    style: TextStyle(
                      color: Theme.of(context).colorScheme.error,
                    ),
                  ),
                const SizedBox(height: 16),
                // Исходная строка 54: Индикатор отражает состояние запроса.
                if (state.signingIn) const CircularProgressIndicator(),
                FilledButton(
                  // Исходная строка 56: null отключает кнопку; контроллер дополнительно защищён флагом до await.
                  onPressed: state.signingIn
                      ? null
                      : () async {
                          // Исходная строка 59: Считываем введённое и запускаем вход. trim только у логина: пробел может быть частью пароля.
                          await state.login(login.text.trim(), password.text);
                          // Исходная строка 60: После попытки очищаем текст контроллера пароля.
                          password.clear();
                        },
                  child: Text(s.signIn),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
```
