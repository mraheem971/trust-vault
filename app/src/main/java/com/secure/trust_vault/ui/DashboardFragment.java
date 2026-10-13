package com.secure.trust_vault.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.secure.trust_vault.MainActivity;
import com.secure.trust_vault.R;
import com.secure.trust_vault.model.VaultItem;
import com.secure.trust_vault.util.VaultPreferences;
import java.io.File;

public class DashboardFragment extends Fragment implements VaultItemAdapter.OnItemClickListener {

    private VaultViewModel viewModel;
    private VaultItemAdapter adapter;

    private TextView tvTotalItems;
    private TextView tvPasswordsCount;
    private TextView tvCardsCount;
    private TextView tvNotesCount;
    private TextView tvDocsCount;
    private TextView tvIdCardsCount;
    private TextView tvPersonalCount;
    private View layoutEmptyState;
    private TextView tvFilterStatus;
    private EditText etSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Smooth aesthetic entrance animation on launch
        view.setAlpha(0f);
        view.setTranslationY(20f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        viewModel = new ViewModelProvider(requireActivity()).get(VaultViewModel.class);

        tvTotalItems = view.findViewById(R.id.tv_total_items_count);
        tvPasswordsCount = view.findViewById(R.id.tv_count_passwords);
        tvCardsCount = view.findViewById(R.id.tv_count_cards);
        tvNotesCount = view.findViewById(R.id.tv_count_notes);
        tvDocsCount = view.findViewById(R.id.tv_count_documents);
        tvIdCardsCount = view.findViewById(R.id.tv_count_id_cards);
        tvPersonalCount = view.findViewById(R.id.tv_count_personal);

        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        tvFilterStatus = view.findViewById(R.id.tv_view_all_filter_clear);
        etSearch = view.findViewById(R.id.et_search);

        RecyclerView rvItems = view.findViewById(R.id.rv_vault_items);
        rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VaultItemAdapter(this);
        rvItems.setAdapter(adapter);

        // Category Filter Cards
        view.findViewById(R.id.card_cat_passwords).setOnClickListener(v -> toggleCategoryFilter(VaultItem.CAT_PASSWORD));
        view.findViewById(R.id.card_cat_cards).setOnClickListener(v -> toggleCategoryFilter(VaultItem.CAT_CARD));
        view.findViewById(R.id.card_cat_notes).setOnClickListener(v -> toggleCategoryFilter(VaultItem.CAT_NOTE));
        view.findViewById(R.id.card_cat_documents).setOnClickListener(v -> toggleCategoryFilter(VaultItem.CAT_DOCUMENT));
        if (view.findViewById(R.id.card_cat_id_cards) != null) {
            view.findViewById(R.id.card_cat_id_cards).setOnClickListener(v -> toggleCategoryFilter(VaultItem.CAT_ID_CARD));
        }
        if (view.findViewById(R.id.card_cat_personal) != null) {
            view.findViewById(R.id.card_cat_personal).setOnClickListener(v -> toggleCategoryFilter(VaultItem.CAT_PERSONAL_INFO));
        }

        tvFilterStatus.setOnClickListener(v -> {
            viewModel.setCategoryFilter(null);
            tvFilterStatus.setText(R.string.view_all);
        });

        // Security Setup Banner configuration (Guides user to configure PIN/Biometrics)
        View securityBanner = view.findViewById(R.id.card_security_setup_banner);
        View btnSetupNow = view.findViewById(R.id.btn_setup_security_now);
        VaultPreferences prefs = new VaultPreferences(requireContext());

        if (!prefs.hasMasterPin() && !prefs.isBiometricEnabled()) {
            securityBanner.setVisibility(View.VISIBLE);
        } else {
            securityBanner.setVisibility(View.GONE);
        }

        if (btnSetupNow != null) {
            btnSetupNow.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToSettings();
                }
            });
        }

        // Quick Lock Button in Header
        view.findViewById(R.id.btn_quick_lock).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                if (!prefs.hasMasterPin() && !prefs.isBiometricEnabled()) {
                    Toast.makeText(requireContext(), "Please set up a Master PIN in Settings before locking", Toast.LENGTH_SHORT).show();
                    ((MainActivity) getActivity()).navigateToSettings();
                } else {
                    ((MainActivity) getActivity()).lockVault();
                }
            }
        });

        // Search Input Watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        observeData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            View securityBanner = getView().findViewById(R.id.card_security_setup_banner);
            if (securityBanner != null) {
                VaultPreferences prefs = new VaultPreferences(requireContext());
                if (!prefs.hasMasterPin() && !prefs.isBiometricEnabled()) {
                    securityBanner.setVisibility(View.VISIBLE);
                } else {
                    securityBanner.setVisibility(View.GONE);
                }
            }
        }
    }

    private void toggleCategoryFilter(String category) {
        String current = viewModel.getCurrentCategoryFilter();
        if (category.equals(current)) {
            viewModel.setCategoryFilter(null);
            tvFilterStatus.setText(R.string.view_all);
        } else {
            viewModel.setCategoryFilter(category);
            tvFilterStatus.setText("Filter: " + category + " (Tap to clear)");
        }
    }

    private void observeData() {
        viewModel.getVaultItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            if (items == null || items.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
            } else {
                layoutEmptyState.setVisibility(View.GONE);
            }

            // Calculate category counts dynamically for live responsiveness
            if (items != null) {
                int idCardCount = 0;
                int personalCount = 0;
                for (VaultItem item : items) {
                    if (VaultItem.CAT_ID_CARD.equals(item.getCategory())) idCardCount++;
                    if (VaultItem.CAT_PERSONAL_INFO.equals(item.getCategory())) personalCount++;
                }
                if (tvIdCardsCount != null) tvIdCardsCount.setText(String.valueOf(idCardCount));
                if (tvPersonalCount != null) tvPersonalCount.setText(String.valueOf(personalCount));
            }
        });

        viewModel.getTotalCount().observe(getViewLifecycleOwner(), count -> {
            tvTotalItems.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.getPasswordCount().observe(getViewLifecycleOwner(), count -> {
            tvPasswordsCount.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.getCardCount().observe(getViewLifecycleOwner(), count -> {
            tvCardsCount.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.getNoteCount().observe(getViewLifecycleOwner(), count -> {
            tvNotesCount.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.getDocCount().observe(getViewLifecycleOwner(), count -> {
            tvDocsCount.setText(String.valueOf(count != null ? count : 0));
        });
    }

    @Override
    public void onItemClick(VaultItem item) {
        showItemDetailsDialog(item);
    }

    @Override
    public void onDeleteClick(VaultItem item) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.TrustDialogTheme)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to permanently delete \"" + item.getTitle() + "\" from your encrypted vault?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteItem(item);
                    Toast.makeText(requireContext(), "Item removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onCopyClick(VaultItem item) {
        String decrypted = viewModel.decryptSecret(item);
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Vault Secret", decrypted);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), item.getTitle() + " secret copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showItemDetailsDialog(VaultItem item) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_vault_item, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.TrustDialogTheme)
                .setView(dialogView)
                .create();

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        AutoCompleteTextView actvCategory = dialogView.findViewById(R.id.actv_category);
        TextInputEditText etTitle = dialogView.findViewById(R.id.et_item_title);
        TextInputEditText etUsername = dialogView.findViewById(R.id.et_item_username);
        TextInputEditText etSecret = dialogView.findViewById(R.id.et_item_secret);
        TextInputEditText etExtra = dialogView.findViewById(R.id.et_item_extra);
        View layoutAttachment = dialogView.findViewById(R.id.layout_attachment_preview);
        TextView tvAttachedFileName = dialogView.findViewById(R.id.tv_attached_file_name);
        ImageButton btnRemoveAttachment = dialogView.findViewById(R.id.btn_remove_attachment);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_item);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_dialog);

        tvDialogTitle.setText("Edit Vault Information");
        etTitle.setText(item.getTitle());
        etUsername.setText(item.getUsername());
        etSecret.setText(viewModel.decryptSecret(item));
        etExtra.setText(viewModel.decryptExtra(item));

        final String[] attachedPathHolder = new String[]{item.getDocumentPath()};

        if (attachedPathHolder[0] != null && !attachedPathHolder[0].isEmpty()) {
            layoutAttachment.setVisibility(View.VISIBLE);
            tvAttachedFileName.setText(new File(attachedPathHolder[0]).getName());

            // Tap on attachment to view
            layoutAttachment.setOnClickListener(v -> viewAttachedDocument(attachedPathHolder[0]));
        } else {
            layoutAttachment.setVisibility(View.GONE);
        }

        btnRemoveAttachment.setOnClickListener(v -> {
            attachedPathHolder[0] = null;
            layoutAttachment.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Document detached", Toast.LENGTH_SHORT).show();
        });

        String[] categories = {
                VaultItem.CAT_PASSWORD,
                VaultItem.CAT_CARD,
                VaultItem.CAT_NOTE,
                VaultItem.CAT_DOCUMENT,
                VaultItem.CAT_ID_CARD,
                VaultItem.CAT_PERSONAL_INFO
        };
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(catAdapter);
        actvCategory.setText(item.getCategory(), false);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String secret = etSecret.getText() != null ? etSecret.getText().toString() : "";
            String extra = etExtra.getText() != null ? etExtra.getText().toString() : "";
            String cat = actvCategory.getText().toString();

            if (title.isEmpty()) {
                etTitle.setError("Name of data / Title is required");
                return;
            }

            item.setTitle(title);
            item.setCategory(cat);
            item.setUsername(username);
            viewModel.updateItem(item, secret, extra, attachedPathHolder[0]);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Item updated securely", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void viewAttachedDocument(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                Toast.makeText(requireContext(), "Document file not found", Toast.LENGTH_SHORT).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(requireContext(), "com.secure.trust_vault.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open Document"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to view file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
