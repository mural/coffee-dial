import SwiftUI
import UniformTypeIdentifiers
import CoffeeDialShared

@main
struct CoffeeDialApp: App {
    @StateObject private var backup = BackupCoordinator()

    var body: some Scene {
        WindowGroup {
            CoffeeDialView(files: backup.files).ignoresSafeArea()
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
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController(backupFiles: files)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
