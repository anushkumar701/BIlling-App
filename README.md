# 🛍️ Retail Billing POS (v1.7.0)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Version](https://img.shields.io/badge/Release-v1.7.0-blue.svg)](https://github.com/anushkumar701/BIlling-App/releases)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Offline First](https://img.shields.io/badge/Architecture-100%25%20Offline--First-orange.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

**Retail Billing POS** is a high-speed, offline-first Point-of-Sale (POS) and smart calculator-first billing application tailored for retail stores, supermarkets, grocery outlets, and busy fruit stalls. Engineered for peak rush-hour efficiency with zero lag, tactile haptic feedback, customizable shop receipts, PDF print support, and intuitive counter workflows.

---

## ✨ What's New in v1.7.0

* ⏳ **Pending Payment ("Pay Later") Mode:**
  * Tag bills as **Pending** when trusted regulars or credit customers pay later.
  * Optional customer name / phone number field (`👤 Ramesh`, `Stall #4`).
  * Instant filter chips in History: view all pending orders and total credit balance at a glance.
  * Direct 1-tap payment settlement from History when the customer returns to pay.
* 💵 **Cash Tender Exact Amount Display:**
  * Replaced the ambiguous "Exact" button with the actual money amount (e.g. **₹760** instead of "Exact") so cashiers see the exact currency note needed immediately.
* 🎯 **Smart Final Price Round-Off Suggestions:**
  * Auto-generates rounded-down discount suggestion chips (e.g. ₹1304 ➔ **₹1300 (-₹4)**, **₹1290 (-₹14)**).
  * Cashiers can close bills with a single tap without manual mental math.
* 🔤 **Smart Expression Backspace Assist:**
  * Fixed backspace behavior when editing expressions with weight units (e.g. `500 × 700g`).
  * Pressing erase now deletes digits (`700` ➔ `70` ➔ `7`) instead of accidentally stripping the `g` suffix.
* 📄 **Native PDF Invoice & Report Export:**
  * Built-in Android `PdfDocument` engine — generates clean, professional A4 PDF invoices and multi-page sales summaries ready for instant thermal printing or sharing.
* 📏 **Clean S / M / L Keypad Presets:**
  * Removed confusing drag handle. Replaced with clean 1-tap **S (Compact)**, **M (Standard)**, and **L (Rush)** presets that persist across launches.
* 🧾 **Improved WhatsApp Receipt Alignment:**
  * Clean dot-leader price alignment (`Apple 1.5kg ......... ₹180.00`) and Unicode borders for crisp readability on mobile screens.

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
