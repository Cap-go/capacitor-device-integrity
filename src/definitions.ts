/// <reference types="@capacitor/cli" />

declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Device Integrity plugin configuration.
     */
    DeviceIntegrity?: DeviceIntegrityPluginConfig;
  }
}

/**
 * Capacitor config for the Device Integrity plugin.
 */
export interface DeviceIntegrityPluginConfig {
  /**
   * Android only. Google Cloud project number used by the Play Integrity Standard API.
   *
   * Example:
   * `plugins.DeviceIntegrity.cloudProjectNumber = '123456789012'`
   */
  cloudProjectNumber?: string;
}

export type DeviceIntegrityPlatform = 'android' | 'ios' | 'web';

export type AttestationFormat = 'apple-app-attest' | 'google-play-integrity-standard';

export interface DeviceIntegrityPlugin {
  /**
   * Returns the native integrity capabilities available on the current platform.
   */
  getCapabilities(): Promise<DeviceIntegrityCapabilities>;

  /**
   * Returns an Android Widevine-derived fingerprint.
   *
   * The default `fingerprint` is SHA-256 over the Widevine device unique ID and a salt.
   * If `hashSalt` is not provided, Android uses the app package name as the salt.
   *
   * The raw Widevine ID is sensitive and is only returned as base64 when `includeRawId` is true.
   */
  getWidevineFingerprint(options?: WidevineFingerprintOptions): Promise<WidevineFingerprintResult>;

  /**
   * Prepares native attestation state and returns the key/provider handle.
   *
   * iOS: creates an App Attest key.
   * Android: prepares a Play Integrity Standard token provider.
   */
  prepareAttestation(options?: PrepareAttestationOptions): Promise<PrepareAttestationResult>;

  /**
   * Creates a registration attestation token bound to a backend-issued challenge.
   */
  createAttestation(options: CreateAttestationOptions): Promise<AttestationTokenResult>;

  /**
   * Creates a request assertion token bound to a request payload.
   */
  createAssertion(options: CreateAssertionOptions): Promise<AttestationTokenResult>;

  /**
   * Creates an iOS DeviceCheck token for server-side fraud-state lookups.
   */
  getDeviceCheckToken(): Promise<DeviceCheckTokenResult>;
}

export interface DeviceIntegrityCapabilities {
  /**
   * Platform currently executing the plugin.
   */
  platform: DeviceIntegrityPlatform;

  /**
   * Android Widevine DRM support.
   */
  widevine: WidevineCapabilities;

  /**
   * iOS App Attest support.
   */
  appAttest: SupportStatus;

  /**
   * iOS DeviceCheck support.
   */
  deviceCheck: SupportStatus;

  /**
   * Android Play Integrity support.
   */
  playIntegrity: SupportStatus;
}

export interface WidevineCapabilities {
  /**
   * Whether the Widevine DRM scheme is supported by the device.
   */
  supported: boolean;

  /**
   * Whether the Widevine device unique ID can be read.
   */
  fingerprintAvailable: boolean;

  /**
   * Whether the Widevine security level property can be read.
   */
  securityLevelScanSupported: boolean;
}

export interface SupportStatus {
  /**
   * Whether the capability is available on the current device.
   */
  supported: boolean;
}

export interface WidevineFingerprintOptions {
  /**
   * Return the raw Widevine device unique ID as base64.
   *
   * Defaults to `false`.
   */
  includeRawId?: boolean;

  /**
   * Optional salt used to derive `fingerprint`.
   *
   * Android uses the app package name when omitted.
   */
  hashSalt?: string;
}

export interface WidevineFingerprintResult {
  /**
   * Always `android`.
   */
  platform: 'android';

  /**
   * Always `widevine`.
   */
  source: 'widevine';

  /**
   * Salted SHA-256 fingerprint for storing alongside a user record.
   */
  fingerprint: string;

  /**
   * Unsalted SHA-256 hash of the Widevine device unique ID.
   */
  widevineIdSha256: string;

  /**
   * Raw Widevine device unique ID encoded as base64.
   *
   * Returned only when `includeRawId` is true.
   */
  widevineIdBase64?: string;

  /**
   * Widevine security level when available, for example `L1` or `L3`.
   */
  securityLevel?: string;

  /**
   * DRM vendor when available.
   */
  vendor?: string;

  /**
   * DRM plugin version when available.
   */
  version?: string;

  /**
   * DRM plugin description when available.
   */
  description?: string;
}

export interface PrepareAttestationOptions {
  /**
   * Android only. Google Cloud project number for Play Integrity.
   *
   * Can be configured globally via `plugins.DeviceIntegrity.cloudProjectNumber`.
   */
  cloudProjectNumber?: string;
}

export interface PrepareAttestationResult {
  /**
   * iOS App Attest key ID or Android Play Integrity provider handle ID.
   */
  keyId: string;

  /**
   * Native attestation format used by the current platform.
   */
  format: AttestationFormat;
}

export interface CreateAttestationOptions {
  /**
   * Key/provider handle returned from `prepareAttestation()`.
   */
  keyId: string;

  /**
   * Backend-issued one-time challenge.
   */
  challenge: string;

  /**
   * Android only. Google Cloud project number for Play Integrity.
   */
  cloudProjectNumber?: string;
}

export interface CreateAssertionOptions {
  /**
   * Key/provider handle returned from `prepareAttestation()`.
   */
  keyId: string;

  /**
   * Backend-issued one-time request payload or nonce.
   */
  payload: string;

  /**
   * Android only. Google Cloud project number for Play Integrity.
   */
  cloudProjectNumber?: string;
}

export interface AttestationTokenResult {
  /**
   * Token/assertion that must be verified server-side.
   */
  token: string;

  /**
   * Key/provider handle used to create the token.
   */
  keyId: string;

  /**
   * Native attestation format used by the current platform.
   */
  format: AttestationFormat;
}

export interface DeviceCheckTokenResult {
  /**
   * iOS DeviceCheck token encoded as base64.
   */
  token: string;
}
