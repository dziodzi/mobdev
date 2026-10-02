import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/calculation_history_item.dart';
import '../models/calculator_persisted_state.dart';

abstract class StorageService {
  Future<void> saveExpression(String expression);
  Future<void> saveHistory(List<CalculationHistoryItem> history);
  Future<void> saveMemory(double memory);
  Future<void> saveLastResult(String lastResult);
  Future<void> saveError(String error);
  Future<void> saveJustEvaluated(bool justEvaluated);

  Future<void> saveAll(CalculatorPersistedState state);
  Future<CalculatorPersistedState> loadAll();
}

class SharedPreferencesStorageService implements StorageService {
  SharedPreferencesStorageService({SharedPreferences? preferences})
      : _preferences = preferences;

  static const _expressionKey = 'calculator.expression';
  static const _historyKey = 'calculator.history';
  static const _memoryKey = 'calculator.memory';
  static const _lastResultKey = 'calculator.lastResult';
  static const _errorKey = 'calculator.error';
  static const _justEvaluatedKey = 'calculator.justEvaluated';

  SharedPreferences? _preferences;

  Future<SharedPreferences> get _prefs async {
    return _preferences ??= await SharedPreferences.getInstance();
  }

  @override
  Future<void> saveExpression(String expression) async {
    await (await _prefs).setString(_expressionKey, expression);
  }

  @override
  Future<void> saveHistory(List<CalculationHistoryItem> history) async {
    final encoded = jsonEncode(history.map((item) => item.toJson()).toList());
    await (await _prefs).setString(_historyKey, encoded);
  }

  @override
  Future<void> saveMemory(double memory) async {
    await (await _prefs).setDouble(_memoryKey, memory);
  }

  @override
  Future<void> saveLastResult(String lastResult) async {
    await (await _prefs).setString(_lastResultKey, lastResult);
  }

  @override
  Future<void> saveError(String error) async {
    await (await _prefs).setString(_errorKey, error);
  }

  @override
  Future<void> saveJustEvaluated(bool justEvaluated) async {
    await (await _prefs).setBool(_justEvaluatedKey, justEvaluated);
  }

  @override
  Future<void> saveAll(CalculatorPersistedState state) async {
    await Future.wait([
      saveExpression(state.expression),
      saveHistory(state.history),
      saveMemory(state.memory),
      saveLastResult(state.lastResult),
      saveError(state.error),
      saveJustEvaluated(state.justEvaluated),
    ]);
  }

  @override
  Future<CalculatorPersistedState> loadAll() async {
    final prefs = await _prefs;
    final historyRaw = prefs.getString(_historyKey);
    final history = <CalculationHistoryItem>[];
    if (historyRaw != null && historyRaw.isNotEmpty) {
      try {
        final decoded = jsonDecode(historyRaw) as List<dynamic>;
        for (final item in decoded) {
          if (item is Map<String, dynamic>) {
            history.add(CalculationHistoryItem.fromJson(item));
          } else if (item is Map) {
            history.add(
              CalculationHistoryItem.fromJson(Map<String, dynamic>.from(item)),
            );
          }
        }
      } catch (_) {
        history.clear();
      }
    }

    return CalculatorPersistedState(
      expression: prefs.getString(_expressionKey) ?? '',
      lastResult: prefs.getString(_lastResultKey) ?? '',
      error: prefs.getString(_errorKey) ?? '',
      memory: prefs.getDouble(_memoryKey) ?? 0,
      history: history,
      justEvaluated: prefs.getBool(_justEvaluatedKey) ?? false,
    );
  }
}

class InMemoryStorageService implements StorageService {
  CalculatorPersistedState _state = const CalculatorPersistedState();

  @override
  Future<void> saveExpression(String expression) async {
    _state = CalculatorPersistedState(
      expression: expression,
      lastResult: _state.lastResult,
      error: _state.error,
      memory: _state.memory,
      history: _state.history,
      justEvaluated: _state.justEvaluated,
    );
  }

  @override
  Future<void> saveHistory(List<CalculationHistoryItem> history) async {
    _state = CalculatorPersistedState(
      expression: _state.expression,
      lastResult: _state.lastResult,
      error: _state.error,
      memory: _state.memory,
      history: List<CalculationHistoryItem>.from(history),
      justEvaluated: _state.justEvaluated,
    );
  }

  @override
  Future<void> saveMemory(double memory) async {
    _state = CalculatorPersistedState(
      expression: _state.expression,
      lastResult: _state.lastResult,
      error: _state.error,
      memory: memory,
      history: _state.history,
      justEvaluated: _state.justEvaluated,
    );
  }

  @override
  Future<void> saveLastResult(String lastResult) async {
    _state = CalculatorPersistedState(
      expression: _state.expression,
      lastResult: lastResult,
      error: _state.error,
      memory: _state.memory,
      history: _state.history,
      justEvaluated: _state.justEvaluated,
    );
  }

  @override
  Future<void> saveError(String error) async {
    _state = CalculatorPersistedState(
      expression: _state.expression,
      lastResult: _state.lastResult,
      error: error,
      memory: _state.memory,
      history: _state.history,
      justEvaluated: _state.justEvaluated,
    );
  }

  @override
  Future<void> saveJustEvaluated(bool justEvaluated) async {
    _state = CalculatorPersistedState(
      expression: _state.expression,
      lastResult: _state.lastResult,
      error: _state.error,
      memory: _state.memory,
      history: _state.history,
      justEvaluated: justEvaluated,
    );
  }

  @override
  Future<void> saveAll(CalculatorPersistedState state) async {
    _state = CalculatorPersistedState(
      expression: state.expression,
      lastResult: state.lastResult,
      error: state.error,
      memory: state.memory,
      history: List<CalculationHistoryItem>.from(state.history),
      justEvaluated: state.justEvaluated,
    );
  }

  @override
  Future<CalculatorPersistedState> loadAll() async => CalculatorPersistedState(
        expression: _state.expression,
        lastResult: _state.lastResult,
        error: _state.error,
        memory: _state.memory,
        history: List<CalculationHistoryItem>.from(_state.history),
        justEvaluated: _state.justEvaluated,
      );
}
