# 🍎 Fruit Billing App (Smart POS & Calculator-First Billing)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Offline First](https://img.shields.io/badge/Architecture-100%25%20Offline--First-orange.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

A high-speed, offline-first Point-of-Sale (POS) and smart calculator billing application specifically optimized for local fruit shops and busy retail stalls. Designed from the ground up for high-crowd rush hours where speed, zero lag, minimal taps, and intuitive arithmetic workflows are critical.

---

## ✨ Key Features

### 🧮 1. Smart Calculator-First Billing (Calc Tab)
* **High-Speed Input Pad:** Purpose-built keypad with large touch targets, tactile haptic feedback, and zero UI clutter.
* **Auto-Decimal Weight Assist:** Automatically inserts a decimal point when entering weights after multiplication (`* 0` or `* 1` becomes `* 0.` or `* 1.`), allowing instantaneous entry like `200 × 0.4` without manual decimal hunting.
* **Live Calculation Preview:** Shows a ghost preview of intermediate calculation totals (`= ₹80.00`) before pressing `=`, preventing mental math fatigue and verification errors.
* **Quick Quantity Shortcuts:** Preset 1-tap buttons for common weight brackets (`100g`, `250g`, `500g`, `750g`).
* **Ergonomic Safety:** The **Save Bill** button is isolated on a dedicated bottom strip with double-tap protection, preventing accidental submissions during calculation.
* **Smart Clear (CC):** Clear current expression when typing, or clear entire bill items with an explicit confirmation dialog.

### 🔍 2. Product Search & Catalog Billing (Billing Tab)
* **Interactive Fruit Catalog:** Visual fruit chips featuring localized fruit emoji badges (🍎, 🍌, 🍊, 🥭, 🍇, etc.) and real-time pricing per kg/piece.
* **2-Row High-Efficiency Grid:** Compact scrollable catalog that displays 8–10 items simultaneously while leaving the majority of screen height for the active receipt.
* **Instant Quantity Modal (`AddProductDialog`):**
  * 1-Tap Weight Presets: `250g`, `500g`, `1 kg`, `1.5 kg`, `2 kg`, `3 kg` (or `1 pc`, `2 pcs`, `5 pcs`, `12 pcs`).
  * Custom decimal input with real-time subtotal computation.
* **Custom / Loose Item Support:** Quick "+ Custom Item" prompt to bill arbitrary non-catalog items with a custom description and price.

### 📋 3. Multi-Draft & Bill Management
* **Active & Held Bills:** Put a bill on **Hold** with one tap when a customer steps aside to pick more fruits, serve the next customer, and resume instantly from the dropdown selector.
* **State Preservation:** Multiple active drafts persist seamlessly across app restarts and navigation.
* **Tap-to-Edit & Delete:** Edit calculation expressions or remove lines with immediate total recalculation and Snackbar **Undo** protection.

### 💳 4. Flexible Payment & Settlement
* **Payment Mode Tracking:** Tag orders as **Cash** or **UPI** with single-tap toggle chips.
* **Discount / Final Price Override:** Set rounded bargain totals (e.g. calculated ₹362 -> final ₹350) with automated difference auditing.
* **Single-Tap Bill Completion:** Instant database commit, receipt generation, and automatic transition to the next sequential bill number (`#001`, `#002`, ...).

### 📊 5. Audit History & Product Management
* **History Tab:** Complete chronological log of finalized bills, daily sales analytics, and itemized receipt modal.
* **Menu Tab:** Full product catalog management—Add, Edit rates, Deactivate (pause without breaking historical bills), or Delete products.

---

## 🏗️ Architecture & Technology Stack

* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3).
* **Language:** 100% [Kotlin](https://kotlinlang.org) with Coroutines & StateFlow.
* **Architecture Pattern:** Clean MVVM / MVI with Unidirectional Data Flow (UDF).
* **Persistence:** [Room Database](https://developer.android.com/training/data-storage/room) with transactional consistency, foreign-key cascades, and offline-first reactive queries.
* **State Management:** Shared `BillingViewModel` between Billing and Calc tabs ensuring synchronized receipt state without duplication.
* **Math Precision:** High-precision `java.math.BigDecimal` financial rounding (`HALF_UP` scale 2).

---

## 📱 App Navigation Structure

| Tab | Role & Cashier Workflow |
| :--- | :--- |
| **Billing** | Search fruit catalog, select weights/pieces, view live bill, tag payment, save bill. |
| **Calc** | Fast arithmetic keypad (`200 × 0.45 = ₹90`), auto-decimal weight assist, live preview. |
| **History** | Itemized sales history, daily gross revenue rollups, and completed bill audit. |
| **Menu** | Add new fruit items, adjust pricing, toggle active status, manage store inventory. |

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
* Android Studio Ladybug | 2024.2.1 or newer
* JDK 17
* Android SDK (API 35, Min SDK 26)

### Build APK from Terminal
```bash
# Clone the repository
git clone https://github.com/anushkumar701/BIlling-App.git
cd BIlling-App

# Compile and generate debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Install Directly to Device via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 Releases

Download the latest pre-compiled Android APK directly from the **[GitHub Releases](https://github.com/anushkumar701/BIlling-App/releases)** page.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
