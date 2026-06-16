# AGENTS.md

Guidance for contributors working on `@capgo/capacitor-device-integrity`.

## Commands

Always use Bun in this repository:

```bash
bun install
bun run build
bun run lint
bun run check:wiring
bun run verify
```

Public documentation and marketing examples should use standard `npm` and `npx` commands.

## Plugin Scope

This plugin exposes review-safe device integrity signals:

- Android Widevine DRM fingerprinting.
- Android Play Integrity Standard API attestation.
- iOS App Attest.
- iOS DeviceCheck.

Do not add iOS device fingerprinting or hidden stable identifier collection. Apple does not provide a public review-safe iOS hardware ID for this use case.

## API Documentation

API docs in the README are generated from JSDoc in `src/definitions.ts`.

Update `src/definitions.ts`, then run:

```bash
bun run docgen
```

Do not edit the `<docgen-index>` or `<docgen-api>` README sections manually.

## Verification

Before submitting changes, run:

```bash
bun run build
bun run lint
bun run check:wiring
bun run verify:android
bun run verify:ios
```

Real App Attest, DeviceCheck, Play Integrity, and Widevine behavior requires physical-device validation.

## Timeout Policy

- Keep CI, script, and runtime timeouts at 10 minutes or less. Use `timeout-minutes: 10` or lower in GitHub Actions and cap timeout values at `600000` ms, `600` seconds, or `10m` unless explicitly requested.
