import Capacitor
import DeviceCheck
import Foundation

@objc(DeviceIntegrityPlugin)
public class DeviceIntegrityPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "DeviceIntegrityPlugin"
    public let jsName = "DeviceIntegrity"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getCapabilities", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getWidevineFingerprint", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "prepareAttestation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createAttestation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createAssertion", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getDeviceCheckToken", returnType: CAPPluginReturnPromise)
    ]

    private let implementation = DeviceIntegrity()
    private let appAttestFormat = "apple-app-attest"

    @objc func getCapabilities(_ call: CAPPluginCall) {
        call.resolve([
            "platform": "ios",
            "widevine": [
                "supported": false,
                "fingerprintAvailable": false,
                "securityLevelScanSupported": false
            ],
            "appAttest": [
                "supported": implementation.isAppAttestSupported()
            ],
            "deviceCheck": [
                "supported": implementation.isDeviceCheckSupported()
            ],
            "playIntegrity": [
                "supported": false
            ]
        ])
    }

    @objc func getWidevineFingerprint(_ call: CAPPluginCall) {
        call.reject("Widevine fingerprinting is only available on Android")
    }

    @objc func prepareAttestation(_ call: CAPPluginCall) {
        do {
            try validateAppAttestSupport()

            DCAppAttestService.shared.generateKey { [weak self] keyId, error in
                guard let self else {
                    return
                }

                if let error {
                    self.reject(call, message: "Error generating App Attest key: \(error.localizedDescription)", error: error)
                    return
                }

                guard let keyId else {
                    self.reject(call, message: DeviceIntegrityError.missingGeneratedValue.localizedDescription)
                    return
                }

                self.resolve(call, payload: [
                    "keyId": keyId,
                    "format": self.appAttestFormat
                ])
            }
        } catch {
            reject(call, message: error.localizedDescription, error: error)
        }
    }

    @objc func createAttestation(_ call: CAPPluginCall) {
        do {
            try validateAppAttestSupport()
            let keyId = try keyId(from: call)
            let challenge = try challenge(from: call)
            let clientDataHash = try implementation.createClientDataHash(from: challenge)

            DCAppAttestService.shared.attestKey(keyId, clientDataHash: clientDataHash) { [weak self] attestation, error in
                guard let self else {
                    return
                }

                if let error {
                    self.reject(call, message: "Error creating App Attest token: \(error.localizedDescription)", error: error)
                    return
                }

                guard let attestation else {
                    self.reject(call, message: DeviceIntegrityError.missingGeneratedValue.localizedDescription)
                    return
                }

                self.resolve(call, payload: [
                    "token": attestation.base64EncodedString(),
                    "keyId": keyId,
                    "format": self.appAttestFormat
                ])
            }
        } catch {
            reject(call, message: error.localizedDescription, error: error)
        }
    }

    @objc func createAssertion(_ call: CAPPluginCall) {
        do {
            try validateAppAttestSupport()
            let keyId = try keyId(from: call)
            let payload = try payload(from: call)
            let clientDataHash = try implementation.createClientDataHash(from: payload)

            DCAppAttestService.shared.generateAssertion(keyId, clientDataHash: clientDataHash) { [weak self] assertion, error in
                guard let self else {
                    return
                }

                if let error {
                    self.reject(call, message: "Error creating App Attest assertion: \(error.localizedDescription)", error: error)
                    return
                }

                guard let assertion else {
                    self.reject(call, message: DeviceIntegrityError.missingGeneratedValue.localizedDescription)
                    return
                }

                self.resolve(call, payload: [
                    "token": assertion.base64EncodedString(),
                    "keyId": keyId,
                    "format": self.appAttestFormat
                ])
            }
        } catch {
            reject(call, message: error.localizedDescription, error: error)
        }
    }

    @objc func getDeviceCheckToken(_ call: CAPPluginCall) {
        do {
            guard implementation.isDeviceCheckSupported() else {
                throw DeviceIntegrityError.deviceCheckNotSupported
            }

            DCDevice.current.generateToken { [weak self] token, error in
                guard let self else {
                    return
                }

                if let error {
                    self.reject(call, message: "Error generating DeviceCheck token: \(error.localizedDescription)", error: error)
                    return
                }

                guard let token else {
                    self.reject(call, message: DeviceIntegrityError.missingGeneratedValue.localizedDescription)
                    return
                }

                self.resolve(call, payload: [
                    "token": token.base64EncodedString()
                ])
            }
        } catch {
            reject(call, message: error.localizedDescription, error: error)
        }
    }

    private func validateAppAttestSupport() throws {
        guard implementation.isAppAttestSupported() else {
            throw DeviceIntegrityError.appAttestNotSupported
        }
    }

    private func keyId(from call: CAPPluginCall) throws -> String {
        guard let keyId = call.getString("keyId"), !keyId.isEmpty else {
            throw DeviceIntegrityError.missingKeyId
        }
        return keyId
    }

    private func challenge(from call: CAPPluginCall) throws -> String {
        guard let challenge = call.getString("challenge"), !challenge.isEmpty else {
            throw DeviceIntegrityError.missingChallenge
        }
        return challenge
    }

    private func payload(from call: CAPPluginCall) throws -> String {
        guard let payload = call.getString("payload"), !payload.isEmpty else {
            throw DeviceIntegrityError.missingPayload
        }
        return payload
    }

    private func resolve(_ call: CAPPluginCall, payload: [String: Any]) {
        DispatchQueue.main.async {
            call.resolve(payload)
        }
    }

    private func reject(_ call: CAPPluginCall, message: String, error: Error? = nil) {
        DispatchQueue.main.async {
            call.reject(message, nil, error)
        }
    }
}
