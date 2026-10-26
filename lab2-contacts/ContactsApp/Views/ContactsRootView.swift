import Contacts
import SwiftUI
import UIKit

struct ContactsRootView: View {
    @Bindable var store: ContactsStore

    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            ZStack {
                content
            }
            .navigationTitle("contacts.title")
            .navigationDestination(for: Contact.self) { contact in
                ContactDetailView(contact: contact)
            }
        }
        .task {
            await store.startIfNeeded()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                Task { await store.sceneDidBecomeActive() }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .CNContactStoreDidChange)) { _ in
            Task { await store.contactStoreDidChange() }
        }
        .alert("permission.settingsAlert.title", isPresented: $store.isSettingsPromptPresented) {
            Button("permission.openSettings", action: openSettings)
            Button("common.cancel", role: .cancel) {}
        } message: {
            Text("permission.settingsAlert.message")
        }
    }

    @ViewBuilder
    private var content: some View {
        switch store.state {
        case .idle, .loading:
            ProgressView("state.loading")

        case .needsPermission:
            ContentUnavailableView {
                Label("state.notDetermined.title", systemImage: "person.crop.circle.badge.questionmark")
            } description: {
                Text("state.notDetermined.message")
            } actions: {
                Button("permission.allow") {
                    Task { await store.requestAccess() }
                }
                .buttonStyle(.borderedProminent)
            }

        case .denied:
            ContentUnavailableView {
                Label("state.denied.title", systemImage: "person.crop.circle.badge.xmark")
            } description: {
                Text("state.denied.message")
            } actions: {
                Button("permission.requestAgain") {
                    Task { await store.requestAccess() }
                }
                .buttonStyle(.borderedProminent)
                Button("permission.openSettings", action: openSettings)
            }

        case .restricted:
            ContentUnavailableView {
                Label("state.restricted.title", systemImage: "lock.circle")
            } description: {
                Text("state.restricted.message")
            } actions: {
                Button("permission.openSettings", action: openSettings)
            }

        case .empty:
            ContentUnavailableView {
                Label("state.empty.title", systemImage: "person.2.slash")
            } description: {
                Text("state.empty.message")
            }

        case .failed(let message):
            ContentUnavailableView {
                Label("state.error.title", systemImage: "exclamationmark.triangle")
            } description: {
                Text(message)
            } actions: {
                Button("common.retry") {
                    Task { await store.retry() }
                }
                .buttonStyle(.borderedProminent)
            }

        case .loaded:
            ContactListView(store: store, openSettings: openSettings)
        }
    }

    private func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        openURL(url)
    }
}
