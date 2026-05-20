import CryptoKit
import DeviceCheck
import Foundation

@objc public class DeviceIntegrity: NSObject {
    @objc public func isAppAttestSupported() -> Bool {
        return DCAppAttestService.shared.isSupported
    }

    @objc public func isDeviceCheckSupported() -> Bool {
        return DCDevice.current.isSupported
    }

    public func createClientDataHash(from input: String) throws -> Data {
        guard let payloadData = input.data(using: .utf8) else {
            throw DeviceIntegrityError.invalidInput
        }
        return Data(SHA256.hash(data: payloadData))
    }
}

public enum DeviceIntegrityError: LocalizedError {
    case appAttestNotSupported
    case deviceCheckNotSupported
    case missingKeyId
    case missingChallenge
    case missingPayload
    case missingGeneratedValue
    case invalidInput

    public var errorDescription: String? {
        switch self {
        case .appAttestNotSupported:
            return "App Attest is not supported on this device"
        case .deviceCheckNotSupported:
            return "DeviceCheck is not supported on this device"
        case .missingKeyId:
            return "keyId is required"
        case .missingChallenge:
            return "challenge is required"
        case .missingPayload:
            return "payload is required"
        case .missingGeneratedValue:
            return "Native API returned no value"
        case .invalidInput:
            return "Invalid input format"
        }
    }
}
