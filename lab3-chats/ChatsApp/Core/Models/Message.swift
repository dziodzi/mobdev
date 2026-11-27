import Foundation

struct Message: Identifiable, Hashable {
    enum Content: Hashable {
        case text(String)
        case image(link: String)
        case unsupported
    }

    let id: Int
    let author: String
    let channel: String
    let content: Content
    let date: Date?
}

extension Message: Decodable {
    private enum CodingKeys: String, CodingKey {
        case id, from, to, data, time
    }

    private enum PayloadKeys: String, CodingKey {
        case text = "Text"
        case image = "Image"
    }

    private struct TextPayload: Decodable {
        let text: String
    }

    private struct ImagePayload: Decodable {
        let link: String?
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(Int.self, forKey: .id)
        author = (try? container.decodeIfPresent(String.self, forKey: .from)) ?? ""
        channel = (try? container.decodeIfPresent(String.self, forKey: .to)) ?? ""

        if let milliseconds = try? container.decodeIfPresent(Double.self, forKey: .time) {
            date = Date(timeIntervalSince1970: milliseconds / 1000)
        } else {
            date = nil
        }

        let payload = try? container.nestedContainer(keyedBy: PayloadKeys.self, forKey: .data)
        if let text = try? payload?.decodeIfPresent(TextPayload.self, forKey: .text) {
            content = .text(text.text)
        } else if let image = try? payload?.decodeIfPresent(ImagePayload.self, forKey: .image),
                  let link = image.link, !link.isEmpty {
            content = .image(link: link)
        } else {
            content = .unsupported
        }
    }
}

struct LossyArray<Element: Decodable>: Decodable {
    let elements: [Element]

    private struct Skipped: Decodable {
        init(from decoder: Decoder) throws {}
    }

    init(from decoder: Decoder) throws {
        var container = try decoder.unkeyedContainer()
        var result: [Element] = []
        while !container.isAtEnd {
            if let element = try? container.decode(Element.self) {
                result.append(element)
            } else if (try? container.decode(Skipped.self)) == nil {
                break
            }
        }
        elements = result
    }
}
