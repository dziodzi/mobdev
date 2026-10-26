import Foundation

enum ContactsAuthorization: Equatable {
    case notDetermined
    case denied
    case restricted
    case authorized
    case limited
}

protocol ContactsService {
    func authorizationStatus() -> ContactsAuthorization
    func requestAccess() async throws -> Bool
    func fetchContacts() async throws -> [Contact]
}
