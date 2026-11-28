import 'package:flutter/material.dart';

import '../state/chat_state.dart';
import 'strings.dart';

class LoginScreen extends StatefulWidget {
  final ChatState state;
  const LoginScreen({super.key, required this.state});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final login = TextEditingController(), password = TextEditingController();
  @override
  void dispose() {
    login.dispose();
    password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = strings(context), state = widget.state;
    return Scaffold(
      appBar: AppBar(title: Text(s.signIn)),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: login,
                  decoration: InputDecoration(labelText: s.login),
                  autofillHints: const [AutofillHints.username],
                ),
                TextField(
                  controller: password,
                  decoration: InputDecoration(labelText: s.password),
                  obscureText: true,
                  autofillHints: const [AutofillHints.password],
                ),
                if (state.error != null)
                  Text(
                    failureText(context, state.error!),
                    style: TextStyle(
                      color: Theme.of(context).colorScheme.error,
                    ),
                  ),
                const SizedBox(height: 16),
                if (state.signingIn) const CircularProgressIndicator(),
                FilledButton(
                  onPressed: state.signingIn
                      ? null
                      : () async {
                          await state.login(login.text.trim(), password.text);
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
