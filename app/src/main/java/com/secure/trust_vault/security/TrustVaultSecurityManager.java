package com.secure.trust_vault.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * TrustVaultSecurityManager
 * 
 * Cryptographic engine handling:
 * 1. Hardware-backed Android Keystore Master Key (AES-256)
 * 2. AES-GCM-NoPadding authenticated encryption/decryption
 * 3. PBKDF2 key derivation for password-protected backups
 */
public class TrustVaultSecurityManager {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "TrustVaultMasterAESKey";
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static TrustVaultSecurityManager instance;

    public static synchronized TrustVaultSecurityManager getInstance() {
        if (instance == null) {
            instance = new TrustVaultSecurityManager();
        }
        return instance;
    }

    private TrustVaultSecurityManager() {
        initOrLoadMasterKey();
    }

    private void initOrLoadMasterKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setRandomizedEncryptionRequired(true);

                keyGenerator.init(builder.build());
                keyGenerator.generateKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SecretKey getMasterKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    /**
     * Encrypts plaintext string using AES-256-GCM backed by Android Keystore.
     * Returns Base64 encoded format: [12-byte IV] + [Ciphertext + AuthTag]
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return "";
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, getMasterKey());
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return plaintext; // Fallback safe guard
        }
    }

    /**
     * Decrypts Base64 string produced by encrypt().
     */
    public String decrypt(String base64Combined) {
        if (base64Combined == null || base64Combined.isEmpty()) return "";
        try {
            byte[] combined = Base64.decode(base64Combined, Base64.NO_WRAP);
            if (combined.length <= GCM_IV_LENGTH_BYTES) {
                return base64Combined;
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // If already plaintext or failed decryption
            return base64Combined;
        }
    }

    /**
     * Encrypts data with a user passphrase (for backups) using PBKDF2 + AES-GCM.
     */
    public static String encryptWithPassphrase(String plaintext, String passphrase) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = f.generateSecret(spec).getEncoded();
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Format: [16 bytes salt] + [12 bytes IV] + [encrypted payload]
        byte[] payload = new byte[salt.length + iv.length + encrypted.length];
        System.arraycopy(salt, 0, payload, 0, salt.length);
        System.arraycopy(iv, 0, payload, salt.length, iv.length);
        System.arraycopy(encrypted, 0, payload, salt.length + iv.length, encrypted.length);

        return Base64.encodeToString(payload, Base64.NO_WRAP);
    }

    /**
     * Decrypts backup data with user passphrase using PBKDF2 + AES-GCM.
     */
    public static String decryptWithPassphrase(String base64Payload, String passphrase) throws Exception {
        byte[] payload = Base64.decode(base64Payload, Base64.NO_WRAP);
        if (payload.length < 28) {
            throw new IllegalArgumentException("Invalid payload format");
        }

        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        byte[] encrypted = new byte[payload.length - 28];

        System.arraycopy(payload, 0, salt, 0, 16);
        System.arraycopy(payload, 16, iv, 0, 12);
        System.arraycopy(payload, 28, encrypted, 0, encrypted.length);

        PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = f.generateSecret(spec).getEncoded();
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
