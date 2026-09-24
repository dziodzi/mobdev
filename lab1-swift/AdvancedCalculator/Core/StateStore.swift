import Foundation

protocol CalculatorStateStoring {
    func load() -> CalculatorState?
    func save(_ state: CalculatorState)
}

/// JSON в UserDefaults: состояние восстанавливается не только после поворота,
/// но и после обычного закрытия и повторного запуска приложения.
final class UserDefaultsStateStore: CalculatorStateStoring {
    private let defaults: UserDefaults
    private let key: String

    init(defaults: UserDefaults = .standard, key: String = "advancedCalculator.state.v1") {
        self.defaults = defaults
        self.key = key
    }

    func load() -> CalculatorState? {
        guard let data = defaults.data(forKey: key),
              let state = try? JSONDecoder().decode(CalculatorState.self, from: data),
              state.expression.count <= ExpressionEvaluator.maximumLength,
              state.memory.isFinite,
              state.lastResult?.isFinite != false,
              state.history.allSatisfy({ $0.result.isFinite && $0.expression.count <= ExpressionEvaluator.maximumLength })
        else { return nil }
        return state
    }

    func save(_ state: CalculatorState) {
        guard let data = try? JSONEncoder().encode(state) else { return }
        defaults.set(data, forKey: key)
    }
}
