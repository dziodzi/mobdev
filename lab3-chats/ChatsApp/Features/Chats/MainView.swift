import SwiftUI

struct MainView: View {
    @Bindable var chat: ChatSession
    let onLogout: () -> Void

    var body: some View {
        GeometryReader { proxy in
            if proxy.size.width > proxy.size.height {
                landscape(width: proxy.size.width)
            } else {
                portrait
            }
        }
        .fullScreenCover(item: $chat.openedImage) { image in
            ImageViewer(link: image.link) {
                chat.openedImage = nil
            }
        }
        .task {
            await chat.loadChannelsIfNeeded()
        }
    }

    private var portrait: some View {
        NavigationStack(path: pathBinding) {
            ChannelListView(chat: chat, highlightsSelection: false, onLogout: onLogout)
                .navigationDestination(for: String.self) { channel in
                    ConversationView(store: chat.conversation(for: channel), chat: chat)
                }
        }
    }

    private func landscape(width: CGFloat) -> some View {
        HStack(spacing: 0) {
            NavigationStack {
                ChannelListView(chat: chat, highlightsSelection: true, onLogout: onLogout)
            }
            .frame(width: min(max(width * 0.36, 260), 380))

            Divider()

            NavigationStack {
                if let channel = chat.selectedChannel {
                    ConversationView(store: chat.conversation(for: channel), chat: chat)
                        .id(channel)
                        .toolbar {
                            ToolbarItem(placement: .topBarLeading) {
                                Button {
                                    chat.select(nil)
                                } label: {
                                    Image(systemName: "xmark")
                                }
                                .accessibilityLabel(Text("chat.close"))
                            }
                        }
                } else {
                    ContentUnavailableView("chats.selectPrompt", systemImage: "bubble.left.and.bubble.right")
                }
            }
        }
    }

    private var pathBinding: Binding<[String]> {
        Binding(
            get: { chat.selectedChannel.map { [$0] } ?? [] },
            set: { chat.select($0.last) }
        )
    }
}
