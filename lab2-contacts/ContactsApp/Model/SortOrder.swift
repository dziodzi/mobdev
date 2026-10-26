import SwiftUI

enum SortOrder: String, CaseIterable, Identifiable {
    case ascending
    case descending

    var id: String { rawValue }

    var titleKey: LocalizedStringKey {
        switch self {
        case .ascending:  return "sort.ascending"
        case .descending: return "sort.descending"
        }
    }

    var systemImage: String {
        switch self {
        case .ascending:  return "arrow.down"
        case .descending: return "arrow.up"
        }
    }
}
