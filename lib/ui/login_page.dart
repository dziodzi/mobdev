import 'package:flutter/material.dart';

import '../state/chat_state.dart';
import 'common.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, required this.state});

  final ChatState state;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _user = TextEditingController();
  final _password = TextEditingController();

  @override
  void initState() {
    super.initState();
    _user.addListener(_changed);
    _password.addListener(_changed);
  }

  void _changed() => setState(() {});

  @override
  void dispose() {
    _user.dispose();
    _password.dispose();
    super.dispose();
  }

  void _login() {
    if (widget.state.authenticating ||
        _user.text.trim().isEmpty ||
        _password.text.isEmpty) {
      return;
    }
    FocusScope.of(context).unfocus();
    widget.state.signIn(_user.text.trim(), _password.text);
  }

  @override
  Widget build(BuildContext context) {
    final t = context.strings;
    final state = widget.state;
    final theme = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: AutofillGroup(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Align(
                      alignment: Alignment.centerLeft,
                      child: CircleAvatar(
                        radius: 28,
                        backgroundColor: theme.colorScheme.primaryContainer,
                        child: Icon(
                          Icons.forum_outlined,
                          color: theme.colorScheme.onPrimaryContainer,
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),
                    Text(t.loginTitle, style: theme.textTheme.headlineMedium),
                    const SizedBox(height: 24),
                    TextField(
                      controller: _user,
                      autofillHints: const [AutofillHints.username],
                      textInputAction: TextInputAction.next,
                      enabled: !state.authenticating,
                      decoration: InputDecoration(labelText: t.user),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _password,
                      autofillHints: const [AutofillHints.password],
                      obscureText: true,
                      enabled: !state.authenticating,
                      onSubmitted: (_) => _login(),
                      decoration: InputDecoration(labelText: t.password),
                    ),
                    const SizedBox(height: 16),
                    if (state.loginProblem case final problem?) ...[
                      ErrorNotice(problemText(context, problem)),
                      const SizedBox(height: 12),
                    ],
                    FilledButton(
                      onPressed:
                          state.authenticating ||
                              _user.text.trim().isEmpty ||
                              _password.text.isEmpty
                          ? null
                          : _login,
                      child: state.authenticating
                          ? SizedBox.square(
                              dimension: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                semanticsLabel: t.loading,
                              ),
                            )
                          : Text(t.signIn),
                    ),
                    const SizedBox(height: 12),
                    Text(t.registration, style: theme.textTheme.bodySmall),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
