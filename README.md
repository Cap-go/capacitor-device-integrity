# @capgo/capacitor-device-integrity

<a href="https://capgo.app/"><img src="https://capgo.app/readme-banner.svg?repo=Cap-go/capacitor-device-integrity" alt="Capgo - Instant updates for Capacitor" /></a>

<div align="center">
  <h2><a href="https://capgo.app/?ref=plugin_device_integrity">Get Instant updates for your App with Capgo</a></h2>
  <h2><a href="https://capgo.app/consulting/?ref=plugin_device_integrity">Missing a feature? We will build the plugin for you</a></h2>
</div>

Device integrity and fraud signals for Capacitor:

- Android: Widevine DRM fingerprint and Play Integrity Standard API attestation.
- iOS: Apple App Attest and DeviceCheck.
- Web: unsupported fallback that reports no native capabilities.

## Security model

This plugin is designed for fraud and abuse detection. It does not make client-side values trustworthy by itself.

- Verify App Attest and Play Integrity tokens on your backend.
- Treat Widevine values as sensitive identifiers.
- Disclose identifier use in your privacy policy.
- Do not use Android Widevine values for advertising or cross-app tracking.
- Do not attempt iOS fingerprinting. Apple does not provide a public review-safe stable device ID for this use case.

## Compatibility

| Plugin version | Capacitor compatibility | Maintained |
| -------------- | ----------------------- | ---------- |
| v8.*.*         | v8.*.*                  | Yes        |
| v7.*.*         | v7.*.*                  | On demand  |
| v6.*.*         | v6.*.*                  | On demand  |

## Install

You can use our AI-Assisted Setup to install the plugin. Add the Capgo skills to your AI tool using the following command:

```bash
npx skills add https://github.com/cap-go/capacitor-skills --skill capacitor-plugins
```

Then use the following prompt:

```text
Use the `capacitor-plugins` skill from `cap-go/capacitor-skills` to install the `@capgo/capacitor-device-integrity` plugin in my project.
```

If you prefer Manual Setup, install the plugin by running the following commands and follow the platform-specific instructions below:

```bash
npm install @capgo/capacitor-device-integrity
npx cap sync
```

## Platform setup

### Android

Widevine fingerprinting does not require extra permissions.

Play Integrity requires Google Play services and a Cloud project number:

```ts
// capacitor.config.ts
plugins: {
  DeviceIntegrity: {
    cloudProjectNumber: '123456789012'
  }
}
```

You can also pass `cloudProjectNumber` directly to the attestation methods.

### iOS

Enable the App Attest capability in Xcode:

1. Open your app target.
2. Go to Signing & Capabilities.
3. Add App Attest.
4. Test on a physical device when validating real App Attest and DeviceCheck behavior.

## Usage

```ts
import { DeviceIntegrity } from '@capgo/capacitor-device-integrity';

const capabilities = await DeviceIntegrity.getCapabilities();

if (capabilities.platform === 'android' && capabilities.widevine.fingerprintAvailable) {
  const widevine = await DeviceIntegrity.getWidevineFingerprint();
  await api.saveDeviceSignal({
    widevineIdSha256: widevine.widevineIdSha256,
    fingerprint: widevine.fingerprint,
    securityLevel: widevine.securityLevel,
  });
}

if (capabilities.appAttest.supported || capabilities.playIntegrity.supported) {
  const prepared = await DeviceIntegrity.prepareAttestation();
  const challenge = await api.createDeviceChallenge();

  const attestation = await DeviceIntegrity.createAttestation({
    keyId: prepared.keyId,
    challenge,
  });

  await api.verifyDeviceAttestation(attestation);
}

if (capabilities.deviceCheck.supported) {
  const deviceCheck = await DeviceIntegrity.getDeviceCheckToken();
  await api.verifyDeviceCheckToken(deviceCheck.token);
}
```

## Backend handling

Store the platform-specific evidence with the user record only after server-side checks:

- Android Widevine: store `widevineIdSha256` and optionally the raw `widevineIdBase64` only when you explicitly need it.
- Android Play Integrity: decode the returned token with Google's server API and validate request hash, package name, certificate digest, and device/app integrity verdicts.
- iOS App Attest: verify the attestation object/assertion against Apple's App Attest rules, app identity, nonce, public key, and counter.
- iOS DeviceCheck: send the token to Apple's DeviceCheck server API and maintain fraud bits on your backend.

## API

<docgen-index>

* [`getCapabilities()`](#getcapabilities)
* [`getWidevineFingerprint(...)`](#getwidevinefingerprint)
* [`prepareAttestation(...)`](#prepareattestation)
* [`createAttestation(...)`](#createattestation)
* [`createAssertion(...)`](#createassertion)
* [`getDeviceCheckToken()`](#getdevicechecktoken)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### getCapabilities()

```typescript
getCapabilities() => Promise<DeviceIntegrityCapabilities>
```

Returns the native integrity capabilities available on the current platform.

**Returns:** <code>Promise&lt;<a href="#deviceintegritycapabilities">DeviceIntegrityCapabilities</a>&gt;</code>

--------------------


### getWidevineFingerprint(...)

```typescript
getWidevineFingerprint(options?: WidevineFingerprintOptions | undefined) => Promise<WidevineFingerprintResult>
```

Returns an Android Widevine-derived fingerprint.

The default `fingerprint` is SHA-256 over the Widevine device unique ID and a salt.
If `hashSalt` is not provided, Android uses the app package name as the salt.

The raw Widevine ID is sensitive and is only returned as base64 when `includeRawId` is true.

| Param         | Type                                                                              |
| ------------- | --------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#widevinefingerprintoptions">WidevineFingerprintOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#widevinefingerprintresult">WidevineFingerprintResult</a>&gt;</code>

--------------------


### prepareAttestation(...)

```typescript
prepareAttestation(options?: PrepareAttestationOptions | undefined) => Promise<PrepareAttestationResult>
```

Prepares native attestation state and returns the key/provider handle.

iOS: creates an App Attest key.
Android: prepares a Play Integrity Standard token provider.

| Param         | Type                                                                            |
| ------------- | ------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#prepareattestationoptions">PrepareAttestationOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#prepareattestationresult">PrepareAttestationResult</a>&gt;</code>

--------------------


### createAttestation(...)

```typescript
createAttestation(options: CreateAttestationOptions) => Promise<AttestationTokenResult>
```

Creates a registration attestation token bound to a backend-issued challenge.

| Param         | Type                                                                          |
| ------------- | ----------------------------------------------------------------------------- |
| **`options`** | <code><a href="#createattestationoptions">CreateAttestationOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#attestationtokenresult">AttestationTokenResult</a>&gt;</code>

--------------------


### createAssertion(...)

```typescript
createAssertion(options: CreateAssertionOptions) => Promise<AttestationTokenResult>
```

Creates a request assertion token bound to a request payload.

| Param         | Type                                                                      |
| ------------- | ------------------------------------------------------------------------- |
| **`options`** | <code><a href="#createassertionoptions">CreateAssertionOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#attestationtokenresult">AttestationTokenResult</a>&gt;</code>

--------------------


### getDeviceCheckToken()

```typescript
getDeviceCheckToken() => Promise<DeviceCheckTokenResult>
```

Creates an iOS DeviceCheck token for server-side fraud-state lookups.

**Returns:** <code>Promise&lt;<a href="#devicechecktokenresult">DeviceCheckTokenResult</a>&gt;</code>

--------------------


### Interfaces


#### DeviceIntegrityCapabilities

| Prop                | Type                                                                        | Description                              |
| ------------------- | --------------------------------------------------------------------------- | ---------------------------------------- |
| **`platform`**      | <code><a href="#deviceintegrityplatform">DeviceIntegrityPlatform</a></code> | Platform currently executing the plugin. |
| **`widevine`**      | <code><a href="#widevinecapabilities">WidevineCapabilities</a></code>       | Android Widevine DRM support.            |
| **`appAttest`**     | <code><a href="#supportstatus">SupportStatus</a></code>                     | iOS App Attest support.                  |
| **`deviceCheck`**   | <code><a href="#supportstatus">SupportStatus</a></code>                     | iOS DeviceCheck support.                 |
| **`playIntegrity`** | <code><a href="#supportstatus">SupportStatus</a></code>                     | Android Play Integrity support.          |


#### WidevineCapabilities

| Prop                             | Type                 | Description                                                 |
| -------------------------------- | -------------------- | ----------------------------------------------------------- |
| **`supported`**                  | <code>boolean</code> | Whether the Widevine DRM scheme is supported by the device. |
| **`fingerprintAvailable`**       | <code>boolean</code> | Whether the Widevine device unique ID can be read.          |
| **`securityLevelScanSupported`** | <code>boolean</code> | Whether the Widevine security level property can be read.   |


#### SupportStatus

| Prop            | Type                 | Description                                                |
| --------------- | -------------------- | ---------------------------------------------------------- |
| **`supported`** | <code>boolean</code> | Whether the capability is available on the current device. |


#### WidevineFingerprintResult

| Prop                   | Type                    | Description                                                                                 |
| ---------------------- | ----------------------- | ------------------------------------------------------------------------------------------- |
| **`platform`**         | <code>'android'</code>  | Always `android`.                                                                           |
| **`source`**           | <code>'widevine'</code> | Always `widevine`.                                                                          |
| **`fingerprint`**      | <code>string</code>     | Salted SHA-256 fingerprint for storing alongside a user record.                             |
| **`widevineIdSha256`** | <code>string</code>     | Unsalted SHA-256 hash of the Widevine device unique ID.                                     |
| **`widevineIdBase64`** | <code>string</code>     | Raw Widevine device unique ID encoded as base64. Returned only when `includeRawId` is true. |
| **`securityLevel`**    | <code>string</code>     | Widevine security level when available, for example `L1` or `L3`.                           |
| **`vendor`**           | <code>string</code>     | DRM vendor when available.                                                                  |
| **`version`**          | <code>string</code>     | DRM plugin version when available.                                                          |
| **`description`**      | <code>string</code>     | DRM plugin description when available.                                                      |


#### WidevineFingerprintOptions

| Prop               | Type                 | Description                                                                                 |
| ------------------ | -------------------- | ------------------------------------------------------------------------------------------- |
| **`includeRawId`** | <code>boolean</code> | Return the raw Widevine device unique ID as base64. Defaults to `false`.                    |
| **`hashSalt`**     | <code>string</code>  | Optional salt used to derive `fingerprint`. Android uses the app package name when omitted. |


#### PrepareAttestationResult

| Prop         | Type                                                            | Description                                                         |
| ------------ | --------------------------------------------------------------- | ------------------------------------------------------------------- |
| **`keyId`**  | <code>string</code>                                             | iOS App Attest key ID or Android Play Integrity provider handle ID. |
| **`format`** | <code><a href="#attestationformat">AttestationFormat</a></code> | Native attestation format used by the current platform.             |


#### PrepareAttestationOptions

| Prop                     | Type                | Description                                                                                                                                |
| ------------------------ | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| **`cloudProjectNumber`** | <code>string</code> | Android only. Google Cloud project number for Play Integrity. Can be configured globally via `plugins.DeviceIntegrity.cloudProjectNumber`. |


#### AttestationTokenResult

| Prop         | Type                                                            | Description                                             |
| ------------ | --------------------------------------------------------------- | ------------------------------------------------------- |
| **`token`**  | <code>string</code>                                             | Token/assertion that must be verified server-side.      |
| **`keyId`**  | <code>string</code>                                             | Key/provider handle used to create the token.           |
| **`format`** | <code><a href="#attestationformat">AttestationFormat</a></code> | Native attestation format used by the current platform. |


#### CreateAttestationOptions

| Prop                     | Type                | Description                                                   |
| ------------------------ | ------------------- | ------------------------------------------------------------- |
| **`keyId`**              | <code>string</code> | Key/provider handle returned from `prepareAttestation()`.     |
| **`challenge`**          | <code>string</code> | Backend-issued one-time challenge.                            |
| **`cloudProjectNumber`** | <code>string</code> | Android only. Google Cloud project number for Play Integrity. |


#### CreateAssertionOptions

| Prop                     | Type                | Description                                                   |
| ------------------------ | ------------------- | ------------------------------------------------------------- |
| **`keyId`**              | <code>string</code> | Key/provider handle returned from `prepareAttestation()`.     |
| **`payload`**            | <code>string</code> | Backend-issued one-time request payload or nonce.             |
| **`cloudProjectNumber`** | <code>string</code> | Android only. Google Cloud project number for Play Integrity. |


#### DeviceCheckTokenResult

| Prop        | Type                | Description                              |
| ----------- | ------------------- | ---------------------------------------- |
| **`token`** | <code>string</code> | iOS DeviceCheck token encoded as base64. |


### Type Aliases


#### DeviceIntegrityPlatform

<code>'android' | 'ios' | 'web'</code>


#### AttestationFormat

<code>'apple-app-attest' | 'google-play-integrity-standard'</code>

</docgen-api>
