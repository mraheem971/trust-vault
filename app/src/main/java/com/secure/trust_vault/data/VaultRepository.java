package com.secure.trust_vault.data;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.secure.trust_vault.model.VaultItem;
import com.secure.trust_vault.security.TrustVaultSecurityManager;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * VaultRepository
 * Coordinates database operations and AES-GCM Keystore encryption/decryption.
 */
public class VaultRepository {

    private final VaultDao vaultDao;
    private final TrustVaultSecurityManager securityManager;
    private final ExecutorService executorService;

    public VaultRepository(Application application) {
        TrustVaultDatabase db = TrustVaultDatabase.getInstance(application);
        this.vaultDao = db.vaultDao();
        this.securityManager = TrustVaultSecurityManager.getInstance();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<VaultItem>> getAllItemsLive() {
        return vaultDao.getAllLive();
    }

    public LiveData<List<VaultItem>> getItemsByCategoryLive(String category) {
        return vaultDao.getByCategoryLive(category);
    }

    public LiveData<List<VaultItem>> searchLive(String query) {
        return vaultDao.searchLive(query);
    }

    public LiveData<Integer> getTotalCountLive() {
        return vaultDao.getTotalCountLive();
    }

    public LiveData<Integer> getCategoryCountLive(String category) {
        return vaultDao.getCategoryCountLive(category);
    }

    /**
     * Encrypts secrets with Keystore before saving into Room database.
     */
    public void insert(String title, String category, String username, String plainSecret, String plainExtra, String docPath) {
        executorService.execute(() -> {
            String encryptedSecret = securityManager.encrypt(plainSecret);
            String encryptedExtra = securityManager.encrypt(plainExtra);
            VaultItem item = new VaultItem(title, category, username, encryptedSecret, encryptedExtra, docPath);
            vaultDao.insert(item);
        });
    }

    public void update(VaultItem item, String plainSecret, String plainExtra, String docPath) {
        executorService.execute(() -> {
            item.setEncryptedSecret(securityManager.encrypt(plainSecret));
            item.setEncryptedExtra(securityManager.encrypt(plainExtra));
            item.setDocumentPath(docPath);
            item.setUpdatedAt(System.currentTimeMillis());
            vaultDao.update(item);
        });
    }

    public void delete(VaultItem item) {
        executorService.execute(() -> vaultDao.delete(item));
    }

    public String decryptSecret(VaultItem item) {
        return securityManager.decrypt(item.getEncryptedSecret());
    }

    public String decryptExtra(VaultItem item) {
        return securityManager.decrypt(item.getEncryptedExtra());
    }

    public List<VaultItem> getAllSync() {
        return vaultDao.getAllSync();
    }

    public void insertAll(List<VaultItem> items) {
        executorService.execute(() -> vaultDao.insertAll(items));
    }
}
