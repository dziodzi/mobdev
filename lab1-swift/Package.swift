// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CalculatorCore",
    products: [.library(name: "CalculatorCore", targets: ["CalculatorCore"])],
    targets: [
        .target(name: "CalculatorCore", path: "AdvancedCalculator/Core"),
        .testTarget(name: "CalculatorCoreTests", dependencies: ["CalculatorCore"], path: "Tests/CalculatorCoreTests")
    ]
)
