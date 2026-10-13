package com.secure.trust_vault.security;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import java.util.concurrent.Executor;

/**
 * BiometricAuthHelper
 * Integrates AndroidX BiometricPrompt for fingerprint/face hardware authentication.
 * If allowPhoneCredentials is false, device lock (phone password/pattern) is strictly disallowed.
 */
public class BiometricAuthHelper {

    public interface AuthCallback {
        void onSuccess();
        void onError(String errorMsg);
    }

    public static boolean isBiometricAvailable(Context context, boolean allowPhoneCredentials) {
        if (context == null) return false;
        try {
            BiometricManager biometricManager = BiometricManager.from(context);
            int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
            if (allowPhoneCredentials) {
                authenticators |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            }
            int canAuthenticate = biometricManager.canAuthenticate(authenticators);
            return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    public static void showBiometricPrompt(
            @NonNull FragmentActivity activity,
            boolean allowPhoneCredentials,
            @NonNull AuthCallback callback) {

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            callback.onError("Activity is not in a valid state");
            return;
        }

        try {
            if (!isBiometricAvailable(activity, allowPhoneCredentials)) {
                callback.onError("Biometric sensor not enrolled or unavailable on this device");
                return;
            }

            Executor executor = ContextCompat.getMainExecutor(activity);

            BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            callback.onSuccess();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            callback.onError(errString.toString());
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                        }
                    });

            int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
            if (allowPhoneCredentials) {
                authenticators |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            }

            BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Trust Vault Authentication")
                    .setSubtitle("Confirm identity with biometric sensor to unlock")
                    .setAllowedAuthenticators(authenticators);

            // When device credentials are not allowed, Android requires setNegativeButtonText
            if (!allowPhoneCredentials) {
                builder.setNegativeButtonText("Use App PIN / Cancel");
            }

            biometricPrompt.authenticate(builder.build());
        } catch (Exception e) {
            callback.onError("Biometrics initialization failed: " + e.getMessage());
        }
    }
}
