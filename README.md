# 🛍️ Retail Billing POS (v1.6.0)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Version](https://img.shields.io/badge/Release-v1.6.0-blue.svg)](https://github.com/anushkumar701/BIlling-App/releases)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Offline First](https://img.shields.io/badge/Architecture-100%25%20Offline--First-orange.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

**Retail Billing POS** is a high-speed, offline-first Point-of-Sale (POS) and smart calculator-first billing application tailored for retail stores, supermarkets, grocery outlets, and busy fruit stalls. Engineered for peak rush-hour efficiency with zero lag, tactile haptic feedback, customizable shop receipts, and intuitive counter workflows.

---

## ✨ What's New in v1.6.0

* 🏬 **Retail Billing POS Rebranding:** Modern, generalized retail point-of-sale branding suitable for all retail counters, grocery stores, and fruit markets.
* 🏪 **Customizable Shop Profile & WhatsApp Invoicing:**
  * Configure your **Shop Name** and **Contact / UPI phone number** in Menu settings.
  * Generates clean, professional WhatsApp & SMS receipts with clean Unicode borders, itemized quantities, rates, discounts, and bold grand totals.
* ⚡ **Rush-Hour Optimized Keypad:**
  * **Unified Right-Hand Thumb Operator Column:** `[⌫]` ➔ `[×]` ➔ `[+]` ➔ `[=]` for blisteringly fast one-handed cashier operations.
  * **New `00` Button:** 1-tap entry of common currency amounts (`100`, `200`, `500`, `1000`).
  * **Long-Press Clear All (`C`):** Hold the backspace key to clear entire expressions instantly with haptic confirmation.
  * **Removed Minus (`−`):** Eliminated unused operator space to maximize button size and thumb ergonomics.
* 📐 **Adjustable Calculator Screen:**
  * Cashiers can dynamically adjust the keypad height between **38dp and 65dp** using a smooth vertical drag handle.
  * 1-Tap quick presets: **S (Compact - 42dp)** (maximizes visible receipt lines), **M (Standard - 50dp)**, and **L (Rush - 60dp)** (extra-large buttons to eliminate fat-finger errors during peak crowds).
  * Remembers sizing preferences automatically across sessions.
* ✏️ **Edit Completed Bills in Sales History:**
  * Re-open any completed bill to add/remove items, adjust quantities/rates, update final prices, or switch payment methods.
  * Updates in-place while strictly preserving the original bill number (e.g. `#105` remains `#105`).
* 🔄 **Daily Resetting Bill Numbers:**
  * Automatically starts at `#001` each morning. Bill numbers never repeat or get skipped even if bills are cancelled or removed.
* 🚀 **Silent Background OTA Updates:**
  * Automatically pre-caches update APKs in the background when connected.
  * Prompts with an instant 1-tap install dialog without making the user wait for download completion.

---

## 📸 Key Features & Workflow

### 🧮 1. Smart Calculator-First Billing (Calc Tab)
* **High-Speed Input Pad:** Built for high-volume transactions where speed is critical.
* **Auto-Gram & Decimal Assist:** Automatically recognizes weight quantities (e.g. `200 × 500g` or `80 × 1.5kg`) and calculates the exact subtotal.
* **Live Calculation Ghost Preview:** Displays the evaluated result before pressing `=`, eliminating calculation errors during customer interaction.
* **Quick Quantity Shortcuts:** Preset 1-tap buttons for rapid weights (`100g`, `250g`, `500g`, `1kg`, etc.).
* **Ergonomic Safety:** Dedicated bottom strip with double-tap protection, preventing accidental bill finalizations.

### 🔍 2. Product Search & Catalog Billing (Billing Tab)
* **Visual Catalog Grid:** Localized emoji badges and quick unit indicators (kg, pcs).
* **Instant Quantity Modal:** Quick presets (`250g`, `500g`, `1kg`, `1.5kg`, `2kg`, `3kg`) or custom decimal entries with instant subtotal calculation.
* **Custom Line Items:** Bill loose or non-catalog items on the fly with a custom name and rate.

### 📋 3. Multi-Draft & Bill Management
* **Active & Held Bills:** Put a transaction on **Hold** when a shopper steps aside to pick another item, serve the next customer, and resume with one tap.
* **State Preservation:** Multiple drafts persist across app restarts and device rotations.
* **Tap-to-Edit & Delete:** Edit calculation expressions or remove lines with immediate total recalculation and Snackbar **Undo** protection.

### 💳 4. Flexible Settlement & Custom Receipts
* **Payment Mode Tracking:** Tag orders as **Cash** or **UPI** with single-tap toggle chips.
* **Smart Cash Tender Suggestions:** Dynamic cash note suggestions (e.g. Exact, ₹100, ₹200, ₹500, ₹2000) based on bill total, with automatic change return calculation.
* **WhatsApp / SMS Receipt Sharing:**
  ```text
  🧾 *RETAIL INVOICE*
  🏪 *Sri Ganesh Retail Stores*
  📞 Contact: +91 98765 43210
  ──────────────────────────────
  Bill No: #001
  Date: 23 Sep 2026, 09:40 AM
  ──────────────────────────────
  *ITEMS:*
  1. Royal Gala Apple
     1.5 kg × ₹120.00 = ₹180.00
  2. Fresh Milk (1L)
     2 pcs × ₹35.00 = ₹70.00
  ──────────────────────────────
  Total Items: 2
  Original Total: ₹250.00
  Discount: -₹10.00
  *GRAND TOTAL: ₹240.00*
  Payment Mode: 💵 Cash
  Cash Returned: ₹60.00
  ──────────────────────────────
  Thank you! Please visit again! 🙏
  ```

### 📊 5. Audit History & Product Management
* **History Tab:** Complete chronological log of finalized bills, daily sales analytics, and itemized receipt modal.
* **Menu Tab:** Full product catalog management—Add, Edit rates, Deactivate (pause without breaking historical bills), or Delete products.
* **Cloud Sync:** Serverless Firestore synchronization for automatic cloud backup.

---

## 🏗️ Architecture & Technology Stack

* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3).
* **Language:** 100% [Kotlin](https://kotlinlang.org) with Coroutines & StateFlow.
* **Architecture Pattern:** Clean MVVM with Unidirectional Data Flow (UDF).
* **Persistence:** [Room Database](https://developer.android.com/training/data-storage/room) with transactional migrations, indexing, and offline-first queries.
* **State Management:** Shared `BillingViewModel` synchronized across Billing and Calc tabs.
* **Financial Math:** Strict `java.math.BigDecimal` financial rounding (`HALF_UP` scale 2).

---

## 📱 App Navigation Structure

| Tab | Role & Cashier Workflow |
| :--- | :--- |
| **Calc** | Default landing screen: Smart calculator, weight shortcuts, adjustable keypad, live total preview. |
| **Billing** | Visual catalog search, weight presets, custom item entry, active receipt list. |
| **History** | Sales history, daily sales analytics, completed bill editing, receipt sharing, wrong bill deletion. |
| **Menu** | Shop Profile customization, product management, cloud backup, update checker. |

---

## 🚀 Build Instructions

### Prerequisites
* Android Studio Ladybug | 2024.2.1 or newer
* JDK 17
* Android SDK (API 35, Min SDK 26)

### Build APK from Terminal
```bash
# Clone the repository
git clone https://github.com/anushkumar701/BIlling-App.git
cd BIlling-App

# Compile debug APK
./gradlew assembleDebug

# Compile optimized release APK (R8 minified & resource shrunk)
./gradlew assembleRelease
```

### Install Directly to Device via ADB
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📦 Releases

Download the latest pre-compiled Android APK directly from the **[GitHub Releases](https://github.com/anushkumar701/BIlling-App/releases)** page.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
