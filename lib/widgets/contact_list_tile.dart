import 'package:devicecontacts/l10n/app_localizations.dart';
import 'package:devicecontacts/models/contact_summary.dart';
import 'package:flutter/material.dart';

/// Строка списка: аватар, имя, основной телефон и признак email.
class ContactListTile extends StatelessWidget {
  const ContactListTile({
    super.key,
    required this.contact,
    required this.onTap,
  });

  final ContactSummary contact;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final name = contact.displayName.trim().isEmpty
        ? l10n.unnamedContact
        : contact.displayName;
    final phone = contact.primaryPhone ?? l10n.phoneNotSpecified;
    final phoneStyle = theme.textTheme.bodyMedium?.copyWith(
      color: contact.primaryPhone == null
          ? theme.colorScheme.onSurfaceVariant
          : theme.colorScheme.onSurface,
    );

    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          child: Row(
            children: [
              _InitialAvatar(initial: contact.initial),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleMedium,
                    ),
                    const SizedBox(height: 2),
                    Text(
                      phone,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: phoneStyle,
                    ),
                  ],
                ),
              ),
              if (contact.hasEmail)
                Tooltip(
                  message: l10n.hasEmail,
                  child: Icon(
                    Icons.alternate_email,
                    color: theme.colorScheme.primary,
                    semanticLabel: l10n.hasEmail,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _InitialAvatar extends StatelessWidget {
  const _InitialAvatar({required this.initial});

  final String initial;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return CircleAvatar(
      backgroundColor: scheme.primaryContainer,
      foregroundColor: scheme.onPrimaryContainer,
      child: initial.isEmpty
          ? const Icon(Icons.person_outline)
          : Text(
              initial,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
    );
  }
}
