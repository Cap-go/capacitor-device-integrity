import type { CapacitorConfig } from '@capacitor/cli';

import pkg from './package.json';

const config: CapacitorConfig = {
  appId: 'app.capgo.deviceintegrity.example',
  appName: 'Device Integrity Example',
  webDir: 'dist',
  plugins: {
    CapacitorUpdater: {
      appId: 'app.capgo.deviceintegrity.example',
      autoUpdate: true,
      autoSplashscreen: true,
      directUpdate: 'always',
      defaultChannel: 'production',
      version: pkg.version,
    },
  },
};

export default config;
