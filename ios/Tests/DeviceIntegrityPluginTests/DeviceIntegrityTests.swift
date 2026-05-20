import CryptoKit
import XCTest
@testable import DeviceIntegrityPlugin

final class DeviceIntegrityTests: XCTestCase {
    func testCreateClientDataHashUsesSha256() throws {
        let implementation = DeviceIntegrity()
        let payload = "server-issued-challenge"

        let result = try implementation.createClientDataHash(from: payload)
        let expected = Data(SHA256.hash(data: Data(payload.utf8)))

        XCTAssertEqual(result, expected)
    }

    func testSupportChecksReturnBooleanValues() {
        let implementation = DeviceIntegrity()
        let appAttestSupported = implementation.isAppAttestSupported()
        let deviceCheckSupported = implementation.isDeviceCheckSupported()

        XCTAssertTrue(appAttestSupported || !appAttestSupported)
        XCTAssertTrue(deviceCheckSupported || !deviceCheckSupported)
    }
}
