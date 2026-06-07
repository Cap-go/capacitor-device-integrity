import { CapacitorUpdater } from '@capgo/capacitor-updater';
import { Capacitor } from '@capacitor/core';
import './style.css';
import { DeviceIntegrity } from '@capgo/capacitor-device-integrity';

const output = document.getElementById('plugin-output');
const hashSaltInput = document.getElementById('hash-salt');
const includeRawIdInput = document.getElementById('include-raw-id');

const setOutput = (value) => {
  output.textContent = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
};

const run = async (action) => {
  try {
    const result = await action();
    setOutput(result);
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
};

document.getElementById('get-capabilities').addEventListener('click', () => {
  void run(() => DeviceIntegrity.getCapabilities());
});

document.getElementById('get-widevine').addEventListener('click', () => {
  const hashSalt = hashSaltInput.value.trim();
  void run(() =>
    DeviceIntegrity.getWidevineFingerprint({
      includeRawId: includeRawIdInput.checked,
      ...(hashSalt ? { hashSalt } : {}),
    })
  );
});

document.getElementById('prepare-attestation').addEventListener('click', () => {
  void run(() => DeviceIntegrity.prepareAttestation());
});

document.getElementById('get-device-check').addEventListener('click', () => {
  void run(() => DeviceIntegrity.getDeviceCheckToken());
});

if (Capacitor.isNativePlatform()) {
  CapacitorUpdater.notifyAppReady().catch((error) => {
    console.error('Capgo notifyAppReady failed', error);
  });
}
