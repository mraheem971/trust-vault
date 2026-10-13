package com.secure.trust_vault.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.secure.trust_vault.model.VaultItem;

/**
 * TrustVaultDatabase
 * Room Database singleton hosting vault_items.
 */
@Database(entities = {VaultItem.class}, version = 2, exportSchema = false)
public abstract class TrustVaultDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "trust_vault.db";
    private static volatile TrustVaultDatabase instance;

    public abstract VaultDao vaultDao();

    public static TrustVaultDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (TrustVaultDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            TrustVaultDatabase.class,
                            DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
