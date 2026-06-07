import { WebPlugin } from '@capacitor/core';
const WEB_ERROR_MESSAGE = 'Device integrity is not available on web. Use the native Android or iOS implementation.';
export class DeviceIntegrityWeb extends WebPlugin {
    async getCapabilities() {
        return {
            platform: 'web',
            widevine: {
                supported: false,
                fingerprintAvailable: false,
                securityLevelScanSupported: false,
            },
            appAttest: { supported: false },
            deviceCheck: { supported: false },
            playIntegrity: { supported: false },
        };
    }
    async getWidevineFingerprint(_options) {
        void _options;
        throw new Error(WEB_ERROR_MESSAGE);
    }
    async prepareAttestation(_options) {
        void _options;
        throw new Error(WEB_ERROR_MESSAGE);
    }
    async createAttestation(_options) {
        void _options;
        throw new Error(WEB_ERROR_MESSAGE);
    }
    async createAssertion(_options) {
        void _options;
        throw new Error(WEB_ERROR_MESSAGE);
    }
    async getDeviceCheckToken() {
        throw new Error(WEB_ERROR_MESSAGE);
    }
}
//# sourceMappingURL=web.js.map
