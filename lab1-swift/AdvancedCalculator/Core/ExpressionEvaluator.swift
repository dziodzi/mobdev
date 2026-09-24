import Foundation

/// Ошибки вычислителя не зависят от SwiftUI и могут проверяться отдельно.
enum CalculatorError: Error, Equatable, LocalizedError {
    case emptyExpression
    case divisionByZero
    case mismatchedParentheses
    case emptyParentheses
    case consecutiveOperators
    case invalidNumber
    case trailingOperator
    case missingOperator
    case unexpectedSymbol(String)
    case invalidExpression
    case numberOutOfRange
    case expressionTooLong
    case nestingTooDeep

    var errorDescription: String? {
        switch self {
        case .emptyExpression: return "Введите выражение."
        case .divisionByZero: return "Деление на ноль невозможно."
        case .mismatchedParentheses: return "Проверьте количество и порядок скобок."
        case .emptyParentheses: return "Внутри скобок должно быть выражение."
        case .consecutiveOperators: return "Два арифметических оператора подряд недопустимы."
        case .invalidNumber: return "Некорректное число: проверьте десятичные точки."
        case .trailingOperator: return "Выражение не может заканчиваться оператором."
        case .missingOperator: return "Между числами или скобками нужен оператор."
        case .unexpectedSymbol(let symbol): return "Недопустимый символ: \(symbol)."
        case .invalidExpression: return "Проверьте выражение: не хватает числа или оператора."
        case .numberOutOfRange: return "Число выходит за допустимый диапазон."
        case .expressionTooLong: return "Выражение слишком длинное. Максимум — 1000 символов."
        case .nestingTooDeep: return "Слишком много вложенных скобок. Максимум — 64 уровня."
        }
    }
}

struct ExpressionEvaluator {
    static let maximumLength = 1000

    func evaluate(_ expression: String) throws -> Double {
        guard expression.count <= Self.maximumLength else {
            throw CalculatorError.expressionTooLong
        }
        let tokens = try Lexer.tokenize(expression)
        guard !tokens.isEmpty else { throw CalculatorError.emptyExpression }
        // Проверка скобок до разбора даёт понятную ошибку даже для незаконченного ввода.
        var balance = 0
        for token in tokens {
            if token == .left { balance += 1 }
            if token == .right { balance -= 1 }
            guard balance >= 0 else { throw CalculatorError.mismatchedParentheses }
            guard balance <= 64 else { throw CalculatorError.nestingTooDeep }
        }
        guard balance == 0 else { throw CalculatorError.mismatchedParentheses }
        if let last = tokens.last, last.isBinaryOperator {
            throw CalculatorError.trailingOperator
        }
        var parser = Parser(tokens: tokens)
        let value = try parser.expression()
        guard parser.isAtEnd else { throw CalculatorError.missingOperator }
        return try checked(value)
    }
}

/// Единый формат для дисплея, памяти и истории. Точка не зависит от языка устройства.
enum NumberText {
    static func format(_ value: Double) -> String {
        guard value != 0 else { return "0" } // Не показывать −0.
        return String(format: "%.15g", locale: Locale(identifier: "en_US_POSIX"), value)
    }

    /// Для нового выражения используем round-trip запись, а не округлённый дисплей.
    static func literal(_ value: Double) -> String {
        guard value != 0 else { return "0" }
        let text = String(value)
        return text.hasSuffix(".0") ? String(text.dropLast(2)) : text
    }
}

private func checked(_ value: Double) throws -> Double {
    guard value.isFinite else { throw CalculatorError.numberOutOfRange }
    return value
}

private enum Token: Equatable {
    case number(Double)
    case plus, minus, multiply, divide, left, right, percent

    var isBinaryOperator: Bool {
        switch self {
        case .plus, .minus, .multiply, .divide: return true
        default: return false
        }
    }
}

private enum Lexer {
    static func tokenize(_ source: String) throws -> [Token] {
        let characters = Array(source)
        var tokens: [Token] = []
        var index = 0
        func isDigit(_ character: Character) -> Bool {
            character >= "0" && character <= "9"
        }
        while index < characters.count {
            let character = characters[index]
            if character.isWhitespace { index += 1; continue }
            if isDigit(character) || character == "." || character == "," {
                let start = index
                var digits = 0
                var dots = 0
                while index < characters.count {
                    let current = characters[index]
                    if isDigit(current) { digits += 1 }
                    else if current == "." || current == "," { dots += 1 }
                    else { break }
                    index += 1
                }
                guard digits > 0 && dots <= 1 else { throw CalculatorError.invalidNumber }
                // Научная запись нужна для повторного использования очень больших/малых результатов.
                if index < characters.count && (characters[index] == "e" || characters[index] == "E") {
                    index += 1
                    if index < characters.count && (characters[index] == "+" || characters[index] == "-") {
                        index += 1
                    }
                    let exponentStart = index
                    while index < characters.count && isDigit(characters[index]) { index += 1 }
                    guard index > exponentStart else { throw CalculatorError.invalidNumber }
                    if index < characters.count && (characters[index] == "." || characters[index] == ",") {
                        throw CalculatorError.invalidNumber
                    }
                }
                let text = String(characters[start..<index]).replacingOccurrences(of: ",", with: ".")
                guard let value = Double(text) else { throw CalculatorError.invalidNumber }
                tokens.append(.number(try checked(value)))
                continue
            }
            switch character {
            case "+": tokens.append(.plus)
            case "-", "−": tokens.append(.minus)
            case "*", "×": tokens.append(.multiply)
            case "/", "÷": tokens.append(.divide)
            case "(": tokens.append(.left)
            case ")": tokens.append(.right)
            case "%": tokens.append(.percent)
            default: throw CalculatorError.unexpectedSymbol(String(character))
            }
            index += 1
        }
        return tokens
    }
}

/// Рекурсивный спуск: expression → term → primary.
/// Умножение и деление выполняются раньше сложения; операции одного уровня — слева направо.
private struct Parser {
    let tokens: [Token]
    var index = 0
    var isAtEnd: Bool { index == tokens.count }
    var current: Token? { isAtEnd ? nil : tokens[index] }

    mutating func expression() throws -> Double {
        var value = try term()
        while current == .plus || current == .minus {
            let operation = tokens[index]
            index += 1
            let right = try term()
            value = try checked(operation == .plus ? value + right : value - right)
        }
        return value
    }

    mutating func term() throws -> Double {
        var value = try primary()
        while current == .multiply || current == .divide {
            let operation = tokens[index]
            index += 1
            let right = try primary()
            if operation == .divide && right == 0 { throw CalculatorError.divisionByZero }
            value = try checked(operation == .multiply ? value * right : value / right)
        }
        return value
    }

    mutating func primary() throws -> Double {
        var sign = 1.0
        if current == .minus || current == .plus {
            // Отрицательные числа разрешены в начале или внутри скобок: 2×(-3).
            // Ввод 2×-3 отклоняется, как два арифметических оператора подряд.
            guard index == 0 || tokens[index - 1] == .left else {
                throw CalculatorError.consecutiveOperators
            }
            sign = current == .minus ? -1 : 1
            index += 1
            if let token = current, token.isBinaryOperator { throw CalculatorError.consecutiveOperators }
        }
        let value: Double
        switch current {
        case .number(let number):
            value = number
            index += 1
        case .left:
            index += 1
            if current == .right { throw CalculatorError.emptyParentheses }
            value = try expression()
            guard current == .right else { throw CalculatorError.missingOperator }
            index += 1
        case .some(let token) where token.isBinaryOperator:
            throw CalculatorError.consecutiveOperators
        default:
            throw CalculatorError.invalidExpression
        }
        var result = sign * value
        // Процент — постфиксное деление на 100, в том числе для выражения в скобках.
        while current == .percent {
            result /= 100
            index += 1
        }
        return try checked(result)
    }
}
