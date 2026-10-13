package com.secure.trust_vault.util;

import java.security.SecureRandom;

/**
 * PasswordGenerator
 * Generates high-entropy passwords with custom criteria and evaluates password strength.
 */
public class PasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private static final SecureRandom random = new SecureRandom();

    public static String generatePassword(int length, boolean upper, boolean lower, boolean digits, boolean symbols) {
        StringBuilder charPool = new StringBuilder();
        if (upper) charPool.append(UPPERCASE);
        if (lower) charPool.append(LOWERCASE);
        if (digits) charPool.append(DIGITS);
        if (symbols) charPool.append(SYMBOLS);

        if (charPool.length() == 0) {
            charPool.append(LOWERCASE).append(DIGITS);
        }

        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(charPool.length());
            result.append(charPool.charAt(index));
        }
        return result.toString();
    }

    public static int calculateStrengthScore(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;

        int len = password.length();
        if (len >= 8) score += 20;
        if (len >= 12) score += 25;
        if (len >= 16) score += 25;

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSymbol = true;
        }

        if (hasUpper) score += 7;
        if (hasLower) score += 7;
        if (hasDigit) score += 8;
        if (hasSymbol) score += 8;

        return Math.min(100, score);
    }

    public static String getStrengthLabel(int score) {
        if (score < 40) return "Weak";
        if (score < 70) return "Moderate";
        if (score < 85) return "Strong";
        return "Very Strong";
    }
}
