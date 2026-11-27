import Foundation

struct HTTPChatAPI: ChatAPI {
    private let baseURL: URL
    private let session: URLSession
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    init(baseURL: URL, session: URLSession = HTTPChatAPI.makeSession()) {
        self.baseURL = baseURL
        self.session = session
    }

    static func makeSession() -> URLSession {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 15
        configuration.timeoutIntervalForResource = 60
        configuration.waitsForConnectivity = false
        return URLSession(configuration: configuration)
    }

    func login(name: String, password: String) async throws -> String {
        var request = URLRequest(url: url(path: ["login"]))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try encoder.encode(LoginBody(name: name, pwd: password))

        let data: Data
        do {
            data = try await perform(request)
        } catch APIError.unauthorized {
            throw APIError.invalidCredentials
        } catch APIError.server(let status) where (400..<500).contains(status) {
            throw APIError.invalidCredentials
        }

        let token = String(decoding: data, as: UTF8.self).trimmingCharacters(in: .whitespacesAndNewlines)
        guard !token.isEmpty else { throw APIError.invalidResponse }
        return token
    }

    func logout(token: String) async throws {
        var request = authorized(URLRequest(url: url(path: ["logout"])), token: token)
        request.httpMethod = "POST"
        _ = try await perform(request)
    }

    func channels(token: String) async throws -> [String] {
        let request = authorized(URLRequest(url: url(path: ["channels"])), token: token)
        let data = try await perform(request)
        do {
            return try decoder.decode([String].self, from: data)
        } catch {
            throw APIError.invalidResponse
        }
    }

    func messages(channel: String, before: Int?, limit: Int, token: String) async throws -> [Message] {
        var query = [
            URLQueryItem(name: "limit", value: String(limit)),
            URLQueryItem(name: "reverse", value: "true")
        ]
        if let before {
            query.append(URLQueryItem(name: "lastKnownId", value: String(before)))
        }
        let request = authorized(URLRequest(url: url(path: ["channel", channel], query: query)), token: token)
        let data = try await perform(request)
        do {
            return try decoder.decode(LossyArray<Message>.self, from: data).elements
        } catch {
            throw APIError.invalidResponse
        }
    }

    func sendText(_ text: String, from author: String, to channel: String, token: String) async throws -> Int? {
        var request = authorized(URLRequest(url: url(path: ["messages"])), token: token)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try encoder.encode(SendBody(from: author, to: channel, data: .init(text: .init(text: text))))

        let data = try await perform(request)
        let body = String(decoding: data, as: UTF8.self).trimmingCharacters(in: .whitespacesAndNewlines)
        if let id = Int(body) {
            return id
        }
        if let response = try? decoder.decode(IDBody.self, from: data) {
            return response.id
        }
        return nil
    }

    func imageData(link: String, size: ImageSize) async throws -> Data {
        let prefix = size == .thumbnail ? "thumb" : "img"
        let segments = [prefix] + link.split(separator: "/").map(String.init)
        return try await perform(URLRequest(url: url(path: segments)))
    }

    private func perform(_ request: URLRequest) async throws -> Data {
        let result: (Data, URLResponse)
        do {
            result = try await session.data(for: request)
        } catch let error as URLError {
            if error.code == .cancelled { throw CancellationError() }
            throw APIError.connection(mayHaveReachedServer: Self.mayHaveReachedServer(error))
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            throw APIError.connection(mayHaveReachedServer: true)
        }

        let (data, response) = result
        guard let http = response as? HTTPURLResponse else { throw APIError.invalidResponse }
        switch http.statusCode {
        case 200..<300: return data
        case 401: throw APIError.unauthorized
        default: throw APIError.server(status: http.statusCode)
        }
    }

    private static func mayHaveReachedServer(_ error: URLError) -> Bool {
        switch error.code {
        case .notConnectedToInternet, .cannotFindHost, .cannotConnectToHost, .dnsLookupFailed,
             .dataNotAllowed, .internationalRoamingOff, .badURL, .unsupportedURL,
             .appTransportSecurityRequiresSecureConnection:
            return false
        default:
            return true
        }
    }

    private func authorized(_ request: URLRequest, token: String) -> URLRequest {
        var request = request
        request.setValue(token, forHTTPHeaderField: "X-Auth-Token")
        return request
    }

    private func url(path: [String], query: [URLQueryItem] = []) -> URL {
        var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false) ?? URLComponents()
        let allowed = CharacterSet.urlPathAllowed.subtracting(CharacterSet(charactersIn: "/?#"))
        let encoded = path.map { $0.addingPercentEncoding(withAllowedCharacters: allowed) ?? $0 }
        components.percentEncodedPath = "/" + encoded.joined(separator: "/")
        components.queryItems = query.isEmpty ? nil : query
        return components.url ?? baseURL
    }
}

private struct LoginBody: Encodable {
    let name: String
    let pwd: String
}

private struct SendBody: Encodable {
    struct Payload: Encodable {
        struct TextValue: Encodable {
            let text: String
        }

        let text: TextValue

        enum CodingKeys: String, CodingKey {
            case text = "Text"
        }
    }

    let from: String
    let to: String
    let data: Payload
}

private struct IDBody: Decodable {
    let id: Int
}
