//
// CompressionTests.swift
// bitchatTests
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//

import XCTest
@testable import bitchat

final class CompressionTests: XCTestCase {
    func testCompressionFlagAndDecoding() {
        let payloadString = String(repeating: "A", count: 200)
        let payload = Data(payloadString.utf8)
        let packet = BitchatPacket(
            type: MessageType.message.rawValue,
            senderID: Data("sender".utf8),
            recipientID: nil,
            timestamp: UInt64(Date().timeIntervalSince1970 * 1000),
            payload: payload,
            signature: nil,
            ttl: 5
        )

        guard let encoded = packet.toBinaryData() else {
            XCTFail("Failed to encode packet")
            return
        }

        // Flags index after version (1) + type (1) + ttl (1) + timestamp (8)
        let flagsIndex = 11
        let flags = encoded[flagsIndex]
        XCTAssertNotEqual(flags & BinaryProtocol.Flags.IS_COMPRESSED, 0)

        guard let decoded = BitchatPacket.from(encoded) else {
            XCTFail("Failed to decode packet")
            return
        }

        XCTAssertEqual(decoded.payload, payload)
    }

    func testSmallPayloadNotCompressed() {
        let payload = Data("Hello".utf8)
        let packet = BitchatPacket(
            type: MessageType.message.rawValue,
            senderID: Data("sender".utf8),
            recipientID: nil,
            timestamp: UInt64(Date().timeIntervalSince1970 * 1000),
            payload: payload,
            signature: nil,
            ttl: 5
        )

        guard let encoded = packet.toBinaryData() else {
            XCTFail("Failed to encode packet")
            return
        }

        let flagsIndex = 11
        let flags = encoded[flagsIndex]
        XCTAssertEqual(flags & BinaryProtocol.Flags.IS_COMPRESSED, 0)

        guard let decoded = BitchatPacket.from(encoded) else {
            XCTFail("Failed to decode packet")
            return
        }

        XCTAssertEqual(decoded.payload, payload)
    }
}
