import SwiftUI

@main
struct ContactsApp: App {
    @State private var store = ContactsStore(service: SystemContactsService())

    var body: some Scene {
        WindowGroup {
            ContactsRootView(store: store)
        }
    }
}
