import { WebPlugin } from '@capacitor/core';

import type {
  AttestationTokenResult,
  CreateAssertionOptions,
  CreateAttestationOptions,
  DeviceCheckTokenResult,
  DeviceIntegrityCapabilities,
  DeviceIntegrityPlugin,
  PrepareAttestationOptions,
  PrepareAttestationResult,
  WidevineFingerprintOptions,
  WidevineFingerprintResult,
} from './definitions';

const WEB_ERROR_MESSAGE = 'Device integrity is not available on web. Use the native Android or iOS implementation.';

export class DeviceIntegrityWeb extends WebPlugin implements DeviceIntegrityPlugin {
  async getCapabilities(): Promise<DeviceIntegrityCapabilities> {
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

  async getWidevineFingerprint(_options?: WidevineFingerprintOptions): Promise<WidevineFingerprintResult> {
    void _options;
    throw new Error(WEB_ERROR_MESSAGE);
  }

  async prepareAttestation(_options?: PrepareAttestationOptions): Promise<PrepareAttestationResult> {
    void _options;
    throw new Error(WEB_ERROR_MESSAGE);
  }

  async createAttestation(_options: CreateAttestationOptions): Promise<AttestationTokenResult> {
    void _options;
    throw new Error(WEB_ERROR_MESSAGE);
  }

  async createAssertion(_options: CreateAssertionOptions): Promise<AttestationTokenResult> {
    void _options;
    throw new Error(WEB_ERROR_MESSAGE);
  }

  async getDeviceCheckToken(): Promise<DeviceCheckTokenResult> {
    throw new Error(WEB_ERROR_MESSAGE);
  }
}
