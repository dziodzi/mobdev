import SwiftUI

struct ConversationView: View {
    @Bindable var store: ConversationStore
    let chat: ChatSession

    var body: some View {
        messages
            .overlay { stateOverlay }
            .overlay(alignment: .top) { refreshBanner }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                ComposerView(store: store)
            }
            .navigationTitle(ChannelName.display(store.channel))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    refreshButton
                }
            }
            .task {
                await store.loadInitialIfNeeded()
            }
            .confirmationDialog("send.retry.title", isPresented: retryDialogBinding, titleVisibility: .visible, presenting: store.retryCandidate) { item in
                Button("send.retry.confirm") {
                    Task { await store.confirmRetry(item.id) }
                }
                Button("send.returnToEditor") {
                    store.returnToEditor(item.id)
                }
                Button("common.cancel", role: .cancel) {}
            } message: { _ in
                Text("send.retry.warning")
            }
    }

    private var messages: some View {
        ScrollView {
            LazyVStack(spacing: 3) {
                olderHeader
                ForEach(store.rows) { row in
                    MessageRowView(
                        row: row,
                        onOpenImage: { chat.openImage($0) },
                        onRetry: { id in Task { await store.retry(id) } },
                        onReturnToEditor: { store.returnToEditor($0) }
                    )
                }
            }
            .scrollTargetLayout()
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .defaultScrollAnchor(.bottom)
        .scrollPosition(id: $store.scrollAnchor)
        .scrollDismissesKeyboard(.interactively)
        .refreshable {
            await store.loadLatest()
        }
    }

    @ViewBuilder
    private var olderHeader: some View {
        if store.status == .loaded && store.hasMoreOlder && !store.messages.isEmpty {
            Group {
                if store.isLoadingOlder {
                    ProgressView()
                        .accessibilityLabel(Text("messages.older.loading"))
                } else if let problem = store.olderProblem {
                    VStack(spacing: 8) {
                        Text("messages.older.failed")
                            .font(.footnote.weight(.semibold))
                        Text(problem.messageKey)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                        Button("common.retry") {
                            Task { await store.loadOlder() }
                        }
                        .buttonStyle(.bordered)
                        .buttonBorderShape(.capsule)
                    }
                    .multilineTextAlignment(.center)
                } else {
                    Button("messages.older.load") {
                        Task { await store.loadOlder() }
                    }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.capsule)
                    .controlSize(.small)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
        }
    }

    @ViewBuilder
    private var stateOverlay: some View {
        if store.rows.isEmpty {
            switch store.status {
            case .idle, .loading:
                ProgressView("messages.loading")
            case .failed(let problem):
                ContentUnavailableView {
                    Label("messages.error.title", systemImage: "wifi.exclamationmark")
                } description: {
                    Text(problem.messageKey)
                } actions: {
                    Button("common.retry") {
                        Task { await store.loadLatest() }
                    }
                    .buttonStyle(.borderedProminent)
                }
            case .loaded:
                ContentUnavailableView {
                    Label("messages.empty.title", systemImage: "bubble.left")
                } description: {
                    Text("messages.empty.message")
                }
            }
        }
    }

    @ViewBuilder
    private var refreshBanner: some View {
        if let problem = store.refreshProblem {
            ProblemBanner(problem: problem) {
                store.dismissRefreshProblem()
            }
            .padding(.horizontal, 12)
            .padding(.top, 8)
        }
    }

    private var refreshButton: some View {
        Button {
            Task { await store.loadLatest() }
        } label: {
            if store.isRefreshing {
                ProgressView()
            } else {
                Image(systemName: "arrow.clockwise")
            }
        }
        .disabled(store.isRefreshing)
        .accessibilityLabel(Text("messages.refresh"))
    }

    private var retryDialogBinding: Binding<Bool> {
        Binding(
            get: { store.retryCandidate != nil },
            set: { isPresented in
                if !isPresented { store.retryCandidate = nil }
            }
        )
    }
}
