import { WebPlugin } from '@capacitor/core';
import type { AttestationTokenResult, CreateAssertionOptions, CreateAttestationOptions, DeviceCheckTokenResult, DeviceIntegrityCapabilities, DeviceIntegrityPlugin, PrepareAttestationOptions, PrepareAttestationResult, WidevineFingerprintOptions, WidevineFingerprintResult } from './definitions';
export declare class DeviceIntegrityWeb extends WebPlugin implements DeviceIntegrityPlugin {
    getCapabilities(): Promise<DeviceIntegrityCapabilities>;
    getWidevineFingerprint(_options?: WidevineFingerprintOptions): Promise<WidevineFingerprintResult>;
    prepareAttestation(_options?: PrepareAttestationOptions): Promise<PrepareAttestationResult>;
    createAttestation(_options: CreateAttestationOptions): Promise<AttestationTokenResult>;
    createAssertion(_options: CreateAssertionOptions): Promise<AttestationTokenResult>;
    getDeviceCheckToken(): Promise<DeviceCheckTokenResult>;
}
