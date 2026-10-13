package com.secure.trust_vault.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.secure.trust_vault.R;
import com.secure.trust_vault.model.VaultItem;
import com.secure.trust_vault.security.TrustVaultSecurityManager;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class BackupFragment extends Fragment {

    private VaultViewModel viewModel;
    private TextInputEditText etBackupPassphrase;
    private TextInputEditText etRestorePayload;
    private TextInputEditText etRestorePassphrase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_backup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(VaultViewModel.class);

        etBackupPassphrase = view.findViewById(R.id.et_backup_passphrase);
        etRestorePayload = view.findViewById(R.id.et_restore_payload);
        etRestorePassphrase = view.findViewById(R.id.et_restore_passphrase);

        MaterialButton btnExport = view.findViewById(R.id.btn_export_backup);
        MaterialButton btnRestore = view.findViewById(R.id.btn_restore_backup);

        btnExport.setOnClickListener(v -> performExport());
        btnRestore.setOnClickListener(v -> performRestore());
    }

    private void performExport() {
        String passphrase = etBackupPassphrase.getText() != null ? etBackupPassphrase.getText().toString() : "";
        if (passphrase.length() < 6) {
            etBackupPassphrase.setError("Passphrase must be at least 6 characters");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<VaultItem> allItems = viewModel.getRepository().getAllSync();
                // Create plain export models
                List<ExportModel> exportModels = new ArrayList<>();
                for (VaultItem item : allItems) {
                    ExportModel m = new ExportModel();
                    m.title = item.getTitle();
                    m.category = item.getCategory();
                    m.username = item.getUsername();
                    m.secret = viewModel.decryptSecret(item);
                    m.extra = viewModel.decryptExtra(item);
                    exportModels.add(m);
                }

                String json = new Gson().toJson(exportModels);
                String encryptedPayload = TrustVaultSecurityManager.encryptWithPassphrase(json, passphrase);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard != null) {
                            ClipData clip = ClipData.newPlainText("Encrypted Trust Vault Backup", encryptedPayload);
                            clipboard.setPrimaryClip(clip);
                        }

                        new MaterialAlertDialogBuilder(requireContext(), R.style.TrustDialogTheme)
                                .setTitle("Backup Generated")
                                .setMessage("Your AES-GCM encrypted backup payload (" + allItems.size() + " items) has been copied to your clipboard. Store it safely!")
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void performRestore() {
        String payload = etRestorePayload.getText() != null ? etRestorePayload.getText().toString().trim() : "";
        String passphrase = etRestorePassphrase.getText() != null ? etRestorePassphrase.getText().toString() : "";

        if (payload.isEmpty()) {
            etRestorePayload.setError("Encrypted payload cannot be empty");
            return;
        }

        if (passphrase.isEmpty()) {
            etRestorePassphrase.setError("Decryption passphrase is required");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String decryptedJson = TrustVaultSecurityManager.decryptWithPassphrase(payload, passphrase);
                Type listType = new TypeToken<ArrayList<ExportModel>>(){}.getType();
                List<ExportModel> models = new Gson().fromJson(decryptedJson, listType);

                if (models != null) {
                    for (ExportModel m : models) {
                        viewModel.addItem(m.title, m.category, m.username, m.secret, m.extra);
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Successfully restored " + (models != null ? models.size() : 0) + " items!", Toast.LENGTH_LONG).show();
                        etRestorePayload.setText("");
                        etRestorePassphrase.setText("");
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        new MaterialAlertDialogBuilder(requireContext(), R.style.TrustDialogTheme)
                                .setTitle("Decryption Failed")
                                .setMessage("Could not restore backup. Verify your passphrase and payload integrity.")
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }
            }
        });
    }

    private static class ExportModel {
        String title;
        String category;
        String username;
        String secret;
        String extra;
    }
}
