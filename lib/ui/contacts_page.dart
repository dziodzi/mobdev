import 'package:flutter/material.dart';

import '../controllers/contact_controller.dart';
import '../l10n/app_localizations.dart';
import '../models/contact_record.dart';
import '../services/contact_repository.dart';

class ContactsPage extends StatefulWidget {
  const ContactsPage({super.key, required this.repository});

  final ContactRepository repository;

  @override
  State<ContactsPage> createState() => _ContactsPageState();
}

class _ContactsPageState extends State<ContactsPage>
    with WidgetsBindingObserver {
  late final ContactController controller;
  late final TextEditingController searchController;

  @override
  void initState() {
    super.initState();
    controller = ContactController(widget.repository);
    searchController = TextEditingController();
    WidgetsBinding.instance.addObserver(this);
    controller.initialize();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      controller.checkAfterSettings();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    controller.dispose();
    searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final text = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(text.appTitle),
        actions: [
          IconButton(
            tooltip: text.refresh,
            onPressed: () => controller.refresh(),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: SafeArea(
        child: AnimatedBuilder(
          animation: controller,
          builder: (context, _) {
            switch (controller.state) {
              case ContactsState.loading:
                return Center(
                  child: Semantics(
                    label: text.loading,
                    child: const CircularProgressIndicator(),
                  ),
                );
              case ContactsState.denied:
                return _permissionMessage(text);
              case ContactsState.error:
                return _centerMessage(
                  icon: Icons.error_outline,
                  title: text.loadError,
                  actions: [
                    FilledButton(
                      onPressed: () => controller.refresh(),
                      child: Text(text.retry),
                    ),
                  ],
                );
              case ContactsState.ready:
                return _contactsList(text);
            }
          },
        ),
      ),
    );
  }

  Widget _permissionMessage(AppLocalizations text) {
    return _centerMessage(
      icon: Icons.lock_outline,
      title: text.permissionTitle,
      description: controller.permanentlyDenied
          ? text.permissionPermanentMessage
          : text.permissionMessage,
      actions: [
        if (!controller.permanentlyDenied)
          FilledButton(
            onPressed: () => controller.refresh(requestAccess: true),
            child: Text(text.requestAccess),
          ),
        if (controller.permanentlyDenied)
          FilledButton(
            onPressed: widget.repository.openSettings,
            child: Text(text.openSettings),
          ),
      ],
    );
  }

  Widget _centerMessage({
    required IconData icon,
    required String title,
    String description = '',
    required List<Widget> actions,
  }) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 60),
            const SizedBox(height: 16),
            Text(
              title,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.titleLarge,
            ),
            if (description.isNotEmpty) ...[
              const SizedBox(height: 12),
              Text(description, textAlign: TextAlign.center),
            ],
            const SizedBox(height: 20),
            ...actions.map(
              (action) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: action,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _contactsList(AppLocalizations text) {
    final contacts = controller.visibleContacts;
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
          child: TextField(
            controller: searchController,
            onChanged: controller.updateQuery,
            decoration: InputDecoration(
              hintText: text.searchHint,
              prefixIcon: const Icon(Icons.search),
              border: const OutlineInputBorder(),
              suffixIcon: controller.query.isEmpty
                  ? null
                  : IconButton(
                      onPressed: () {
                        searchController.clear();
                        controller.updateQuery('');
                      },
                      icon: const Icon(Icons.clear),
                    ),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            children: [
              Expanded(child: Text(text.countLabel(contacts.length))),
              DropdownButton<bool>(
                value: controller.ascending,
                items: [
                  DropdownMenuItem(
                    value: true,
                    child: Text(text.sortAscending),
                  ),
                  DropdownMenuItem(
                    value: false,
                    child: Text(text.sortDescending),
                  ),
                ],
                onChanged: (value) {
                  if (value != null) controller.updateSort(value);
                },
              ),
            ],
          ),
        ),
        Expanded(
          child: contacts.isEmpty
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Text(
                      controller.totalContacts == 0
                          ? text.noContacts
                          : text.noResults,
                      textAlign: TextAlign.center,
                    ),
                  ),
                )
              : ListView.builder(
                  key: const PageStorageKey('contacts_list'),
                  itemCount: contacts.length,
                  itemBuilder: (context, index) {
                    final contact = contacts[index];
                    final name = _contactName(contact, text);
                    return ListTile(
                      leading: CircleAvatar(
                        child: Text(name.characters.first.toUpperCase()),
                      ),
                      title: Text(name),
                      subtitle: Text(
                        contact.primaryPhone.isEmpty
                            ? text.noPhone
                            : contact.primaryPhone,
                      ),
                      trailing: contact.hasEmail
                          ? const Icon(Icons.email_outlined)
                          : const Icon(Icons.chevron_right),
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute<void>(
                          builder: (_) => ContactDetailsPage(contact: contact),
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  String _contactName(ContactRecord contact, AppLocalizations text) {
    return contact.name.isEmpty ? text.unknownContact : contact.name;
  }
}

class ContactDetailsPage extends StatelessWidget {
  const ContactDetailsPage({super.key, required this.contact});

  final ContactRecord contact;

  @override
  Widget build(BuildContext context) {
    final text = AppLocalizations.of(context)!;
    final name = contact.name.isEmpty ? text.unknownContact : contact.name;
    return Scaffold(
      appBar: AppBar(title: Text(text.detailsTitle)),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text(name, style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 20),
            _section(
              text.phones,
              contact.phones.isEmpty ? [text.noPhone] : contact.phones,
              Icons.phone_outlined,
            ),
            _section(
              text.emails,
              contact.emails.isEmpty ? [text.noEmail] : contact.emails,
              Icons.email_outlined,
            ),
            if (contact.organization.isNotEmpty)
              _section(text.organization, [
                contact.organization,
              ], Icons.business_outlined),
            if (contact.addresses.isNotEmpty)
              _section(
                text.addresses,
                contact.addresses,
                Icons.location_on_outlined,
              ),
            if (contact.notes.isNotEmpty)
              _section(text.notes, contact.notes, Icons.notes_outlined),
          ],
        ),
      ),
    );
  }

  Widget _section(String title, List<String> values, IconData icon) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 20),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
              ],
            ),
            ...values.map(
              (value) => Padding(
                padding: const EdgeInsets.only(top: 8, left: 28),
                child: SelectableText(value),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
