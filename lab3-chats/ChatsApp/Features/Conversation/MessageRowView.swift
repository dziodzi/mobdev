import SwiftUI

struct MessageRowView: View {
    let row: ChatRow
    let onOpenImage: (String) -> Void
    let onRetry: (UUID) -> Void
    let onReturnToEditor: (UUID) -> Void

    var body: some View {
        VStack(spacing: 2) {
            if let timestamp = row.timestamp {
                Text(timestamp, format: .dateTime.day().month(.abbreviated).hour().minute())
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(.secondary)
                    .padding(.top, 12)
                    .padding(.bottom, 4)
            }
            HStack(alignment: .bottom, spacing: 0) {
                if row.isMine {
                    Spacer(minLength: 48)
                }
                VStack(alignment: row.isMine ? .trailing : .leading, spacing: 3) {
                    if row.showsAuthor, let author = row.author {
                        Text(verbatim: author)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 12)
                            .padding(.top, 6)
                    }
                    bubble
                    status
                }
                if !row.isMine {
                    Spacer(minLength: 48)
                }
            }
        }
    }

    @ViewBuilder
    private var bubble: some View {
        let shape = DesignSystem.bubbleShape(isMine: row.isMine)
        switch row.content {
        case .text(let text):
            Text(verbatim: text)
                .font(.body)
                .foregroundStyle(row.isMine ? Color.white : Color.primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(bubbleColor, in: shape)
                .textSelection(.enabled)
        case .image(let link):
            Button {
                onOpenImage(link)
            } label: {
                ThumbnailView(link: link)
                    .clipShape(shape)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("message.image.open"))
        case .unsupported:
            Text("message.unsupported")
                .font(.body.italic())
                .foregroundStyle(.secondary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(DesignSystem.incomingBubble, in: shape)
        }
    }

    private var bubbleColor: Color {
        guard row.isMine else { return DesignSystem.incomingBubble }
        if row.deliveryStatus == .unconfirmed {
            return DesignSystem.outgoingBubble.opacity(0.55)
        }
        return DesignSystem.outgoingBubble
    }

    @ViewBuilder
    private var status: some View {
        switch row.deliveryStatus {
        case .sending?:
            HStack(spacing: 4) {
                ProgressView()
                    .controlSize(.mini)
                Text("status.sending")
            }
            .font(.caption2)
            .foregroundStyle(.secondary)
            .padding(.horizontal, 4)
        case .sent?:
            Text("status.sent")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 4)
        case .unconfirmed?:
            if case .local(let item) = row.kind {
                VStack(alignment: .trailing, spacing: 6) {
                    Label("status.unconfirmed", systemImage: "exclamationmark.circle.fill")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.red)
                    HStack(spacing: 8) {
                        Button("send.returnToEditor") {
                            onReturnToEditor(item.id)
                        }
                        Button("send.retry") {
                            onRetry(item.id)
                        }
                        .fontWeight(.semibold)
                    }
                    .font(.caption)
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.capsule)
                    .controlSize(.mini)
                }
                .padding(.horizontal, 4)
            }
        case nil:
            EmptyView()
        }
    }
}

struct ThumbnailView: View {
    let link: String

    @Environment(ImageRepository.self) private var images
    @State private var loaded: UIImage?
    @State private var failed = false

    var body: some View {
        let image = loaded ?? images.cachedImage(link: link, size: .thumbnail)
        ZStack {
            DesignSystem.incomingBubble
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else if failed {
                VStack(spacing: 6) {
                    Image(systemName: "photo")
                        .font(.title2)
                    Text("image.thumbnail.failed")
                        .font(.caption2)
                }
                .foregroundStyle(.secondary)
            } else {
                ProgressView()
            }
        }
        .frame(width: DesignSystem.thumbnailSide, height: DesignSystem.thumbnailSide)
        .clipped()
        .task(id: link) {
            await load()
        }
    }

    private func load() async {
        guard images.cachedImage(link: link, size: .thumbnail) == nil else { return }
        do {
            loaded = try await images.image(link: link, size: .thumbnail)
            failed = false
        } catch {
            if !error.isCancellation {
                failed = true
            }
        }
    }
}
