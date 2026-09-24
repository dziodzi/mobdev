import Foundation

struct HistoryEntry: Codable, Equatable, Identifiable {
    let id: UUID
    let expression: String
    let result: Double

    init(expression: String, result: Double) {
        id = UUID()
        self.expression = expression
        self.result = result
    }
}

struct CalculatorState: Codable, Equatable {
    var expression = ""
    var history: [HistoryEntry] = []
    var memory: Double = 0
    var lastResult: Double?
    var errorMessage: String?
    // Нужно восстановить также поведение следующей кнопки после результата.
    var justEvaluated = false
}

enum CalculatorAction {
    case input(String)
    case setExpression(String)
    case equals, clear, backspace, toggleSign
    case memoryAdd, memorySubtract, memoryRecall, memoryClear
    case recallHistory(UUID)
    case clearHistory
}

/// Чистая бизнес-логика: не импортирует SwiftUI/Combine и тестируется через Swift Package.
final class CalculatorSession {
    private(set) var state: CalculatorState
    private let evaluator = ExpressionEvaluator()
    private let store: CalculatorStateStoring

    init(store: CalculatorStateStoring) {
        self.store = store
        state = store.load() ?? CalculatorState()
        state.history = Array(state.history.prefix(10))
    }

    func send(_ action: CalculatorAction) {
        defer { store.save(state) }
        switch action {
        case .setExpression(let expression):
            setExpression(expression)
        case .input(let text):
            var expression = state.expression
            if state.justEvaluated {
                let continuesResult = ["+", "−", "-", "×", "*", "÷", "/", "%"].contains(text)
                expression = continuesResult ? state.lastResult.map(NumberText.literal) ?? "" : ""
            }
            setExpression(expression + text)
        case .equals:
            do {
                let result = try evaluator.evaluate(state.expression)
                state.lastResult = result
                state.errorMessage = nil
                state.justEvaluated = true
                state.history.insert(HistoryEntry(expression: state.expression, result: result), at: 0)
                state.history = Array(state.history.prefix(10))
            } catch { report(error) }
        case .clear:
            state.expression = ""
            state.lastResult = nil
            state.errorMessage = nil
            state.justEvaluated = false
        case .backspace:
            if !state.expression.isEmpty { state.expression.removeLast() }
            state.errorMessage = nil
            state.justEvaluated = false
        case .toggleSign:
            let base = state.justEvaluated ? state.lastResult.map(NumberText.literal) ?? "" : state.expression
            setExpression(SignEditor.toggleLastOperand(in: base))
        case .memoryAdd:
            changeMemory(subtract: false)
        case .memorySubtract:
            changeMemory(subtract: true)
        case .memoryRecall:
            // MR подставляет память в конец после оператора/открывающей скобки.
            // Если там уже число, заменяет текущий ввод, не создавая неявное умножение.
            let value = NumberText.literal(state.memory)
            let trimmed = state.expression.trimmingCharacters(in: .whitespacesAndNewlines)
            if !state.justEvaluated, let last = trimmed.last, "+−-×*÷/(".contains(last) {
                let operand = state.memory < 0 ? "(\(value))" : value
                setExpression(trimmed + operand)
            } else {
                setExpression(value)
            }
        case .memoryClear:
            state.memory = 0
        case .recallHistory(let id):
            guard let item = state.history.first(where: { $0.id == id }) else { return }
            setExpression(item.expression)
            state.lastResult = item.result
        case .clearHistory:
            state.history.removeAll()
        }
    }

    private func setExpression(_ expression: String) {
        // Длинный ручной ввод не обрезается молча и не может привести к падению парсера.
        guard expression.count <= ExpressionEvaluator.maximumLength else {
            state.errorMessage = CalculatorError.expressionTooLong.localizedDescription
            return
        }
        state.expression = expression
        state.errorMessage = nil
        state.justEvaluated = false
    }

    private func changeMemory(subtract: Bool) {
        do {
            let value: Double
            if state.justEvaluated, let result = state.lastResult { value = result }
            else if state.expression.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                value = state.lastResult ?? 0
            } else { value = try evaluator.evaluate(state.expression) }
            let next = subtract ? state.memory - value : state.memory + value
            guard next.isFinite else { throw CalculatorError.numberOutOfRange }
            state.memory = next
            state.errorMessage = nil
        } catch { report(error) }
    }

    private func report(_ error: Error) {
        state.errorMessage = error.localizedDescription
        state.justEvaluated = false
    }
}

private enum SignEditor {
    /// Меняет знак последнего числа или завершённой группы, не всего выражения.
    static func toggleLastOperand(in expression: String) -> String {
        let text = expression.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return "−" }
        if text == "−" || text == "-" { return "" }
        let characters = Array(text)
        var end = characters.count
        while end > 0 && characters[end - 1] == "%" { end -= 1 }
        guard end > 0 else { return text }
        let start: Int
        if characters[end - 1] == ")" {
            var depth = 0
            var found: Int?
            for index in stride(from: end - 1, through: 0, by: -1) {
                if characters[index] == ")" { depth += 1 }
                if characters[index] == "(" { depth -= 1 }
                if depth == 0 { found = index; break }
            }
            guard let found else { return text }
            start = found
        } else {
            let head = String(characters[..<end])
            let pattern = #"(?:[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+)(?:[eE][+\-]?[0-9]+)?$"#
            guard let range = head.range(of: pattern, options: .regularExpression) else {
                // Сначала вводится число, затем меняется его знак.
                return text
            }
            start = head.distance(from: head.startIndex, to: range.lowerBound)
        }
        var prefix = String(characters[..<start])
        let operand = String(characters[start...])
        // Простой начальный знак: −5 → 5, (−5 → (5.
        if let last = prefix.last, last == "−" || last == "-" {
            let before = String(prefix.dropLast()).trimmingCharacters(in: .whitespaces)
            if before.isEmpty || before.last == "(" {
                prefix.removeLast()
                return prefix + operand
            }
        }
        // Обратное преобразование для скобок, созданных этой же кнопкой.
        if operand.hasPrefix("(-("), operand.hasSuffix("))") {
            let innerGroup = String(operand.dropFirst(2).dropLast())
            if isSingleGroup(innerGroup) {
                return prefix + String(operand.dropFirst(3).dropLast(2))
            }
        }
        // Для любого атома обёртка сохраняет приоритет и не порождает соседних операторов.
        return prefix + "(-(\(operand)))"
    }

    private static func isSingleGroup(_ text: String) -> Bool {
        let characters = Array(text)
        guard characters.first == "(", characters.last == ")" else { return false }
        var depth = 0
        for (index, character) in characters.enumerated() {
            if character == "(" { depth += 1 }
            if character == ")" { depth -= 1 }
            if depth < 0 || (depth == 0 && index < characters.count - 1) { return false }
        }
        return depth == 0
    }
}
