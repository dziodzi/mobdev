import Foundation

enum AppConfig {
    static let baseURL: URL = {
        guard let url = URL(string: "http://faerytea.name:8008/") else {
            preconditionFailure("Invalid server URL")
        }
        return url
    }()

    static let pageSize = 20
    static let keychainService = "com.antondementiev.chatsapp.session"
}
