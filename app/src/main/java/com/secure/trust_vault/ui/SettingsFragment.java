package com.secure.trust_vault.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.secure.trust_vault.MainActivity;
import com.secure.trust_vault.R;
import com.secure.trust_vault.util.VaultPreferences;

public class SettingsFragment extends Fragment {

    private VaultPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferences = new VaultPreferences(requireContext());

        MaterialSwitch switchAllowPhonePassword = view.findViewById(R.id.switch_allow_phone_password);
        MaterialSwitch switchLockOnStart = view.findViewById(R.id.switch_lock_on_start);
        MaterialSwitch switchBiometric = view.findViewById(R.id.switch_biometric);
        MaterialSwitch switchAutoLock = view.findViewById(R.id.switch_auto_lock);
        MaterialSwitch switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        MaterialButton btnSetupPin = view.findViewById(R.id.btn_setup_master_pin);
        MaterialButton btnRemovePin = view.findViewById(R.id.btn_remove_master_pin);
        MaterialButton btnLockNow = view.findViewById(R.id.btn_lock_vault_now);

        switchAllowPhonePassword.setChecked(preferences.isAllowPhoneLock());
        switchLockOnStart.setChecked(preferences.isLockOnStartEnabled());
        switchBiometric.setChecked(preferences.isBiometricEnabled());
        switchAutoLock.setChecked(preferences.isAutoLockEnabled());
        switchDarkMode.setChecked(preferences.isDarkMode());

        updatePinButtonsState(btnSetupPin, btnRemovePin);

        switchAllowPhonePassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setAllowPhoneLock(isChecked);
            if (isChecked) {
                Toast.makeText(requireContext(), "Phone screen password/pattern allowed as backup", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Phone password disallowed. Only biometric sensor or Master PIN works.", Toast.LENGTH_LONG).show();
            }
        });

        // Toggle lock on app opening/start
        switchLockOnStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !preferences.hasMasterPin() && !preferences.isBiometricEnabled()) {
                buttonView.setChecked(false);
                Toast.makeText(requireContext(), "Please set up a Master PIN or enable Biometrics first", Toast.LENGTH_LONG).show();
                showSetupPinDialog(btnSetupPin, btnRemovePin);
                return;
            }
            preferences.setLockOnStartEnabled(isChecked);
            Toast.makeText(requireContext(), isChecked ? "Lock will be required when opening app" : "Lock removed from start. App opens directly!", Toast.LENGTH_SHORT).show();
        });

        btnSetupPin.setOnClickListener(v -> showSetupPinDialog(btnSetupPin, btnRemovePin));

        // Remove Master PIN entirely
        btnRemovePin.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext(), R.style.TrustDialogTheme)
                    .setTitle("Remove Master PIN?")
                    .setMessage("This will remove the Master PIN. The app will open directly without asking for a PIN code.")
                    .setPositiveButton("Remove PIN", (dialog, which) -> {
                        preferences.removeMasterPin();
                        // Also disable lock on start if no biometric remains
                        if (!preferences.isBiometricEnabled()) {
                            preferences.setLockOnStartEnabled(false);
                            switchLockOnStart.setChecked(false);
                        }
                        updatePinButtonsState(btnSetupPin, btnRemovePin);
                        Toast.makeText(requireContext(), "Master PIN removed. App lock disabled.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                boolean allowPhone = preferences.isAllowPhoneLock();
                if (!com.secure.trust_vault.security.BiometricAuthHelper.isBiometricAvailable(requireContext(), allowPhone)) {
                    buttonView.setChecked(false);
                    Toast.makeText(requireContext(), "Biometric hardware is not enrolled or unavailable on this device", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            preferences.setBiometricEnabled(isChecked);
            Toast.makeText(requireContext(), "Biometric unlock " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        switchAutoLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setAutoLockEnabled(isChecked);
            Toast.makeText(requireContext(), "Auto-lock " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setDarkMode(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        btnLockNow.setOnClickListener(v -> {
            if (!preferences.hasMasterPin() && !preferences.isBiometricEnabled()) {
                Toast.makeText(requireContext(), "Please set up a Master PIN or enable Biometrics first", Toast.LENGTH_SHORT).show();
                showSetupPinDialog(btnSetupPin, btnRemovePin);
                return;
            }
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).lockVault();
            }
        });
    }

    private void updatePinButtonsState(MaterialButton btnSetupPin, MaterialButton btnRemovePin) {
        if (preferences.hasMasterPin()) {
            btnSetupPin.setText("Change Master Vault PIN");
            btnRemovePin.setVisibility(View.VISIBLE);
        } else {
            btnSetupPin.setText("Set Master Vault PIN");
            btnRemovePin.setVisibility(View.GONE);
        }
    }

    private void showSetupPinDialog(MaterialButton btnSetupPin, MaterialButton btnRemovePin) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_vault_item, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.TrustDialogTheme)
                .setTitle(preferences.hasMasterPin() ? "Change Master PIN" : "Set Master PIN")
                .setMessage("Choose an independent PIN for Trust Vault. Phone screen passwords will NOT be able to open this vault.")
                .setView(dialogView)
                .create();

        // Configure dialog fields specifically for Master PIN setup
        dialogView.findViewById(R.id.til_category).setVisibility(View.GONE);
        dialogView.findViewById(R.id.til_title).setVisibility(View.GONE);
        dialogView.findViewById(R.id.til_extra).setVisibility(View.GONE);
        if (dialogView.findViewById(R.id.btn_upload_doc) != null) {
            dialogView.findViewById(R.id.btn_upload_doc).setVisibility(View.GONE);
            dialogView.findViewById(R.id.btn_scan_doc).setVisibility(View.GONE);
        }

        TextInputLayout tilUsername = dialogView.findViewById(R.id.til_username);
        tilUsername.setHint("Enter New Master PIN (e.g. 4-8 digits)");
        TextInputEditText etPin = dialogView.findViewById(R.id.et_item_username);
        etPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        TextInputLayout tilSecret = dialogView.findViewById(R.id.til_secret);
        tilSecret.setHint("Confirm New Master PIN");
        TextInputEditText etConfirmPin = dialogView.findViewById(R.id.et_item_secret);
        etConfirmPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_item);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_dialog);
        btnSave.setText("Save Master PIN");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String p1 = etPin.getText() != null ? etPin.getText().toString().trim() : "";
            String p2 = etConfirmPin.getText() != null ? etConfirmPin.getText().toString().trim() : "";

            if (p1.length() < 4) {
                etPin.setError("PIN must be at least 4 digits");
                return;
            }
            if (!p1.equals(p2)) {
                etConfirmPin.setError("PINs do not match");
                return;
            }

            preferences.setMasterPin(p1);
            // Default lock on start to true once user sets a PIN intentionally
            preferences.setLockOnStartEnabled(true);
            updatePinButtonsState(btnSetupPin, btnRemovePin);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Master Vault PIN configured successfully!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}
