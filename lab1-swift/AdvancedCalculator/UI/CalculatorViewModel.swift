import SwiftUI

@MainActor
final class CalculatorViewModel: ObservableObject {
    @Published private(set) var state: CalculatorState
    private let session: CalculatorSession

    init() {
        let session = CalculatorSession(store: UserDefaultsStateStore())
        self.session = session
        state = session.state
    }

    func send(_ action: CalculatorAction) {
        session.send(action)
        state = session.state
    }
}
