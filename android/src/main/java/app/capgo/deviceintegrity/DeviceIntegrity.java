package app.capgo.deviceintegrity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaDrm;
import android.media.UnsupportedSchemeException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class DeviceIntegrity {

    private static final UUID WIDEVINE_UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");
    private static final String PROPERTY_SECURITY_LEVEL = "securityLevel";
    private static final String STANDARD_BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final String URL_SAFE_BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private final WidevineDrmReader widevineDrmReader;

    public DeviceIntegrity() {
        this(new AndroidWidevineDrmReader());
    }

    DeviceIntegrity(WidevineDrmReader widevineDrmReader) {
        this.widevineDrmReader = widevineDrmReader;
    }

    public WidevineCapabilities getWidevineCapabilities() {
        if (!widevineDrmReader.isWidevineSupported()) {
            return new WidevineCapabilities(false, false, false);
        }

        try {
            WidevineData data = widevineDrmReader.readWidevineData();
            return new WidevineCapabilities(
                true,
                data.deviceUniqueId != null && data.deviceUniqueId.length > 0,
                !isBlank(data.securityLevel)
            );
        } catch (DeviceIntegrityException error) {
            return new WidevineCapabilities(true, false, false);
        }
    }

    public WidevineFingerprint getWidevineFingerprint(String packageName, boolean includeRawId, String hashSalt)
        throws DeviceIntegrityException {
        if (!widevineDrmReader.isWidevineSupported()) {
            throw new DeviceIntegrityException("Widevine DRM is not supported on this device");
        }

        WidevineData data = widevineDrmReader.readWidevineData();
        if (data.deviceUniqueId == null || data.deviceUniqueId.length == 0) {
            throw new DeviceIntegrityException("Widevine device unique ID is not available on this device");
        }

        String salt = isBlank(hashSalt) ? packageName : hashSalt;
        byte[] fingerprintInput = joinSaltAndId(salt, data.deviceUniqueId);

        return new WidevineFingerprint(
            sha256Hex(fingerprintInput),
            sha256Hex(data.deviceUniqueId),
            includeRawId ? base64Encode(data.deviceUniqueId, false, true) : null,
            data.securityLevel,
            data.vendor,
            data.version,
            data.description
        );
    }

    public boolean isPlayIntegritySupported(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.android.vending", 0);
            return true;
        } catch (PackageManager.NameNotFoundException error) {
            return false;
        }
    }

    public String createRequestHash(String payload) throws NoSuchAlgorithmException {
        byte[] hash = sha256(payload.getBytes(StandardCharsets.UTF_8));
        return base64Encode(hash, true, false);
    }

    static String sha256Hex(byte[] value) throws DeviceIntegrityException {
        try {
            byte[] hash = sha256(value);
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                result.append(String.format("%02x", b & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new DeviceIntegrityException("SHA-256 is not available", error);
        }
    }

    private static byte[] sha256(byte[] value) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(value);
    }

    private static byte[] joinSaltAndId(String salt, byte[] deviceUniqueId) {
        byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[saltBytes.length + 1 + deviceUniqueId.length];
        System.arraycopy(saltBytes, 0, result, 0, saltBytes.length);
        result[saltBytes.length] = 0;
        System.arraycopy(deviceUniqueId, 0, result, saltBytes.length + 1, deviceUniqueId.length);
        return result;
    }

    static String base64Encode(byte[] input, boolean urlSafe, boolean padding) {
        if (input.length == 0) {
            return "";
        }

        String alphabet = urlSafe ? URL_SAFE_BASE64 : STANDARD_BASE64;
        StringBuilder output = new StringBuilder(((input.length + 2) / 3) * 4);

        for (int index = 0; index < input.length; index += 3) {
            int remaining = input.length - index;
            int first = input[index] & 0xff;
            int second = remaining > 1 ? input[index + 1] & 0xff : 0;
            int third = remaining > 2 ? input[index + 2] & 0xff : 0;

            int triple = (first << 16) | (second << 8) | third;
            output.append(alphabet.charAt((triple >> 18) & 0x3f));
            output.append(alphabet.charAt((triple >> 12) & 0x3f));

            if (remaining > 1) {
                output.append(alphabet.charAt((triple >> 6) & 0x3f));
            } else if (padding) {
                output.append('=');
            }

            if (remaining > 2) {
                output.append(alphabet.charAt(triple & 0x3f));
            } else if (padding) {
                output.append('=');
            }
        }

        return output.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    interface WidevineDrmReader {
        boolean isWidevineSupported();

        WidevineData readWidevineData() throws DeviceIntegrityException;
    }

    static final class AndroidWidevineDrmReader implements WidevineDrmReader {

        @Override
        public boolean isWidevineSupported() {
            try {
                return MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID);
            } catch (RuntimeException error) {
                return false;
            }
        }

        @Override
        public WidevineData readWidevineData() throws DeviceIntegrityException {
            MediaDrm mediaDrm = null;
            try {
                mediaDrm = new MediaDrm(WIDEVINE_UUID);
                return new WidevineData(
                    readDeviceUniqueId(mediaDrm),
                    readStringProperty(mediaDrm, PROPERTY_SECURITY_LEVEL),
                    readStringProperty(mediaDrm, MediaDrm.PROPERTY_VENDOR),
                    readStringProperty(mediaDrm, MediaDrm.PROPERTY_VERSION),
                    readStringProperty(mediaDrm, MediaDrm.PROPERTY_DESCRIPTION)
                );
            } catch (UnsupportedSchemeException error) {
                throw new DeviceIntegrityException("Widevine DRM is not supported on this device", error);
            } catch (RuntimeException error) {
                throw new DeviceIntegrityException("Unable to read Widevine DRM properties", error);
            } finally {
                if (mediaDrm != null) {
                    mediaDrm.release();
                }
            }
        }

        private byte[] readDeviceUniqueId(MediaDrm mediaDrm) {
            try {
                return mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID);
            } catch (RuntimeException error) {
                return null;
            }
        }

        private String readStringProperty(MediaDrm mediaDrm, String propertyName) {
            try {
                return mediaDrm.getPropertyString(propertyName);
            } catch (RuntimeException error) {
                return null;
            }
        }
    }

    static final class WidevineCapabilities {

        final boolean supported;
        final boolean fingerprintAvailable;
        final boolean securityLevelScanSupported;

        WidevineCapabilities(boolean supported, boolean fingerprintAvailable, boolean securityLevelScanSupported) {
            this.supported = supported;
            this.fingerprintAvailable = fingerprintAvailable;
            this.securityLevelScanSupported = securityLevelScanSupported;
        }
    }

    static final class WidevineData {

        final byte[] deviceUniqueId;
        final String securityLevel;
        final String vendor;
        final String version;
        final String description;

        WidevineData(byte[] deviceUniqueId, String securityLevel, String vendor, String version, String description) {
            this.deviceUniqueId = deviceUniqueId;
            this.securityLevel = securityLevel;
            this.vendor = vendor;
            this.version = version;
            this.description = description;
        }
    }

    static final class WidevineFingerprint {

        final String fingerprint;
        final String widevineIdSha256;
        final String widevineIdBase64;
        final String securityLevel;
        final String vendor;
        final String version;
        final String description;

        WidevineFingerprint(
            String fingerprint,
            String widevineIdSha256,
            String widevineIdBase64,
            String securityLevel,
            String vendor,
            String version,
            String description
        ) {
            this.fingerprint = fingerprint;
            this.widevineIdSha256 = widevineIdSha256;
            this.widevineIdBase64 = widevineIdBase64;
            this.securityLevel = securityLevel;
            this.vendor = vendor;
            this.version = version;
            this.description = description;
        }
    }

    static final class DeviceIntegrityException extends Exception {

        DeviceIntegrityException(String message) {
            super(message);
        }

        DeviceIntegrityException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
