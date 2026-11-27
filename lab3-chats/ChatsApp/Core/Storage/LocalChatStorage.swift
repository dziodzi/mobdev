import Foundation

struct ChannelLocalState: Codable, Equatable {
    var draft: String = ""
    var outgoing: [OutgoingMessage] = []

    var isEmpty: Bool { draft.isEmpty && outgoing.isEmpty }
}

protocol LocalChatStorage {
    func load(user: String) -> [String: ChannelLocalState]
    func save(_ state: [String: ChannelLocalState], user: String)
    func deleteAll(user: String)
}

final class UserDefaultsLocalChatStorage: LocalChatStorage {
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load(user: String) -> [String: ChannelLocalState] {
        guard let data = defaults.data(forKey: key(for: user)),
              let state = try? JSONDecoder().decode([String: ChannelLocalState].self, from: data) else {
            return [:]
        }
        return state
    }

    func save(_ state: [String: ChannelLocalState], user: String) {
        guard !state.isEmpty else {
            defaults.removeObject(forKey: key(for: user))
            return
        }
        guard let data = try? JSONEncoder().encode(state) else { return }
        defaults.set(data, forKey: key(for: user))
    }

    func deleteAll(user: String) {
        defaults.removeObject(forKey: key(for: user))
    }

    private func key(for user: String) -> String {
        "chats.local.v1.\(user)"
    }
}
