import SwiftUI

private enum Palette {
    static let background = Color(red: 0.94, green: 0.96, blue: 0.95)
    static let ink = Color(red: 0.09, green: 0.16, blue: 0.17)
    static let muted = Color(red: 0.35, green: 0.43, blue: 0.43)
    static let accent = Color(red: 0.06, green: 0.43, blue: 0.36)
    static let operatorFill = Color(red: 0.83, green: 0.90, blue: 0.87)
}

@MainActor
struct CalculatorView: View {
    @ObservedObject var model: CalculatorViewModel
    @FocusState private var expressionFocused: Bool
    private let columns = Array(repeating: GridItem(.flexible(), spacing: 9), count: 4)
    private let keys = ["C", "(", ")", "⌫", "7", "8", "9", "÷", "4", "5", "6", "×", "1", "2", "3", "−", "±", "0", ".", "+"]

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    header
                    if geometry.size.width >= 700 {
                        HStack(alignment: .top, spacing: 24) {
                            calculator.frame(maxWidth: .infinity)
                            history.frame(maxWidth: .infinity)
                        }
                    } else {
                        calculator
                        history
                    }
                    Text("% — деление на 100 · ± — знак последнего числа")
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                        .frame(maxWidth: .infinity)
                }
                .padding(20)
                .frame(maxWidth: 1050)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(Palette.background.ignoresSafeArea())
        }
        .preferredColorScheme(.light)
    }

    private var header: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 3) {
                Text("Калькулятор").font(.system(size: 29, weight: .bold, design: .rounded))
                Text("Лабораторная работа №1")
                    .font(.subheadline)
                    .foregroundStyle(Palette.muted)
            }
            Spacer()
            Image(systemName: "function")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Palette.accent)
                .padding(13)
                .background(Palette.operatorFill, in: RoundedRectangle(cornerRadius: 16))
                .accessibilityHidden(true)
        }
        .foregroundStyle(Palette.ink)
    }

    private var calculator: some View {
        VStack(spacing: 12) {
            display
            memoryBar
            LazyVGrid(columns: columns, spacing: 9) {
                ForEach(keys, id: \.self) { key in
                    keypadButton(key)
                }
            }
            HStack(spacing: 9) {
                keypadButton("%")
                    .frame(maxWidth: .infinity)
                Button { perform(.equals) } label: {
                    HStack {
                        Text("Вычислить").font(.headline)
                        Spacer()
                        Text("=").font(.system(size: 29, weight: .medium))
                    }
                    .padding(.horizontal, 22)
                    .frame(maxWidth: .infinity, minHeight: 56)
                    .foregroundStyle(.white)
                    .background(Palette.accent, in: RoundedRectangle(cornerRadius: 15))
                }
                .buttonStyle(PressStyle())
                .frame(maxWidth: .infinity)
                .layoutPriority(1)
                .accessibilityLabel("Вычислить выражение")
                .accessibilityIdentifier("equalsButton")
            }
        }
    }

    private var display: some View {
        VStack(alignment: .trailing, spacing: 12) {
            HStack {
                Text("ВЫРАЖЕНИЕ")
                    .font(.caption2.weight(.semibold))
                    .tracking(1.5)
                    .foregroundStyle(.white.opacity(0.55))
                Spacer()
                if expressionFocused {
                    Button("Готово") { expressionFocused = false }
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.white)
                }
            }
            TextField("0", text: Binding(
                get: { model.state.expression },
                set: { model.send(.setExpression($0)) }
            ), axis: .vertical)
                .font(.system(size: 29, weight: .medium, design: .rounded))
                .monospacedDigit()
                .multilineTextAlignment(.trailing)
                .foregroundStyle(.white)
                .tint(.white)
                .lineLimit(1...3)
                .keyboardType(.asciiCapable)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .focused($expressionFocused)
                .accessibilityLabel("Арифметическое выражение")
                .accessibilityIdentifier("expressionField")
            Rectangle().fill(.white.opacity(0.15)).frame(height: 1)
            if let error = model.state.errorMessage {
                Label(error, systemImage: "exclamationmark.circle")
                    .font(.subheadline)
                    .foregroundStyle(Color(red: 1, green: 0.72, blue: 0.65))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityIdentifier("errorMessage")
            } else {
                HStack(alignment: .firstTextBaseline) {
                    Text(model.state.justEvaluated ? "РЕЗУЛЬТАТ" : "ПОСЛЕДНИЙ РЕЗУЛЬТАТ")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.55))
                    Spacer(minLength: 8)
                    Text(model.state.lastResult.map(NumberText.format) ?? "—")
                        .font(.system(size: 27, weight: .semibold, design: .rounded))
                        .monospacedDigit()
                        .lineLimit(2)
                        .minimumScaleFactor(0.5)
                        .foregroundStyle(.white)
                        .textSelection(.enabled)
                        .accessibilityIdentifier("resultLabel")
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, minHeight: 153)
        .background(Palette.ink, in: RoundedRectangle(cornerRadius: 22))
    }

    private var memoryBar: some View {
        VStack(spacing: 8) {
            HStack {
                Text("ПАМЯТЬ").font(.caption2.weight(.semibold)).tracking(1)
                Spacer()
                Text(NumberText.format(model.state.memory))
                    .font(.caption.monospacedDigit())
                    .lineLimit(1)
                    .accessibilityLabel("Значение памяти: \(NumberText.format(model.state.memory))")
            }
            .foregroundStyle(Palette.muted)
            HStack(spacing: 8) {
                memoryButton("MC", label: "Очистить память", action: .memoryClear)
                memoryButton("MR", label: "Вставить значение памяти", action: .memoryRecall)
                memoryButton("M−", label: "Вычесть из памяти", action: .memorySubtract)
                memoryButton("M+", label: "Добавить в память", action: .memoryAdd)
            }
        }
        .padding(.horizontal, 3)
        .padding(.vertical, 5)
    }

    private var history: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label("История", systemImage: "clock.arrow.circlepath")
                    .font(.headline)
                Text("\(model.state.history.count)/10")
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
                Spacer()
                if !model.state.history.isEmpty {
                    Button("Очистить") { perform(.clearHistory) }
                        .font(.caption.weight(.medium))
                        .foregroundStyle(Palette.accent)
                }
            }
            .foregroundStyle(Palette.ink)
            if model.state.history.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Здесь будут ваши вычисления").font(.subheadline.weight(.medium))
                    Text("Нажмите =, чтобы сохранить результат. Коснитесь записи, чтобы вернуть выражение.")
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(.white.opacity(0.75), in: RoundedRectangle(cornerRadius: 16))
            } else {
                ForEach(model.state.history) { item in
                    Button { perform(.recallHistory(item.id)) } label: {
                        HStack(alignment: .center, spacing: 12) {
                            VStack(alignment: .leading, spacing: 5) {
                                Text(item.expression)
                                    .font(.subheadline)
                                    .foregroundStyle(Palette.muted)
                                    .multilineTextAlignment(.leading)
                                Text("= \(NumberText.format(item.result))")
                                    .font(.system(.title3, design: .rounded, weight: .semibold))
                                    .foregroundStyle(Palette.ink)
                            }
                            Spacer(minLength: 0)
                            Image(systemName: "arrow.up.left")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Palette.accent)
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(.white, in: RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(PressStyle())
                    .accessibilityLabel("Вернуть выражение \(item.expression), результат \(NumberText.format(item.result))")
                }
            }
        }
    }

    private func memoryButton(_ title: String, label: String, action: CalculatorAction) -> some View {
        Button { perform(action) } label: {
            Text(title)
                .font(.system(.subheadline, design: .rounded, weight: .semibold))
                .frame(maxWidth: .infinity, minHeight: 40)
                .foregroundStyle(Palette.accent)
                .background(Palette.operatorFill.opacity(0.6), in: RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(PressStyle())
        .accessibilityLabel(label)
    }

    private func keypadButton(_ key: String) -> some View {
        let special = ["C", "⌫", "(", ")", "±", "%", "+", "−", "×", "÷"].contains(key)
        return Button { press(key) } label: {
            Text(key)
                .font(.system(size: 24, weight: special ? .medium : .regular, design: .rounded))
                .frame(maxWidth: .infinity, minHeight: 54)
                .foregroundStyle(key == "C" ? Color(red: 0.67, green: 0.25, blue: 0.20) : Palette.ink)
                .background(special ? Palette.operatorFill : .white, in: RoundedRectangle(cornerRadius: 15))
        }
        .buttonStyle(PressStyle())
        .accessibilityLabel(key == "⌫" ? "Удалить последний символ" : key == "C" ? "Очистить ввод" : key == "±" ? "Сменить знак последнего числа" : key)
        .accessibilityIdentifier("key_\(key)")
    }

    private func press(_ key: String) {
        switch key {
        case "C": perform(.clear)
        case "⌫": perform(.backspace)
        case "±": perform(.toggleSign)
        default: perform(.input(key))
        }
    }

    private func perform(_ action: CalculatorAction) {
        expressionFocused = false
        model.send(action)
    }
}

private struct PressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.72 : 1)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.easeOut(duration: 0.10), value: configuration.isPressed)
    }
}
