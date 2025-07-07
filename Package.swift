// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "bitchat",
    platforms: [
        .iOS(.v16),
        .macOS(.v13)
    ],
    products: [
        .executable(
            name: "bitchat",
            targets: ["bitchat"]
        ),
    ],
    targets: [
        .executableTarget(
            name: "bitchat",
            path: "bitchat"
        ),
        .testTarget(
            name: "bitchatTests",
            dependencies: ["bitchat"],
            path: "bitchatTests",
            exclude: ["Info.plist"],
            sources: [
                "BinaryProtocolTests.swift",
                "BitchatMessageTests.swift",
                "BloomFilterTests.swift",
                "MessagePaddingTests.swift",
                "PasswordProtectedRoomTests.swift"
            ]
        ),
    ]
)