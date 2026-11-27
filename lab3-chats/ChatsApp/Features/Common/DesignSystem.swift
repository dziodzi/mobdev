import SwiftUI

enum DesignSystem {
    static let outgoingBubble = Color(.systemBlue)
    static let incomingBubble = Color(.systemGray5)
    static let bubbleRadius: CGFloat = 18
    static let bubbleTailRadius: CGFloat = 5
    static let thumbnailSide: CGFloat = 200

    static let appIconGradient = LinearGradient(
        colors: [Color(red: 0.40, green: 0.89, blue: 0.45), Color(red: 0.13, green: 0.74, blue: 0.27)],
        startPoint: .top,
        endPoint: .bottom
    )

    static let avatarGradient = LinearGradient(
        colors: [Color(.systemGray2), Color(.systemGray)],
        startPoint: .top,
        endPoint: .bottom
    )

    static func bubbleShape(isMine: Bool) -> UnevenRoundedRectangle {
        UnevenRoundedRectangle(
            topLeadingRadius: bubbleRadius,
            bottomLeadingRadius: isMine ? bubbleRadius : bubbleTailRadius,
            bottomTrailingRadius: isMine ? bubbleTailRadius : bubbleRadius,
            topTrailingRadius: bubbleRadius,
            style: .continuous
        )
    }
}

struct AvatarCircle: View {
    let initial: String
    let size: CGFloat

    var body: some View {
        Text(verbatim: initial)
            .font(.system(size: size * 0.42, weight: .semibold, design: .rounded))
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(DesignSystem.avatarGradient, in: Circle())
            .accessibilityHidden(true)
    }
}

struct ProblemBanner: View {
    let problem: NetworkProblem
    let onDismiss: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(.orange)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text("refresh.failed")
                    .font(.footnote.weight(.semibold))
                Text(problem.messageKey)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("common.dismiss"))
        }
        .padding(12)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

extension NetworkProblem {
    var messageKey: LocalizedStringKey {
        switch self {
        case .connection: return "error.connection"
        case .server: return "error.server"
        }
    }
}

extension SessionStore.LoginError {
    var messageKey: LocalizedStringKey {
        switch self {
        case .emptyFields: return "login.error.empty"
        case .invalidCredentials: return "login.error.invalid"
        case .connection: return "error.connection"
        case .unavailable: return "error.server"
        }
    }
}
