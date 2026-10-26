import SwiftUI

struct ContactDetailView: View {
    let contact: Contact

    var body: some View {
        List {
            Section {
                VStack(spacing: 8) {
                    AvatarView(initials: contact.initials, size: 88)
                    ContactNameText(contact: contact)
                        .font(.title2.weight(.semibold))
                        .multilineTextAlignment(.center)
                    if let subtitle = subtitle {
                        Text(verbatim: subtitle)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                }
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
            }

            if contact.phones.isEmpty && contact.emails.isEmpty {
                Section {
                    Text("detail.noContactInfo")
                        .foregroundStyle(.secondary)
                }
            }

            labeledSection("detail.phones", items: contact.phones)
            labeledSection("detail.emails", items: contact.emails)
            labeledSection("detail.addresses", items: contact.postalAddresses)
            labeledSection("detail.urls", items: contact.urls)

            if hasOtherFields {
                Section("detail.other") {
                    if let nickname = contact.nickname {
                        LabeledContent("detail.nickname", value: nickname)
                    }
                    if let organization = contact.organization {
                        LabeledContent("detail.organization", value: organization)
                    }
                    if let department = contact.department {
                        LabeledContent("detail.department", value: department)
                    }
                    if let jobTitle = contact.jobTitle {
                        LabeledContent("detail.jobTitle", value: jobTitle)
                    }
                    if let birthday = birthdayText {
                        LabeledContent("detail.birthday", value: birthday)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func labeledSection(_ title: LocalizedStringKey, items: [Contact.LabeledValue]) -> some View {
        if !items.isEmpty {
            Section(title) {
                ForEach(items) { item in
                    VStack(alignment: .leading, spacing: 2) {
                        if let label = item.label {
                            Text(verbatim: label)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        } else {
                            Text("detail.label.other")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Text(verbatim: item.value)
                            .textSelection(.enabled)
                    }
                }
            }
        }
    }

    private var subtitle: String? {
        let parts = [contact.jobTitle, contact.hasName ? contact.organization : nil].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: ", ")
    }

    private var hasOtherFields: Bool {
        contact.nickname != nil || contact.organization != nil || contact.department != nil
            || contact.jobTitle != nil || birthdayText != nil
    }

    private var birthdayText: String? {
        guard var components = contact.birthday else { return nil }
        let hasYear = components.year != nil && components.year != NSDateComponentUndefined
        if !hasYear { components.year = 2000 }
        components.calendar = nil

        let calendar = Calendar(identifier: .gregorian)
        guard let date = calendar.date(from: components) else { return nil }

        let formatter = DateFormatter()
        formatter.locale = .current
        formatter.calendar = calendar
        formatter.setLocalizedDateFormatFromTemplate(hasYear ? "dMMMMy" : "dMMMM")
        return formatter.string(from: date)
    }
}
