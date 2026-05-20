'use strict';

var core = require('@capacitor/core');

const DeviceIntegrity = core.registerPlugin('DeviceIntegrity', {
    web: () => Promise.resolve().then(function () { return web; }).then((m) => new m.DeviceIntegrityWeb()),
});

const WEB_ERROR_MESSAGE = 'Device integrity is not available on web. Use the native Android or iOS implementation.';
class DeviceIntegrityWeb extends core.WebPlugin {
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
        throw new Error(WEB_ERROR_MESSAGE);
    }
    async prepareAttestation(_options) {
        throw new Error(WEB_ERROR_MESSAGE);
    }
    async createAttestation(_options) {
        throw new Error(WEB_ERROR_MESSAGE);
    }
    async createAssertion(_options) {
        throw new Error(WEB_ERROR_MESSAGE);
    }
    async getDeviceCheckToken() {
        throw new Error(WEB_ERROR_MESSAGE);
    }
}

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    DeviceIntegrityWeb: DeviceIntegrityWeb
});

exports.DeviceIntegrity = DeviceIntegrity;
//# sourceMappingURL=plugin.cjs.js.map
