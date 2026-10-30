import 'package:devicecontacts/l10n/app_localizations.dart';
import 'package:devicecontacts/models/contact_access_state.dart';
import 'package:devicecontacts/utils/contact_labels.dart';
import 'package:flutter/material.dart';

/// Экран отсутствия доступа к адресной книге.
class PermissionView extends StatelessWidget {
  const PermissionView({
    super.key,
    required this.status,
    required this.isBusy,
    required this.onRequest,
    required this.onOpenSettings,
  });

  final ContactAccessStatus status;
  final bool isBusy;
  final VoidCallback onRequest;
  final VoidCallback onOpenSettings;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final showRequest = status.canRequestPermission;
    final showSettings = status.requiresSettings;

    return LayoutBuilder(
      builder: (context, constraints) {
        return SingleChildScrollView(
          child: ConstrainedBox(
            constraints: BoxConstraints(minHeight: constraints.maxHeight),
            child: Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 420),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.contacts_outlined,
                        size: 64,
                        color: theme.colorScheme.primary,
                      ),
                      const SizedBox(height: 16),
                      Text(
                        l10n.permissionTitle,
                        style: theme.textTheme.headlineSmall,
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 12),
                      Text(
                        l10n.permissionExplanation,
                        style: theme.textTheme.bodyMedium,
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 12),
                      Text(
                        localizedPermissionMessage(l10n, status),
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 24),
                      if (isBusy) const CircularProgressIndicator(),
                      if (!isBusy && showRequest)
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton(
                            onPressed: onRequest,
                            child: Text(l10n.allowAccess),
                          ),
                        ),
                      if (!isBusy && showSettings) ...[
                        if (showRequest) const SizedBox(height: 8),
                        SizedBox(
                          width: double.infinity,
                          child: showRequest
                              ? OutlinedButton(
                                  onPressed: onOpenSettings,
                                  child: Text(l10n.openSettings),
                                )
                              : FilledButton(
                                  onPressed: onOpenSettings,
                                  child: Text(l10n.openSettings),
                                ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}
