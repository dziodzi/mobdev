import 'package:devicecontacts/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

/// Поле поиска по имени с кнопкой очистки.
class ContactsSearchBar extends StatelessWidget {
  const ContactsSearchBar({
    super.key,
    required this.controller,
    required this.onChanged,
    required this.onClear,
  });

  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final VoidCallback onClear;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final hasText = controller.text.isNotEmpty;
    return TextField(
      key: const Key('search-field'),
      controller: controller,
      onChanged: onChanged,
      textInputAction: TextInputAction.search,
      decoration: InputDecoration(
        hintText: l10n.searchHint,
        prefixIcon: const Icon(Icons.search),
        suffixIcon: hasText
            ? IconButton(
                key: const Key('clear-search'),
                tooltip: l10n.clearSearch,
                onPressed: onClear,
                icon: const Icon(Icons.close),
              )
            : null,
      ),
    );
  }
}
