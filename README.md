# 🛍️ Retail Billing POS (v1.0 Production Release)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Version](https://img.shields.io/badge/Release-v1.0-blue.svg)](https://github.com/anushkumar701/BIlling-App/releases)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Offline First](https://img.shields.io/badge/Architecture-100%25%20Offline--First-orange.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

**Retail Billing POS** is a high-speed, offline-first Point-of-Sale (POS) and smart calculator-first billing application tailored for retail stores, supermarkets, grocery outlets, and busy fruit stalls. Engineered for peak rush-hour efficiency with zero lag, tactile haptic feedback, customizable shop receipts, native PDF invoice & report generation, and intuitive counter workflows.

## 📥 Official Downloads (v1.0 Production)

Download the ready-to-run applications for mobile and desktop:

| Platform | Format | Download Link | Notes |
| :--- | :--- | :--- | :--- |
| 📱 **Android Phone & Tablet** | `.apk` (2.0 MB) | [Download app-release.apk](https://github.com/anushkumar701/BIlling-App/releases/download/v1.0/app-release.apk) | Android 8.0+ (Phones, POS Terminals, Tablets) |
| 🪟 **Windows Desktop POS** | `.exe` (79 MB) | [Download FruitBilling-v1.0-Windows.exe](https://github.com/anushkumar701/BIlling-App/releases/download/v1.0/FruitBilling-v1.0-Windows.exe) | Windows 10/11 (Single-Click Portable Executable) |
| 🪟 **Windows Portable Package** | `.zip` (110 MB) | [Download FruitBilling-v1.0-Windows-Portable.zip](https://github.com/anushkumar701/BIlling-App/releases/download/v1.0/FruitBilling-v1.0-Windows-Portable.zip) | Extract & Run `Fruit & Grocery POS.exe` |

📦 **All GitHub Releases**: [Browse Releases](https://github.com/anushkumar701/BIlling-App/releases)


---

## ✨ Features Overview (v1.0 Production)

* 📊 **End-of-Day (Z-Report) Cash Drawer Reconciliation (New in v1.0):**
  * Reconcile physical drawer cash against recorded register collections at closing time.
  * Auto-computes **Shortage** or **Surplus** (Excess).
  * 1-Tap formatted Z-Report closing summary sharing via WhatsApp or SMS to store owner.

* 🏆 **Fast-Moving / Top-Selling Product Analytics (New in v1.0):**
  * Auto-identifies top 5 highest revenue and order-volume products.
  * Real-time visual progress bars directly inside the Business Summary tab.

* 🗑️ **30-Day Auto-Purge Recycle Bin (New in v1.0):**
  * Accidental bill deletion prevention: deleted bills are safely stored in the Recycle Bin for 30 days.
  * 1-tap restore to active sales history.
  * Automatic background purge after 30 days keeps device storage completely clean.

* 🧮 **Smart Calculator-First POS (Calc Tab):**
  * High-speed arithmetic entry optimized for counter cashiers.
  * Auto-gram & decimal conversion (e.g. `200 × 500g`, `80 × 1.5kg`).
  * Live ghost calculation preview before pressing `=`.
  * Ergonomic right-hand operator layout with rapid `00` button.
  * Clean **S (Compact • 42dp)**, **M (Standard • 50dp)**, and **L (Rush • 60dp)** keypad sizing presets.

* ⏳ **Pending Payments ("Pay Later" / Customer Tabs):**
  * Tag transactions as **Pending** when trusted regulars or credit customers pay later.
  * Attach optional customer names or table/stall numbers (`👤 Ramesh`, `Counter 2`).
  * Quick filter chip in History to view all unsettled dues at a glance.
  * 1-tap direct settlement (`Set Paid Cash` / `Set Paid UPI`) right from the bill card.
  * Credit dues are safely segregated from daily cash/UPI collected revenue.

* 💵 **Smart Cash Tender & Bargaining Suggestions:**
  * Exact amount chip displays actual money due (e.g. **₹760**) instead of generic text.
  * Instant rounded cash note suggestions (e.g. ₹760, ₹800, ₹1000, ₹2000) with automatic change return math.
  * **Smart Final Price Round-Off**: 1-tap round-down chips for quick bargaining (e.g. ₹1,304 ➔ **₹1,300**, **₹1,290**).

* 📄 **Native PDF Invoice & Sales Reports:**
  * Built-in Android `PdfDocument` engine — no third-party cloud required.
  * **Single Bill Invoices**: Professional A4 printable receipts with custom shop name, phone, item breakdown, and totals.
  * **Date-Range Sales Reports**: Multi-page sales summary reports complete with revenue totals, collected vs pending dues, and full transaction logs.
  * Instant integration with Android's system Print Spooler (thermal, Wi-Fi, cloud printers, or Save as PDF).

* 📱 **Professional WhatsApp Receipt Sharing:**
  * Dot-leader price alignment for crisp readability on mobile screens.
  * Customized shop name and contact details.

* 🔍 **Visual Catalog & Line Item Management (Billing Tab):**
  * Visual emoji product grid with quick search.
  * Preset quantity chips (`250g`, `500g`, `1kg`, `1.5kg`, `2kg`, `3kg`) or custom decimal entries.
  * Custom line items on the fly with custom names and rates.
  * Hold/Resume bill drafts without losing current cart state.

* 🗑️ **Recycle Bin for Deleted Bills (30-Day Auto-Purge & Restore):**
  * When a bill is removed from History, it moves to the **Recycle Bin** instead of permanent deletion.
  * Dedicated Recycle Bin viewer showing remaining days before auto-purge (`X days left`).
  * 1-tap **Restore** to return any deleted bill back into active sales history.
  * **Automatic 30-day purge**: Bills in the trash older than 30 days are automatically cleaned up in the background.
  * Option to **Empty Bin** or permanently delete specific bills on demand.

* 💬 **Login-Free In-App Survey & Feedback:**
  * Clean, respectful feedback dialog automatically suggested after 2 days of app usage.
  * Accessible anytime via **Menu ➔ App Survey & Feedback** (ideal when uninstalling or requesting features).
  * 100% login-free: rating selector (1-5 stars), quick sentiment tags, and optional suggestion box.

* 🔒 **100% Offline-First & Private:**
  * All transaction and product data stored securely on-device with SQLite/Room.
  * Optional Google Drive cloud backup and CSV export.

---

## 📱 App Navigation Structure

| Tab | Role & Cashier Workflow |
| :--- | :--- |
| **Calc** | Default landing screen: Smart calculator, weight shortcuts, adjustable keypad, live total preview. |
| **Billing** | Visual catalog search, weight presets, custom item entry, active receipt list. |
| **History** | Sales history, daily sales analytics, completed bill editing, receipt sharing, PDF/CSV export, pending bill settlement. |
| **Menu** | Shop Profile customization, product catalog management, cloud backup, OTA updates. |

---

## 🏗️ Architecture & Technology Stack

* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3).
* **Language:** 100% [Kotlin](https://kotlinlang.org) with Coroutines & StateFlow.
* **Architecture Pattern:** Clean MVVM with Unidirectional Data Flow (UDF).
* **Persistence:** [Room Database](https://developer.android.com/training/data-storage/room) with transactional migrations.
* **Financial Math:** Strict `java.math.BigDecimal` financial rounding (`HALF_UP` scale 2).
* **Document Engine:** Android Native `android.graphics.pdf.PdfDocument` & `android.print.PrintManager`.

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

# Compile optimized production release APK (R8 minified & resource shrunk)
./gradlew assembleRelease
```

### Install Directly to Device via ADB
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
