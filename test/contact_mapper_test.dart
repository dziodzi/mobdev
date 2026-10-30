import 'package:devicecontacts/repositories/contact_mapper.dart';
import 'package:devicecontacts/repositories/device_contacts_repository.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('данные контакта', () {
    test('контакт только с именем', () {
      final summary = mapContactSummary(
        Contact(id: '1', displayName: 'Анна', name: Name(first: 'Анна')),
      );
      expect(summary?.displayName, 'Анна');
      expect(summary?.primaryPhone, isNull);
      expect(summary?.hasEmail, isFalse);
      expect(summary?.initial, 'А');
    });

    test('контакт с телефоном берёт основной номер', () {
      final summary = mapContactSummary(
        Contact(
          id: '1',
          displayName: 'Анна',
          phones: [
            Phone(number: '111', isPrimary: false),
            Phone(
              number: '222',
              isPrimary: true,
              label: Label(PhoneLabel.work),
            ),
          ],
        ),
      );
      expect(summary?.primaryPhone, '222');
    });

    test('несколько телефонов сохраняются в деталях', () {
      final details = mapContactDetails(
        Contact(
          id: '1',
          displayName: 'Анна',
          phones: [
            Phone(number: '111', label: Label(PhoneLabel.mobile)),
            Phone(number: '222', label: Label(PhoneLabel.home)),
          ],
          emails: [
            Email(address: 'a@example.com', label: Label(EmailLabel.work)),
            Email(address: 'b@example.com', label: Label(EmailLabel.home)),
          ],
        ),
      );
      expect(details.phones.map((item) => item.value), ['111', '222']);
      expect(details.phones.map((item) => item.labelKey), ['mobile', 'home']);
      expect(details.emails, hasLength(2));
      expect(details.hasEmail, isTrue);
    });

    test('контакт без телефона и без email не падает', () {
      final summary = mapContactSummary(
        Contact(id: '1', displayName: 'Без связи'),
      );
      final details = mapContactDetails(
        Contact(id: '1', displayName: 'Без связи'),
      );
      expect(summary?.primaryPhone, isNull);
      expect(summary?.hasEmail, isFalse);
      expect(details.phones, isEmpty);
      expect(details.emails, isEmpty);
    });

    test('дополнительные поля попадают в детали', () {
      final details = mapContactDetails(
        Contact(
          id: '1',
          displayName: 'Анна Смирнова',
          name: Name(first: 'Анна', last: 'Смирнова', nickname: 'Аня'),
          addresses: [
            Address(
              formatted: 'Москва, ул. Пример, 1',
              label: Label(AddressLabel.home),
            ),
          ],
          organizations: [
            Organization(name: 'Университет', jobTitle: 'Студент'),
          ],
          websites: [
            Website(url: 'https://example.com', label: Label(WebsiteLabel.homepage)),
          ],
          socialMedias: [
            SocialMedia(username: 'anna', label: Label(SocialMediaLabel.skype)),
          ],
          events: [
            Event(year: 2000, month: 4, day: 15, label: Label(EventLabel.birthday)),
          ],
          relations: [
            Relation(name: 'Иван', label: Label(RelationLabel.brother)),
          ],
          notes: [Note(note: 'Одногруппница')],
        ),
      );
      expect(details.addresses.single.value, 'Москва, ул. Пример, 1');
      expect(details.organizations.single.jobTitle, 'Студент');
      expect(details.websites.single.labelKey, 'homepage');
      expect(details.socialMedias.single.value, 'anna');
      expect(details.events.single.year, 2000);
      expect(details.relations.single.labelKey, 'brother');
      expect(details.notes, ['Одногруппница']);
      expect(details.name.nickname, 'Аня');
    });

    test('пустое имя собирается из частей', () {
      final summary = mapContactSummary(
        Contact(id: '1', name: Name(first: 'Иван', last: 'Петров')),
      );
      expect(summary?.displayName, 'Иван Петров');
    });

    test('контакт без id не попадает в список', () {
      expect(mapContactSummary(Contact(displayName: 'Без id')), isNull);
    });
  });

  group('mapPermissionStatus', () {
    test('переводит все статусы плагина', () {
      expect(
        mapPermissionStatus(PermissionStatus.granted).name,
        'granted',
      );
      expect(mapPermissionStatus(PermissionStatus.denied).name, 'denied');
      expect(
        mapPermissionStatus(PermissionStatus.permanentlyDenied).name,
        'permanentlyDenied',
      );
      expect(
        mapPermissionStatus(PermissionStatus.restricted).name,
        'restricted',
      );
      expect(mapPermissionStatus(PermissionStatus.limited).name, 'limited');
      expect(
        mapPermissionStatus(PermissionStatus.notDetermined).name,
        'notDetermined',
      );
    });
  });
}
