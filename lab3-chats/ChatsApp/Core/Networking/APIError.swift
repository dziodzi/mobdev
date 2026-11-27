import Foundation

enum APIError: Error, Equatable {
    case invalidCredentials
    case unauthorized
    case connection(mayHaveReachedServer: Bool)
    case server(status: Int)
    case invalidResponse
}

enum NetworkProblem: Equatable {
    case connection
    case server
}

extension APIError {
    var problem: NetworkProblem {
        switch self {
        case .connection: return .connection
        default: return .server
        }
    }

    var mayHaveBeenDelivered: Bool {
        switch self {
        case .connection(let mayHaveReachedServer): return mayHaveReachedServer
        case .server, .invalidResponse: return true
        case .invalidCredentials, .unauthorized: return false
        }
    }
}

extension Error {
    var isCancellation: Bool {
        if self is CancellationError { return true }
        if let urlError = self as? URLError, urlError.code == .cancelled { return true }
        return false
    }
}
