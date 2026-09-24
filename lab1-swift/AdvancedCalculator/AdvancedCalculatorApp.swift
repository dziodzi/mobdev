import SwiftUI

@main
@MainActor
struct AdvancedCalculatorApp: App {
    // Владение моделью выше адаптивного интерфейса: поворот не создаёт новое состояние.
    @StateObject private var model = CalculatorViewModel()

    var body: some Scene {
        WindowGroup {
            CalculatorView(model: model)
        }
    }
}
