import { registerPlugin } from '@capacitor/core';
const DeviceIntegrity = registerPlugin('DeviceIntegrity', {
    web: () => import('./web').then((m) => new m.DeviceIntegrityWeb()),
});
export * from './definitions';
export { DeviceIntegrity };
//# sourceMappingURL=index.js.map