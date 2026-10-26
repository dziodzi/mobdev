import SwiftUI

struct ContactListView: View {
    @Bindable var store: ContactsStore
    let openSettings: () -> Void

    var body: some View {
        let visible = store.visibleContacts

        List {
            if store.isLimitedAccess {
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("contacts.limited.message")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                        Button("contacts.limited.manage", action: openSettings)
                            .font(.footnote)
                    }
                }
            }

            Section {
                ForEach(visible) { contact in
                    NavigationLink(value: contact) {
                        ContactRowView(contact: contact)
                    }
                }
            } header: {
                Text("contacts.count \(visible.count)")
            }
        }
        .listStyle(.plain)
        .overlay {
            if visible.isEmpty && !store.searchText.isEmpty {
                ContentUnavailableView.search(text: store.searchText)
            }
        }
        .searchable(text: $store.searchText, prompt: Text("search.prompt"))
        .autocorrectionDisabled()
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Picker("sort.title", selection: $store.sortOrder) {
                        ForEach(SortOrder.allCases) { order in
                            Label(order.titleKey, systemImage: order.systemImage)
                                .tag(order)
                        }
                    }
                } label: {
                    Label("sort.title", systemImage: "arrow.up.arrow.down")
                }
            }
        }
    }
}
