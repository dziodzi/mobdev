import Foundation

struct OutgoingMessage: Identifiable, Codable, Equatable {
    enum Status: String, Codable {
        case sending
        case sent
        case unconfirmed
    }

    let id: UUID
    let text: String
    var status: Status
    var serverId: Int?
    var mayHaveBeenDelivered: Bool
    let createdAt: Date

    init(id: UUID = UUID(),
         text: String,
         status: Status = .sending,
         serverId: Int? = nil,
         mayHaveBeenDelivered: Bool = false,
         createdAt: Date = Date()) {
        self.id = id
        self.text = text
        self.status = status
        self.serverId = serverId
        self.mayHaveBeenDelivered = mayHaveBeenDelivered
        self.createdAt = createdAt
    }
}
