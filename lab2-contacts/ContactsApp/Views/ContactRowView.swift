import SwiftUI

struct ContactRowView: View {
    let contact: Contact

    var body: some View {
        HStack(spacing: 12) {
            AvatarView(initials: contact.initials, size: 40)

            VStack(alignment: .leading, spacing: 2) {
                ContactNameText(contact: contact)
                    .font(.body)
                    .lineLimit(1)

                if let phone = contact.primaryPhone {
                    Text(verbatim: phone)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                } else {
                    Text("contacts.noPhone")
                        .font(.subheadline)
                        .foregroundStyle(.tertiary)
                }
            }

            Spacer(minLength: 8)

            if contact.hasEmail {
                Image(systemName: "envelope.fill")
                    .foregroundStyle(.tint)
                    .accessibilityLabel(Text("contacts.hasEmail"))
            }
        }
        .padding(.vertical, 2)
    }
}

struct ContactNameText: View {
    let contact: Contact

    var body: some View {
        if contact.hasName {
            Text(verbatim: contact.displayName)
        } else {
            Text("contacts.noName")
                .foregroundStyle(.secondary)
        }
    }
}

struct AvatarView: View {
    let initials: String
    let size: CGFloat

    var body: some View {
        ZStack {
            Circle()
                .fill(.gray.gradient)
            if initials.isEmpty {
                Image(systemName: "person.fill")
                    .font(.system(size: size * 0.45))
                    .foregroundStyle(.white)
            } else {
                Text(verbatim: initials)
                    .font(.system(size: size * 0.4, weight: .semibold))
                    .foregroundStyle(.white)
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}
