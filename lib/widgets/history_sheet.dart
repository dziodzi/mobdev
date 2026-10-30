import 'package:flutter/material.dart';

import '../models/calculation_history_item.dart';

class HistorySheet extends StatelessWidget {
  const HistorySheet({
    super.key,
    required this.history,
    required this.onSelect,
  });

  final List<CalculationHistoryItem> history;
  final ValueChanged<CalculationHistoryItem> onSelect;

  static Future<void> show({
    required BuildContext context,
    required List<CalculationHistoryItem> history,
    required ValueChanged<CalculationHistoryItem> onSelect,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      isScrollControlled: true,
      builder: (sheetContext) {
        final maxHeight = MediaQuery.sizeOf(sheetContext).height * 0.6;
        return ConstrainedBox(
          constraints: BoxConstraints(maxHeight: maxHeight),
          child: HistorySheet(
            history: history,
            onSelect: (item) {
              onSelect(item);
              Navigator.of(sheetContext).pop();
            },
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    if (history.isEmpty) {
      return const Padding(
        padding: EdgeInsets.fromLTRB(24, 8, 24, 32),
        child: Text('История пока пуста'),
      );
    }

    return SafeArea(
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(8, 0, 8, 16),
        itemCount: history.length,
        separatorBuilder: (context, index) => const Divider(height: 1),
        itemBuilder: (context, index) {
          final item = history[index];
          return ListTile(
            title: Text(item.expression),
            subtitle: Text('= ${item.result}'),
            trailing: const Icon(Icons.north_west),
            onTap: () => onSelect(item),
          );
        },
      ),
    );
  }
}
