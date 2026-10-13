package com.secure.trust_vault.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.secure.trust_vault.model.VaultItem;
import java.util.List;

/**
 * VaultDao
 * Data Access Object providing reactive LiveData queries and CRUD operations.
 */
@Dao
public interface VaultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(VaultItem item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<VaultItem> items);

    @Update
    void update(VaultItem item);

    @Delete
    void delete(VaultItem item);

    @Query("SELECT * FROM vault_items ORDER BY updated_at DESC")
    LiveData<List<VaultItem>> getAllLive();

    @Query("SELECT * FROM vault_items ORDER BY updated_at DESC")
    List<VaultItem> getAllSync();

    @Query("SELECT * FROM vault_items WHERE category = :category ORDER BY updated_at DESC")
    LiveData<List<VaultItem>> getByCategoryLive(String category);

    @Query("SELECT * FROM vault_items WHERE title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    LiveData<List<VaultItem>> searchLive(String query);

    @Query("SELECT COUNT(*) FROM vault_items")
    LiveData<Integer> getTotalCountLive();

    @Query("SELECT COUNT(*) FROM vault_items WHERE category = :category")
    LiveData<Integer> getCategoryCountLive(String category);

    @Query("DELETE FROM vault_items")
    void clearAll();
}
