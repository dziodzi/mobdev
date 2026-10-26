import Contacts
import Foundation

struct SystemContactsService: ContactsService {
    func authorizationStatus() -> ContactsAuthorization {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        switch status {
        case .notDetermined: return .notDetermined
        case .restricted:    return .restricted
        case .denied:        return .denied
        case .authorized:    return .authorized
        default:
            if #available(iOS 18.0, *), status == .limited {
                return .limited
            }
            return .denied
        }
    }

    func requestAccess() async throws -> Bool {
        try await CNContactStore().requestAccess(for: .contacts)
    }

    func fetchContacts() async throws -> [Contact] {
        try await Task.detached(priority: .userInitiated) {
            try Self.readAllContacts()
        }.value
    }

    private static let keysToFetch: [CNKeyDescriptor] = [
        CNContactFormatter.descriptorForRequiredKeys(for: .fullName),
        CNContactGivenNameKey as CNKeyDescriptor,
        CNContactFamilyNameKey as CNKeyDescriptor,
        CNContactNicknameKey as CNKeyDescriptor,
        CNContactOrganizationNameKey as CNKeyDescriptor,
        CNContactDepartmentNameKey as CNKeyDescriptor,
        CNContactJobTitleKey as CNKeyDescriptor,
        CNContactBirthdayKey as CNKeyDescriptor,
        CNContactPhoneNumbersKey as CNKeyDescriptor,
        CNContactEmailAddressesKey as CNKeyDescriptor,
        CNContactPostalAddressesKey as CNKeyDescriptor,
        CNContactUrlAddressesKey as CNKeyDescriptor
    ]

    private static func readAllContacts() throws -> [Contact] {
        let store = CNContactStore()
        let request = CNContactFetchRequest(keysToFetch: keysToFetch)
        request.unifyResults = true

        var result: [Contact] = []
        try store.enumerateContacts(with: request) { cnContact, _ in
            result.append(Contact(cnContact))
        }
        return result
    }
}

private extension Contact {
    init(_ cn: CNContact) {
        let fullName = CNContactFormatter.string(from: cn, style: .fullName) ?? ""
        let name = fullName.isEmpty ? cn.organizationName : fullName

        self.init(
            id: cn.identifier,
            givenName: cn.givenName,
            familyName: cn.familyName,
            displayName: name.trimmingCharacters(in: .whitespacesAndNewlines),
            nickname: cn.nickname.nilIfEmpty,
            organization: cn.organizationName.nilIfEmpty,
            department: cn.departmentName.nilIfEmpty,
            jobTitle: cn.jobTitle.nilIfEmpty,
            birthday: cn.birthday,
            phones: Self.labeled(cn.phoneNumbers, prefix: "phone") { $0.stringValue },
            emails: Self.labeled(cn.emailAddresses, prefix: "email") { $0 as String },
            postalAddresses: Self.labeled(cn.postalAddresses, prefix: "address") {
                CNPostalAddressFormatter.string(from: $0, style: .mailingAddress)
            },
            urls: Self.labeled(cn.urlAddresses, prefix: "url") { $0 as String }
        )
    }

    static func labeled<T>(_ values: [CNLabeledValue<T>],
                           prefix: String,
                           transform: (T) -> String) -> [LabeledValue] {
        values.enumerated().compactMap { index, item in
            let text = transform(item.value).trimmingCharacters(in: .whitespacesAndNewlines)
            guard !text.isEmpty else { return nil }
            let label = item.label.map { CNLabeledValue<T>.localizedString(forLabel: $0) }
            return LabeledValue(id: "\(prefix)-\(index)-\(item.identifier)",
                                label: label?.nilIfEmpty,
                                value: text)
        }
    }
}

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}
