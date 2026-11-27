import Foundation

enum ChannelName {
    private static let suffix = "@channel"

    static func display(_ channel: String) -> String {
        guard channel.hasSuffix(suffix), channel.count > suffix.count else { return channel }
        return String(channel.dropLast(suffix.count))
    }

    static func initial(_ channel: String) -> String {
        guard let first = display(channel).first(where: { $0.isLetter || $0.isNumber }) else { return "#" }
        return String(first).uppercased()
    }
}
