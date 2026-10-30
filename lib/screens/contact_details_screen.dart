import 'package:devicecontacts/controllers/contacts_scope.dart';
import 'package:devicecontacts/l10n/app_localizations.dart';
import 'package:devicecontacts/models/contact_details.dart';
import 'package:devicecontacts/models/contacts_error.dart';
import 'package:devicecontacts/utils/contact_labels.dart';
import 'package:devicecontacts/widgets/error_view.dart';
import 'package:devicecontacts/widgets/loading_view.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

/// Подробности одного контакта. Список целиком здесь не перечитывается.
class ContactDetailsScreen extends StatefulWidget {
  const ContactDetailsScreen({super.key, required this.contactId});

  final String contactId;

  @override
  State<ContactDetailsScreen> createState() => _ContactDetailsScreenState();
}

class _ContactDetailsScreenState extends State<ContactDetailsScreen> {
  ContactDetails? _details;
  ContactsErrorKind? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _load();
    });
  }

  Future<void> _load() async {
    if (!mounted) {
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    final details = await ContactsScope.read(
      context,
    ).getContactDetails(widget.contactId);
    if (!mounted) {
      return;
    }
    setState(() {
      _details = details;
      _error = details == null
          ? ContactsScope.read(context).detailsError ??
                ContactsErrorKind.detailsNotFound
          : null;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final details = _details;
    final title = details == null || details.displayName.trim().isEmpty
        ? l10n.unnamedContact
        : details.displayName;
    return Scaffold(
      appBar: AppBar(
        title: Text(title, maxLines: 1, overflow: TextOverflow.ellipsis),
      ),
      body: SafeArea(
        child: _loading
            ? LoadingView(message: l10n.loadingDetails)
            : _error != null || details == null
            ? ErrorView(
                message: localizedErrorMessage(
                  l10n,
                  _error ?? ContactsErrorKind.detailsNotFound,
                ),
                onRetry: _load,
              )
            : LayoutBuilder(
                builder: (context, constraints) {
                  return _DetailsBody(
                    details: details,
                    horizontalPadding: constraints.maxWidth >= 700 ? 24 : 16,
                  );
                },
              ),
      ),
    );
  }
}

class _DetailsBody extends StatelessWidget {
  const _DetailsBody({
    required this.details,
    required this.horizontalPadding,
  });

  final ContactDetails details;
  final double horizontalPadding;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final name = details.displayName.trim().isEmpty
        ? l10n.unnamedContact
        : details.displayName;
    return ListView(
      padding: EdgeInsets.fromLTRB(
        horizontalPadding,
        16,
        horizontalPadding,
        24,
      ),
      children: [
        _Header(details: details, name: name),
        const SizedBox(height: 16),
        if (!details.hasExtraFields)
          Text(l10n.noExtraFields, textAlign: TextAlign.center),
        _section(context, l10n.sectionName, _nameRows(l10n, details.name)),
        _section(
          context,
          l10n.sectionPhones,
          _labeledRows(context, details.phones),
        ),
        _section(
          context,
          l10n.sectionEmails,
          _labeledRows(context, details.emails),
        ),
        _section(
          context,
          l10n.sectionAddresses,
          _labeledRows(context, details.addresses),
        ),
        _section(
          context,
          l10n.sectionOrganization,
          _organizationRows(l10n, details.organizations),
        ),
        _section(
          context,
          l10n.sectionWebsites,
          _labeledRows(context, details.websites),
        ),
        _section(
          context,
          l10n.sectionSocial,
          _labeledRows(context, details.socialMedias),
        ),
        _section(
          context,
          l10n.sectionEvents,
          _eventRows(context, details.events),
        ),
        _section(
          context,
          l10n.sectionRelations,
          _labeledRows(context, details.relations),
        ),
        _section(
          context,
          l10n.sectionNotes,
          [
            for (final note in details.notes)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Text(note),
              ),
          ],
        ),
      ],
    );
  }

  List<Widget> _nameRows(AppLocalizations l10n, ContactNameParts name) {
    return [
      if (name.prefix != null) _valueRow(l10n.fieldPrefix, name.prefix!),
      if (name.first != null) _valueRow(l10n.fieldFirstName, name.first!),
      if (name.middle != null) _valueRow(l10n.fieldMiddleName, name.middle!),
      if (name.last != null) _valueRow(l10n.fieldLastName, name.last!),
      if (name.suffix != null) _valueRow(l10n.fieldSuffix, name.suffix!),
      if (name.nickname != null) _valueRow(l10n.fieldNickname, name.nickname!),
      if (name.phonetic != null) _valueRow(l10n.fieldPhonetic, name.phonetic!),
      if (name.previousFamilyName != null)
        _valueRow(l10n.fieldPreviousFamilyName, name.previousFamilyName!),
    ];
  }

  List<Widget> _labeledRows(BuildContext context, List<LabeledValue> values) {
    final l10n = AppLocalizations.of(context)!;
    return [
      for (final item in values)
        _valueRow(
          localizedContactLabel(l10n, item.labelKey, item.customLabel),
          item.value,
        ),
    ];
  }

  List<Widget> _organizationRows(
    AppLocalizations l10n,
    List<OrganizationInfo> organizations,
  ) {
    return [
      for (final organization in organizations) ...[
        if (organization.name != null)
          _valueRow(l10n.fieldCompany, organization.name!),
        if (organization.jobTitle != null)
          _valueRow(l10n.fieldJobTitle, organization.jobTitle!),
        if (organization.department != null)
          _valueRow(l10n.fieldDepartment, organization.department!),
        if (organization.jobDescription != null)
          _valueRow(l10n.fieldJobDescription, organization.jobDescription!),
        if (organization.officeLocation != null)
          _valueRow(l10n.fieldOfficeLocation, organization.officeLocation!),
        if (organization.phoneticName != null)
          _valueRow(l10n.fieldPhonetic, organization.phoneticName!),
        if (organization.symbol != null)
          _valueRow(l10n.fieldSymbol, organization.symbol!),
      ],
    ];
  }

  List<Widget> _eventRows(BuildContext context, List<ContactEvent> events) {
    final l10n = AppLocalizations.of(context)!;
    final locale = Localizations.localeOf(context).toString();
    return [
      for (final event in events)
        _valueRow(
          localizedContactLabel(l10n, event.labelKey, event.customLabel),
          _formatEvent(locale, event),
        ),
    ];
  }

  Widget _section(BuildContext context, String title, List<Widget> children) {
    if (children.isEmpty) {
      return const SizedBox.shrink();
    }
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: theme.textTheme.titleSmall?.copyWith(
                  color: theme.colorScheme.primary,
                ),
              ),
              const SizedBox(height: 8),
              ...children,
            ],
          ),
        ),
      ),
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.details, required this.name});

  final ContactDetails details;
  final String name;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    return Column(
      children: [
        _Photo(details: details),
        const SizedBox(height: 12),
        Text(
          name,
          style: theme.textTheme.headlineSmall,
          textAlign: TextAlign.center,
        ),
        if (details.isFavorite == true) ...[
          const SizedBox(height: 4),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.star, color: theme.colorScheme.tertiary, size: 18),
              const SizedBox(width: 4),
              Flexible(
                child: Text(
                  l10n.favoriteContact,
                  style: theme.textTheme.bodySmall,
                ),
              ),
            ],
          ),
        ],
      ],
    );
  }
}

class _Photo extends StatelessWidget {
  const _Photo({required this.details});

  final ContactDetails details;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final bytes = details.photoBytes;
    final fallback = CircleAvatar(
      radius: 42,
      backgroundColor: scheme.primaryContainer,
      foregroundColor: scheme.onPrimaryContainer,
      child: details.initial.isEmpty
          ? const Icon(Icons.person_outline, size: 36)
          : Text(
              details.initial,
              style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w600),
            ),
    );
    if (bytes == null) {
      return fallback;
    }
    return ClipOval(
      child: Image.memory(
        bytes,
        width: 84,
        height: 84,
        fit: BoxFit.cover,
        errorBuilder: (context, error, stackTrace) => fallback,
      ),
    );
  }
}

Widget _valueRow(String label, String value) {
  return Builder(
    builder: (context) {
      final theme = Theme.of(context);
      return Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 2),
            Text(value),
          ],
        ),
      );
    },
  );
}

String _formatEvent(String locale, ContactEvent event) {
  try {
    final date = DateTime(event.year ?? 2000, event.month, event.day);
    if (event.year == null) {
      return DateFormat.MMMd(locale).format(date);
    }
    return DateFormat.yMMMd(locale).format(date);
  } catch (error) {
    debugPrint('Event date format failed: $error');
    final day = event.day.toString().padLeft(2, '0');
    final month = event.month.toString().padLeft(2, '0');
    if (event.year == null) {
      return '$day.$month';
    }
    return '$day.$month.${event.year}';
  }
}
