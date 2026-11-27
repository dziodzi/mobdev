import SwiftUI

struct ImageViewer: View {
    let link: String
    let onClose: () -> Void

    @Environment(ImageRepository.self) private var images
    @State private var image: UIImage?
    @State private var failed = false
    @State private var attempt = 0
    @State private var scale: CGFloat = 1
    @State private var lastScale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black
                    .ignoresSafeArea()
                content
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarBackground(Color.black.opacity(0.6), for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                            .fontWeight(.semibold)
                    }
                    .accessibilityLabel(Text("image.close"))
                }
            }
        }
        .environment(\.colorScheme, .dark)
        .task(id: attempt) {
            await load()
        }
    }

    @ViewBuilder
    private var content: some View {
        if let displayed = image ?? images.cachedImage(link: link, size: .full) {
            Image(uiImage: displayed)
                .resizable()
                .scaledToFit()
                .scaleEffect(scale)
                .offset(offset)
                .gesture(zoomGesture.simultaneously(with: panGesture))
                .onTapGesture(count: 2) {
                    withAnimation(.spring) {
                        if scale > 1 {
                            resetZoom()
                        } else {
                            scale = 2.5
                            lastScale = 2.5
                        }
                    }
                }
                .accessibilityLabel(Text("image.full"))
                .accessibilityAddTraits(.isImage)
        } else if failed {
            ContentUnavailableView {
                Label("image.error.title", systemImage: "photo.badge.exclamationmark")
            } description: {
                Text("image.error.message")
            } actions: {
                Button("common.retry") {
                    attempt += 1
                }
                .buttonStyle(.borderedProminent)
            }
        } else {
            ProgressView("image.loading")
                .tint(.white)
        }
    }

    private var zoomGesture: some Gesture {
        MagnifyGesture()
            .onChanged { value in
                scale = min(max(lastScale * value.magnification, 1), 5)
            }
            .onEnded { _ in
                lastScale = scale
                if scale <= 1 {
                    withAnimation(.spring) { resetZoom() }
                }
            }
    }

    private var panGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                guard scale > 1 else { return }
                offset = CGSize(
                    width: lastOffset.width + value.translation.width,
                    height: lastOffset.height + value.translation.height
                )
            }
            .onEnded { value in
                if scale > 1 {
                    lastOffset = offset
                } else if value.translation.height > 120 {
                    onClose()
                }
            }
    }

    private func resetZoom() {
        scale = 1
        lastScale = 1
        offset = .zero
        lastOffset = .zero
    }

    private func load() async {
        guard images.cachedImage(link: link, size: .full) == nil else { return }
        failed = false
        do {
            image = try await images.image(link: link, size: .full)
        } catch {
            if !error.isCancellation {
                failed = true
            }
        }
    }
}
