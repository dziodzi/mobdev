import XCTest
@testable import ContactsApp

final class MockContactsService: ContactsService {
    var status: ContactsAuthorization
    var contacts: [Contact]
    var error: Error?
    var statusAfterRequest: ContactsAuthorization?
    private(set) var fetchCalls = 0
    private(set) var requestCalls = 0

    init(status: ContactsAuthorization, contacts: [Contact] = []) {
        self.status = status
        self.contacts = contacts
    }

    func authorizationStatus() -> ContactsAuthorization { status }

    func requestAccess() async throws -> Bool {
        requestCalls += 1
        if let next = statusAfterRequest { status = next }
        return status == .authorized || status == .limited
    }

    func fetchContacts() async throws -> [Contact] {
        fetchCalls += 1
        if let error { throw error }
        return contacts
    }
}

@MainActor
final class ContactsStoreTests: XCTestCase {
    private let sample = [
        Contact(id: "1", givenName: "Борис", displayName: "Борис", phones: [.init(id: "p", label: nil, value: "+7 900")]),
        Contact(id: "2", givenName: "Анна", displayName: "Анна", emails: [.init(id: "e", label: nil, value: "a@a.ru")]),
        Contact(id: "3", givenName: "Вера", displayName: "Вера")
    ]

    func testLoadsOnceAfterRepeatedStartAndActivation() async {
        let service = MockContactsService(status: .authorized, contacts: sample)
        let store = ContactsStore(service: service)

        await store.startIfNeeded()
        await store.startIfNeeded()
        await store.sceneDidBecomeActive()

        XCTAssertEqual(service.fetchCalls, 1)
        XCTAssertEqual(store.state, .loaded)
    }

    func testSearchAndSortDoNotReload() async {
        let service = MockContactsService(status: .authorized, contacts: sample)
        let store = ContactsStore(service: service)
        await store.startIfNeeded()

        store.searchText = "ан"
        XCTAssertEqual(store.visibleContacts.map(\.displayName), ["Анна"])
        store.searchText = ""
        store.sortOrder = .descending
        XCTAssertEqual(store.visibleContacts.map(\.displayName), ["Вера", "Борис", "Анна"])

        XCTAssertEqual(service.fetchCalls, 1)
    }

    func testAscendingSort() async {
        let store = ContactsStore(service: MockContactsService(status: .authorized, contacts: sample))
        await store.startIfNeeded()
        XCTAssertEqual(store.visibleContacts.map(\.displayName), ["Анна", "Борис", "Вера"])
    }

    func testEmptySearchResult() async {
        let store = ContactsStore(service: MockContactsService(status: .authorized, contacts: sample))
        await store.startIfNeeded()
        store.searchText = "zzz"
        XCTAssertTrue(store.visibleContacts.isEmpty)
    }

    func testDeniedIsDifferentFromEmpty() async {
        let denied = ContactsStore(service: MockContactsService(status: .denied))
        await denied.startIfNeeded()
        XCTAssertEqual(denied.state, .denied)

        let empty = ContactsStore(service: MockContactsService(status: .authorized, contacts: []))
        await empty.startIfNeeded()
        XCTAssertEqual(empty.state, .empty)
    }

    func testFirstLaunchRequestsPermission() async {
        let service = MockContactsService(status: .notDetermined, contacts: sample)
        service.statusAfterRequest = .authorized
        let store = ContactsStore(service: service)

        await store.startIfNeeded()

        XCTAssertEqual(service.requestCalls, 1)
        XCTAssertEqual(store.state, .loaded)
    }

    func testReturnFromSettingsWithPermissionLoadsContacts() async {
        let service = MockContactsService(status: .denied, contacts: sample)
        let store = ContactsStore(service: service)
        await store.startIfNeeded()
        XCTAssertEqual(service.fetchCalls, 0)

        service.status = .authorized
        await store.sceneDidBecomeActive()

        XCTAssertEqual(store.state, .loaded)
        XCTAssertEqual(service.fetchCalls, 1)
    }

    func testRequestAgainWhenDeniedOffersSettings() async {
        let service = MockContactsService(status: .denied)
        let store = ContactsStore(service: service)
        await store.startIfNeeded()

        await store.requestAccess()

        XCTAssertTrue(store.isSettingsPromptPresented)
        XCTAssertEqual(store.state, .denied)
    }

    func testFetchErrorShowsFailedStateAndRetryWorks() async {
        struct ReadError: Error {}
        let service = MockContactsService(status: .authorized, contacts: sample)
        service.error = ReadError()
        let store = ContactsStore(service: service)

        await store.startIfNeeded()
        if case .failed = store.state {} else { XCTFail("ожидалось состояние ошибки") }

        service.error = nil
        await store.retry()
        XCTAssertEqual(store.state, .loaded)
    }

    func testContactStoreChangeReloads() async {
        let service = MockContactsService(status: .authorized, contacts: sample)
        let store = ContactsStore(service: service)
        await store.startIfNeeded()

        await store.contactStoreDidChange()

        XCTAssertEqual(service.fetchCalls, 2)
    }

    func testContactWithoutPhoneAndEmail() {
        let contact = Contact(id: "x", displayName: "")
        XCTAssertNil(contact.primaryPhone)
        XCTAssertFalse(contact.hasEmail)
        XCTAssertFalse(contact.hasName)
    }
}
