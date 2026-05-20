// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapgoCapacitorDeviceIntegrity",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapgoCapacitorDeviceIntegrity",
            targets: ["DeviceIntegrityPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "DeviceIntegrityPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/DeviceIntegrityPlugin"),
        .testTarget(
            name: "DeviceIntegrityPluginTests",
            dependencies: ["DeviceIntegrityPlugin"],
            path: "ios/Tests/DeviceIntegrityPluginTests")
    ]
)
