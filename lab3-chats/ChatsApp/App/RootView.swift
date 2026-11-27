import SwiftUI

struct RootView: View {
    let session: SessionStore

    var body: some View {
        switch session.phase {
        case .loggedOut:
            LoginView(session: session)
        case .loggedIn(let chat):
            MainView(chat: chat, onLogout: { session.logout() })
                .id(chat.id)
        }
    }
}
