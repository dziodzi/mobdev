import Foundation

struct ChatRow: Identifiable, Equatable {
    enum Kind: Equatable {
        case server(Message)
        case local(OutgoingMessage)
    }

    let id: String
    let kind: Kind
    let isMine: Bool
    let showsAuthor: Bool
    let timestamp: Date?
    let deliveryStatus: OutgoingMessage.Status?

    var content: Message.Content {
        switch kind {
        case .server(let message): return message.content
        case .local(let item): return .text(item.text)
        }
    }

    var author: String? {
        if case .server(let message) = kind { return message.author }
        return nil
    }

    static func serverID(_ id: Int) -> String { "s-\(id)" }
    static func localID(_ id: UUID) -> String { "l-\(id.uuidString)" }

    static func build(messages: [Message],
                      outgoing: [OutgoingMessage],
                      me: String,
                      confirmed: Set<Int>) -> [ChatRow] {
        var rows: [ChatRow] = []
        rows.reserveCapacity(messages.count + outgoing.count)
        var previousAuthor: String?
        var previousDate: Date?

        for message in messages {
            let isMine = message.author == me
            let showsTime = needsTimestamp(message.date, after: previousDate)
            rows.append(ChatRow(
                id: serverID(message.id),
                kind: .server(message),
                isMine: isMine,
                showsAuthor: !isMine && (message.author != previousAuthor || showsTime),
                timestamp: showsTime ? message.date : nil,
                deliveryStatus: isMine && confirmed.contains(message.id) ? .sent : nil
            ))
            previousAuthor = message.author
            if let date = message.date {
                previousDate = date
            }
        }

        for item in outgoing {
            let showsTime = needsTimestamp(item.createdAt, after: previousDate)
            rows.append(ChatRow(
                id: localID(item.id),
                kind: .local(item),
                isMine: true,
                showsAuthor: false,
                timestamp: showsTime ? item.createdAt : nil,
                deliveryStatus: item.status
            ))
            previousAuthor = me
            previousDate = item.createdAt
        }
        return rows
    }

    private static func needsTimestamp(_ date: Date?, after previous: Date?) -> Bool {
        guard let date else { return false }
        guard let previous else { return true }
        return date.timeIntervalSince(previous) > 60 * 60
    }
}
