import SwiftUI
import CoffeeDialShared

@main
struct CoffeeDialApp: App {
    var body: some Scene {
        WindowGroup {
            CoffeeDialView().ignoresSafeArea()
        }
    }
}

struct CoffeeDialView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
