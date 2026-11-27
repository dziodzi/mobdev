import SwiftUI

struct ComposerView: View {
    @Bindable var store: ConversationStore

    var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            TextField("composer.placeholder", text: draftBinding, axis: .vertical)
                .lineLimit(1...6)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(Color(.systemGray4), lineWidth: 1)
                )
                .accessibilityLabel(Text("composer.label"))

            Button {
                Task { await store.send() }
            } label: {
                if store.isSending {
                    ProgressView()
                        .frame(width: 34, height: 34)
                } else {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.title)
                        .symbolRenderingMode(.palette)
                        .foregroundStyle(.white, store.canSend ? DesignSystem.outgoingBubble : Color(.systemGray3))
                }
            }
            .disabled(!store.canSend)
            .accessibilityLabel(Text("composer.send"))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(.bar)
    }

    private var draftBinding: Binding<String> {
        Binding(
            get: { store.draftText },
            set: { store.updateDraft($0) }
        )
    }
}
