package com.secure.trust_vault.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * VaultPreferences
 * Manages master PIN, device lock permission, biometric preferences, and auto-lock tracking.
 */
public class VaultPreferences {

    private static final String PREF_NAME = "trust_vault_prefs";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_AUTO_LOCK_ENABLED = "auto_lock_enabled";
    private static final String KEY_DARK_MODE = "dark_mode_active";
    private static final String KEY_LAST_ACTIVE_TIMESTAMP = "last_active_time";

    // Dedicated App Master PIN & Phone Credentials Protection
    private static final String KEY_ALLOW_PHONE_LOCK = "allow_phone_lock";
    private static final String KEY_MASTER_PIN_HASH = "master_pin_hash";

    private final SharedPreferences prefs;

    public VaultPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isAutoLockEnabled() {
        return prefs.getBoolean(KEY_AUTO_LOCK_ENABLED, false);
    }

    public void setAutoLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_LOCK_ENABLED, enabled).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, true);
    }

    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public void updateLastActiveTimestamp() {
        prefs.edit().putLong(KEY_LAST_ACTIVE_TIMESTAMP, System.currentTimeMillis()).apply();
    }

    public long getLastActiveTimestamp() {
        return prefs.getLong(KEY_LAST_ACTIVE_TIMESTAMP, 0);
    }

    /**
     * Determines whether phone password/PIN/pattern fallback is permitted.
     * Default: FALSE (Phone passwords will NEVER unlock this app unless explicitly enabled by user).
     */
    public boolean isAllowPhoneLock() {
        return prefs.getBoolean(KEY_ALLOW_PHONE_LOCK, false);
    }

    public void setAllowPhoneLock(boolean allow) {
        prefs.edit().putBoolean(KEY_ALLOW_PHONE_LOCK, allow).apply();
    }

    /**
     * Master Vault PIN (independent app pin).
     */
    public String getMasterPinHash() {
        return prefs.getString(KEY_MASTER_PIN_HASH, "");
    }

    public void setMasterPin(String plainPin) {
        if (plainPin == null || plainPin.trim().isEmpty()) {
            prefs.edit().remove(KEY_MASTER_PIN_HASH).apply();
        } else {
            // Hash with SHA-256 for secure local verification
            prefs.edit().putString(KEY_MASTER_PIN_HASH, hashString(plainPin.trim())).apply();
        }
    }

    public boolean hasMasterPin() {
        String hash = getMasterPinHash();
        return hash != null && !hash.isEmpty();
    }

    public boolean verifyMasterPin(String candidatePin) {
        if (!hasMasterPin()) return false;
        return getMasterPinHash().equals(hashString(candidatePin.trim()));
    }

    private String hashString(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
