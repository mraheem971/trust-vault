# Trust Vault 🛡️

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Encryption](https://img.shields.io/badge/Security-AES--256--GCM-0284C7?style=for-the-badge&logo=shield&logoColor=white)](https://developer.android.com/training/articles/keystore)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-8B5CF6?style=for-the-badge)](https://developer.android.com/topic/architecture)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

> **Trust Vault** is a high-security, offline-first Android digital vault engineered to protect passwords, private documents, credit cards, identity IDs, and confidential notes. Built with hardware-backed Android Keystore, AES-256-GCM authenticated encryption, and a custom Dark Navy design.

---

## ✨ Highlights & Features

- 🔐 **Military-Grade Hardware Encryption**:
  - Keys generated and isolated inside **Android Keystore (Hardware TEE / StrongBox)**.
  - AES-256-GCM encryption with 128-bit authentication tag and randomized 12-byte IVs for zero pattern leakage.
- 📱 **Zero Lockout on First Launch**:
  - Fresh installations open instantly with a smooth animated dashboard.
  - Users configure their independent Master PIN or Biometric unlock at their own convenience under **Settings**.
- 🛡️ **Phone Password Isolation**:
  - By default, general phone screen passwords/patterns **cannot** unlock Trust Vault. Only your independent Master PIN or biometric sensor is accepted.
- 📁 **Document Vault & Camera Scanner**:
  - Upload confidential PDF/image documents or scan IDs directly via camera.
  - Files are sandboxed inside internal app storage and protected by Android FileProvider.
- ⚡ **Real-Time Live Search & Categorized Sections**:
  - Organize and filter items across **Passwords**, **Payment Cards**, **Private Notes**, **Documents**, **ID & Passports**, and **Personal Data**.
  - Instant client-side search across all stored assets.
- 🎲 **Strong Password Generator**:
  - Customizable generator for secure passwords (uppercase, lowercase, numbers, symbols, custom lengths up to 32 characters) with one-tap clipboard copy.
- 📦 **Password-Protected Encrypted Backups**:
  - Export and restore complete vaults with PBKDF2 (65,536 iterations) key derivation + AES-256-GCM encryption.
- 🎨 **Modern Dark Navy UI**:
  - Aesthetic deep navy color scheme (`#0B111E`), electric cyan accents (`#06B6D4`), glassmorphism cards, and fluid transition animations.

---

## 🏛️ Architecture & Tech Stack

```
trust-vault/
├── app/src/main/java/com/secure/trust_vault/
│   ├── data/                 # Room Database & DAO interfaces
│   │   ├── TrustVaultDatabase.java
│   │   ├── VaultDao.java
│   │   └── VaultRepository.java
│   ├── model/                # Data entities (VaultItem)
│   ├── security/             # Keystore cryptographic engine & Biometric auth
│   │   ├── TrustVaultSecurityManager.java
│   │   └── BiometricAuthHelper.java
│   ├── ui/                   # MVVM Views & ViewModels
│   │   ├── DashboardFragment.java
│   │   ├── GeneratorFragment.java
│   │   ├── BackupFragment.java
│   │   ├── SettingsFragment.java
│   │   ├── VaultViewModel.java
│   │   └── VaultItemAdapter.java
│   ├── util/                 # Storage management & Secure preferences
│   │   ├── DocumentVaultManager.java
│   │   └── VaultPreferences.java
│   ├── MainActivity.java     # Entry activity & navigation coordinator
│   └── TrustVaultApp.java    # Application class with global crash guard
```

- **Architecture**: Model-View-ViewModel (MVVM)
- **Database**: AndroidX Room (v2.6.1)
- **Security**: Android Keystore, AES-256-GCM, PBKDF2WithHmacSHA256, AndroidX Biometric (v1.1.0)
- **UI Components**: Google Material Components 3 (Material3 DayNight)
- **Min SDK**: 26 (Android 8.0 Oreo) | **Target SDK**: 35 (Android 15)

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Ladybug / Koala** or newer
- **JDK 17** or **JDK 21**
- Android device or emulator running **Android 8.0 (API 26)** or higher

### Build & Run
```bash
# 1. Clone the repository
git clone https://github.com/mraheem971/trust-vault.git
cd trust-vault

# 2. Build the Debug APK
./gradlew assembleDebug

# 3. Install on connected Android device/emulator
./gradlew installDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Security Best Practices Implemented

1. **Hardware-Backed Cryptography**: Master keys never enter application memory in raw plaintext; all transformations are processed inside the Android Keystore subsystem.
2. **Flag Secure Protection**: `FLAG_SECURE` is enforced to prevent screenshots, screen recorders, and task switcher previews from capturing sensitive vault records.
3. **Internal Sandboxing**: Uploaded and scanned documents are isolated within private internal cache directories inaccessible to other third-party apps.
4. **PBKDF2 Key Stretching**: Backup exports require a master passphrase and utilize 65,536 rounds of HMAC-SHA256 hashing to thwart brute-force attacks.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
