import SwiftUI

@main
struct ChatsAppApp: App {
    @State private var session: SessionStore
    @State private var images: ImageRepository

    init() {
        let api = HTTPChatAPI(baseURL: AppConfig.baseURL)
        let dependencies = AppDependencies(
            api: api,
            credentials: KeychainCredentialsStore(service: AppConfig.keychainService),
            storage: UserDefaultsLocalChatStorage()
        )
        _session = State(initialValue: SessionStore(dependencies: dependencies))
        _images = State(initialValue: ImageRepository(api: api))
    }

    var body: some Scene {
        WindowGroup {
            RootView(session: session)
                .environment(images)
        }
    }
}
