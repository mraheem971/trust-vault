package com.secure.trust_vault;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.secure.trust_vault.model.VaultItem;
import com.secure.trust_vault.security.BiometricAuthHelper;
import com.secure.trust_vault.ui.BackupFragment;
import com.secure.trust_vault.ui.DashboardFragment;
import com.secure.trust_vault.ui.GeneratorFragment;
import com.secure.trust_vault.ui.SettingsFragment;
import com.secure.trust_vault.ui.VaultViewModel;
import com.secure.trust_vault.util.DocumentVaultManager;
import com.secure.trust_vault.util.VaultPreferences;
import java.io.File;

public class MainActivity extends AppCompatActivity {

    private FrameLayout lockOverlay;
    private FrameLayout splashScreen;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAdd;
    private VaultViewModel viewModel;
    private VaultPreferences preferences;
    private boolean isUnlocked = false;
    private boolean isSplashFinished = false;

    // Document attachment & Camera capture handling
    private ActivityResultLauncher<String> documentPickerLauncher;
    private ActivityResultLauncher<Uri> cameraScanLauncher;
    private Uri currentCameraPhotoUri;
    private File currentCameraPhotoFile;
    private DocumentSelectedCallback documentSelectedCallback;

    public interface DocumentSelectedCallback {
        void onDocumentSelected(String internalFilePath, String displayName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Prevent screen capture / recents snapshot for security
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        preferences = new VaultPreferences(this);
        if (preferences.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        viewModel = new ViewModelProvider(this).get(VaultViewModel.class);

        splashScreen = findViewById(R.id.splash_screen);
        lockOverlay = findViewById(R.id.lock_overlay);
        bottomNav = findViewById(R.id.bottom_navigation);
        fabAdd = findViewById(R.id.fab_add);

        // Hide navigation and fab until splash animation completes
        bottomNav.setVisibility(View.GONE);
        fabAdd.hide();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            if (bottomNav != null) {
                // Ensure bottom navigation respects navigation bar height so labels are never hidden
                bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            }
            return insets;
        });

        setupFilePickers();
        setupNavigation();
        setupAddDialog();

        // Default to Dashboard
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

        runSplashScreenAnimation();
    }

    private void setupFilePickers() {
        // Document / File upload picker
        documentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && documentSelectedCallback != null) {
                        String savedPath = DocumentVaultManager.copyUriToVault(this, uri, "document");
                        if (savedPath != null) {
                            documentSelectedCallback.onDocumentSelected(savedPath, new File(savedPath).getName());
                        } else {
                            Toast.makeText(this, "Failed to import document", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Camera document scan launcher
        cameraScanLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && currentCameraPhotoFile != null && documentSelectedCallback != null) {
                        documentSelectedCallback.onDocumentSelected(
                                currentCameraPhotoFile.getAbsolutePath(),
                                currentCameraPhotoFile.getName());
                    }
                });
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                loadFragment(new DashboardFragment());
                fabAdd.show();
                return true;
            } else if (itemId == R.id.nav_generator) {
                loadFragment(new GeneratorFragment());
                fabAdd.hide();
                return true;
            } else if (itemId == R.id.nav_backup) {
                loadFragment(new BackupFragment());
                fabAdd.hide();
                return true;
            } else if (itemId == R.id.nav_settings) {
                loadFragment(new SettingsFragment());
                fabAdd.hide();
                return true;
            }
            return false;
        });
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void navigateToSettings() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_settings);
        }
    }

    private void runSplashScreenAnimation() {
        if (splashScreen == null) {
            setupAuthentication();
            return;
        }

        View splashIcon = findViewById(R.id.splash_icon);
        View splashGlow = findViewById(R.id.splash_glow);
        View splashTitle = findViewById(R.id.splash_title);
        View splashSubtitle = findViewById(R.id.splash_subtitle);
        View splashProgress = findViewById(R.id.splash_progress);

        // Initial setup for entrance
        if (splashIcon != null) {
            splashIcon.setScaleX(0.7f);
            splashIcon.setScaleY(0.7f);
            splashIcon.setAlpha(0f);
        }
        if (splashGlow != null) {
            splashGlow.setScaleX(0.5f);
            splashGlow.setScaleY(0.5f);
            splashGlow.setAlpha(0f);
        }
        if (splashTitle != null) {
            splashTitle.setAlpha(0f);
            splashTitle.setTranslationY(20f);
        }
        if (splashSubtitle != null) {
            splashSubtitle.setAlpha(0f);
            splashSubtitle.setTranslationY(15f);
        }
        if (splashProgress != null) {
            splashProgress.setAlpha(0f);
        }

        // 1. Icon & Glow smooth pop in with overshoot
        if (splashIcon != null) {
            splashIcon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(700)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                    .start();
        }

        if (splashGlow != null) {
            splashGlow.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .alpha(0.85f)
                    .setDuration(900)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }

        // 2. Title & Subtitle fade in with slight upward slide
        if (splashTitle != null) {
            splashTitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(250)
                    .setDuration(500)
                    .start();
        }

        if (splashSubtitle != null) {
            splashSubtitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(350)
                    .setDuration(500)
                    .start();
        }

        if (splashProgress != null) {
            splashProgress.animate()
                    .alpha(1f)
                    .setStartDelay(450)
                    .setDuration(400)
                    .start();
        }

        // 3. Smooth exit transition after showcase (total ~1.8 seconds)
        splashScreen.postDelayed(() -> {
            splashScreen.animate()
                    .alpha(0f)
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        splashScreen.setVisibility(View.GONE);
                        isSplashFinished = true;
                        setupAuthentication();
                    })
                    .start();
        }, 1800);
    }

    private void setupAuthentication() {
        MaterialButton btnBiometric = findViewById(R.id.btn_unlock_biometric);
        MaterialButton btnUnlockPin = findViewById(R.id.btn_unlock_pin);
        TextInputEditText etLockPin = findViewById(R.id.et_lock_pin);

        btnBiometric.setOnClickListener(v -> authenticateBiometric());

        btnUnlockPin.setOnClickListener(v -> {
            String enteredPin = etLockPin.getText() != null ? etLockPin.getText().toString().trim() : "";
            if (!preferences.hasMasterPin()) {
                // If user hasn't created a master PIN yet, prompt them or allow first unlock
                if (enteredPin.length() >= 4) {
                    preferences.setMasterPin(enteredPin);
                    unlockVault();
                    Toast.makeText(this, "Master PIN created and Vault Unlocked", Toast.LENGTH_SHORT).show();
                } else {
                    etLockPin.setError("Enter at least 4 digits to create Master PIN");
                }
            } else {
                if (preferences.verifyMasterPin(enteredPin)) {
                    etLockPin.setText("");
                    unlockVault();
                    Toast.makeText(this, "Vault Unlocked with Master PIN", Toast.LENGTH_SHORT).show();
                } else {
                    etLockPin.setError("Incorrect Master PIN");
                }
            }
        });

        // If user hasn't created a master PIN yet, adjust button hint
        if (!preferences.hasMasterPin()) {
            btnUnlockPin.setText("Create & Unlock with PIN");
            etLockPin.setHint("Create New Master PIN (4+ digits)");
        } else {
            btnUnlockPin.setText("Unlock with Vault PIN");
            etLockPin.setHint("Enter Vault Master PIN");
        }

        // Show/hide biometric button depending on whether hardware is available
        boolean allowPhone = preferences.isAllowPhoneLock();
        if (BiometricAuthHelper.isBiometricAvailable(this, allowPhone)) {
            btnBiometric.setVisibility(View.VISIBLE);
        } else {
            btnBiometric.setVisibility(View.VISIBLE);
        }

        // Only lock on startup IF user has enabled 'Lock on start' AND has security configured
        if (preferences.isLockOnStartEnabled() && (preferences.hasMasterPin() || preferences.isBiometricEnabled())) {
            lockVault();
        } else {
            // User did not set lock or removed lock from opening/start: Open directly!
            unlockVault();
        }
    }

    public void lockVault() {
        // Do not lock if user has never configured a PIN or Biometrics
        if (!preferences.hasMasterPin() && !preferences.isBiometricEnabled()) {
            unlockVault();
            return;
        }

        isUnlocked = false;
        if (lockOverlay != null) {
            lockOverlay.setVisibility(View.VISIBLE);
            lockOverlay.animate().alpha(1f).setDuration(250).start();
        }
        if (fabAdd != null) {
            fabAdd.hide();
        }
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }

        // Update notice if phone password is disallowed
        TextView tvNotice = findViewById(R.id.tv_phone_lock_notice);
        if (tvNotice != null) {
            if (preferences.isAllowPhoneLock()) {
                tvNotice.setText("ℹ️ Phone lock password enabled as fallback");
            } else {
                tvNotice.setText("🛡️ Phone screen password disallowed. Only Sensor or Master PIN works.");
            }
        }
    }

    public void unlockVault() {
        isUnlocked = true;
        if (lockOverlay != null) {
            lockOverlay.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> lockOverlay.setVisibility(View.GONE))
                    .start();
        }
        if (isSplashFinished && bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
            if (bottomNav.getSelectedItemId() == R.id.nav_dashboard && fabAdd != null) {
                fabAdd.show();
            }
        }
    }

    private void authenticateBiometric() {
        if (isFinishing() || isDestroyed()) return;

        boolean allowPhone = preferences.isAllowPhoneLock();
        if (!BiometricAuthHelper.isBiometricAvailable(this, allowPhone)) {
            Toast.makeText(this, "Biometric sensor unavailable or not enrolled. Please use Master PIN.", Toast.LENGTH_SHORT).show();
            return;
        }

        BiometricAuthHelper.showBiometricPrompt(this, allowPhone, new BiometricAuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                unlockVault();
                Toast.makeText(MainActivity.this, "Vault Unlocked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMsg) {
                // Do not allow access; user can use independent Master PIN
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAddDialog() {
        fabAdd.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_vault_item, null);
            AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.TrustDialogTheme)
                    .setView(dialogView)
                    .create();

            AutoCompleteTextView actvCategory = dialogView.findViewById(R.id.actv_category);
            TextInputEditText etTitle = dialogView.findViewById(R.id.et_item_title);
            TextInputEditText etUsername = dialogView.findViewById(R.id.et_item_username);
            TextInputEditText etSecret = dialogView.findViewById(R.id.et_item_secret);
            TextInputEditText etExtra = dialogView.findViewById(R.id.et_item_extra);

            MaterialButton btnUploadDoc = dialogView.findViewById(R.id.btn_upload_doc);
            MaterialButton btnScanDoc = dialogView.findViewById(R.id.btn_scan_doc);
            View layoutAttachment = dialogView.findViewById(R.id.layout_attachment_preview);
            TextView tvAttachedFileName = dialogView.findViewById(R.id.tv_attached_file_name);
            ImageButton btnRemoveAttachment = dialogView.findViewById(R.id.btn_remove_attachment);

            MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_item);
            MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_dialog);

            final String[] attachedFilePath = new String[]{null};

            String[] categories = {
                    VaultItem.CAT_PASSWORD,
                    VaultItem.CAT_CARD,
                    VaultItem.CAT_NOTE,
                    VaultItem.CAT_DOCUMENT,
                    VaultItem.CAT_ID_CARD,
                    VaultItem.CAT_PERSONAL_INFO
            };
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
            actvCategory.setAdapter(catAdapter);
            actvCategory.setText(VaultItem.CAT_PASSWORD, false);

            // Upload Document Action
            btnUploadDoc.setOnClickListener(btn -> {
                documentSelectedCallback = (internalPath, displayName) -> {
                    attachedFilePath[0] = internalPath;
                    layoutAttachment.setVisibility(View.VISIBLE);
                    tvAttachedFileName.setText(displayName);
                    Toast.makeText(this, "Document Attached: " + displayName, Toast.LENGTH_SHORT).show();
                };
                documentPickerLauncher.launch("*/*");
            });

            // Camera Scan Action
            btnScanDoc.setOnClickListener(btn -> {
                try {
                    currentCameraPhotoFile = DocumentVaultManager.createScanFile(this);
                    currentCameraPhotoUri = FileProvider.getUriForFile(
                            this, "com.secure.trust_vault.fileprovider", currentCameraPhotoFile);

                    documentSelectedCallback = (internalPath, displayName) -> {
                        attachedFilePath[0] = internalPath;
                        layoutAttachment.setVisibility(View.VISIBLE);
                        tvAttachedFileName.setText(displayName);
                        Toast.makeText(this, "Scan Attached: " + displayName, Toast.LENGTH_SHORT).show();
                    };
                    cameraScanLauncher.launch(currentCameraPhotoUri);
                } catch (Exception e) {
                    Toast.makeText(this, "Error opening camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            btnRemoveAttachment.setOnClickListener(btn -> {
                attachedFilePath[0] = null;
                layoutAttachment.setVisibility(View.GONE);
            });

            btnCancel.setOnClickListener(view -> dialog.dismiss());

            btnSave.setOnClickListener(view -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
                String secret = etSecret.getText() != null ? etSecret.getText().toString() : "";
                String extra = etExtra.getText() != null ? etExtra.getText().toString() : "";
                String category = actvCategory.getText().toString();

                if (title.isEmpty()) {
                    etTitle.setError("Name of data / Title is required");
                    return;
                }

                viewModel.addItem(title, category, username, secret, extra, attachedFilePath[0]);
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Encrypted and added to Vault section: " + category, Toast.LENGTH_SHORT).show();
            });

            dialog.show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (preferences.isAutoLockEnabled()) {
            preferences.updateLastActiveTimestamp();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences.isAutoLockEnabled() && !isUnlocked) {
            lockVault();
        }
    }
}