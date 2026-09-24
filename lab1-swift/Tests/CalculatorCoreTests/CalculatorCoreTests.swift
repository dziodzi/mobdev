import XCTest
#if SWIFT_PACKAGE
@testable import CalculatorCore
#else
@testable import AdvancedCalculator
#endif

final class ExpressionEvaluatorTests: XCTestCase {
    private let evaluator = ExpressionEvaluator()

    func testExamplesFromAssignment() throws {
        for (expression, expected) in [("12 + 5 × 3", 27.0), ("(12 + 5) × 3", 51), ("20 ÷ 4 + 7", 12), ("15.5 × 2", 31)] {
            XCTAssertEqual(try evaluator.evaluate(expression), expected, accuracy: 1e-12, expression)
        }
    }

    func testPrecedenceAndLeftAssociativity() throws {
        for (expression, expected) in [("2+3*4-8/2", 10.0), ("20/5*2", 8), ("10-3-2", 5), ("24/4/2", 3), ("3*(2+(8-4)/2)", 12)] {
            XCTAssertEqual(try evaluator.evaluate(expression), expected, accuracy: 1e-12, expression)
        }
    }

    func testDecimalFormats() throws {
        for (expression, expected) in [(".5 + 1.", 1.5), ("1,5×2", 3), ("0.1+0.2", 0.3), ("1e3+2E-2", 1000.02), ("1e+2", 100)] {
            XCTAssertEqual(try evaluator.evaluate(expression), expected, accuracy: 1e-12, expression)
        }
    }

    func testUnarySigns() throws {
        for (expression, expected) in [("-5+2", -3.0), ("−5+2", -3), ("2×(-3)", -6), ("-(2+3)", -5), ("+4", 4), ("(-(-3))", 3)] {
            XCTAssertEqual(try evaluator.evaluate(expression), expected, accuracy: 1e-12, expression)
        }
    }

    func testPercentIsPostfixDivisionBy100() throws {
        for (expression, expected) in [("50%", 0.5), ("200×10%", 20), ("200+10%", 200.1), ("(20+30)%", 0.5), ("50%%", 0.005), ("-10%", -0.1)] {
            XCTAssertEqual(try evaluator.evaluate(expression), expected, accuracy: 1e-12, expression)
        }
    }

    func testWhitespace() throws {
        XCTAssertEqual(try evaluator.evaluate(" \n ( 12 + 5 ) × 3 \t"), 51)
    }

    func testDivisionByZero() {
        for expression in ["1/0", "1÷(2-2)", "0/0", "1/(-0)"] {
            assertError(expression, .divisionByZero)
        }
    }

    func testParenthesesErrors() {
        for expression in ["(1+2", "1+2)", ")(1+2", "(()", "1)(2"] {
            assertError(expression, .mismatchedParentheses)
        }
        assertError("()", .emptyParentheses)
    }

    func testConsecutiveOperators() {
        for expression in ["2++3", "2+*3", "2×÷3", "2*-3", "2--3", "--2", "2 + + 3"] {
            assertError(expression, .consecutiveOperators)
        }
    }

    func testInvalidNumbers() {
        for expression in ["1..2", "1,2.3", ".", "1e", "1e+", "1e2.3"] {
            assertError(expression, .invalidNumber)
        }
    }

    func testEmptyExpression() {
        assertError("", .emptyExpression)
        assertError(" \n \t", .emptyExpression)
    }

    func testTrailingOperator() {
        for expression in ["1+", "3×", "2/", "7−"] { assertError(expression, .trailingOperator) }
    }

    func testMissingOperator() {
        for expression in ["2(3)", "(2)(3)", "1 2", "(1 2)", "1%2"] {
            assertError(expression, .missingOperator)
        }
    }

    func testInvalidCharacters() {
        assertError("2a+3", .unexpectedSymbol("a"))
        assertError("2^3", .unexpectedSymbol("^"))
    }

    func testOverflow() {
        assertError("1e309", .numberOutOfRange)
        assertError("1e308×10", .numberOutOfRange)
    }

    func testLengthLimit() {
        assertError(String(repeating: "1", count: 1001), .expressionTooLong)
    }

    func testNestingLimit() {
        assertError(String(repeating: "(", count: 65) + "1" + String(repeating: ")", count: 65), .nestingTooDeep)
    }

    func testMaximumSupportedNesting() throws {
        let text = String(repeating: "(", count: 64) + "1" + String(repeating: ")", count: 64)
        XCTAssertEqual(try evaluator.evaluate(text), 1)
    }

    func testZeroAndScientificFormattingCanBeEvaluatedAgain() throws {
        XCTAssertEqual(NumberText.format(-0.0), "0")
        for value in [1e20, 1e-20, -1e30, 0.125, -3.0] {
            XCTAssertEqual(try evaluator.evaluate(NumberText.format(value)), value, accuracy: abs(value) * 1e-14)
        }
    }

    func testDeterministicGeneratedExpressions() throws {
        for a in 1...10 {
            for b in 1...10 {
                let expression = "\(a)+\(b)*3-(\(a)+\(b))/2"
                let expected = Double(a) + Double(b) * 3 - Double(a + b) / 2
                XCTAssertEqual(try evaluator.evaluate(expression), expected, accuracy: 1e-12)
            }
        }
    }

    func testMalformedInputNeverCrashes() {
        let alphabet = Array("0123.+−×÷()%abc")
        var seed: UInt64 = 27
        for _ in 0..<1000 {
            var text = ""
            for _ in 0..<24 {
                seed = seed &* 6364136223846793005 &+ 1
                text.append(alphabet[Int((seed >> 32) % UInt64(alphabet.count))])
            }
            do {
                let value = try evaluator.evaluate(text)
                XCTAssertTrue(value.isFinite)
            }
            catch { XCTAssertTrue(error is CalculatorError) }
        }
    }

    private func assertError(_ expression: String, _ expected: CalculatorError, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertThrowsError(try evaluator.evaluate(expression), expression, file: file, line: line) { error in
            XCTAssertEqual(error as? CalculatorError, expected, expression, file: file, line: line)
        }
    }
}

private final class MemoryStore: CalculatorStateStoring {
    var saved: CalculatorState?
    func load() -> CalculatorState? { saved }
    func save(_ state: CalculatorState) { saved = state }
}

final class CalculatorSessionTests: XCTestCase {
    private func session() -> CalculatorSession { CalculatorSession(store: MemoryStore()) }
    private func enter(_ text: String, in session: CalculatorSession) { session.send(.setExpression(text)) }
    private func compute(_ text: String, in session: CalculatorSession) {
        enter(text, in: session)
        session.send(.equals)
    }

    func testEqualsUpdatesHistoryAndResult() {
        let model = session()
        compute("12+5×3", in: model)
        XCTAssertEqual(model.state.lastResult, 27)
        XCTAssertEqual(model.state.history.first?.expression, "12+5×3")
        XCTAssertEqual(model.state.history.first?.result, 27)
        XCTAssertTrue(model.state.justEvaluated)
    }

    func testHistoryKeepsLatestTen() {
        let model = session()
        for value in 1...12 { compute("\(value)+1", in: model) }
        XCTAssertEqual(model.state.history.count, 10)
        XCTAssertEqual(model.state.history.first?.expression, "12+1")
        XCTAssertEqual(model.state.history.last?.expression, "3+1")
    }

    func testHistoryRecallIsEditable() throws {
        let model = session()
        compute("2+3", in: model)
        let item = try XCTUnwrap(model.state.history.first)
        compute("9+1", in: model)
        model.send(.recallHistory(item.id))
        XCTAssertEqual(model.state.expression, "2+3")
        XCTAssertFalse(model.state.justEvaluated)
        model.send(.input("×2"))
        model.send(.equals)
        XCTAssertEqual(model.state.lastResult, 8)
    }

    func testFailureDoesNotPolluteHistory() {
        let model = session()
        compute("1+1", in: model)
        compute("1/0", in: model)
        XCTAssertEqual(model.state.history.count, 1)
        XCTAssertNotNil(model.state.errorMessage)
        XCTAssertEqual(model.state.lastResult, 2)
    }

    func testClearDoesNotEraseMemoryOrHistory() {
        let model = session()
        compute("7", in: model)
        model.send(.memoryAdd)
        model.send(.clear)
        XCTAssertEqual(model.state.expression, "")
        XCTAssertNil(model.state.lastResult)
        XCTAssertNil(model.state.errorMessage)
        XCTAssertEqual(model.state.memory, 7)
        XCTAssertEqual(model.state.history.count, 1)
    }

    func testBackspaceIsSafeAndClearsError() {
        let model = session()
        model.send(.backspace)
        XCTAssertEqual(model.state.expression, "")
        compute("2++3", in: model)
        model.send(.backspace)
        XCTAssertEqual(model.state.expression, "2++")
        XCTAssertNil(model.state.errorMessage)
    }

    func testInputAfterResultStartsNewExpression() {
        let model = session()
        compute("2+3", in: model)
        model.send(.input("7"))
        XCTAssertEqual(model.state.expression, "7")
        XCTAssertFalse(model.state.justEvaluated)
    }

    func testOperatorAfterResultContinuesCalculation() {
        let model = session()
        compute("2+3", in: model)
        model.send(.input("×"))
        model.send(.input("4"))
        model.send(.equals)
        XCTAssertEqual(model.state.lastResult, 20)
    }

    func testMemoryOperations() {
        let model = session()
        enter("10+5", in: model)
        model.send(.memoryAdd)
        XCTAssertEqual(model.state.memory, 15)
        enter("3", in: model)
        model.send(.memorySubtract)
        XCTAssertEqual(model.state.memory, 12)
        model.send(.memoryRecall)
        XCTAssertEqual(model.state.expression, "12")
        model.send(.memoryClear)
        XCTAssertEqual(model.state.memory, 0)
    }

    func testMemoryUsesExactResult() {
        let model = session()
        compute("1/3", in: model)
        model.send(.memoryAdd)
        XCTAssertEqual(model.state.memory, 1.0 / 3.0)
    }

    func testContinuationPreservesFullPrecision() {
        let model = session()
        compute("1/3", in: model)
        model.send(.input("×"))
        model.send(.input("3"))
        model.send(.equals)
        XCTAssertEqual(model.state.lastResult, 1)
    }

    func testMemoryRecallPreservesFullPrecision() throws {
        let model = session()
        compute("1/3", in: model)
        model.send(.memoryAdd)
        model.send(.memoryRecall)
        XCTAssertEqual(try ExpressionEvaluator().evaluate(model.state.expression), 1.0 / 3.0)
    }

    func testRecallNegativeMemoryAfterOperator() {
        let model = session()
        enter("-3", in: model)
        model.send(.memoryAdd)
        enter("2×", in: model)
        model.send(.memoryRecall)
        XCTAssertEqual(model.state.expression, "2×(-3)")
        model.send(.equals)
        XCTAssertEqual(model.state.lastResult, -6)
    }

    func testRecallZeroMemory() {
        let model = session()
        enter("2+", in: model)
        model.send(.memoryRecall)
        XCTAssertEqual(model.state.expression, "2+0")
    }

    func testInvalidExpressionDoesNotChangeMemory() {
        let model = session()
        enter("8", in: model)
        model.send(.memoryAdd)
        enter("1/0", in: model)
        model.send(.memorySubtract)
        XCTAssertEqual(model.state.memory, 8)
        XCTAssertNotNil(model.state.errorMessage)
    }

    func testMemoryOverflowIsHandled() {
        let model = session()
        enter("1e308", in: model)
        model.send(.memoryAdd)
        model.send(.memoryAdd)
        XCTAssertEqual(model.state.memory, 1e308)
        XCTAssertNotNil(model.state.errorMessage)
    }

    func testToggleLastNumber() throws {
        let model = session()
        enter("12+5", in: model)
        model.send(.toggleSign)
        XCTAssertEqual(try ExpressionEvaluator().evaluate(model.state.expression), 7)
        model.send(.toggleSign)
        XCTAssertEqual(model.state.expression, "12+5")
    }

    func testToggleLastGroup() throws {
        let model = session()
        enter("2×(3+4)", in: model)
        model.send(.toggleSign)
        XCTAssertEqual(try ExpressionEvaluator().evaluate(model.state.expression), -14)
        model.send(.toggleSign)
        XCTAssertEqual(model.state.expression, "2×(3+4)")
    }

    func testToggleNegativeNumber() {
        let model = session()
        enter("−5", in: model)
        model.send(.toggleSign)
        XCTAssertEqual(model.state.expression, "5")
    }

    func testToggleEmptyInputTwice() {
        let model = session()
        model.send(.toggleSign)
        XCTAssertEqual(model.state.expression, "−")
        model.send(.toggleSign)
        XCTAssertEqual(model.state.expression, "")
    }

    func testToggleAfterOperatorDoesNotCreateInvalidGroup() {
        let model = session()
        enter("2+", in: model)
        model.send(.toggleSign)
        XCTAssertEqual(model.state.expression, "2+")
    }

    func testTogglePercentAndExponent() throws {
        let model = session()
        for expression in ["2×10%", "2×1e-3"] {
            enter(expression, in: model)
            let original = try ExpressionEvaluator().evaluate(expression)
            model.send(.toggleSign)
            XCTAssertEqual(try ExpressionEvaluator().evaluate(model.state.expression), -original)
        }
    }

    func testToggleResult() {
        let model = session()
        compute("2+3", in: model)
        model.send(.toggleSign)
        model.send(.equals)
        XCTAssertEqual(model.state.lastResult, -5)
    }

    func testClearHistoryDoesNotChangeInput() {
        let model = session()
        compute("4+4", in: model)
        model.send(.clearHistory)
        XCTAssertTrue(model.state.history.isEmpty)
        XCTAssertEqual(model.state.expression, "4+4")
    }

    func testEveryChangeIsPersisted() {
        let store = MemoryStore()
        let model = CalculatorSession(store: store)
        compute("2+3", in: model)
        model.send(.memoryAdd)
        enter("9/0", in: model)
        model.send(.equals)
        let restored = CalculatorSession(store: store)
        XCTAssertEqual(restored.state, model.state)
        XCTAssertEqual(restored.state.expression, "9/0")
        XCTAssertEqual(restored.state.memory, 5)
        XCTAssertEqual(restored.state.lastResult, 5)
        XCTAssertEqual(restored.state.history.count, 1)
        XCTAssertNotNil(restored.state.errorMessage)
    }

    func testResultContinuationIsRestored() {
        let store = MemoryStore()
        let model = CalculatorSession(store: store)
        compute("2+3", in: model)
        let restored = CalculatorSession(store: store)
        restored.send(.input("+"))
        XCTAssertEqual(restored.state.expression, "5+")
    }

    func testLengthLimitDoesNotDestroyCurrentInput() {
        let model = session()
        enter("123", in: model)
        enter(String(repeating: "1", count: 1001), in: model)
        XCTAssertEqual(model.state.expression, "123")
        XCTAssertNotNil(model.state.errorMessage)
    }

    func testUserDefaultsRoundTripAndCorruptData() throws {
        let suite = "CalculatorTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let store = UserDefaultsStateStore(defaults: defaults, key: "test")
        var state = CalculatorState()
        state.expression = "2÷0"
        state.memory = -3.5
        state.lastResult = 27
        state.history = [HistoryEntry(expression: "12+5×3", result: 27)]
        state.errorMessage = "Деление на ноль невозможно."
        store.save(state)
        XCTAssertEqual(store.load(), state)
        defaults.set(Data("not-json".utf8), forKey: "test")
        XCTAssertNil(store.load())
    }
}
