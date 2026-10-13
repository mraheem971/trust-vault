package com.secure.trust_vault.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.secure.trust_vault.R;
import com.secure.trust_vault.util.PasswordGenerator;

public class GeneratorFragment extends Fragment {

    private TextView tvPassword;
    private TextView tvStrengthLabel;
    private TextView tvLengthDisplay;
    private ProgressBar pbStrength;
    private Slider sliderLength;
    private MaterialSwitch switchUpper;
    private MaterialSwitch switchLower;
    private MaterialSwitch switchDigits;
    private MaterialSwitch switchSymbols;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_generator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPassword = view.findViewById(R.id.tv_generated_password);
        tvStrengthLabel = view.findViewById(R.id.tv_strength_label);
        tvLengthDisplay = view.findViewById(R.id.tv_length_display);
        pbStrength = view.findViewById(R.id.pb_strength);
        sliderLength = view.findViewById(R.id.slider_length);
        switchUpper = view.findViewById(R.id.switch_upper);
        switchLower = view.findViewById(R.id.switch_lower);
        switchDigits = view.findViewById(R.id.switch_digits);
        switchSymbols = view.findViewById(R.id.switch_symbols);

        MaterialButton btnCopy = view.findViewById(R.id.btn_copy_password);
        MaterialButton btnRegenerate = view.findViewById(R.id.btn_regenerate_password);

        sliderLength.addOnChangeListener((slider, value, fromUser) -> {
            int len = (int) value;
            tvLengthDisplay.setText(String.valueOf(len));
            generateAndDisplay();
        });

        View.OnClickListener toggleListener = v -> generateAndDisplay();
        switchUpper.setOnClickListener(toggleListener);
        switchLower.setOnClickListener(toggleListener);
        switchDigits.setOnClickListener(toggleListener);
        switchSymbols.setOnClickListener(toggleListener);

        btnRegenerate.setOnClickListener(v -> generateAndDisplay());

        btnCopy.setOnClickListener(v -> {
            String pass = tvPassword.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Generated Password", pass);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), R.string.copied_toast, Toast.LENGTH_SHORT).show();
            }
        });

        generateAndDisplay();
    }

    private void generateAndDisplay() {
        int length = (int) sliderLength.getValue();
        boolean upper = switchUpper.isChecked();
        boolean lower = switchLower.isChecked();
        boolean digits = switchDigits.isChecked();
        boolean symbols = switchSymbols.isChecked();

        // Ensure at least one option is picked
        if (!upper && !lower && !digits && !symbols) {
            switchLower.setChecked(true);
            lower = true;
        }

        String password = PasswordGenerator.generatePassword(length, upper, lower, digits, symbols);
        tvPassword.setText(password);

        int score = PasswordGenerator.calculateStrengthScore(password);
        pbStrength.setProgress(score);
        String label = PasswordGenerator.getStrengthLabel(score);
        tvStrengthLabel.setText("Strength: " + label + " (" + score + "%)");

        int color;
        if (score < 40) {
            color = ContextCompat.getColor(requireContext(), R.color.status_danger);
        } else if (score < 70) {
            color = ContextCompat.getColor(requireContext(), R.color.status_warning);
        } else {
            color = ContextCompat.getColor(requireContext(), R.color.status_success);
        }

        pbStrength.setProgressTintList(ColorStateList.valueOf(color));
        tvStrengthLabel.setTextColor(color);
    }
}
