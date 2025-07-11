![ChatGPT Image Jul 5, 2025 at 06_07_31 PM](https://github.com/user-attachments/assets/2660f828-49c7-444d-beca-d8b01854667a)
# bitchat

A secure, decentralized, peer-to-peer messaging app that works over Bluetooth mesh networks. No internet required, no servers, no phone numbers - just pure encrypted communication.

## License

This project is released into the public domain. See the [LICENSE](LICENSE) file for details.

## Features

- **Decentralized Mesh Network**: Automatic peer discovery and multi-hop message relay over Bluetooth LE
- **End-to-End Encryption**: X25519 key exchange + AES-256-GCM for private messages
- **Room-Based Chats**: Topic-based group messaging with optional password protection
- **Store & Forward**: Messages cached for offline peers and delivered when they reconnect
- **Privacy First**: No accounts, no phone numbers, no persistent identifiers
- **IRC-Style Commands**: Familiar `/join`, `/msg`, `/who` style interface
- **Message Retention**: Optional room-wide message saving controlled by room owners
- **Universal App**: Native support for iOS and macOS
- **Cover Traffic**: Timing obfuscation and dummy messages for enhanced privacy
- **Emergency Wipe**: Triple-tap to instantly clear all data
- **Performance Optimizations**: LZ4 message compression, adaptive battery modes, and optimized networking

## Setup

### Option 1: Using XcodeGen (Recommended)

1. Install XcodeGen if you haven't already:
   ```bash
   brew install xcodegen
   ```

2. Generate the Xcode project:
   ```bash
   cd bitchat
   xcodegen generate
   ```

3. Open the generated project:
   ```bash
   open bitchat.xcodeproj
   ```

### Option 2: Using Swift Package Manager

1. Open the project in Xcode:
   ```bash
   cd bitchat
   open Package.swift
   ```

2. Select your target device and run

### Option 3: Manual Xcode Project

1. Open Xcode and create a new iOS/macOS App
2. Copy all Swift files from the `bitchat` directory into your project
3. Update Info.plist with Bluetooth permissions
4. Grant Bluetooth access at runtime when the app first launches
5. Set deployment target to iOS 16.0 / macOS 13.0

## Usage

### Basic Commands

- `/j #room` - Join or create a room
- `/m @user message` - Send a private message
- `/w` - List online users
- `/rooms` - Show all discovered rooms
- `/clear` - Clear chat messages
- `/pass [password]` - Set/change room password (owner only)
- `/transfer @user` - Transfer room ownership
- `/save` - Toggle message retention for room (owner only)

### Getting Started

1. Launch bitchat on your device
2. Set your nickname (or use the auto-generated one)
3. You'll automatically connect to nearby peers
4. Join a room with `/j #general` or start chatting in public
5. Messages relay through the mesh network to reach distant peers

### Room Features

- **Password Protection**: Room owners can set passwords with `/pass`
- **Message Retention**: Owners can enable mandatory message saving with `/save`
- **@ Mentions**: Use `@nickname` to mention users (with autocomplete)
- **Ownership Transfer**: Pass control to trusted users with `/transfer`

## Security & Privacy

### Encryption
- **Private Messages**: X25519 key exchange + AES-256-GCM encryption
- **Room Messages**: Argon2id password derivation + AES-256-GCM
- **Digital Signatures**: Ed25519 for message authenticity
- **Forward Secrecy**: New key pairs generated each session

### Privacy Features
- **No Registration**: No accounts, emails, or phone numbers required
- **Ephemeral by Default**: Messages exist only in device memory
- **Cover Traffic**: Random delays and dummy messages prevent traffic analysis
- **Emergency Wipe**: Triple-tap logo to instantly clear all data
- **Local-First**: Works completely offline, no servers involved

## Performance & Efficiency

### Message Compression
- **LZ4 Compression**: Automatic compression for messages >100 bytes
- **30-70% bandwidth savings** on typical text messages
- **Smart compression**: Skips already-compressed data

### Battery Optimization
- **Adaptive Power Modes**: Automatically adjusts based on battery level
  - Performance mode: Full features when charging or >60% battery
  - Balanced mode: Default operation (30-60% battery)
  - Power saver: Reduced scanning when <30% battery
  - Ultra-low power: Emergency mode when <10% battery
- **Background efficiency**: Automatic power saving when app backgrounded
- **Configurable scanning**: Duty cycle adapts to battery state

### Network Efficiency
- **Optimized Bloom filters**: Faster duplicate detection with less memory
- **Message aggregation**: Batches small messages to reduce transmissions
- **Adaptive connection limits**: Adjusts peer connections based on power mode

## Technical Architecture

### Binary Protocol
bitchat uses an efficient binary protocol optimized for Bluetooth LE:
- Compact packet format with 1-byte type field
- TTL-based message routing (max 7 hops)
- Automatic fragmentation for large messages
- Message deduplication via unique IDs

### Mesh Networking
- Each device acts as both client and peripheral
- Automatic peer discovery and connection management
- Store-and-forward for offline message delivery
- Adaptive duty cycling for battery optimization

For detailed protocol documentation, see the [Technical Whitepaper](WHITEPAPER.md).

## Building for Production

1. Set your development team in project settings
2. Configure code signing
3. Archive and distribute through App Store or TestFlight

## Android Compatibility

The protocol is designed to be platform-agnostic. An Android client can be built using:
- Bluetooth LE APIs
- Same packet structure and encryption
- Compatible service/characteristic UUIDs

## Building the Android App

1. Install **Android Studio** or the Android SDK command line tools and ensure the SDK (API level 34) is available. Set the `ANDROID_HOME` environment variable so that Gradle can locate the SDK.
2. From the `android` directory run:

```bash
./gradlew assembleDebug
```

The debug APK will be written to `app/build/outputs/apk/debug/app-debug.apk`.
3. Deploy the APK to a connected device or emulator with:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Signing the Release APK

1. Generate a keystore if you don't already have one:

```bash
keytool -genkeypair -v -keystore release.jks -alias bitchat \
  -keyalg RSA -keysize 2048 -validity 10000
```

2. Export the signing credentials so Gradle can locate them:

```bash
export BITCHAT_KEYSTORE=/path/to/release.jks
export BITCHAT_KEYSTORE_PASSWORD=<keystore-password>
export BITCHAT_KEY_ALIAS=bitchat
export BITCHAT_KEY_PASSWORD=<key-password>
```

3. Build the signed release APK:

```bash
./gradlew assembleRelease
```

The signed APK will be written to `app/build/outputs/apk/release/app-release.apk`.


## Running Tests

Use the helper script `run_tests.sh` to execute the Swift package tests. The script checks for the presence of SwiftUI and skips the tests on platforms other than macOS.

```bash
./run_tests.sh
```

On macOS you can also open the package in Xcode and run the **bitchat** scheme (⌘U) or invoke:

```bash
xcodebuild test -scheme bitchat-Package
```
