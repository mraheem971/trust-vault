package com.secure.trust_vault.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.secure.trust_vault.data.VaultRepository;
import com.secure.trust_vault.model.VaultItem;
import java.util.List;

/**
 * VaultViewModel
 * MVVM ViewModel exposing clean reactive LiveData streams for UI views.
 */
public class VaultViewModel extends AndroidViewModel {

    private final VaultRepository repository;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> categoryFilter = new MutableLiveData<>(null);

    private final LiveData<List<VaultItem>> vaultItems;
    private final LiveData<Integer> totalCount;
    private final LiveData<Integer> passwordCount;
    private final LiveData<Integer> cardCount;
    private final LiveData<Integer> noteCount;
    private final LiveData<Integer> docCount;

    public VaultViewModel(@NonNull Application application) {
        super(application);
        this.repository = new VaultRepository(application);

        this.totalCount = repository.getTotalCountLive();
        this.passwordCount = repository.getCategoryCountLive(VaultItem.CAT_PASSWORD);
        this.cardCount = repository.getCategoryCountLive(VaultItem.CAT_CARD);
        this.noteCount = repository.getCategoryCountLive(VaultItem.CAT_NOTE);
        this.docCount = repository.getCategoryCountLive(VaultItem.CAT_DOCUMENT);

        // Reactive stream combining search query and category filters
        this.vaultItems = Transformations.switchMap(searchQuery, query -> {
            if (query != null && !query.trim().isEmpty()) {
                return repository.searchLive(query.trim());
            } else {
                return Transformations.switchMap(categoryFilter, category -> {
                    if (category != null && !category.isEmpty()) {
                        return repository.getItemsByCategoryLive(category);
                    } else {
                        return repository.getAllItemsLive();
                    }
                });
            }
        });
    }

    public LiveData<List<VaultItem>> getVaultItems() {
        return vaultItems;
    }

    public LiveData<Integer> getTotalCount() { return totalCount; }
    public LiveData<Integer> getPasswordCount() { return passwordCount; }
    public LiveData<Integer> getCardCount() { return cardCount; }
    public LiveData<Integer> getNoteCount() { return noteCount; }
    public LiveData<Integer> getDocCount() { return docCount; }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void setCategoryFilter(String category) {
        categoryFilter.setValue(category);
    }

    public String getCurrentCategoryFilter() {
        return categoryFilter.getValue();
    }

    public void addItem(String title, String category, String username, String secret, String extra, String docPath) {
        repository.insert(title, category, username, secret, extra, docPath);
    }

    public void addItem(String title, String category, String username, String secret, String extra) {
        repository.insert(title, category, username, secret, extra, null);
    }

    public void updateItem(VaultItem item, String secret, String extra, String docPath) {
        repository.update(item, secret, extra, docPath);
    }

    public void deleteItem(VaultItem item) {
        repository.delete(item);
    }

    public String decryptSecret(VaultItem item) {
        return repository.decryptSecret(item);
    }

    public String decryptExtra(VaultItem item) {
        return repository.decryptExtra(item);
    }

    public VaultRepository getRepository() {
        return repository;
    }
}
