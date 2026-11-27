import SwiftUI

struct ChannelListView: View {
    @Bindable var chat: ChatSession
    let highlightsSelection: Bool
    let onLogout: () -> Void

    @State private var isConfirmingLogout = false

    var body: some View {
        content
            .navigationTitle("chats.title")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        isConfirmingLogout = true
                    } label: {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                    }
                    .accessibilityLabel(Text("logout.button"))
                }
            }
            .confirmationDialog("logout.confirm.title", isPresented: $isConfirmingLogout, titleVisibility: .visible) {
                Button("logout.button", role: .destructive, action: onLogout)
                Button("common.cancel", role: .cancel) {}
            } message: {
                Text("logout.confirm.message")
            }
    }

    @ViewBuilder
    private var content: some View {
        switch chat.channelsStatus {
        case .idle, .loading:
            ProgressView("chats.loading")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .failed(let problem):
            ContentUnavailableView {
                Label("chats.error.title", systemImage: "wifi.exclamationmark")
            } description: {
                Text(problem.messageKey)
            } actions: {
                Button("common.retry") {
                    Task { await chat.reloadChannels() }
                }
                .buttonStyle(.borderedProminent)
            }
        case .loaded:
            if chat.channels.isEmpty {
                ContentUnavailableView {
                    Label("chats.empty.title", systemImage: "bubble.left.and.bubble.right")
                } description: {
                    Text("chats.empty.message")
                } actions: {
                    Button("common.refresh") {
                        Task { await chat.reloadChannels() }
                    }
                }
            } else {
                list
            }
        }
    }

    private var list: some View {
        List {
            if let problem = chat.channelsRefreshProblem {
                ProblemBanner(problem: problem) {
                    chat.dismissChannelsRefreshProblem()
                }
                .listRowSeparator(.hidden)
            }
            ForEach(chat.channels, id: \.self) { channel in
                let isSelected = highlightsSelection && chat.selectedChannel == channel
                Button {
                    chat.select(channel)
                } label: {
                    ChannelRow(channel: channel, isSelected: isSelected)
                }
                .buttonStyle(.plain)
                .listRowBackground(isSelected ? Color.accentColor : nil)
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
        }
        .listStyle(.plain)
        .refreshable {
            await chat.reloadChannels()
        }
    }
}

struct ChannelRow: View {
    let channel: String
    let isSelected: Bool

    var body: some View {
        HStack(spacing: 12) {
            AvatarCircle(initial: ChannelName.initial(channel), size: 46)
            Text(verbatim: ChannelName.display(channel))
                .font(.headline)
                .foregroundStyle(isSelected ? Color.white : Color.primary)
                .lineLimit(2)
            Spacer(minLength: 0)
        }
        .padding(.vertical, 6)
        .contentShape(Rectangle())
    }
}
