import 'package:devicecontacts/controllers/contacts_controller.dart';
import 'package:flutter/widgets.dart';

/// Передаёт один контроллер всему дереву виджетов.
///
/// InheritedNotifier сам перестраивает экраны, когда контроллер вызывает
/// notifyListeners(). Контроллер при этом не создаётся в build().
class ContactsScope extends InheritedNotifier<ContactsController> {
  const ContactsScope({
    super.key,
    required ContactsController controller,
    required super.child,
  }) : super(notifier: controller);

  static ContactsController of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<ContactsScope>();
    assert(scope != null, 'ContactsScope is missing above this widget');
    return scope!.notifier!;
  }

  /// Читает контроллер без подписки. Нужен в initState и restoreState.
  static ContactsController read(BuildContext context) {
    final scope = context.getInheritedWidgetOfExactType<ContactsScope>();
    assert(scope != null, 'ContactsScope is missing above this widget');
    return scope!.notifier!;
  }
}
