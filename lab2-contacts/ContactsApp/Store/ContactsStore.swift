import Foundation
import Observation

@MainActor
@Observable
final class ContactsStore {
    enum State: Equatable {
        case idle
        case loading
        case needsPermission
        case denied
        case restricted
        case empty
        case loaded
        case failed(String)
    }

    private(set) var state: State = .idle
    private(set) var contacts: [Contact] = []
    private(set) var isLimitedAccess = false
    var searchText = ""
    var sortOrder: SortOrder = .ascending
    var isSettingsPromptPresented = false

    private(set) var fetchCount = 0

    private let service: ContactsService
    private var lastStatus: ContactsAuthorization?
    private var hasStarted = false
    private var isFetching = false

    init(service: ContactsService) {
        self.service = service
    }

    var visibleContacts: [Contact] {
        let query = Contact.normalize(searchText.trimmingCharacters(in: .whitespaces))
        let filtered = query.isEmpty
            ? contacts
            : contacts.filter { $0.searchKey.contains(query) }
        guard sortOrder == .descending else { return filtered }
        let named = filtered.filter(\.hasName)
        let unnamed = filtered.filter { !$0.hasName }
        return named.reversed() + unnamed
    }

    func startIfNeeded() async {
        guard !hasStarted else { return }
        hasStarted = true

        if service.authorizationStatus() == .notDetermined {
            state = .needsPermission
            _ = try? await service.requestAccess()
        }
        await syncWithAuthorization()
    }

    func sceneDidBecomeActive() async {
        guard hasStarted else { return }
        await syncWithAuthorization()
    }

    func requestAccess() async {
        let before = service.authorizationStatus()
        switch before {
        case .notDetermined, .denied:
            let granted = (try? await service.requestAccess()) ?? false
            await syncWithAuthorization()
            if !granted, before == .denied, service.authorizationStatus() == .denied {
                isSettingsPromptPresented = true
            }
        default:
            await syncWithAuthorization()
        }
    }

    func retry() async {
        await loadContacts()
    }

    func contactStoreDidChange() async {
        guard lastStatus == .authorized || lastStatus == .limited else { return }
        await loadContacts()
    }

    private func syncWithAuthorization() async {
        let status = service.authorizationStatus()
        let changed = status != lastStatus
        lastStatus = status
        isLimitedAccess = (status == .limited)

        switch status {
        case .notDetermined:
            clearContacts()
            state = .needsPermission
        case .denied:
            clearContacts()
            state = .denied
        case .restricted:
            clearContacts()
            state = .restricted
        case .authorized, .limited:
            if changed {
                await loadContacts()
            }
        }
    }

    private func loadContacts() async {
        guard !isFetching else { return }
        isFetching = true
        defer { isFetching = false }

        if contacts.isEmpty {
            state = .loading
        }
        fetchCount += 1

        do {
            let fetched = try await service.fetchContacts()
            contacts = Self.sortedByName(fetched)
            state = contacts.isEmpty ? .empty : .loaded
        } catch {
            clearContacts()
            state = .failed(error.localizedDescription)
        }
    }

    private func clearContacts() {
        contacts = []
    }

    static func sortedByName(_ list: [Contact]) -> [Contact] {
        list.sorted { lhs, rhs in
            if lhs.hasName != rhs.hasName { return lhs.hasName }
            return lhs.displayName.localizedStandardCompare(rhs.displayName) == .orderedAscending
        }
    }
}
