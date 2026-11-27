import 'package:flutter/material.dart';

import '../data/models.dart';
import '../l10n/generated/app_localizations.dart';

extension LocalizedContext on BuildContext {
  AppLocalizations get strings => AppLocalizations.of(this)!;
}

String problemText(BuildContext context, Problem problem) {
  final t = context.strings;
  return switch (problem) {
    Problem.credentials => t.badCredentials,
    Problem.unauthorized => t.expired,
    Problem.connection => t.offline,
    Problem.server => t.serverError,
    Problem.rejected => t.requestError,
    Problem.storage => t.storageError,
  };
}

class ErrorNotice extends StatelessWidget {
  const ErrorNotice(this.text, {super.key, this.retry});

  final String text;
  final VoidCallback? retry;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Semantics(
      liveRegion: true,
      child: ColoredBox(
        color: colors.errorContainer,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  text,
                  style: TextStyle(color: colors.onErrorContainer),
                ),
              ),
              if (retry != null)
                TextButton(
                  onPressed: retry,
                  child: Text(context.strings.retry),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
