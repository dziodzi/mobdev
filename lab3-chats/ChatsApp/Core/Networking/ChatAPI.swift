import Foundation

enum ImageSize: Hashable {
    case thumbnail
    case full
}

protocol ChatAPI {
    func login(name: String, password: String) async throws -> String
    func logout(token: String) async throws
    func channels(token: String) async throws -> [String]
    func messages(channel: String, before: Int?, limit: Int, token: String) async throws -> [Message]
    func sendText(_ text: String, from author: String, to channel: String, token: String) async throws -> Int?
    func imageData(link: String, size: ImageSize) async throws -> Data
}
