import Foundation

struct Contact: Identifiable, Hashable {
    struct LabeledValue: Identifiable, Hashable {
        let id: String
        let label: String?
        let value: String
    }

    let id: String
    let givenName: String
    let familyName: String
    let displayName: String
    let nickname: String?
    let organization: String?
    let department: String?
    let jobTitle: String?
    let birthday: DateComponents?
    let phones: [LabeledValue]
    let emails: [LabeledValue]
    let postalAddresses: [LabeledValue]
    let urls: [LabeledValue]

    let searchKey: String

    init(id: String,
         givenName: String = "",
         familyName: String = "",
         displayName: String,
         nickname: String? = nil,
         organization: String? = nil,
         department: String? = nil,
         jobTitle: String? = nil,
         birthday: DateComponents? = nil,
         phones: [LabeledValue] = [],
         emails: [LabeledValue] = [],
         postalAddresses: [LabeledValue] = [],
         urls: [LabeledValue] = []) {
        self.id = id
        self.givenName = givenName
        self.familyName = familyName
        self.displayName = displayName
        self.nickname = nickname
        self.organization = organization
        self.department = department
        self.jobTitle = jobTitle
        self.birthday = birthday
        self.phones = phones
        self.emails = emails
        self.postalAddresses = postalAddresses
        self.urls = urls
        self.searchKey = Contact.normalize(displayName)
    }

    var primaryPhone: String? { phones.first?.value }

    var hasEmail: Bool { !emails.isEmpty }

    var hasName: Bool { !displayName.isEmpty }

    var initials: String {
        let parts = [givenName, familyName].filter { !$0.isEmpty }
        let source = parts.isEmpty ? displayName.split(separator: " ").map(String.init) : parts
        return source.prefix(2).compactMap { $0.first.map(String.init) }.joined().uppercased()
    }

    static func normalize(_ text: String) -> String {
        text.folding(options: [.caseInsensitive, .diacriticInsensitive, .widthInsensitive],
                     locale: .current)
    }
}
