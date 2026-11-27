import SwiftUI

struct LoginView: View {
    let session: SessionStore

    @State private var username = ""
    @State private var password = ""
    @FocusState private var focusedField: Field?

    private enum Field {
        case username
        case password
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 28) {
                header
                fields
                feedback
                loginButton
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 40)
            .frame(maxWidth: 460)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color(.systemGroupedBackground))
    }

    private var header: some View {
        VStack(spacing: 14) {
            Image(systemName: "message.fill")
                .font(.system(size: 42, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 88, height: 88)
                .background(DesignSystem.appIconGradient, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                .accessibilityHidden(true)
            Text("login.title")
                .font(.largeTitle.bold())
                .multilineTextAlignment(.center)
            Text("login.subtitle")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    private var fields: some View {
        VStack(spacing: 0) {
            TextField("login.username", text: $username)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.next)
                .focused($focusedField, equals: .username)
                .onSubmit { focusedField = .password }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            Divider()
                .padding(.leading, 16)
            SecureField("login.password", text: $password)
                .textContentType(.password)
                .submitLabel(.go)
                .focused($focusedField, equals: .password)
                .onSubmit(submit)
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
        }
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .disabled(session.isLoggingIn)
        .onChange(of: username) { session.clearLoginError() }
        .onChange(of: password) { session.clearLoginError() }
    }

    @ViewBuilder
    private var feedback: some View {
        if session.notice == .sessionExpired {
            Label("session.expired", systemImage: "clock.badge.exclamationmark")
                .font(.callout)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        if let error = session.loginError {
            Label {
                Text(error.messageKey)
            } icon: {
                Image(systemName: "exclamationmark.circle.fill")
            }
            .font(.callout.weight(.medium))
            .foregroundStyle(.red)
            .multilineTextAlignment(.center)
            .accessibilityAddTraits(.updatesFrequently)
        }
    }

    private var loginButton: some View {
        Button(action: submit) {
            ZStack {
                Text("login.button")
                    .font(.headline)
                    .opacity(session.isLoggingIn ? 0 : 1)
                if session.isLoggingIn {
                    ProgressView()
                        .tint(.white)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 34)
        }
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.capsule)
        .controlSize(.large)
        .disabled(!canSubmit)
        .accessibilityLabel(session.isLoggingIn ? Text("login.inProgress") : Text("login.button"))
    }

    private var canSubmit: Bool {
        !session.isLoggingIn
            && !username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !password.isEmpty
    }

    private func submit() {
        guard !session.isLoggingIn else { return }
        focusedField = nil
        Task {
            await session.login(username: username, password: password)
        }
    }
}
