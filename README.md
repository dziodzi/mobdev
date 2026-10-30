# Лабораторная работа №2 — «Контакты устройства»

Мобильное приложение «Контакты» читает настоящую адресную книгу устройства, показывает список, ищет по имени и открывает подробности выбранного контакта. Демонстрационный список контактов не используется.

## Цель

Научиться получать данные из системной адресной книги, правильно запрашивать разрешение только на чтение, не загружать книгу повторно без причины и сохранять поиск с сортировкой при повороте экрана.

## Технологический стек

- Flutter и Dart
- Material 3
- `flutter_contacts` `^2.6.0`
- `flutter_localizations` и `intl`
- `ChangeNotifier` для состояния

Основная платформа проверки — Android Emulator. Идентификатор приложения: `ru.student.devicecontacts.devicecontacts`.

## Основные возможности

1. Просмотр контактов устройства.
2. Поиск по имени без учёта регистра, с частичным совпадением.
3. Сортировка по имени: А → Я и Я → А.
4. Счётчик найденных контактов.
5. Экран подробностей выбранного контакта.
6. Полный цикл разрешений: проверка, запрос, отказ, настройки, возврат в приложение.
7. Сохранение поиска, сортировки и кэша при повороте.

Приложение не создаёт, не изменяет и не удаляет контакты. Запрашивается только `PermissionType.read`.

## Архитектура

```text
main.dart
    ↓
ContactsApp (контроллер создаётся один раз)
    ↓
ContactsScreen / ContactDetailsScreen
    ↓
ContactsController
    ↓
ContactsRepository
    ↓
FlutterContacts
    ↓
Системная адресная книга
```

Экран только показывает состояние. Контроллер решает, когда проверять разрешение, читать книгу, фильтровать и сортировать. Репозиторий скрывает вызовы плагина. В тестах вместо устройства подставляется `FakeContactsRepository`.

## Структура каталогов

```text
lib/
├── main.dart
├── app.dart
├── models/
│   ├── contact_summary.dart
│   ├── contact_details.dart
│   ├── contact_access_state.dart
│   ├── contact_sort_order.dart
│   └── contacts_error.dart
├── repositories/
│   ├── contacts_repository.dart
│   ├── device_contacts_repository.dart
│   └── contact_mapper.dart
├── controllers/
│   ├── contacts_controller.dart
│   ├── contacts_scope.dart
│   └── contact_list_logic.dart
├── screens/
│   ├── contacts_screen.dart
│   └── contact_details_screen.dart
├── widgets/
│   ├── contact_list_tile.dart
│   ├── contacts_search_bar.dart
│   ├── sorting_control.dart
│   ├── permission_view.dart
│   ├── empty_state_view.dart
│   ├── error_view.dart
│   └── loading_view.dart
├── utils/
│   └── contact_labels.dart
└── l10n/
    ├── app_ru.arb
    ├── app_en.arb
    └── сгенерированные app_localizations*.dart
```

## Используемые библиотеки

| Библиотека | Зачем |
| --- | --- |
| `flutter_contacts` 2.6.0 | Чтение адресной книги и разрешений |
| `flutter_localizations` | Делегаты локализации Material/Cupertino |
| `intl` | Формат дат событий и генерация строк |
| `cupertino_icons` | Стандартные иконки шаблона Flutter |

## Разрешения Android

В `android/app/src/main/AndroidManifest.xml` внутри `manifest`, но вне `application`, объявлено только:

```xml
<uses-permission
    android:name="android.permission.READ_CONTACTS" />
```

`WRITE_CONTACTS` не добавляется. Название приложения берётся из `android/app/src/main/res/values/strings.xml` (`Контакты`) и `values-en` (`Contacts`).

На iOS в `ios/Runner/Info.plist` указан `NSContactsUsageDescription`. Локализованные тексты системного запроса лежат в `ios/Runner/ru.lproj/InfoPlist.strings` и `ios/Runner/en.lproj/InfoPlist.strings`.

## Работа с контактами

1. При запуске `ContactsController.initialize()` вызывает `checkPermission()`.
2. Проверка идёт через `FlutterContacts.permissions.check(PermissionType.read)`.
3. Если статус `granted` или `limited`, один раз вызывается `FlutterContacts.getAll()` только с именем, телефонами и email.
4. Если доступа нет, `getAll()` не вызывается.
5. Кнопка «Разрешить доступ» вызывает `request(PermissionType.read)`.
6. При `permanentlyDenied` и `restricted` системный запрос больше не показывается, доступна кнопка «Открыть настройки».
7. `ContactsController` подписан на `WidgetsBindingObserver`. В `AppLifecycleState.resumed` разрешение проверяется снова. Если оно не изменилось и кэш уже есть, повторного чтения нет.
8. Подробности одного контакта читаются через `FlutterContacts.get(id)`.

## Поиск и сортировка

Поиск и сортировка вынесены в чистые функции `filterContacts` и `sortContacts` в `lib/controllers/contact_list_logic.dart`.

- Поиск ищет подстроку в имени, без учёта регистра. «ё» и «е» считаются одинаковыми.
- Пустой запрос возвращает весь кэш.
- Сортировка по возрастанию и убыванию. Одинаковые имена дополнительно сравниваются по `id`, поэтому порядок стабильный.
- Счётчик «Найдено контактов» считает текущий результат поиска, а не всю книгу.

Изменение текста поиска не вызывает `getAll()`.

## Кэширование

Список хранится в полях `contacts` контроллера. Полное чтение выполняется только когда:

- разрешение только что получено;
- кэша ещё нет;
- сменилась область доступа (`granted` ↔ `limited`);
- пользователь явно нажал обновление.

В отладочной сборке в лог пишется номер полной загрузки и количество контактов. Имена, телефоны и email в лог не попадают.

## Сохранение состояния при повороте

`ContactsController` создаётся в `initState()` корневого `ContactsApp` и не пересоздаётся в `build()`. Поворот меняет ограничения вёрстки, но не уничтожает этот `State`, поэтому остаются:

- поисковый запрос;
- выбранная сортировка;
- загруженный список;
- статус разрешения.

Дополнительно `ContactsScreen` использует `RestorationMixin`: запрос и индекс сортировки регистрируются как `RestorableString` и `RestorableInt`. Ориентация не блокируется. Адресная книга не копируется в SharedPreferences.

## Как запустить

Нужны Flutter SDK, Android SDK и запущенный эмулятор или устройство. В адресной книге должны быть контакты: приложение их не создаёт.

```bash
flutter pub get
flutter gen-l10n
flutter run
```

При первом запуске нажмите «Разрешить доступ» и подтвердите чтение контактов в системном окне.

## Как запустить тесты

```bash
flutter analyze
flutter test
```

Тесты используют `FakeContactsRepository` и модели `flutter_contacts`. К реальной адресной книге они не обращаются.

Сборка отладочного APK:

```bash
flutter build apk --debug
```

