<div align="center">
  
<img src="http://avinas.me/wp-content/uploads/2025/11/BillMe-1.png" alt="BillMe icon" width="100%"/>

# 📱 BillMe

### Modern Mobile Shop Management System

[![Android](https://img.shields.io/badge/Android-14+-3DDC84.svg?logo=android&logoColor=white)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-BUSL%201.1-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-2.0.0-green.svg)](CHANGELOG.md)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![GitHub Stars](https://img.shields.io/github/stars/avinaxhroy/BillMe?style=social)](https://github.com/avinaxhroy/BillMe)

<p align="center">
  <strong>🚀Enterprise-grade Mobile Shop Management app for Android with GST compliance, IMEI-aware inventory, ML Kit OCR, thermal printing, and professional invoice generation that Simply Works</strong>
</p>

<p align="center">
  <a href="#-features">Features</a> •
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-screenshots">Screenshots</a> •
  <a href="#-tech-stack">Tech Stack</a> •
  <a href="#-installation">Installation</a> •
  <a href="#-documentation">Documentation</a> •
  <a href="#-contributing">Contributing</a> •
  <a href="#-license">License</a>
</p>

<img src="http://avinas.me/wp-content/uploads/2025/11/BillME.png" alt="BillMe Banner" width="100%"/>

</div>

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 💼 Business Features

- 💳 **Quick Billing System**  
  Fast cart-based invoice creation with IMEI tracking
  
- 📦 **IMEI-Aware Inventory**  
  Individual device tracking with duplicate detection
  
- 🔐 **Advanced IMEI Scanner**  
  ML Kit-powered camera scanner (Single/Dual/Bulk modes)
  
- 📱 **Offline First**  
  Works completely without internet connection
  
- 🖨️ **Thermal Printing**  
  ESC/POS Bluetooth thermal printer support
  
- 📊 **Sales Dashboard**  
  Real-time analytics with profit tracking
  
- 🔄 **Auto-Save & Backup**  
  Daily automated backups with restore capability
  
- 📈 **GST Compliance**  
  4 GST modes with complete tax calculations
  
- � **Professional Invoices**  
  PDF generation with bilingual support
  
- 🤖 **OCR Invoice Scanning**  
  Extract data from supplier invoices automatically

</td>
<td width="50%">

### 🛠️ Technical Features

- ✅ **Clean Architecture**  
  Layered separation with MVVM pattern
  
- ✅ **Jetpack Compose**  
  Modern declarative Material 3 UI
  
- ✅ **Room Database**  
  15 entities with comprehensive relationships
  
- ✅ **Hilt DI**  
  Complete dependency injection setup
  
- ✅ **Coroutines & Flow**  
  Reactive async programming with Kotlin
  
- ✅ **CameraX + ML Kit**  
  Text recognition and barcode scanning
  
- ✅ **iText7 PDF Generation**  
  Professional invoice PDFs with signatures
  
- ✅ **WorkManager Integration**  
  Scheduled backups and background tasks
  
- ✅ **DataStore Preferences**  
  Type-safe settings persistence
  
- ✅ **Comprehensive Tests**  
  Unit tests for core business logic

</td>
</tr>
</table>

### 🎯 Key Capabilities

<details>
<summary><b>📋 GST & Tax Management</b></summary>

- **4 GST Modes:**
  - `FULL_GST`: Complete tax invoice with GST breakdown
  - `PARTIAL_GST`: Simplified GST display for registered businesses
  - `GST_REFERENCE`: Shows GSTIN only, no calculations
  - `NO_GST`: For non-GST businesses
  
- **Tax Features:**
  - Intrastate (CGST + SGST) and Interstate (IGST) support
  - HSN/SAC code management
  - Multi-tier GST rates (0%, 5%, 12%, 18%, 28%)
  - Cess calculation
  - State-wise GST configuration
  - GST reports and return filing data

</details>

<details>
<summary><b>📱 IMEI & Product Management</b></summary>

- **Individual IMEI Tracking:**
  - Unique IMEI per device unit
  - Dual IMEI support (IMEI1 + IMEI2)
  - Serial number tracking
  - Warranty management with dates
  - Purchase price history per unit
  
- **Smart IMEI Scanner:**
  - Camera-based scanning with ML Kit
  - Auto-detection of single/dual IMEI
  - Bulk scanning mode for multiple devices
  - Real-time duplicate checking
  - Luhn algorithm validation
  - Manual entry fallback

- **Product Features:**
  - Brand, model, variant, color tracking
  - Stock status management
  - Multiple suppliers
  - Barcode support
  - Cost price history
  - Stock adjustments with reasons

</details>

<details>
<summary><b>🧾 Invoice & Billing</b></summary>

- **Professional Invoices:**
  - PDF generation with iText7
  - Customer and Owner copies
  - Bilingual support (English/Hindi)
  - Digital signature embedding
  - GST breakdown display
  - Terms and conditions
  
- **Invoice Types:**
  - Regular sales invoice
  - Proforma invoice
  - Credit note
  - Debit note
  - Export invoice
  - SEZ invoice

- **Billing Features:**
  - Quick cart-based billing
  - IMEI-based product addition
  - Discount application (% or fixed)
  - Multiple payment methods
  - Draft transactions
  - Transaction history

</details>

<details>
<summary><b>🤖 OCR & Automation</b></summary>

- **Invoice Scanning:**
  - Scan supplier invoices with camera
  - Extract product details automatically
  - IMEI detection from bills
  - Brand, model, price extraction
  - Smart field validation
  
- **OCR Features:**
  - ML Kit Text Recognition
  - Image quality assessment
  - Intelligent text filtering
  - Template matching
  - Manual correction interface
  - Multi-page support

</details>

<details>
<summary><b>💾 Backup & Data Management</b></summary>

- **Backup System:**
  - Daily automated backups
  - Full and incremental modes
  - ZIP compression
  - Database + PDFs + Signatures
  - Metadata preservation
  - One-click restore
  
- **Data Export:**
  - CSV export for invoices
  - JSON data export
  - Invoice PDF bulk export
  - Database statistics

</details>

<details>
<summary><b>📊 Analytics & Reports</b></summary>

- **Business Metrics:**
  - Daily/Weekly/Monthly sales
  - Profit margins and tracking
  - Top-selling products
  - Customer lifetime value (LTV)
  - Customer segmentation
  - Retention rate analysis
  
- **Reports:**
  - GST collection reports
  - State-wise GST analytics
  - Revenue trends
  - Inventory valuation
  - Low stock alerts
  - Transaction history

</details>

<details>
<summary><b>👥 Customer Management</b></summary>

- **Customer Features:**
  - Profile with GST details
  - Purchase history tracking
  - Segmentation (VIP, REGULAR, OCCASIONAL, NEW)
  - Loyalty points system
  - Lifetime value calculations
  - Top customers identification
  
</details>

---

## 📸 Screenshots

<div align="center">

| Dashboard | Billing | Inventory | Analytics |
|-----------|---------|-----------|-----------|
| <img src="https://via.placeholder.com/200x400/2196F3/FFFFFF?text=Dashboard" width="200"/> | <img src="https://via.placeholder.com/200x400/4CAF50/FFFFFF?text=Billing" width="200"/> | <img src="https://via.placeholder.com/200x400/FF9800/FFFFFF?text=Inventory" width="200"/> | <img src="https://via.placeholder.com/200x400/9C27B0/FFFFFF?text=Analytics" width="200"/> |

*Replace these placeholders with actual app screenshots*

</div>

---

## 🚀 Quick Start

<details open>
<summary><b>Get started in 3 steps</b></summary>

### 1️⃣ Clone the Repository
```bash
git clone https://github.com/avinaxhroy/BillMe.git
cd BillMe
```

### 2️⃣ Open in Android Studio
- Launch Android Studio
- Select `File → Open`
- Navigate to the `BillMe` folder
- Wait for Gradle sync

### 3️⃣ Build and Run
```bash
# Build the project
./gradlew build

# Run on connected device or emulator
./gradlew installDebug
```

</details>

> 📚 For detailed setup instructions, see [SETUP.md](SETUP.md)

---

## 🛠️ Tech Stack

<div align="center">

| Category | Technology | Version |
|----------|------------|---------|
| **Language** | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white) | 2.0.21 |
| **UI Framework** | ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white) | BOM 2024.12.01 |
| **Architecture** | ![MVVM](https://img.shields.io/badge/MVVM-Clean%20Architecture-green) | - |
| **Database** | ![Room](https://img.shields.io/badge/Room-SQLite-blue) | 2.6.1 |
| **DI Container** | ![Hilt](https://img.shields.io/badge/Hilt-Dagger-orange) | 2.51.1 |
| **Async** | ![Coroutines](https://img.shields.io/badge/Coroutines%20%26%20Flow-Kotlin-purple) | 1.8.1 |
| **Camera** | ![CameraX](https://img.shields.io/badge/CameraX-Jetpack-blue) | 1.4.0 |
| **ML/OCR** | ![ML Kit](https://img.shields.io/badge/ML%20Kit-Text%20Recognition-red) | 16.0.0 |
| **PDF** | ![iText7](https://img.shields.io/badge/iText7-PDF%20Generation-green) | 8.0.2 |
| **Printing** | ![ESC/POS](https://img.shields.io/badge/ESC%2FPOS-Thermal%20Printer-orange) | 3.3.0 |
| **Testing** | ![JUnit](https://img.shields.io/badge/JUnit%204%20%7C%20Mockito-Testing-red) | 4.13.2 |
| **Build** | ![Gradle](https://img.shields.io/badge/Gradle%208.13-02303A?logo=gradle) | 8.13.0 |
| **Min SDK** | ![API 24+](https://img.shields.io/badge/API-24%2B%20(Android%207.0)-brightgreen) | 24 |
| **Target SDK** | ![API 35](https://img.shields.io/badge/API-35%20(Android%2015)-blue) | 35 |

</div>

### 📦 Key Dependencies

<details>
<summary><b>Core Libraries</b></summary>

```kotlin
// Jetpack Compose & Material 3
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.8.2")
implementation("androidx.navigation:navigation-compose:2.7.6")

// Lifecycle & ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
```

</details>

<details>
<summary><b>Database & Persistence</b></summary>

```kotlin
// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.0")

// Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
```

</details>

<details>
<summary><b>Dependency Injection</b></summary>

```kotlin
// Hilt
implementation("com.google.dagger:hilt-android:2.51.1")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
implementation("androidx.hilt:hilt-work:1.1.0")
ksp("com.google.dagger:hilt-compiler:2.51.1")
```

</details>

<details>
<summary><b>Camera & Machine Learning</b></summary>

```kotlin
// CameraX
implementation("androidx.camera:camera-core:1.4.0")
implementation("androidx.camera:camera-camera2:1.4.0")
implementation("androidx.camera:camera-lifecycle:1.4.0")
implementation("androidx.camera:camera-view:1.4.0")

// ML Kit
implementation("com.google.mlkit:text-recognition:16.0.0")
implementation("com.google.mlkit:barcode-scanning:17.3.0")
```

</details>

<details>
<summary><b>PDF & Printing</b></summary>

```kotlin
// PDF Generation
implementation("com.itextpdf:itext7-core:8.0.2")

// Thermal Printing
implementation("com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.3.0")
```

</details>

<details>
<summary><b>Background & Security</b></summary>

```kotlin
// WorkManager
implementation("androidx.work:work-runtime-ktx:2.8.1")

// Security & Encryption
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// Permissions
implementation("com.google.accompanist:accompanist-permissions:0.32.0")
```

</details>

<details>
<summary><b>Other</b></summary>

```kotlin
// Image Loading
implementation("io.coil-kt:coil-compose:2.5.0")

// Google Play Services
implementation("com.google.android.gms:play-services-auth:21.0.0")

// Testing
testImplementation("junit:junit:4.13.2")
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
```

</details>

---

## 📦 Installation

### Prerequisites

<details>
<summary><b>Required Software</b></summary>

- ✅ **Android Studio** - [Download](https://developer.android.com/studio) (Latest stable version)
- ✅ **JDK 11+** - [Download](https://adoptium.net/) or use Homebrew
- ✅ **Android SDK 34** - Install via Android Studio SDK Manager
- ✅ **Git** - Version control system

</details>

### Step-by-Step Installation

```bash
# 1. Clone the repository
git clone https://github.com/avinaxhroy/BillMe.git
cd BillMe

# 2. Verify Gradle wrapper
chmod +x gradlew

# 3. Build the project
./gradlew clean build

# 4. Run tests
./gradlew test

# 5. Install on device/emulator
./gradlew installDebug
```

For platform-specific setup (macOS, Linux, Windows), see [SETUP.md](SETUP.md)

---

## 📂 Project Structure

```
BillMe/
├── 📱 app/                          # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/billme/app/
│   │   │   │   ├── 🧠 core/        # Core business logic & utilities
│   │   │   │   │   ├── automation/     # Smart pricing, IMEI helpers
│   │   │   │   │   ├── backup/         # Enhanced backup system (ZIP, scheduling)
│   │   │   │   │   ├── builder/        # Invoice builder pattern
│   │   │   │   │   ├── camera/         # CameraX IMEI scanner
│   │   │   │   │   ├── database/       # DB initialization, health monitoring
│   │   │   │   │   ├── generator/      # Code generators, receipt builders
│   │   │   │   │   ├── ocr/            # ML Kit OCR engine, text processing
│   │   │   │   │   ├── pdf/            # iText7 PDF generation
│   │   │   │   │   ├── performance/    # Performance monitoring
│   │   │   │   │   ├── printing/       # ESC/POS thermal printing
│   │   │   │   │   ├── scanner/        # Unified IMEI scanner (3 modes)
│   │   │   │   │   ├── security/       # Encryption, signature verification
│   │   │   │   │   ├── service/        # Business services (Analytics, Billing, GST)
│   │   │   │   │   ├── sharing/        # File sharing, PDF operations
│   │   │   │   │   └── util/           # Utilities (IMEI validator, DateTimeUtils)
│   │   │   │   ├── 📊 data/        # Data layer
│   │   │   │   │   ├── datastore/      # DataStore preferences
│   │   │   │   │   ├── local/          # Room database (15 entities)
│   │   │   │   │   │   ├── dao/        # Data Access Objects
│   │   │   │   │   │   ├── entity/     # Database entities
│   │   │   │   │   │   ├── converter/  # Type converters
│   │   │   │   │   │   ├── BillMeDatabase.kt
│   │   │   │   │   │   └── DatabaseManager.kt
│   │   │   │   │   ├── model/          # Data models (GST, OCR, Reports, etc.)
│   │   │   │   │   └── repository/     # Repositories (Product, Invoice, GST, etc.)
│   │   │   │   ├── 💉 di/          # Dependency injection (Hilt modules)
│   │   │   │   ├── 🔌 hardware/    # Hardware abstractions
│   │   │   │   │   ├── BarcodeScanner.kt    # Barcode scanner interface
│   │   │   │   │   ├── ThermalPrinter.kt    # Thermal printer interface
│   │   │   │   │   └── MockHardwareImpl.kt  # Mock implementations
│   │   │   │   ├── 🔧 service/     # Background services
│   │   │   │   │   ├── AutoSaveService.kt   # Draft auto-save
│   │   │   │   │   └── ReceiptService.kt    # Receipt generation
│   │   │   │   └── 🎨 ui/          # UI layer (Jetpack Compose)
│   │   │   │       ├── component/      # Reusable components (Dialogs, Cards)
│   │   │   │       ├── navigation/     # Navigation graph
│   │   │   │       ├── screen/         # Feature screens
│   │   │   │       │   ├── addproduct/     # Product management
│   │   │   │       │   ├── addpurchase/    # Purchase entry
│   │   │   │       │   ├── backup/         # Backup & restore UI
│   │   │   │       │   ├── billing/        # Enhanced billing screen
│   │   │   │       │   ├── dashboard/      # Analytics dashboard
│   │   │   │       │   ├── database/       # Database management
│   │   │   │       │   ├── inventory/      # Inventory management
│   │   │   │       │   ├── invoicehistory/ # Invoice history
│   │   │   │       │   ├── quickbill/      # Quick billing
│   │   │   │       │   ├── reports/        # Business reports
│   │   │   │       │   ├── settings/       # App settings
│   │   │   │       │   └── setup/          # Setup wizard
│   │   │   │       ├── theme/          # Material 3 theme
│   │   │   │       ├── util/           # UI utilities
│   │   │   │       └── viewmodel/      # ViewModels
│   │   │   ├── res/                    # Android resources
│   │   │   │   ├── drawable/           # Vector drawables
│   │   │   │   ├── mipmap-*/           # App icons
│   │   │   │   ├── values/             # Colors, strings, themes
│   │   │   │   └── xml/                # Backup rules, file paths
│   │   │   └── AndroidManifest.xml     # App manifest
│   │   └── test/                       # Unit tests
│   │       └── java/                   # Test files
│   ├── build.gradle.kts                # App build configuration
│   ├── proguard-rules.pro              # ProGuard configuration
│   └── lint.xml                        # Lint configuration
├── 📁 android/                         # Android launcher resources
│   ├── mipmap-*/                       # Launcher icons (all densities)
│   └── values/
│       └── ic_launcher_background.xml
├── 🔧 .github/                         # GitHub configuration
│   ├── workflows/                      # CI/CD workflows
│   │   └── android.yml                 # Android build workflow
│   ├── ISSUE_TEMPLATE/                 # Issue templates
│   ├── PULL_REQUEST_TEMPLATE.md        # PR template
│   └── FUNDING.yml                     # Sponsorship info
├── 📄 Documentation
│   ├── README.md                       # Project overview (this file)
│   ├── SETUP.md                        # Complete setup guide
│   ├── CONTRIBUTING.md                 # Contribution guidelines
│   ├── CODE_OF_CONDUCT.md              # Community standards
│   ├── SECURITY.md                     # Security policy
│   ├── CHANGELOG.md                    # Version history
│   ├── QUICK_REFERENCE.md              # Quick reference guide
│   ├── START_HERE.md                   # Getting started guide
│   └── LICENSE                         # BUSL-1.1 License
└── 🏗️ Build Configuration
    ├── build.gradle.kts                # Root build script
    ├── settings.gradle.kts             # Gradle settings
    ├── gradle.properties               # Gradle properties
    ├── gradlew                         # Gradle wrapper (Unix)
    └── gradlew.bat                     # Gradle wrapper (Windows)
```

### 🗂️ Key Directories Explained

<details>
<summary><b>core/ - Business Logic & Utilities</b></summary>

- **automation/**: Smart pricing algorithms, IMEI helper utilities
- **backup/**: Complete backup system with ZIP compression, scheduling
- **builder/**: Builder pattern for complex objects (Invoice builder)
- **camera/**: CameraX integration for IMEI scanning
- **database/**: Database initialization, health monitoring, seeding
- **generator/**: Code generators, receipt content builders
- **ocr/**: ML Kit OCR engine, text processing pipeline, field extraction
- **pdf/**: iText7 PDF generation with signatures and bilingual support
- **performance/**: Performance monitoring and optimization
- **printing/**: ESC/POS thermal printing implementation
- **scanner/**: Unified IMEI scanner with 3 modes (Single/Dual/Bulk)
- **security/**: Encryption, signature management, secure storage
- **service/**: Core business services (Analytics, Billing, GST calculation, IMEI management)
- **sharing/**: File sharing, PDF operations, file management
- **util/**: Common utilities (IMEI validator, date/time helpers, formatters)

</details>

<details>
<summary><b>data/ - Data Layer</b></summary>

- **datastore/**: Type-safe preferences with DataStore
- **local/**: Room database with 15 entities, DAOs, and type converters
  - **dao/**: Data access objects for all entities
  - **entity/**: Database entities (Product, Invoice, Customer, GST, etc.)
  - **converter/**: Type converters for BigDecimal, Instant, Enums
- **model/**: Data models for business logic (GST, OCR, Reports, Dashboard)
- **repository/**: Repositories for data operations (Product, Invoice, GST, Customer, Analytics)

</details>

<details>
<summary><b>ui/ - User Interface</b></summary>

- **component/**: Reusable Composable components (Dialogs, Cards, Buttons)
- **navigation/**: Navigation graph and routes
- **screen/**: Feature screens organized by functionality
  - Complete screens for billing, inventory, reports, settings, etc.
- **theme/**: Material 3 theme configuration (colors, typography, shapes)
- **viewmodel/**: ViewModels with StateFlow for state management

</details>

---

## ⚙️ Configuration

### Default Settings

The app comes pre-configured with sensible defaults:

```kotlin
// Shop Configuration
shopName = "Mobile Shop Pro"
gsttaxEnabled = true
gsttaxRate = 18.0
currency = "INR" // ₹

// Auto-Backup
backupEnabled = true
backupFrequency = "DAILY"

// Receipt Printing
paperWidth = 58  // mm
printQuality = "HIGH"
```

### Customization

Edit `app/src/main/res/values/strings.xml` to customize:
- App name
- Default shop name
- Currency symbol
- Tax rates

---

## 🧪 Testing

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Suite
```bash
# Unit tests only
./gradlew testDebugUnitTest

# Specific test class
./gradlew test --tests com.billme.app.core.util.ImeiValidatorTest
```

### Generate Coverage Report
```bash
./gradlew testDebugUnitTest
# Report: app/build/reports/tests/testDebugUnitTest/index.html
```

### Current Test Coverage
- ✅ IMEI Validation: 100%
- ✅ Luhn Algorithm: Comprehensive test cases
- ✅ Edge cases and error conditions

---

## 🚀 Development

### Build Variants

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Clean build
./gradlew clean build
```

### Code Style

This project follows [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- ✅ Use meaningful variable/function names
- ✅ Keep functions small and focused
- ✅ Add KDoc for public APIs
- ✅ Use Kotlin idioms
- ✅ Prefer immutability

### Git Workflow

```bash
# Create feature branch
git checkout -b feat/your-feature

# Make changes and commit
git add .
git commit -m "feat: add new feature"

# Push and create PR
git push origin feat/your-feature
```

---

## 📚 Documentation

### Core Documentation
- 📖 **[README.md](README.md)** - Project overview (you are here!)
- 🔧 **[SETUP.md](SETUP.md)** - Complete setup guide
- 🤝 **[CONTRIBUTING.md](CONTRIBUTING.md)** - How to contribute
- 👥 **[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)** - Community standards
- 🔒 **[SECURITY.md](SECURITY.md)** - Security policy
- ⚖️ **[LICENSE](LICENSE)** - BUSL-1.1 License terms

### External Resources
- [Android Developer Guide](https://developer.android.com/guide)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://dagger.dev/hilt)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Read** [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines
2. **Check** [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community standards
3. **Fork** the repository
4. **Create** a feature branch: `git checkout -b feat/amazing-feature`
5. **Commit** your changes: `git commit -m 'feat: add amazing feature'`
6. **Push** to the branch: `git push origin feat/amazing-feature`
7. **Open** a Pull Request

### Contribution Guidelines

<details>
<summary><b>What We're Looking For</b></summary>

- 🐛 Bug fixes
- ✨ New features
- 📝 Documentation improvements
- 🧪 Test coverage enhancements
- 🎨 UI/UX improvements
- ♿ Accessibility enhancements
- 🌍 Localization/translations

</details>

### Good First Issues

Looking for a place to start? Check out issues labeled [`good first issue`](https://github.com/avinaxhroy/BillMe/labels/good%20first%20issue).

---

## 🔒 License

This project is licensed under the **Business Source License 1.1** (BUSL-1.1).

<details>
<summary><b>What does this mean?</b></summary>

### ✅ You Can
- **Contribute** improvements and modifications
- **Fork** and create derivative works (non-production)
- **Study** and learn from the code
- **Modify** the code for personal/educational use

### ❌ You Cannot
- **Use** for commercial/production purposes (without license)
- **Rebrand** or repackage as your own work
- **Remove** license and copyright notices
- **Claim** ownership of the original code

### 📅 License Change
This project will automatically convert to the **MIT License** on **November 4, 2028**.

</details>

For more details, see [LICENSE](LICENSE).

---

## 📞 Support

Need help? Here's how to get support:

### 📖 Documentation
- Check [SETUP.md](SETUP.md) for setup issues
- Read [CONTRIBUTING.md](CONTRIBUTING.md) for contribution questions
- Review [SECURITY.md](SECURITY.md) for security concerns

### 💬 Community
- 🐛 **Bug Reports**: [Create an issue](https://github.com/avinaxhroy/BillMe/issues/new?template=bug_report.md)
- 💡 **Feature Requests**: [Open a discussion](https://github.com/avinaxhroy/BillMe/discussions/new)
- 🔒 **Security Issues**: See [SECURITY.md](SECURITY.md)
- 💬 **General Questions**: [GitHub Discussions](https://github.com/avinaxhroy/BillMe/discussions)

### 📊 Project Stats

![GitHub Issues](https://img.shields.io/github/issues/avinaxhroy/BillMe)
![GitHub Pull Requests](https://img.shields.io/github/issues-pr/avinaxhroy/BillMe)
![GitHub last commit](https://img.shields.io/github/last-commit/avinaxhroy/BillMe)
![GitHub repo size](https://img.shields.io/github/repo-size/avinaxhroy/BillMe)

---

## 🗺️ Roadmap

### Phase 1 ✅ (Completed - MVP)
- [x] Room database with 5 core entities
- [x] IMEI validation with Luhn algorithm
- [x] Material 3 UI with Jetpack Compose
- [x] Basic transaction management
- [x] Dashboard with analytics
- [x] Unit tests

### Phase 2 ✅ (Completed - Enterprise Features)
- [x] Complete GST system (4 modes)
- [x] Professional invoice generation (PDF)
- [x] ML Kit OCR for invoice scanning
- [x] CameraX IMEI scanner (3 modes)
- [x] Thermal printer support (ESC/POS)
- [x] Enhanced backup & restore system
- [x] Customer lifetime value analytics
- [x] Database expansion (15 entities)
- [x] Digital signature support
- [x] Bilingual invoice support

### Phase 3 � (In Progress - Q1 2025)
- [ ] Google Drive cloud backup integration
- [ ] Multi-language support (Hindi, Tamil, Bengali)
- [ ] Dark mode enhancements
- [ ] Bulk import/export (CSV, Excel)
- [ ] SMS notifications for customers
- [ ] WhatsApp Business integration
- [ ] Advanced analytics dashboard
- [ ] Predictive inventory management

### Phase 4 🔮 (Planned - Q2-Q3 2025)
- [ ] Multi-store management
- [ ] Web dashboard portal
- [ ] REST API for third-party integrations
- [ ] Tablet-optimized interface
- [ ] Advanced ML-based insights
- [ ] Receipt customization templates
- [ ] Email invoice delivery
- [ ] Payment gateway integration
- [ ] Barcode label printing
- [ ] Supplier portal

### Future Ideas 💭 (Beyond 2025)
- [ ] Wear OS companion app
- [ ] Widget support for quick actions
- [ ] Voice commands (Google Assistant)
- [ ] AI-powered pricing suggestions
- [ ] Augmented reality product preview
- [ ] Blockchain-based warranty tracking
- [ ] IoT device integration
- [ ] Advanced fraud detection
- [ ] Cryptocurrency payment support

---

## 📊 Current Status

| Feature | Status | Version |
|---------|--------|---------|
| GST Compliance | ✅ Complete | 2.0.0 |
| Invoice Generation | ✅ Complete | 2.0.0 |
| OCR Scanning | ✅ Complete | 2.0.0 |
| IMEI Scanner | ✅ Complete | 2.0.0 |
| Thermal Printing | ✅ Complete | 2.0.0 |
| Backup & Restore | ✅ Complete | 2.0.0 |
| Customer Analytics | ✅ Complete | 2.0.0 |
| Cloud Backup | 🚧 In Progress | 2.1.0 |
| Multi-language | 📅 Planned | 2.1.0 |
| Multi-store | 📅 Planned | 3.0.0 |

---

## 🏆 Contributors

Thanks to all contributors who have helped make BillMe better!

<a href="https://github.com/avinaxhroy/BillMe/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=avinaxhroy/BillMe" />
</a>

---

## 💖 Sponsors

Support this project by becoming a sponsor. Your logo will show up here with a link to your website.

[![Sponsor](https://img.shields.io/badge/Sponsor-❤️-red?style=for-the-badge)](https://github.com/sponsors/avinaxhroy)

---

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=avinaxhroy/BillMe&type=Date)](https://star-history.com/#avinaxhroy/BillMe&Date)

---

## 📜 Changelog

See [CHANGELOG.md](CHANGELOG.md) for a list of changes in each version.

---

## 🙏 Acknowledgments

- Material Design team for the beautiful design system
- JetBrains for Kotlin
- Google for Android and Jetpack libraries
- All open-source contributors

---

<div align="center">

### Made with ❤️ by [@avinaxhroy](https://github.com/avinaxhroy)

**If you find this project useful, please consider giving it a ⭐!**

[![GitHub Stars](https://img.shields.io/github/stars/avinaxhroy/BillMe?style=social)](https://github.com/avinaxhroy/BillMe)
[![GitHub Forks](https://img.shields.io/github/forks/avinaxhroy/BillMe?style=social)](https://github.com/avinaxhroy/BillMe/fork)
[![GitHub Watchers](https://img.shields.io/github/watchers/avinaxhroy/BillMe?style=social)](https://github.com/avinaxhroy/BillMe)

---

**📌 Repository**: [github.com/avinaxhroy/BillMe](https://github.com/avinaxhroy/BillMe)  
**📧 Contact**: [Open an issue](https://github.com/avinaxhroy/BillMe/issues/new)  
**📅 Last Updated**: November 2025

</div>
