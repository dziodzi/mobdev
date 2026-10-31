class ContactRecord {
  const ContactRecord({
    required this.id,
    required this.name,
    this.phones = const [],
    this.emails = const [],
    this.organization = '',
    this.addresses = const [],
    this.notes = const [],
  });

  final String id;
  final String name;
  final List<String> phones;
  final List<String> emails;
  final String organization;
  final List<String> addresses;
  final List<String> notes;

  String get primaryPhone => phones.isEmpty ? '' : phones.first;
  bool get hasEmail => emails.isNotEmpty;
}
