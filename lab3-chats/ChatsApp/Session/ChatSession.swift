import Foundation
import Observation

struct OpenedImage: Identifiable, Equatable {
    let id = UUID()
    let link: String
}

@MainActor
@Observable
final class ChatSession {
    enum ListStatus: Equatable {
        case idle
        case loading
        case loaded
        case failed(NetworkProblem)
    }

    let id = UUID()
    let credentials: Credentials

    private(set) var channels: [String] = []
    private(set) var channelsStatus: ListStatus = .idle
    private(set) var channelsRefreshProblem: NetworkProblem?
    var selectedChannel: String?
    var openedImage: OpenedImage?

    @ObservationIgnored var onUnauthorized: (() -> Void)?
    @ObservationIgnored private let api: ChatAPI
    @ObservationIgnored private let storage: LocalChatStorage
    @ObservationIgnored private var localState: [String: ChannelLocalState]
    @ObservationIgnored private var conversations: [String: ConversationStore] = [:]
    @ObservationIgnored private var isLoadingChannels = false
    @ObservationIgnored private(set) var isInvalidated = false

    init(credentials: Credentials, api: ChatAPI, storage: LocalChatStorage) {
        self.credentials = credentials
        self.api = api
        self.storage = storage
        self.localState = storage.load(user: credentials.username)
    }

    var username: String { credentials.username }

    func conversation(for channel: String) -> ConversationStore {
        if let existing = conversations[channel] {
            return existing
        }
        let store = ConversationStore(
            channel: channel,
            session: self,
            api: api,
            localState: localState[channel] ?? ChannelLocalState()
        )
        conversations[channel] = store
        return store
    }

    func loadChannelsIfNeeded() async {
        guard channelsStatus == .idle else { return }
        await loadChannels()
    }

    func reloadChannels() async {
        await loadChannels()
    }

    func dismissChannelsRefreshProblem() {
        channelsRefreshProblem = nil
    }

    func select(_ channel: String?) {
        selectedChannel = channel
    }

    func openImage(_ link: String) {
        openedImage = OpenedImage(link: link)
    }

    func saveLocalState(_ state: ChannelLocalState, for channel: String) {
        guard !isInvalidated else { return }
        localState[channel] = state.isEmpty ? nil : state
        storage.save(localState, user: username)
    }

    func invalidate() {
        isInvalidated = true
        conversations.values.forEach { $0.invalidate() }
    }

    func problem(for error: Error) -> NetworkProblem? {
        guard !isInvalidated, !error.isCancellation else { return nil }
        if let apiError = error as? APIError {
            if apiError == .unauthorized {
                onUnauthorized?()
                return nil
            }
            return apiError.problem
        }
        return .server
    }

    private func loadChannels() async {
        guard !isLoadingChannels, !isInvalidated else { return }
        isLoadingChannels = true
        defer { isLoadingChannels = false }
        if channels.isEmpty {
            channelsStatus = .loading
        }

        do {
            let result = try await api.channels(token: credentials.token)
            guard !isInvalidated else { return }
            channels = result.sorted { $0.localizedStandardCompare($1) == .orderedAscending }
            channelsStatus = .loaded
            channelsRefreshProblem = nil
        } catch {
            guard !isInvalidated else { return }
            guard let problem = problem(for: error) else {
                if channelsStatus == .loading { channelsStatus = .idle }
                return
            }
            if channels.isEmpty {
                channelsStatus = .failed(problem)
            } else {
                channelsRefreshProblem = problem
            }
        }
    }
}
