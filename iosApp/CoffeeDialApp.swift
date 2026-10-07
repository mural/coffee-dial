import SwiftUI
import UniformTypeIdentifiers
import CoffeeDialShared
import AuthenticationServices
import GoogleSignIn

@main
struct CoffeeDialApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var backup = BackupCoordinator()
    @StateObject private var account = AccountCoordinator()

    var body: some Scene {
        WindowGroup {
            CoffeeDialView(files: backup.files, account: account.repository).ignoresSafeArea()
                .onOpenURL { url in _ = GIDSignIn.sharedInstance.handle(url) }
                .task { account.restore() }
                .onChange(of: scenePhase) { phase in
                    if phase == .active { account.checkAppleCredential() }
                }
                .fileExporter(
                    isPresented: $backup.exporting,
                    document: backup.document,
                    contentType: .json,
                    defaultFilename: backup.filename
                ) { result in
                    switch result {
                    case .success: backup.files.finish(result: "Backup exportado.")
                    case .failure: backup.files.finish(result: "No se pudo guardar el backup.")
                    }
                    backup.document = nil
                }
                .fileImporter(isPresented: $backup.importing, allowedContentTypes: [.json, .plainText, .data]) { result in
                    backup.read(result)
                }
                .onChange(of: backup.exporting) { presented in
                    if !presented && backup.files.busy {
                        backup.files.finish(result: nil)
                    }
                }
                .onChange(of: backup.importing) { presented in
                    // read() owns completion once a URL has been selected.
                    if !presented && !backup.reading && backup.files.busy {
                        backup.files.finish(result: nil)
                    }
                }
        }
    }
}

private final class BackupCoordinator: ObservableObject {
    let files = BackupFiles()
    @Published var exporting = false
    @Published var importing = false
    @Published var document: BackupDocument?
    var reading = false
    var filename = "coffee-dial.json"

    init() {
        files.exportAction = { [weak self] in
            guard let self, let text = self.files.pendingExport else { return }
            self.document = BackupDocument(text: text)
            self.filename = "coffee-dial-\(Int(Date().timeIntervalSince1970)).json"
            self.exporting = true
            return
        }
        files.importAction = { [weak self] in
            self?.importing = true
            return
        }
    }

    func read(_ result: Result<URL, Error>) {
        guard case .success(let url) = result else {
            files.finish(result: "No se pudo abrir el archivo.")
            return
        }
        reading = true
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            let access = url.startAccessingSecurityScopedResource()
            defer { if access { url.stopAccessingSecurityScopedResource() } }
            do {
                let handle = try FileHandle(forReadingFrom: url)
                defer { try? handle.close() }
                var data = Data()
                while let chunk = try handle.read(upToCount: 8192), !chunk.isEmpty {
                    guard data.count + chunk.count <= 10 * 1024 * 1024 else {
                        throw CocoaError(.fileReadTooLarge)
                    }
                    data.append(chunk)
                }
                guard let text = String(data: data, encoding: .utf8) else {
                    throw CocoaError(.fileReadInapplicableStringEncoding)
                }
                DispatchQueue.main.async {
                    self?.reading = false
                    self?.files.receiveImport(text: text)
                }
            } catch {
                DispatchQueue.main.async {
                    self?.reading = false
                    self?.files.finish(result: "No se pudo leer el archivo. Elegí un backup UTF-8 de hasta 10 MB.")
                }
            }
        }
    }
}

private struct BackupDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }
    let text: String

    init(text: String) { self.text = text }
    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents,
              let text = String(data: data, encoding: .utf8) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        self.text = text
    }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}

struct CoffeeDialView: UIViewControllerRepresentable {
    let files: BackupFiles
    let account: IosAuthRepository
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController(backupFiles: files, authRepository: account)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// SDKs own credentials. Only provider-confirmed profile data crosses into shared UI.
private final class AccountCoordinator: NSObject, ObservableObject,
    ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    let repository = IosAuthRepository()
    private var controller: ASAuthorizationController?
    private var restored = false
    private var generation = 0
    private var appleGeneration = 0
    private var revocationObserver: NSObjectProtocol?
    private let defaults = UserDefaults.standard

    override init() {
        super.init()
        repository.onSyncTokenObtained = { [weak self] token in
            self?.defaults.set(token, forKey: "coffee.syncToken")
        }
        repository.onSyncTokenCleared = { [weak self] in
            self?.defaults.removeObject(forKey: "coffee.syncToken")
        }
        repository.onAuthenticationRequired = { [weak self] in
            self?.defaults.removeObject(forKey: "coffee.auth.provider")
            self?.defaults.removeObject(forKey: "coffee.apple.idToken")
        }
        repository.refreshGoogleAction = { [weak self] done in
            guard let self, let user = GIDSignIn.sharedInstance.currentUser,
                  self.defaults.string(forKey: "coffee.auth.provider") == "google" else {
                done(nil); return
            }
            let attempt = self.generation
            user.refreshTokensIfNeeded { [weak self] refreshed, error in
                guard let self, self.generation == attempt, error == nil,
                      refreshed?.userID == user.userID else { done(nil); return }
                self.repository.googleIdToken = refreshed?.idToken?.tokenString
                done(self.repository.googleIdToken)
            }
        }
        revocationObserver = NotificationCenter.default.addObserver(
            forName: ASAuthorizationAppleIDProvider.credentialRevokedNotification,
            object: nil, queue: .main
        ) { [weak self] _ in self?.checkAppleCredential() }
        repository.googleAction = { [weak self] in self?.google() }
        repository.appleAction = { [weak self] in self?.apple() }
        repository.signOutAction = { [weak self] in
            guard let self else { return }
            self.generation += 1
            if #available(iOS 16.0, *) {
                self.controller?.cancel()
            }
            self.controller = nil
            GIDSignIn.sharedInstance.signOut()
            self.defaults.removeObject(forKey: "coffee.auth.provider")
            self.defaults.removeObject(forKey: "coffee.apple.idToken")
            self.defaults.removeObject(forKey: "coffee.syncToken")
            // Keep Apple's first-consent profile for subsequent logins to the same subject.
        }
        // Email-only legacy entries were never verified and must not restore a session.
        for key in ["user_email", "user_name", "user_provider"] { defaults.removeObject(forKey: key) }
    }

    private var window: UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
        ?? UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first
    }
    private var presenter: UIViewController? {
        var presenter = window?.rootViewController
        while let presented = presenter?.presentedViewController { presenter = presented }
        return presenter
    }
    private var googleConfigured: Bool {
        guard let id = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String,
              id.hasSuffix(".apps.googleusercontent.com") else { return false }
        let reversed = id.split(separator: ".").reversed().joined(separator: ".")
        let types = Bundle.main.object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]] ?? []
        return types.contains { ($0["CFBundleURLSchemes"] as? [String])?.contains(reversed) == true }
    }
    private var appleConfigured: Bool {
        Bundle.main.object(forInfoDictionaryKey: "CoffeeDialAppleSignInEnabled") as? String == "YES"
    }

    func restore() {
        guard !restored else { return }
        restored = true
        if let syncToken = defaults.string(forKey: "coffee.syncToken") {
            repository.syncToken = syncToken
        }
        let attempt = generation
        if defaults.string(forKey: "coffee.auth.provider") == "google", googleConfigured {
            GIDSignIn.sharedInstance.restorePreviousSignIn { [weak self] user, _ in
                guard let self, self.generation == attempt, let user else { return }
                self.acceptGoogle(user)
            }
        } else {
            checkAppleCredential()
        }
    }

    deinit {
        if let revocationObserver { NotificationCenter.default.removeObserver(revocationObserver) }
    }

    func checkAppleCredential() {
        guard defaults.string(forKey: "coffee.auth.provider") == "apple", appleConfigured,
              let id = defaults.string(forKey: "coffee.apple.id") else { return }
        let attempt = generation
        ASAuthorizationAppleIDProvider().getCredentialState(forUserID: id) { [weak self] state, error in
            DispatchQueue.main.async {
                guard let self, self.generation == attempt, error == nil else { return }
                switch state {
                case .authorized:
                    if let savedToken = self.defaults.string(forKey: "coffee.apple.idToken") {
                        self.repository.appleIdToken = savedToken
                    }
                    if let syncToken = self.defaults.string(forKey: "coffee.syncToken") {
                        self.repository.syncToken = syncToken
                    }
                    self.acceptApple(id: id, email: nil, name: nil)
                case .revoked, .notFound, .transferred:
                    self.generation += 1
                    self.defaults.removeObject(forKey: "coffee.auth.provider")
                    self.defaults.removeObject(forKey: "coffee.apple.idToken")
                    self.defaults.removeObject(forKey: "coffee.syncToken")
                    self.repository.cancelled()
                @unknown default: break
                }
            }
        }
    }

    private func google() {
        generation += 1
        let attempt = generation
        guard googleConfigured else {
            repository.failed(message: "Google todavía no está configurado para esta versión de Coffee Dial.")
            return
        }
        guard let presenter else { repository.cancelled(); return }
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { [weak self] result, error in
            guard let self, self.generation == attempt else { return }
            if let user = result?.user { self.acceptGoogle(user) }
            else if (error as NSError?)?.code == GIDSignInError.canceled.rawValue { self.repository.cancelled() }
            else { self.repository.failed(message: "No se pudo iniciar sesión con Google. Intentá nuevamente.") }
        }
    }

    private func acceptGoogle(_ google: GIDGoogleUser) {
        guard let id = google.userID, !id.isEmpty else { repository.cancelled(); return }
        defaults.set("google", forKey: "coffee.auth.provider")
        repository.googleIdToken = google.idToken?.tokenString
        if let syncToken = defaults.string(forKey: "coffee.syncToken") {
            repository.syncToken = syncToken
        }
        repository.authenticated(user: User(id: id, email: google.profile?.email,
            displayName: google.profile?.name, photoUrl: nil, provider: .google, linkedAt: 0))
    }

    private func apple() {
        generation += 1
        appleGeneration = generation
        guard appleConfigured else {
            repository.failed(message: "Apple todavía no está configurado para esta versión de Coffee Dial.")
            return
        }
        guard window != nil else { repository.cancelled(); return }
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        let controller = ASAuthorizationController(authorizationRequests: [request])
        self.controller = controller
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        window ?? ASPresentationAnchor()
    }
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard self.controller === controller, generation == appleGeneration else { return }
        defer { self.controller = nil }
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            repository.failed(message: "Apple no devolvió una credencial válida.")
            return
        }
        let name = credential.fullName.map { PersonNameComponentsFormatter().string(from: $0) }
        let idToken = credential.identityToken.flatMap { String(data: $0, encoding: .utf8) }
        if let idToken { defaults.set(idToken, forKey: "coffee.apple.idToken") }
        repository.appleIdToken = idToken
        acceptApple(id: credential.user, email: credential.email, name: name)
    }
    private func acceptApple(id: String, email: String?, name: String?) {
        guard !id.isEmpty else { repository.cancelled(); return }
        let sameUser = defaults.string(forKey: "coffee.apple.id") == id
        let savedEmail = email ?? (sameUser ? defaults.string(forKey: "coffee.apple.email") : nil)
        let savedName = name.flatMap { $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : $0 } ?? (sameUser ? defaults.string(forKey: "coffee.apple.name") : nil)
        defaults.set(id, forKey: "coffee.apple.id")
        defaults.set(savedEmail, forKey: "coffee.apple.email")
        defaults.set(savedName, forKey: "coffee.apple.name")
        defaults.set("apple", forKey: "coffee.auth.provider")
        if let token = defaults.string(forKey: "coffee.apple.idToken") {
            repository.appleIdToken = token
        }
        if let syncToken = defaults.string(forKey: "coffee.syncToken") {
            repository.syncToken = syncToken
        }
        repository.authenticated(user: User(id: id, email: savedEmail, displayName: savedName,
            photoUrl: nil, provider: .apple, linkedAt: 0))
    }
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        guard self.controller === controller, generation == appleGeneration else { return }
        self.controller = nil
        if (error as NSError).code == ASAuthorizationError.canceled.rawValue { repository.cancelled() }
        else { repository.failed(message: "No se pudo iniciar sesión con Apple. Revisá la cuenta del dispositivo y volvé a intentar.") }
    }
}
