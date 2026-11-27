import Foundation
import Observation

@MainActor
@Observable
final class ConversationStore {
    enum Status: Equatable {
        case idle
        case loading
        case loaded
        case failed(NetworkProblem)
    }

    let channel: String

    private(set) var messages: [Message] = []
    private(set) var outgoing: [OutgoingMessage]
    private(set) var draftText: String
    private(set) var status: Status = .idle
    private(set) var hasMoreOlder = true
    private(set) var isLoadingOlder = false
    private(set) var olderProblem: NetworkProblem?
    private(set) var isRefreshing = false
    private(set) var refreshProblem: NetworkProblem?
    private(set) var confirmedServerIDs: Set<Int> = []
    var scrollAnchor: String?
    var retryCandidate: OutgoingMessage?

    @ObservationIgnored private weak var session: ChatSession?
    @ObservationIgnored private let api: ChatAPI
    @ObservationIgnored private let username: String
    @ObservationIgnored private let token: String
    @ObservationIgnored private var isInvalidated = false

    init(channel: String, session: ChatSession, api: ChatAPI, localState: ChannelLocalState) {
        self.channel = channel
        self.session = session
        self.api = api
        self.username = session.username
        self.token = session.credentials.token
        self.draftText = localState.draft
        self.outgoing = localState.outgoing.map { item in
            var restored = item
            if restored.status == .sending {
                restored.status = .unconfirmed
                restored.mayHaveBeenDelivered = true
            }
            return restored
        }
    }

    var rows: [ChatRow] {
        ChatRow.build(messages: messages, outgoing: outgoing, me: username, confirmed: confirmedServerIDs)
    }

    var isSending: Bool {
        outgoing.contains { $0.status == .sending }
    }

    var canSend: Bool {
        !isSending && !draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func updateDraft(_ text: String) {
        guard draftText != text, !isInvalidated else { return }
        draftText = text
        persist()
    }

    func loadInitialIfNeeded() async {
        guard status == .idle else { return }
        await loadLatest()
    }

    func loadLatest() async {
        guard !isRefreshing, !isInvalidated else { return }
        isRefreshing = true
        defer { isRefreshing = false }

        let isFirstLoad = status != .loaded
        if isFirstLoad {
            status = .loading
        }

        do {
            let page = try await api.messages(channel: channel, before: nil, limit: AppConfig.pageSize, token: token)
            guard !isInvalidated else { return }
            merge(page)
            if isFirstLoad {
                hasMoreOlder = page.count >= AppConfig.pageSize
                scrollAnchor = rows.last?.id
            }
            status = .loaded
            refreshProblem = nil
        } catch {
            guard !isInvalidated else { return }
            guard let problem = session?.problem(for: error) else {
                if status == .loading { status = .idle }
                return
            }
            if isFirstLoad {
                status = .failed(problem)
            } else {
                refreshProblem = problem
            }
        }
    }

    func loadOlder() async {
        guard status == .loaded, hasMoreOlder, !isLoadingOlder, !isInvalidated,
              let oldest = messages.first?.id else { return }
        isLoadingOlder = true
        olderProblem = nil
        defer { isLoadingOlder = false }

        do {
            let page = try await api.messages(channel: channel, before: oldest, limit: AppConfig.pageSize, token: token)
            guard !isInvalidated else { return }
            merge(page)
            hasMoreOlder = page.count >= AppConfig.pageSize
            scrollAnchor = ChatRow.serverID(oldest)
        } catch {
            guard !isInvalidated, let problem = session?.problem(for: error) else { return }
            olderProblem = problem
        }
    }

    func dismissRefreshProblem() {
        refreshProblem = nil
    }

    func send() async {
        let text = draftText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !isSending, !isInvalidated else { return }

        let item = OutgoingMessage(text: text)
        outgoing.append(item)
        draftText = ""
        persist()
        scrollAnchor = ChatRow.localID(item.id)
        await deliver(item.id)
    }

    func retry(_ id: UUID) async {
        guard let item = outgoing.first(where: { $0.id == id }), item.status == .unconfirmed else { return }
        if item.mayHaveBeenDelivered {
            retryCandidate = item
            return
        }
        await deliver(id)
    }

    func confirmRetry(_ id: UUID) async {
        retryCandidate = nil
        guard let item = outgoing.first(where: { $0.id == id }), item.status == .unconfirmed else { return }
        await deliver(id)
    }

    func returnToEditor(_ id: UUID) {
        guard let item = outgoing.first(where: { $0.id == id }), item.status == .unconfirmed else { return }
        outgoing.removeAll { $0.id == id }
        let current = draftText.trimmingCharacters(in: .whitespacesAndNewlines)
        draftText = current.isEmpty ? item.text : item.text + "\n" + draftText
        retryCandidate = nil
        persist()
    }

    func invalidate() {
        isInvalidated = true
    }

    private func deliver(_ id: UUID) async {
        let otherIsSending = outgoing.contains { $0.status == .sending && $0.id != id }
        guard !isInvalidated, !otherIsSending, let text = outgoing.first(where: { $0.id == id })?.text else { return }
        updateOutgoing(id) { $0.status = .sending }

        do {
            let serverId = try await api.sendText(text, from: username, to: channel, token: token)
            guard !isInvalidated else { return }
            updateOutgoing(id) {
                $0.status = .sent
                $0.serverId = serverId
            }
            if let serverId, messages.contains(where: { $0.id == serverId }) {
                reconcileOutgoing()
            }
        } catch {
            guard !isInvalidated else { return }
            let mayHaveBeenDelivered = (error as? APIError)?.mayHaveBeenDelivered ?? true
            updateOutgoing(id) {
                $0.status = .unconfirmed
                $0.mayHaveBeenDelivered = mayHaveBeenDelivered
            }
            _ = session?.problem(for: error)
        }
    }

    private func updateOutgoing(_ id: UUID, _ change: (inout OutgoingMessage) -> Void) {
        guard let index = outgoing.firstIndex(where: { $0.id == id }) else { return }
        var item = outgoing[index]
        change(&item)
        outgoing[index] = item
        persist()
    }

    private func merge(_ page: [Message]) {
        guard !page.isEmpty else { return }
        var byID = Dictionary(messages.map { ($0.id, $0) }, uniquingKeysWith: { _, latest in latest })
        for message in page {
            byID[message.id] = message
        }
        messages = byID.values.sorted { $0.id < $1.id }
        reconcileOutgoing()
    }

    private func reconcileOutgoing() {
        let serverIDs = Set(messages.map(\.id))
        var confirmed = confirmedServerIDs
        var remaining: [OutgoingMessage] = []

        for item in outgoing {
            guard item.status == .sent else {
                remaining.append(item)
                continue
            }
            if let serverId = item.serverId {
                if serverIDs.contains(serverId) {
                    confirmed.insert(serverId)
                } else {
                    remaining.append(item)
                }
            } else if let match = messages.last(where: {
                $0.author == username && $0.content == .text(item.text) && !confirmed.contains($0.id)
            }) {
                confirmed.insert(match.id)
            } else {
                remaining.append(item)
            }
        }

        if remaining != outgoing {
            outgoing = remaining
        }
        if confirmed != confirmedServerIDs {
            confirmedServerIDs = confirmed
        }
    }

    private func persist() {
        let unsent = outgoing.filter { $0.status != .sent }
        session?.saveLocalState(ChannelLocalState(draft: draftText, outgoing: unsent), for: channel)
    }
}
