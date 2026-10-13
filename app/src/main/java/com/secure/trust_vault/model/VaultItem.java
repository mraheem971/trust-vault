package com.secure.trust_vault.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * VaultItem
 * Entity representing an encrypted asset within the Vault.
 * Categories: PASSWORD, CARD, NOTE, DOCUMENT, ID_CARD, SECRETS
 */
@Entity(tableName = "vault_items")
public class VaultItem {

    public static final String CAT_PASSWORD = "PASSWORD";
    public static final String CAT_CARD = "CARD";
    public static final String CAT_NOTE = "NOTE";
    public static final String CAT_DOCUMENT = "DOCUMENT";
    public static final String CAT_ID_CARD = "ID_CARD";
    public static final String CAT_PERSONAL_INFO = "PERSONAL_INFO";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "title")
    private String title = "";

    @NonNull
    @ColumnInfo(name = "category")
    private String category = CAT_PASSWORD;

    @ColumnInfo(name = "username")
    private String username;

    // Encrypted with AES-GCM via Keystore
    @ColumnInfo(name = "encrypted_secret")
    private String encryptedSecret;

    // Encrypted additional notes / metadata
    @ColumnInfo(name = "encrypted_extra")
    private String encryptedExtra;

    // Optional internal file URI / document path encrypted
    @ColumnInfo(name = "document_path")
    private String documentPath;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public VaultItem() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    @androidx.room.Ignore
    public VaultItem(@NonNull String title, @NonNull String category, String username, String encryptedSecret, String encryptedExtra) {
        this(title, category, username, encryptedSecret, encryptedExtra, null);
    }

    @androidx.room.Ignore
    public VaultItem(@NonNull String title, @NonNull String category, String username, String encryptedSecret, String encryptedExtra, String documentPath) {
        this.title = title;
        this.category = category;
        this.username = username;
        this.encryptedSecret = encryptedSecret;
        this.encryptedExtra = encryptedExtra;
        this.documentPath = documentPath;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }

    @NonNull
    public String getCategory() { return category; }
    public void setCategory(@NonNull String category) { this.category = category; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEncryptedSecret() { return encryptedSecret; }
    public void setEncryptedSecret(String encryptedSecret) { this.encryptedSecret = encryptedSecret; }

    public String getEncryptedExtra() { return encryptedExtra; }
    public void setEncryptedExtra(String encryptedExtra) { this.encryptedExtra = encryptedExtra; }

    public String getDocumentPath() { return documentPath; }
    public void setDocumentPath(String documentPath) { this.documentPath = documentPath; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
