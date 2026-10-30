import 'package:devicecontacts/controllers/contacts_scope.dart';
import 'package:devicecontacts/l10n/app_localizations.dart';
import 'package:devicecontacts/models/contact_access_state.dart';
import 'package:devicecontacts/models/contact_sort_order.dart';
import 'package:devicecontacts/models/contacts_error.dart';
import 'package:devicecontacts/screens/contact_details_screen.dart';
import 'package:devicecontacts/widgets/contact_list_tile.dart';
import 'package:devicecontacts/widgets/contacts_search_bar.dart';
import 'package:devicecontacts/widgets/empty_state_view.dart';
import 'package:devicecontacts/widgets/error_view.dart';
import 'package:devicecontacts/widgets/loading_view.dart';
import 'package:devicecontacts/widgets/permission_view.dart';
import 'package:devicecontacts/widgets/sorting_control.dart';
import 'package:flutter/material.dart';

/// Главный экран: поиск, сортировка и ленивый список контактов.
class ContactsScreen extends StatefulWidget {
  const ContactsScreen({super.key});

  @override
  State<ContactsScreen> createState() => _ContactsScreenState();
}

class _ContactsScreenState extends State<ContactsScreen> with RestorationMixin {
  final TextEditingController _searchController = TextEditingController();
  final RestorableString _searchQuery = RestorableString('');
  final RestorableInt _sortOrderIndex = RestorableInt(
    ContactSortOrder.ascending.index,
  );

  @override
  String? get restorationId => 'contacts_screen';

  @override
  void restoreState(RestorationBucket? oldBucket, bool initialRestore) {
    registerForRestoration(_searchQuery, 'search_query');
    registerForRestoration(_sortOrderIndex, 'sort_order');
    final controller = ContactsScope.read(context);
    if (oldBucket != null) {
      final index = _sortOrderIndex.value.clamp(
        0,
        ContactSortOrder.values.length - 1,
      );
      controller.restoreViewState(
        searchQuery: _searchQuery.value,
        sortOrder: ContactSortOrder.values[index],
      );
    } else {
      _searchQuery.value = controller.searchQuery;
      _sortOrderIndex.value = controller.sortOrder.index;
    }
    _searchController.text = controller.searchQuery;
  }

  @override
  void dispose() {
    _searchController.dispose();
    _searchQuery.dispose();
    _sortOrderIndex.dispose();
    super.dispose();
  }

  void _onSearchChanged(String value) {
    _searchQuery.value = value;
    ContactsScope.read(context).searchContacts(value);
  }

  void _onSearchCleared() {
    _searchController.clear();
    _onSearchChanged('');
  }

  void _onSortChanged(ContactSortOrder order) {
    _sortOrderIndex.value = order.index;
    ContactsScope.read(context).changeSortOrder(order);
  }

  void _openDetails(String id) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (context) => ContactDetailsScreen(contactId: id),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final controller = ContactsScope.of(context);
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.appTitle),
        actions: [
          IconButton(
            tooltip: l10n.refresh,
            onPressed: controller.isLoading ? null : controller.refreshContacts,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            return _buildBody(context, l10n, constraints);
          },
        ),
      ),
    );
  }

  Widget _buildBody(
    BuildContext context,
    AppLocalizations l10n,
    BoxConstraints constraints,
  ) {
    final controller = ContactsScope.of(context);
    final waitingForFirstData =
        controller.isLoading &&
        !controller.hasLoadedContacts &&
        (controller.permissionStatus == ContactAccessStatus.unknown ||
            controller.permissionStatus.canReadContacts);
    if (waitingForFirstData) {
      return LoadingView(
        key: const Key('loading-view'),
        message: controller.permissionStatus == ContactAccessStatus.unknown
            ? l10n.checkingPermission
            : l10n.loadingContacts,
      );
    }

    if (controller.errorKind == ContactsErrorKind.permissionCheckFailed &&
        !controller.hasLoadedContacts &&
        controller.permissionStatus == ContactAccessStatus.unknown) {
      return ErrorView(
        key: const Key('error-view'),
        message: l10n.errorPermissionCheck,
        onRetry: controller.checkPermission,
      );
    }

    if (!controller.permissionStatus.canReadContacts) {
      return PermissionView(
        key: const Key('permission-view'),
        status: controller.permissionStatus,
        isBusy: controller.isLoading,
        onRequest: controller.requestPermission,
        onOpenSettings: controller.openSettings,
      );
    }

    if (controller.errorKind == ContactsErrorKind.loadFailed &&
        !controller.hasLoadedContacts) {
      return ErrorView(
        key: const Key('error-view'),
        message: l10n.errorLoadContacts,
        onRetry: controller.refreshContacts,
      );
    }

    final wide = constraints.maxWidth >= 700;
    return Column(
      children: [
        Padding(
          padding: EdgeInsets.fromLTRB(wide ? 24 : 16, 8, wide ? 24 : 16, 8),
          child: wide
              ? Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(flex: 3, child: _searchField()),
                    const SizedBox(width: 12),
                    Flexible(child: _countAndSort(l10n)),
                  ],
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _searchField(),
                    const SizedBox(height: 8),
                    _countAndSort(l10n),
                  ],
                ),
        ),
        if (controller.isLimitedAccess)
          Padding(
            padding: EdgeInsets.fromLTRB(wide ? 24 : 16, 0, wide ? 24 : 16, 8),
            child: _LimitedBanner(message: l10n.limitedAccessBanner),
          ),
        if (controller.errorKind == ContactsErrorKind.loadFailed)
          Padding(
            padding: EdgeInsets.fromLTRB(wide ? 24 : 16, 0, wide ? 24 : 16, 8),
            child: _ErrorBanner(
              message: l10n.errorLoadContacts,
              retryLabel: l10n.retry,
              onRetry: controller.refreshContacts,
            ),
          ),
        if (controller.isLoading) const LinearProgressIndicator(),
        Expanded(child: _listArea(l10n)),
      ],
    );
  }

  Widget _searchField() {
    return ContactsSearchBar(
      controller: _searchController,
      onChanged: _onSearchChanged,
      onClear: _onSearchCleared,
    );
  }

  Widget _countAndSort(AppLocalizations l10n) {
    final controller = ContactsScope.of(context);
    return Row(
      children: [
        Expanded(
          child: Text(
            l10n.foundContacts(controller.foundCount),
            key: const Key('found-count'),
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
        ),
        SortingControl(
          order: controller.sortOrder,
          onChanged: _onSortChanged,
        ),
      ],
    );
  }

  Widget _listArea(AppLocalizations l10n) {
    final controller = ContactsScope.of(context);
    if (controller.contacts.isEmpty) {
      return EmptyStateView(
        key: const Key('empty-contacts'),
        icon: Icons.person_off_outlined,
        title: l10n.emptyContactsTitle,
        message: l10n.emptyContactsMessage,
      );
    }
    if (controller.filteredContacts.isEmpty) {
      return EmptyStateView(
        key: const Key('empty-search'),
        icon: Icons.search_off,
        title: l10n.emptySearchTitle,
        message: l10n.emptySearchMessage,
      );
    }

    final contacts = controller.filteredContacts;
    return RefreshIndicator(
      onRefresh: controller.refreshContacts,
      child: ListView.builder(
        key: const Key('contacts-list'),
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(12, 4, 12, 16),
        itemCount: contacts.length,
        itemBuilder: (context, index) {
          final contact = contacts[index];
          return Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: ContactListTile(
              contact: contact,
              onTap: () => _openDetails(contact.id),
            ),
          );
        },
      ),
    );
  }
}

class _LimitedBanner extends StatelessWidget {
  const _LimitedBanner({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: scheme.secondaryContainer,
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            Icon(Icons.info_outline, color: scheme.onSecondaryContainer),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                message,
                style: TextStyle(color: scheme.onSecondaryContainer),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({
    required this.message,
    required this.retryLabel,
    required this.onRetry,
  });

  final String message;
  final String retryLabel;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: scheme.errorContainer,
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            Icon(Icons.error_outline, color: scheme.onErrorContainer),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                message,
                style: TextStyle(color: scheme.onErrorContainer),
              ),
            ),
            TextButton(onPressed: onRetry, child: Text(retryLabel)),
          ],
        ),
      ),
    );
  }
}
