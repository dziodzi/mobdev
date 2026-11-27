import Foundation
import Observation

struct AppDependencies {
    let api: ChatAPI
    let credentials: CredentialsStore
    let storage: LocalChatStorage
}

@MainActor
@Observable
final class SessionStore {
    enum Phase {
        case loggedOut
        case loggedIn(ChatSession)
    }

    enum LoginError: Equatable {
        case emptyFields
        case invalidCredentials
        case connection
        case unavailable
    }

    enum Notice: Equatable {
        case sessionExpired
    }

    private(set) var phase: Phase = .loggedOut
    private(set) var isLoggingIn = false
    private(set) var loginError: LoginError?
    private(set) var notice: Notice?

    @ObservationIgnored private let dependencies: AppDependencies

    init(dependencies: AppDependencies) {
        self.dependencies = dependencies
        if let saved = dependencies.credentials.load() {
            phase = .loggedIn(startSession(saved))
        }
    }

    var currentChat: ChatSession? {
        if case .loggedIn(let chat) = phase { return chat }
        return nil
    }

    func login(username: String, password: String) async {
        guard !isLoggingIn else { return }
        let name = username.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty, !password.isEmpty else {
            loginError = .emptyFields
            return
        }

        isLoggingIn = true
        loginError = nil
        defer { isLoggingIn = false }

        do {
            let token = try await dependencies.api.login(name: name, password: password)
            let credentials = Credentials(username: name, token: token)
            try? dependencies.credentials.save(credentials)
            notice = nil
            phase = .loggedIn(startSession(credentials))
        } catch {
            loginError = Self.loginError(for: error)
        }
    }

    func clearLoginError() {
        loginError = nil
    }

    func logout() {
        guard let chat = currentChat else { return }
        chat.invalidate()
        dependencies.credentials.delete()
        dependencies.storage.deleteAll(user: chat.username)
        notice = nil
        phase = .loggedOut

        let api = dependencies.api
        let token = chat.credentials.token
        Task {
            try? await api.logout(token: token)
        }
    }

    private func endExpiredSession(_ sessionID: UUID) {
        guard let chat = currentChat, chat.id == sessionID else { return }
        chat.invalidate()
        dependencies.credentials.delete()
        notice = .sessionExpired
        phase = .loggedOut
    }

    private func startSession(_ credentials: Credentials) -> ChatSession {
        let chat = ChatSession(credentials: credentials, api: dependencies.api, storage: dependencies.storage)
        let sessionID = chat.id
        chat.onUnauthorized = { [weak self] in
            self?.endExpiredSession(sessionID)
        }
        return chat
    }

    static func loginError(for error: Error) -> LoginError {
        switch error as? APIError {
        case .invalidCredentials?, .unauthorized?:
            return .invalidCredentials
        case .connection?:
            return .connection
        default:
            return .unavailable
        }
    }
}
