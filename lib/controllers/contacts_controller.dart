import 'package:devicecontacts/controllers/contact_list_logic.dart';
import 'package:devicecontacts/models/contact_access_state.dart';
import 'package:devicecontacts/models/contact_details.dart';
import 'package:devicecontacts/models/contact_sort_order.dart';
import 'package:devicecontacts/models/contact_summary.dart';
import 'package:devicecontacts/models/contacts_error.dart';
import 'package:devicecontacts/repositories/contacts_repository.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';

/// Состояние списка контактов, поиска, сортировки и разрешений.
///
/// Контроллер создаётся один раз в корне приложения и переживает поворот
/// экрана. Поиск и сортировка работают по кэшу и не вызывают getAll().
class ContactsController extends ChangeNotifier with WidgetsBindingObserver {
  ContactsController(this._repository);

  final ContactsRepository _repository;

  List<ContactSummary> _contacts = const [];
  List<ContactSummary> _filteredContacts = const [];
  String _searchQuery = '';
  ContactSortOrder _sortOrder = ContactSortOrder.ascending;
  ContactAccessStatus _permissionStatus = ContactAccessStatus.unknown;
  bool _isLoading = true;
  bool _isLoadingDetails = false;
  ContactsErrorKind? _errorKind;
  ContactsErrorKind? _detailsError;
  bool _hasLoadedContacts = false;
  bool _lifecycleBound = false;
  bool _resumeCheckInProgress = false;
  bool _wasPaused = false;
  Future<void>? _initializeFuture;
  int _loadToken = 0;

  /// Сколько раз приложение полностью читало адресную книгу.
  int fullLoadCount = 0;

  List<ContactSummary> get contacts => _contacts;

  List<ContactSummary> get filteredContacts => _filteredContacts;

  String get searchQuery => _searchQuery;

  ContactSortOrder get sortOrder => _sortOrder;

  ContactAccessStatus get permissionStatus => _permissionStatus;

  bool get isLoading => _isLoading;

  bool get isLoadingDetails => _isLoadingDetails;

  /// Код ошибки списка. null, если ошибки нет.
  ///
  /// Экран переводит код в локализованное сообщение и не показывает его
  /// пользователю как есть.
  String? get errorMessage => _errorKind?.name;

  ContactsErrorKind? get errorKind => _errorKind;

  String? get detailsErrorMessage => _detailsError?.name;

  ContactsErrorKind? get detailsError => _detailsError;

  bool get hasLoadedContacts => _hasLoadedContacts;

  /// Количество контактов в текущем результате поиска, а не во всей книге.
  int get foundCount => _filteredContacts.length;

  bool get isLimitedAccess =>
      _permissionStatus == ContactAccessStatus.limited;

  /// Подписывается на возвращение приложения из фона и настроек.
  void bindLifecycle() {
    if (_lifecycleBound) {
      return;
    }
    WidgetsBinding.instance.addObserver(this);
    _lifecycleBound = true;
  }

  @override
  void dispose() {
    if (_lifecycleBound) {
      WidgetsBinding.instance.removeObserver(this);
      _lifecycleBound = false;
    }
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Первый запуск тоже присылает resumed. Повторно проверяем разрешение
    // только после ухода в фон, например после настроек.
    if (state == AppLifecycleState.paused) {
      _wasPaused = true;
    }
    if (state == AppLifecycleState.resumed && _wasPaused) {
      _wasPaused = false;
      onAppResumed();
    }
  }

  /// Первая проверка разрешения. Повторный вызов ничего не делает.
  Future<void> initialize() {
    return _initializeFuture ??= checkPermission();
  }

  /// Проверяет разрешение и загружает контакты, только если доступ есть.
  Future<void> checkPermission() async {
    _isLoading = true;
    _errorKind = null;
    notifyListeners();
    try {
      final status = await _repository.checkPermission();
      await _applyPermission(status);
    } catch (error) {
      debugPrint('Permission check failed: $error');
      _errorKind = ContactsErrorKind.permissionCheckFailed;
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Запрашивает чтение контактов по нажатию кнопки.
  ///
  /// Если система уже не показывает диалог, метод ничего не делает:
  /// бесконечные запросы не отправляются.
  Future<void> requestPermission() async {
    if (!_permissionStatus.canRequestPermission) {
      return;
    }
    _isLoading = true;
    _errorKind = null;
    notifyListeners();
    try {
      final status = await _repository.requestPermission();
      await _applyPermission(status);
    } catch (error) {
      debugPrint('Permission request failed: $error');
      _errorKind = ContactsErrorKind.permissionCheckFailed;
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Открывает настройки. Новое разрешение будет проверено при возврате.
  Future<void> openSettings() async {
    try {
      await _repository.openSettings();
    } catch (error) {
      debugPrint('Failed to open app settings: $error');
      _errorKind = ContactsErrorKind.permissionCheckFailed;
      notifyListeners();
    }
  }

  /// Повторная проверка после возвращения приложения на передний план.
  ///
  /// Если разрешение и кэш не изменились, адресная книга не читается снова.
  Future<void> onAppResumed() async {
    if (_resumeCheckInProgress) {
      return;
    }
    _resumeCheckInProgress = true;
    try {
      final status = await _repository.checkPermission();
      final unchangedGranted =
          status == _permissionStatus &&
          _hasLoadedContacts &&
          status.canReadContacts;
      final stillBlocked =
          status == _permissionStatus && !status.canReadContacts;
      if (unchangedGranted || stillBlocked) {
        return;
      }
      await _applyPermission(status);
    } catch (error) {
      debugPrint('Resume permission check failed: $error');
      _errorKind = ContactsErrorKind.permissionCheckFailed;
      notifyListeners();
    } finally {
      _resumeCheckInProgress = false;
    }
  }

  /// Загружает список, если доступ есть, а кэш ещё пуст.
  Future<void> loadContacts() async {
    if (!_permissionStatus.canReadContacts || _hasLoadedContacts) {
      return;
    }
    await _fetchAndCache();
  }

  /// Фильтрует уже загруженный кэш. Адресная книга не читается.
  void searchContacts(String query) {
    if (_searchQuery == query) {
      return;
    }
    _searchQuery = query;
    _applyFilterAndSort();
    notifyListeners();
  }

  /// Меняет порядок сортировки уже загруженного кэша.
  void changeSortOrder(ContactSortOrder order) {
    if (_sortOrder == order) {
      return;
    }
    _sortOrder = order;
    _applyFilterAndSort();
    notifyListeners();
  }

  /// Восстанавливает поиск и сортировку без обращения к адресной книге.
  void restoreViewState({
    required String searchQuery,
    required ContactSortOrder sortOrder,
  }) {
    if (_searchQuery == searchQuery && _sortOrder == sortOrder) {
      return;
    }
    _searchQuery = searchQuery;
    _sortOrder = sortOrder;
    _applyFilterAndSort();
    notifyListeners();
  }

  /// Загружает один контакт по id. getAll() при этом не вызывается.
  Future<ContactDetails?> getContactDetails(String id) async {
    _isLoadingDetails = true;
    _detailsError = null;
    notifyListeners();
    try {
      final details = await _repository.getDetails(id);
      if (details == null) {
        _detailsError = ContactsErrorKind.detailsNotFound;
      }
      return details;
    } catch (error) {
      debugPrint('Contact details load failed: $error');
      _detailsError = ContactsErrorKind.detailsFailed;
      return null;
    } finally {
      _isLoadingDetails = false;
      notifyListeners();
    }
  }

  /// Явное обновление: заново проверяет разрешение и, если можно, читает книгу.
  Future<void> refreshContacts() async {
    try {
      final status = await _repository.checkPermission();
      _permissionStatus = status;
      if (!status.canReadContacts) {
        _clearCache();
        _errorKind = null;
        _isLoading = false;
        notifyListeners();
        return;
      }
      await _fetchAndCache();
    } catch (error) {
      debugPrint('Contacts refresh failed: $error');
      _errorKind = ContactsErrorKind.loadFailed;
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> _applyPermission(ContactAccessStatus status) async {
    final previous = _permissionStatus;
    _permissionStatus = status;
    if (!status.canReadContacts) {
      _clearCache();
      _errorKind = null;
      _isLoading = false;
      notifyListeners();
      return;
    }

    final gainedAccess = !previous.canReadContacts;
    final scopeChanged = previous.canReadContacts && previous != status;
    if (gainedAccess || scopeChanged || !_hasLoadedContacts) {
      await _fetchAndCache();
      return;
    }

    _isLoading = false;
    notifyListeners();
  }

  Future<void> _fetchAndCache() async {
    final token = ++_loadToken;
    _isLoading = true;
    _errorKind = null;
    notifyListeners();
    try {
      fullLoadCount++;
      if (kDebugMode) {
        debugPrint('Full contacts load #$fullLoadCount');
      }
      final loaded = await _repository.getAllSummaries();
      if (token != _loadToken) {
        return;
      }
      _contacts = List.unmodifiable(loaded);
      _hasLoadedContacts = true;
      _errorKind = null;
      _applyFilterAndSort();
      if (kDebugMode) {
        debugPrint(
          'Full contacts load #$fullLoadCount finished, count=${loaded.length}',
        );
      }
    } catch (error) {
      if (token != _loadToken) {
        return;
      }
      debugPrint('Full contacts load failed: $error');
      _errorKind = ContactsErrorKind.loadFailed;
    } finally {
      if (token == _loadToken) {
        _isLoading = false;
        notifyListeners();
      }
    }
  }

  void _applyFilterAndSort() {
    _filteredContacts = List.unmodifiable(
      sortContacts(filterContacts(_contacts, _searchQuery), _sortOrder),
    );
  }

  void _clearCache() {
    _loadToken++;
    _contacts = const [];
    _filteredContacts = const [];
    _hasLoadedContacts = false;
  }
}
