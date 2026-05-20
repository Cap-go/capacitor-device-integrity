import { registerPlugin } from '@capacitor/core';

import type { DeviceIntegrityPlugin } from './definitions';

const DeviceIntegrity = registerPlugin<DeviceIntegrityPlugin>('DeviceIntegrity', {
  web: () => import('./web').then((m) => new m.DeviceIntegrityWeb()),
});

export * from './definitions';
export { DeviceIntegrity };
