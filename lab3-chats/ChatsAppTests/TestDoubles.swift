import Foundation
@testable import ChatsApp

final class MockChatAPI: ChatAPI {
    var loginResult: Result<String, Error> = .success("token")
    var channelsResult: Result<[String], Error> = .success([])
    var messagesHandler: (String, Int?) throws -> [Message] = { _, _ in [] }
    var sendResult: Result<Int?, Error> = .success(nil)
    var delay: UInt64 = 0

    private(set) var loginCalls = 0
    private(set) var channelsCalls = 0
    private(set) var messagesCalls: [(channel: String, before: Int?)] = []
    private(set) var sendCalls: [String] = []
    private(set) var logoutCalls = 0

    func login(name: String, password: String) async throws -> String {
        loginCalls += 1
        try await pause()
        return try loginResult.get()
    }

    func logout(token: String) async throws {
        logoutCalls += 1
    }

    func channels(token: String) async throws -> [String] {
        channelsCalls += 1
        try await pause()
        return try channelsResult.get()
    }

    func messages(channel: String, before: Int?, limit: Int, token: String) async throws -> [Message] {
        messagesCalls.append((channel, before))
        try await pause()
        return Array(try messagesHandler(channel, before).prefix(limit))
    }

    func sendText(_ text: String, from author: String, to channel: String, token: String) async throws -> Int? {
        sendCalls.append(text)
        try await pause()
        return try sendResult.get()
    }

    func imageData(link: String, size: ImageSize) async throws -> Data {
        Data()
    }

    private func pause() async throws {
        if delay > 0 {
            try await Task.sleep(nanoseconds: delay)
        }
    }
}

final class InMemoryCredentialsStore: CredentialsStore {
    var stored: Credentials?
    private(set) var deleteCalls = 0

    init(_ stored: Credentials? = nil) {
        self.stored = stored
    }

    func load() -> Credentials? { stored }
    func save(_ credentials: Credentials) throws { stored = credentials }
    func delete() {
        deleteCalls += 1
        stored = nil
    }
}

final class InMemoryLocalChatStorage: LocalChatStorage {
    private(set) var data: [String: [String: ChannelLocalState]] = [:]

    func load(user: String) -> [String: ChannelLocalState] { data[user] ?? [:] }
    func save(_ state: [String: ChannelLocalState], user: String) { data[user] = state }
    func deleteAll(user: String) { data[user] = nil }
}

enum Fixtures {
    static func message(_ id: Int, author: String = "alice", text: String? = nil, channel: String = "general@channel") -> Message {
        Message(id: id, author: author, channel: channel, content: .text(text ?? "m\(id)"), date: Date(timeIntervalSince1970: TimeInterval(id)))
    }

    static func history(_ range: ClosedRange<Int>) -> [Message] {
        range.map { message($0) }
    }

    static func page(from history: [Message], before: Int?, limit: Int = 20) -> [Message] {
        let older = history.filter { message in before.map { message.id < $0 } ?? true }
        return Array(older.sorted { $0.id > $1.id }.prefix(limit))
    }
}
