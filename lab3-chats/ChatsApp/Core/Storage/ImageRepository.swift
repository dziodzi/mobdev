import Observation
import UIKit

@MainActor
@Observable
final class ImageRepository {
    @ObservationIgnored private let api: ChatAPI
    @ObservationIgnored private let cache = NSCache<NSString, UIImage>()
    @ObservationIgnored private var inFlight: [String: Task<UIImage, Error>] = [:]

    init(api: ChatAPI) {
        self.api = api
        cache.countLimit = 200
    }

    func cachedImage(link: String, size: ImageSize) -> UIImage? {
        cache.object(forKey: key(link, size) as NSString)
    }

    func image(link: String, size: ImageSize) async throws -> UIImage {
        let key = key(link, size)
        if let cached = cache.object(forKey: key as NSString) {
            return cached
        }
        if let running = inFlight[key] {
            return try await running.value
        }

        let api = self.api
        let task = Task.detached(priority: .userInitiated) { () throws -> UIImage in
            let data = try await api.imageData(link: link, size: size)
            guard let image = UIImage(data: data) else { throw APIError.invalidResponse }
            return image
        }
        inFlight[key] = task
        defer { inFlight[key] = nil }

        let image = try await task.value
        cache.setObject(image, forKey: key as NSString)
        return image
    }

    private func key(_ link: String, _ size: ImageSize) -> String {
        "\(size == .thumbnail ? "thumb" : "full")|\(link)"
    }
}
