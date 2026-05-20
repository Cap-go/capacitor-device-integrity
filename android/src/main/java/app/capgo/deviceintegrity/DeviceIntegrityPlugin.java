package app.capgo.deviceintegrity;

import app.capgo.deviceintegrity.DeviceIntegrity.DeviceIntegrityException;
import app.capgo.deviceintegrity.DeviceIntegrity.WidevineCapabilities;
import app.capgo.deviceintegrity.DeviceIntegrity.WidevineFingerprint;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.StandardIntegrityException;
import com.google.android.play.core.integrity.StandardIntegrityManager;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CapacitorPlugin(name = "DeviceIntegrity")
public class DeviceIntegrityPlugin extends Plugin {

    private static final String CONFIG_CLOUD_PROJECT_NUMBER = "cloudProjectNumber";
    private static final String DEFAULT_ANDROID_KEY_ID = "android-standard-integrity";
    private static final String FORMAT_PLAY_INTEGRITY = "google-play-integrity-standard";
    private static final String INTEGRITY_ERROR = "INTEGRITY_ERROR";

    private final DeviceIntegrity implementation = new DeviceIntegrity();
    private final Map<String, StandardIntegrityManager.StandardIntegrityTokenProvider> tokenProviders = new ConcurrentHashMap<>();

    @PluginMethod
    public void getCapabilities(PluginCall call) {
        WidevineCapabilities widevine = implementation.getWidevineCapabilities();

        JSObject result = new JSObject();
        result.put("platform", "android");
        result.put("widevine", widevineCapabilitiesToJson(widevine));
        result.put("appAttest", supportToJson(false));
        result.put("deviceCheck", supportToJson(false));
        result.put("playIntegrity", supportToJson(implementation.isPlayIntegritySupported(getContext())));
        call.resolve(result);
    }

    @PluginMethod
    public void getWidevineFingerprint(PluginCall call) {
        boolean includeRawId = Boolean.TRUE.equals(call.getBoolean("includeRawId", false));
        String hashSalt = call.getString("hashSalt");

        try {
            WidevineFingerprint result = implementation.getWidevineFingerprint(getContext().getPackageName(), includeRawId, hashSalt);
            JSObject response = new JSObject();
            response.put("platform", "android");
            response.put("source", "widevine");
            response.put("fingerprint", result.fingerprint);
            response.put("widevineIdSha256", result.widevineIdSha256);
            putOptional(response, "widevineIdBase64", result.widevineIdBase64);
            putOptional(response, "securityLevel", result.securityLevel);
            putOptional(response, "vendor", result.vendor);
            putOptional(response, "version", result.version);
            putOptional(response, "description", result.description);
            call.resolve(response);
        } catch (DeviceIntegrityException error) {
            call.reject(error.getMessage(), "WIDEVINE_ERROR", error);
        }
    }

    @PluginMethod
    public void prepareAttestation(PluginCall call) {
        if (!implementation.isPlayIntegritySupported(getContext())) {
            call.reject("Play Integrity is not supported on this device");
            return;
        }

        prepareProvider(call, DEFAULT_ANDROID_KEY_ID, (provider) -> {
            JSObject response = new JSObject();
            response.put("keyId", DEFAULT_ANDROID_KEY_ID);
            response.put("format", FORMAT_PLAY_INTEGRITY);
            call.resolve(response);
        });
    }

    @PluginMethod
    public void createAttestation(PluginCall call) {
        String keyId = call.getString("keyId");
        String challenge = call.getString("challenge");

        if (isBlank(keyId)) {
            call.reject("keyId is required");
            return;
        }

        if (isBlank(challenge)) {
            call.reject("challenge is required");
            return;
        }

        requestStandardIntegrityToken(call, keyId, challenge);
    }

    @PluginMethod
    public void createAssertion(PluginCall call) {
        String keyId = call.getString("keyId");
        String payload = call.getString("payload");

        if (isBlank(keyId)) {
            call.reject("keyId is required");
            return;
        }

        if (isBlank(payload)) {
            call.reject("payload is required");
            return;
        }

        requestStandardIntegrityToken(call, keyId, payload);
    }

    @PluginMethod
    public void getDeviceCheckToken(PluginCall call) {
        call.reject("DeviceCheck is only available on iOS");
    }

    private JSObject widevineCapabilitiesToJson(WidevineCapabilities capabilities) {
        JSObject result = new JSObject();
        result.put("supported", capabilities.supported);
        result.put("fingerprintAvailable", capabilities.fingerprintAvailable);
        result.put("securityLevelScanSupported", capabilities.securityLevelScanSupported);
        return result;
    }

    private JSObject supportToJson(boolean supported) {
        JSObject result = new JSObject();
        result.put("supported", supported);
        return result;
    }

    private void putOptional(JSObject object, String key, String value) {
        if (!isBlank(value)) {
            object.put(key, value);
        }
    }

    private void requestStandardIntegrityToken(PluginCall call, String keyId, String payload) {
        if (!implementation.isPlayIntegritySupported(getContext())) {
            call.reject("Play Integrity is not supported on this device");
            return;
        }

        final String requestHash;
        try {
            requestHash = implementation.createRequestHash(payload);
        } catch (NoSuchAlgorithmException error) {
            call.reject("Failed to generate request hash", INTEGRITY_ERROR, error);
            return;
        }

        withProvider(call, keyId, (provider) -> {
            StandardIntegrityManager.StandardIntegrityTokenRequest tokenRequest =
                StandardIntegrityManager.StandardIntegrityTokenRequest.builder().setRequestHash(requestHash).build();

            provider
                .request(tokenRequest)
                .addOnSuccessListener((token) -> {
                    JSObject result = new JSObject();
                    result.put("token", token.token());
                    result.put("keyId", keyId);
                    result.put("format", FORMAT_PLAY_INTEGRITY);
                    call.resolve(result);
                })
                .addOnFailureListener((error) -> rejectPlayIntegrityError(call, "Play Integrity token request failed", error));
        });
    }

    private void withProvider(PluginCall call, String keyId, ProviderReadyCallback callback) {
        StandardIntegrityManager.StandardIntegrityTokenProvider existingProvider = tokenProviders.get(keyId);
        if (existingProvider != null) {
            callback.onReady(existingProvider);
            return;
        }

        if (DEFAULT_ANDROID_KEY_ID.equals(keyId)) {
            prepareProvider(call, keyId, callback);
            return;
        }

        call.reject("Unknown Android keyId. Call prepareAttestation() before requesting an attestation token.");
    }

    private void prepareProvider(PluginCall call, String keyId, ProviderReadyCallback callback) {
        final long cloudProjectNumber;
        try {
            cloudProjectNumber = resolveCloudProjectNumber(call);
        } catch (IllegalArgumentException error) {
            call.reject(error.getMessage());
            return;
        }

        StandardIntegrityManager integrityManager = IntegrityManagerFactory.createStandard(getContext());
        StandardIntegrityManager.PrepareIntegrityTokenRequest prepareRequest =
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder().setCloudProjectNumber(cloudProjectNumber).build();

        integrityManager
            .prepareIntegrityToken(prepareRequest)
            .addOnSuccessListener((provider) -> {
                tokenProviders.put(keyId, provider);
                callback.onReady(provider);
            })
            .addOnFailureListener((error) -> rejectPlayIntegrityError(call, "Failed to prepare Play Integrity token request", error));
    }

    private long resolveCloudProjectNumber(PluginCall call) {
        Long callValueLong = call.getLong(CONFIG_CLOUD_PROJECT_NUMBER);
        if (callValueLong != null) {
            return callValueLong;
        }

        String callValueString = call.getString(CONFIG_CLOUD_PROJECT_NUMBER);
        if (!isBlank(callValueString)) {
            return parseCloudProjectNumber(callValueString);
        }

        String configValueString = getConfig().getString(CONFIG_CLOUD_PROJECT_NUMBER);
        if (!isBlank(configValueString)) {
            return parseCloudProjectNumber(configValueString);
        }

        throw new IllegalArgumentException(
            "cloudProjectNumber is required on Android. Set DeviceIntegrity.cloudProjectNumber in capacitor config or pass it in options."
        );
    }

    private long parseCloudProjectNumber(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("cloudProjectNumber must be a valid integer string");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void rejectPlayIntegrityError(PluginCall call, String message, Exception error) {
        if (error instanceof StandardIntegrityException) {
            StandardIntegrityException integrityError = (StandardIntegrityException) error;
            call.reject(
                message + " with code " + integrityError.getErrorCode() + ": " + integrityError.getMessage(),
                INTEGRITY_ERROR,
                error
            );
            return;
        }

        call.reject(message, INTEGRITY_ERROR, error);
    }

    @FunctionalInterface
    private interface ProviderReadyCallback {
        void onReady(StandardIntegrityManager.StandardIntegrityTokenProvider provider);
    }
}
