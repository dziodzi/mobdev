import 'package:devicecontacts/l10n/app_localizations.dart';
import 'package:devicecontacts/models/contact_sort_order.dart';
import 'package:flutter/material.dart';

/// Выбор сортировки по имени: А → Я или Я → А.
class SortingControl extends StatelessWidget {
  const SortingControl({
    super.key,
    required this.order,
    required this.onChanged,
  });

  final ContactSortOrder order;
  final ValueChanged<ContactSortOrder> onChanged;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final label = order == ContactSortOrder.ascending
        ? l10n.sortAscending
        : l10n.sortDescending;
    return PopupMenuButton<ContactSortOrder>(
      key: const Key('sort-control'),
      tooltip: l10n.sortTooltip,
      onSelected: onChanged,
      itemBuilder: (context) {
        return [
          PopupMenuItem(
            value: ContactSortOrder.ascending,
            child: Text(l10n.sortAscending),
          ),
          PopupMenuItem(
            value: ContactSortOrder.descending,
            child: Text(l10n.sortDescending),
          ),
        ];
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Flexible(
              child: Text(
                label,
                key: const Key('sort-label'),
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const Icon(Icons.arrow_drop_down),
          ],
        ),
      ),
    );
  }
}
