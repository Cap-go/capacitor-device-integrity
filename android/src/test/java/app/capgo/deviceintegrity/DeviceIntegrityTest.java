package app.capgo.deviceintegrity;

import app.capgo.deviceintegrity.DeviceIntegrity.DeviceIntegrityException;
import app.capgo.deviceintegrity.DeviceIntegrity.WidevineCapabilities;
import app.capgo.deviceintegrity.DeviceIntegrity.WidevineData;
import app.capgo.deviceintegrity.DeviceIntegrity.WidevineDrmReader;
import app.capgo.deviceintegrity.DeviceIntegrity.WidevineFingerprint;
import java.security.MessageDigest;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class DeviceIntegrityTest {

    @Test
    public void getWidevineCapabilitiesReturnsUnsupportedWhenSchemeUnavailable() {
        DeviceIntegrity implementation = new DeviceIntegrity(new FakeWidevineReader(false, null));

        WidevineCapabilities capabilities = implementation.getWidevineCapabilities();

        Assert.assertFalse(capabilities.supported);
        Assert.assertFalse(capabilities.fingerprintAvailable);
        Assert.assertFalse(capabilities.securityLevelScanSupported);
    }

    @Test
    public void getWidevineCapabilitiesDetectsFingerprintAndSecurityLevel() {
        WidevineData data = new WidevineData(new byte[] { 1, 2, 3 }, "L1", "Widevine", "1.0", "description");
        DeviceIntegrity implementation = new DeviceIntegrity(new FakeWidevineReader(true, data));

        WidevineCapabilities capabilities = implementation.getWidevineCapabilities();

        Assert.assertTrue(capabilities.supported);
        Assert.assertTrue(capabilities.fingerprintAvailable);
        Assert.assertTrue(capabilities.securityLevelScanSupported);
    }

    @Test
    public void getWidevineFingerprintUsesPackageNameAsDefaultSalt() throws Exception {
        byte[] widevineId = new byte[] { 1, 2, 3, 4 };
        DeviceIntegrity implementation = new DeviceIntegrity(
            new FakeWidevineReader(true, new WidevineData(widevineId, "L3", "Vendor", "2.0", "DRM"))
        );

        WidevineFingerprint result = implementation.getWidevineFingerprint("app.example.test", false, null);

        Assert.assertEquals(expectedSaltedHash("app.example.test", widevineId), result.fingerprint);
        Assert.assertEquals(expectedSha256Hex(widevineId), result.widevineIdSha256);
        Assert.assertNull(result.widevineIdBase64);
        Assert.assertEquals("L3", result.securityLevel);
        Assert.assertEquals("Vendor", result.vendor);
        Assert.assertEquals("2.0", result.version);
        Assert.assertEquals("DRM", result.description);
    }

    @Test
    public void getWidevineFingerprintReturnsRawIdOnlyWhenRequested() throws Exception {
        DeviceIntegrity implementation = new DeviceIntegrity(
            new FakeWidevineReader(true, new WidevineData(new byte[] { 1, 2, 3, 4 }, null, null, null, null))
        );

        WidevineFingerprint result = implementation.getWidevineFingerprint("app.example.test", true, "custom-salt");

        Assert.assertEquals("AQIDBA==", result.widevineIdBase64);
        Assert.assertEquals(expectedSaltedHash("custom-salt", new byte[] { 1, 2, 3, 4 }), result.fingerprint);
    }

    @Test(expected = DeviceIntegrityException.class)
    public void getWidevineFingerprintRejectsMissingUniqueId() throws Exception {
        DeviceIntegrity implementation = new DeviceIntegrity(
            new FakeWidevineReader(true, new WidevineData(new byte[0], null, null, null, null))
        );

        implementation.getWidevineFingerprint("app.example.test", false, null);
    }

    private static String expectedSaltedHash(String salt, byte[] widevineId) throws Exception {
        byte[] saltBytes = salt.getBytes("UTF-8");
        byte[] input = Arrays.copyOf(saltBytes, saltBytes.length + 1 + widevineId.length);
        input[saltBytes.length] = 0;
        System.arraycopy(widevineId, 0, input, saltBytes.length + 1, widevineId.length);
        return expectedSha256Hex(input);
    }

    private static String expectedSha256Hex(byte[] input) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(input);
        StringBuilder result = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            result.append(String.format("%02x", b & 0xff));
        }
        return result.toString();
    }

    private static final class FakeWidevineReader implements WidevineDrmReader {

        private final boolean supported;
        private final WidevineData data;

        FakeWidevineReader(boolean supported, WidevineData data) {
            this.supported = supported;
            this.data = data;
        }

        @Override
        public boolean isWidevineSupported() {
            return supported;
        }

        @Override
        public WidevineData readWidevineData() throws DeviceIntegrityException {
            if (data == null) {
                throw new DeviceIntegrityException("No data");
            }
            return data;
        }
    }
}
