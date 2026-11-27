import XCTest
@testable import ChatsApp

@MainActor
final class SessionTests: XCTestCase {

    private func makeSession(api: MockChatAPI = MockChatAPI(),
                             credentials: InMemoryCredentialsStore = InMemoryCredentialsStore(),
                             storage: InMemoryLocalChatStorage = InMemoryLocalChatStorage()) -> SessionStore {
        SessionStore(dependencies: AppDependencies(api: api, credentials: credentials, storage: storage))
    }

    func testLoginScreenSkippedWhenCredentialsSaved() {
        let session = makeSession(credentials: InMemoryCredentialsStore(Credentials(username: "anton", token: "t")))
        XCTAssertEqual(session.currentChat?.username, "anton")
    }

    func testLoginErrorsAreSeparated() async {
        let api = MockChatAPI()
        let session = makeSession(api: api)

        api.loginResult = .failure(APIError.invalidCredentials)
        await session.login(username: "anton", password: "wrong")
        XCTAssertEqual(session.loginError, .invalidCredentials)

        api.loginResult = .failure(APIError.connection(mayHaveReachedServer: false))
        await session.login(username: "anton", password: "x")
        XCTAssertEqual(session.loginError, .connection)

        api.loginResult = .failure(APIError.server(status: 503))
        await session.login(username: "anton", password: "x")
        XCTAssertEqual(session.loginError, .unavailable)
        XCTAssertNil(session.currentChat)
    }

    func testSuccessfulLoginStoresTokenNotPassword() async {
        let credentials = InMemoryCredentialsStore()
        let session = makeSession(credentials: credentials)

        await session.login(username: "anton", password: "secret")

        XCTAssertEqual(credentials.stored, Credentials(username: "anton", token: "token"))
        XCTAssertNotNil(session.currentChat)
    }

    func testRepeatedLoginTapSendsOneRequest() async {
        let api = MockChatAPI()
        api.delay = 50_000_000
        let session = makeSession(api: api)

        async let first: Void = session.login(username: "anton", password: "x")
        async let second: Void = session.login(username: "anton", password: "x")
        _ = await (first, second)

        XCTAssertEqual(api.loginCalls, 1)
    }

    func testConcurrentUnauthorizedEndsSessionOnce() async {
        let api = MockChatAPI()
        api.channelsResult = .failure(APIError.unauthorized)
        api.messagesHandler = { _, _ in throw APIError.unauthorized }
        let credentials = InMemoryCredentialsStore(Credentials(username: "anton", token: "t"))
        let session = makeSession(api: api, credentials: credentials)
        guard let chat = session.currentChat else { return XCTFail("нет сессии") }

        async let channels: Void = chat.reloadChannels()
        async let messages: Void = chat.conversation(for: "a@channel").loadLatest()
        _ = await (channels, messages)

        XCTAssertNil(session.currentChat)
        XCTAssertEqual(session.notice, .sessionExpired)
        XCTAssertEqual(credentials.deleteCalls, 1)
    }

    func testLogoutDeletesCredentialsAndDrafts() async {
        let credentials = InMemoryCredentialsStore(Credentials(username: "anton", token: "t"))
        let storage = InMemoryLocalChatStorage()
        let session = makeSession(credentials: credentials, storage: storage)
        session.currentChat?.conversation(for: "a@channel").updateDraft("черновик")
        XCTAssertNotNil(storage.data["anton"])

        session.logout()

        XCTAssertNil(credentials.stored)
        XCTAssertNil(storage.data["anton"])
        XCTAssertNil(session.currentChat)
    }

    func testResultsAfterLogoutAreIgnored() async {
        let api = MockChatAPI()
        api.delay = 50_000_000
        api.channelsResult = .success(["a@channel"])
        let session = makeSession(api: api, credentials: InMemoryCredentialsStore(Credentials(username: "anton", token: "t")))
        guard let chat = session.currentChat else { return XCTFail("нет сессии") }

        let loading = Task { await chat.reloadChannels() }
        session.logout()
        await loading.value

        XCTAssertTrue(chat.channels.isEmpty)
    }
}

@MainActor
final class ConversationTests: XCTestCase {

    private func makeChat(api: MockChatAPI, storage: InMemoryLocalChatStorage = InMemoryLocalChatStorage(), user: String = "anton") -> ChatSession {
        ChatSession(credentials: Credentials(username: user, token: "t"), api: api, storage: storage)
    }

    func testPaginationLoadsPagesWithoutDuplicates() async {
        let api = MockChatAPI()
        let history = Fixtures.history(1...45)
        api.messagesHandler = { _, before in Fixtures.page(from: history, before: before) }
        let store = makeChat(api: api).conversation(for: "a@channel")

        await store.loadInitialIfNeeded()
        await store.loadInitialIfNeeded()
        await store.loadOlder()
        await store.loadLatest()
        await store.loadOlder()

        XCTAssertEqual(store.messages.map(\.id), Array(1...45))
        XCTAssertFalse(store.hasMoreOlder)
        XCTAssertTrue(api.messagesCalls.count == 4)
    }

    func testFailedOlderPageKeepsLoadedMessages() async {
        let api = MockChatAPI()
        let history = Fixtures.history(1...30)
        api.messagesHandler = { _, before in Fixtures.page(from: history, before: before) }
        let store = makeChat(api: api).conversation(for: "a@channel")
        await store.loadInitialIfNeeded()

        api.messagesHandler = { _, _ in throw APIError.connection(mayHaveReachedServer: false) }
        await store.loadOlder()

        XCTAssertEqual(store.messages.count, 20)
        XCTAssertEqual(store.olderProblem, .connection)
    }

    func testFailedRefreshKeepsLoadedMessages() async {
        let api = MockChatAPI()
        api.messagesHandler = { _, _ in Fixtures.history(1...5) }
        let store = makeChat(api: api).conversation(for: "a@channel")
        await store.loadInitialIfNeeded()

        api.messagesHandler = { _, _ in throw APIError.server(status: 500) }
        await store.loadLatest()

        XCTAssertEqual(store.messages.count, 5)
        XCTAssertEqual(store.refreshProblem, .server)
        XCTAssertEqual(store.status, .loaded)
    }

    func testChannelsSeparateResponses() async {
        let api = MockChatAPI()
        api.messagesHandler = { channel, _ in [Fixtures.message(channel == "a@channel" ? 1 : 2, channel: channel)] }
        let chat = makeChat(api: api)

        async let a: Void = chat.conversation(for: "a@channel").loadInitialIfNeeded()
        async let b: Void = chat.conversation(for: "b@channel").loadInitialIfNeeded()
        _ = await (a, b)

        XCTAssertEqual(chat.conversation(for: "a@channel").messages.map(\.id), [1])
        XCTAssertEqual(chat.conversation(for: "b@channel").messages.map(\.id), [2])
    }

    func testWhitespaceOnlyMessageIsNotSent() async {
        let api = MockChatAPI()
        let store = makeChat(api: api).conversation(for: "a@channel")
        store.updateDraft("   \n\t  ")

        await store.send()

        XCTAssertTrue(api.sendCalls.isEmpty)
        XCTAssertTrue(store.outgoing.isEmpty)
    }

    func testRepeatedSendTapSendsOneRequest() async {
        let api = MockChatAPI()
        api.delay = 50_000_000
        api.sendResult = .success(7)
        let store = makeChat(api: api).conversation(for: "a@channel")
        store.updateDraft("привет")

        async let first: Void = store.send()
        async let second: Void = store.send()
        _ = await (first, second)

        XCTAssertEqual(api.sendCalls, ["привет"])
        XCTAssertEqual(store.outgoing.count, 1)
    }

    func testTextTypedDuringSendingIsKept() async throws {
        let api = MockChatAPI()
        api.delay = 80_000_000
        api.sendResult = .success(7)
        let store = makeChat(api: api).conversation(for: "a@channel")
        store.updateDraft("первое")

        let sending = Task { await store.send() }
        try await Task.sleep(nanoseconds: 20_000_000)
        store.updateDraft("второе")
        await sending.value

        XCTAssertEqual(store.draftText, "второе")
        XCTAssertEqual(store.outgoing.first?.status, .sent)
    }

    func testFailedSendKeepsTextAndRetryUpdatesSameItem() async {
        let api = MockChatAPI()
        api.sendResult = .failure(APIError.connection(mayHaveReachedServer: true))
        let store = makeChat(api: api).conversation(for: "a@channel")
        store.updateDraft("важное сообщение")

        await store.send()
        XCTAssertEqual(store.outgoing.count, 1)
        XCTAssertEqual(store.outgoing.first?.status, .unconfirmed)
        XCTAssertEqual(store.outgoing.first?.text, "важное сообщение")
        guard let id = store.outgoing.first?.id else { return XCTFail("нет сообщения") }

        await store.retry(id)
        XCTAssertEqual(store.retryCandidate?.id, id)
        XCTAssertEqual(api.sendCalls.count, 1)

        api.sendResult = .success(99)
        await store.confirmRetry(id)
        XCTAssertEqual(store.outgoing.count, 1)
        XCTAssertEqual(store.outgoing.first?.id, id)
        XCTAssertEqual(store.outgoing.first?.serverId, 99)

        api.messagesHandler = { _, _ in [Fixtures.message(99, author: "anton", text: "важное сообщение")] }
        await store.loadLatest()
        XCTAssertTrue(store.outgoing.isEmpty)
        XCTAssertEqual(store.rows.count, 1)
        XCTAssertEqual(store.rows.first?.deliveryStatus, .sent)
    }

    func testDefinitelyNotSentRetriesWithoutWarning() async {
        let api = MockChatAPI()
        api.sendResult = .failure(APIError.connection(mayHaveReachedServer: false))
        let store = makeChat(api: api).conversation(for: "a@channel")
        store.updateDraft("текст")
        await store.send()
        guard let id = store.outgoing.first?.id else { return XCTFail("нет сообщения") }

        api.sendResult = .success(5)
        await store.retry(id)

        XCTAssertNil(store.retryCandidate)
        XCTAssertEqual(api.sendCalls.count, 2)
        XCTAssertEqual(store.outgoing.first?.status, .sent)
    }

    func testReturnToEditorRestoresText() async {
        let api = MockChatAPI()
        api.sendResult = .failure(APIError.server(status: 500))
        let store = makeChat(api: api).conversation(for: "a@channel")
        store.updateDraft("верни меня")
        await store.send()
        guard let id = store.outgoing.first?.id else { return XCTFail("нет сообщения") }

        store.returnToEditor(id)

        XCTAssertTrue(store.outgoing.isEmpty)
        XCTAssertEqual(store.draftText, "верни меня")
    }

    func testDraftsPersistPerChannelAndPerUser() {
        let storage = InMemoryLocalChatStorage()
        let api = MockChatAPI()
        let first = makeChat(api: api, storage: storage)
        first.conversation(for: "a@channel").updateDraft("черновик A")
        first.conversation(for: "b@channel").updateDraft("черновик B")

        let restarted = makeChat(api: api, storage: storage)
        XCTAssertEqual(restarted.conversation(for: "a@channel").draftText, "черновик A")
        XCTAssertEqual(restarted.conversation(for: "b@channel").draftText, "черновик B")

        let otherUser = makeChat(api: api, storage: storage, user: "boris")
        XCTAssertEqual(otherUser.conversation(for: "a@channel").draftText, "")
    }

    func testUnfinishedSendRestoredAsUnconfirmed() {
        let storage = InMemoryLocalChatStorage()
        storage.save(["a@channel": ChannelLocalState(draft: "", outgoing: [OutgoingMessage(text: "в пути", status: .sending)])], user: "anton")

        let store = makeChat(api: MockChatAPI(), storage: storage).conversation(for: "a@channel")

        XCTAssertEqual(store.outgoing.first?.status, .unconfirmed)
        XCTAssertEqual(store.outgoing.first?.mayHaveBeenDelivered, true)
    }
}

final class DecodingTests: XCTestCase {

    func testDecodesTextImageAndSkipsBrokenItems() throws {
        let json = """
        [
          {"id": 3, "from": "a", "to": "x@channel", "data": {"Text": {"text": "hi"}}, "time": 1795774159000},
          {"id": 2, "from": "b", "to": "x@channel", "data": {"Image": {"link": "u/p.jpg"}}},
          {"broken": true},
          {"id": 1, "from": "c", "to": "x@channel", "data": {"Video": {}}}
        ]
        """
        let messages = try JSONDecoder().decode(LossyArray<Message>.self, from: Data(json.utf8)).elements

        XCTAssertEqual(messages.map(\.id), [3, 2, 1])
        XCTAssertEqual(messages[0].content, .text("hi"))
        XCTAssertEqual(messages[1].content, .image(link: "u/p.jpg"))
        XCTAssertEqual(messages[2].content, .unsupported)
        XCTAssertNotNil(messages[0].date)
    }

    func testChannelDisplayName() {
        XCTAssertEqual(ChannelName.display("general@channel"), "general")
        XCTAssertEqual(ChannelName.initial("general@channel"), "G")
    }
}
