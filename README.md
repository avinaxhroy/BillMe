# BillMe — Mobile Shop Android App

[![Android](https://img.shields.io/badge/Android-14+-green.svg?logo=android)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-BUSL%201.1-blue.svg)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/avinaxhroy/BillMe?style=social)](https://github.com/avinaxhroy/BillMe)

> A robust offline-first Android mobile shop app with IMEI-aware inventory management, quick billing, thermal printing, and modern Material 3 design.

## � Features

### Core Features
- 💳 **Quick Billing System** - Fast and intuitive invoice creation
- 📦 **IMEI-Aware Inventory** - Track devices by IMEI with duplicate detection
- 🔐 **IMEI Validation** - Complete Luhn algorithm implementation
- 📱 **Offline First** - Works without internet connection
- 🖨️ **Thermal Printing** - Direct ESC/POS thermal printer support
- 📊 **Sales Dashboard** - Real-time sales analytics and insights
- 🔄 **Auto-Save** - Automatic transaction draft persistence
- 📈 **Profit Tracking** - Automatic profit calculations

### Technical Highlights
- ✅ **Clean Architecture** - Separation of concerns with layers
- ✅ **MVVM Pattern** - State management with ViewModels and StateFlow
- ✅ **Jetpack Compose** - Modern declarative UI framework
- ✅ **Room Database** - Local SQLite with type-safe data access
- ✅ **Hilt DI** - Complete dependency injection setup
- ✅ **Coroutines** - Reactive programming with Kotlin Flow
- ✅ **Comprehensive Tests** - Unit tests for core functionality

## 📋 Table of Contents

- [Quick Start](#-quick-start)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Project Structure](#-project-structure)
- [Configuration](#-configuration)
- [Development](#-development)
- [Testing](#-testing)
- [Contributing](#-contributing)
- [License](#-license)
- [Support](#-support)

## 🚀 Quick Start

### Clone and Setup
```bash
git clone https://github.com/avinaxhroy/BillMe.git
cd BillMe
```

### Build and Run
```bash
# Build the project
./gradlew build

# Run on connected device or emulator
./gradlew installDebug
```

For detailed setup instructions, see [SETUP.md](SETUP.md).

## 📋 Prerequisites

- **Android Studio** - Latest stable version (or later)
- **JDK** - Version 11 or higher
- **Android SDK** - Platform 34 (Android 14)
- **Gradle** - Included with wrapper

## 📦 Installation

### Step 1: System Setup
Follow the detailed instructions in [SETUP.md](SETUP.md) to install:
- Java Development Kit (JDK)
- Android Studio
- Android SDK and Build Tools

### Step 2: Clone Repository
```bash
git clone https://github.com/avinaxhroy/BillMe.git
cd BillMe
```

### Step 3: Open in Android Studio
- Launch Android Studio
- Select `File → Open`
- Navigate to the `BillMe` folder
- Wait for Gradle to sync

### Step 4: Build and Run
```bash
# Build APK
./gradlew build

# Run on device/emulator
./gradlew installDebug
```

## 🏗️ Project Structure

```
BillMe/
├── app/
│   ├── build.gradle.kts              # Dependencies and build config
│   ├── lint.xml                      # Lint rules
│   ├── proguard-rules.pro            # ProGuard configuration
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # App manifest
│       │   ├── assets/               # Static assets
│       │   ├── res/                  # Resources (drawable, values, xml)
│       │   └── java/com/billme/app/
│       │       ├── BillMeApplication.kt
│       │       ├── MainActivity.kt
│       │       ├── core/             # Core utilities
│       │       │   ├── automation/   # IMEI scanning, pricing engine
│       │       │   ├── backup/       # Backup scheduling and workers
│       │       │   ├── database/     # DB initialization and migration
│       │       │   ├── security/     # Encryption and security
│       │       │   └── util/         # Utilities (DateTime, validators)
│       │       ├── data/             # Data layer
│       │       │   ├── datastore/    # Proto DataStore
│       │       │   ├── local/        # Room entities, DAOs, database
│       │       │   ├── model/        # Data models
│       │       │   └── repository/   # Repository implementations
│       │       ├── di/               # Dependency injection modules
│       │       ├── hardware/         # Hardware interfaces
│       │       │   ├── BarcodeScanner.kt
│       │       │   ├── ThermalPrinter.kt
│       │       │   └── MockHardwareImpl.kt
│       │       ├── service/          # Background services
│       │       │   ├── AutoSaveService.kt
│       │       │   └── ReceiptService.kt
│       │       └── ui/               # UI layer
│       │           ├── component/    # Reusable Compose components
│       │           ├── navigation/   # Navigation setup
│       │           ├── screen/       # Feature screens
│       │           ├── theme/        # Material 3 theme
│       │           ├── util/         # UI utilities
│       │           └── viewmodel/    # ViewModels
│       └── test/
│           └── java/com/billme/app/  # Unit tests
├── android/                          # Android resources
├── .github/                          # GitHub configuration
│   ├── workflows/                    # CI/CD workflows
│   ├── ISSUE_TEMPLATE/               # Issue templates
│   └── PULL_REQUEST_TEMPLATE.md      # PR template
├── build.gradle.kts                  # Project-level build config
├── settings.gradle.kts               # Project structure settings
├── gradle.properties                 # Gradle properties
├── gradlew                           # Gradle wrapper (macOS/Linux)
├── SETUP.md                          # Detailed setup guide
├── CONTRIBUTING.md                   # Contribution guidelines
├── CODE_OF_CONDUCT.md                # Community code of conduct
├── SECURITY.md                       # Security policy
└── LICENSE                           # Business Source License 1.1
```

## 🔧 Configuration

### App Settings
The app comes pre-configured with sensible defaults:

```kotlin
// Shop Configuration
shopName = "Mobile Shop Pro"
gsttaxEnabled = true
gsttaxRate = 18.0
currency = "INR"

// Auto-Backup
backupEnabled = true
backupFrequency = "DAILY"

// Receipt Printing
paperWidth = 58  // mm
printQuality = "HIGH"
```

### Database
- **Type**: SQLite via Room
- **Entities**: Product, Transaction, TransactionLineItem, Customer, AppSetting
- **Auto-migration**: Supported with versioning
- **Offline-First**: Full offline support with sync capabilities

## 🧪 Testing

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests com.billme.app.core.util.ImeiValidatorTest
```

### Generate Test Coverage Report
```bash
./gradlew testDebugUnitTest
```

### Current Test Coverage
- ✅ IMEI Validation: 100%
- ✅ Luhn Algorithm: Comprehensive test cases
- ✅ Edge cases and error conditions

## � Development

### Building Variants
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Build with specific variant
./gradlew assembleDebug assembleRelease
```

### Development Tools
- **Gradle Build System**: `./gradlew build`
- **Static Analysis**: `./gradlew lint`
- **Code Format**: Follow Kotlin conventions
- **Debugging**: Android Studio debugger with breakpoints

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable/function names
- Add documentation for public APIs
- Keep functions small and focused

## 🔐 License

This project is licensed under the **Business Source License 1.1** (BUSL-1.1). See [LICENSE](LICENSE) for details.

### What You Can Do ✅
- **Contribute** improvements and modifications
- **Create** derivative works
- **Modify** the code for non-production use
- **Study** how the code works

### What You Cannot Do ❌
- **Use** the software for production purposes without a license
- **Rebrand** or repackage as your own work
- **Remove** license and copyright notices
- **Claim** ownership of the original work

**License Change Date**: 2028-11-04 → MIT License

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

1. **Read** [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines
2. **Check** [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community standards
3. **Fork** the repository
4. **Create** a feature branch: `git checkout -b feat/your-feature`
5. **Commit** with clear messages following our style
6. **Test** your changes: `./gradlew test`
7. **Push** and open a pull request

### Reporting Issues
- **Bug Report**: Use the bug report template in issues
- **Feature Request**: Describe use case and expected behavior
- **Security Issue**: See [SECURITY.md](SECURITY.md)

## 🔒 Security

For security vulnerabilities, please follow the guidelines in [SECURITY.md](SECURITY.md). Do not open public issues for security concerns.

## 📞 Support

- 💬 **Issues**: [GitHub Issues](https://github.com/avinaxhroy/BillMe/issues)
- 📖 **Documentation**: [SETUP.md](SETUP.md), [CONTRIBUTING.md](CONTRIBUTING.md)
- 🐛 **Bug Reports**: Use issue templates
- 💡 **Feature Requests**: Create an issue with `[FEATURE]` prefix

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| **Language** | Kotlin 1.9+ |
| **UI Framework** | Jetpack Compose |
| **Architecture** | MVVM + Clean Architecture |
| **Database** | Room (SQLite) |
| **DI Container** | Hilt |
| **Navigation** | Navigation Compose |
| **Async** | Coroutines + Flow |
| **Date/Time** | Kotlinx DateTime |
| **Testing** | JUnit 4, Mockito |
| **Build System** | Gradle 8.0+ |
| **Target SDK** | Android 14 (API 34) |

## � Development Roadmap

### Phase 1 ✅ (Current)
- [x] Database setup with Room
- [x] IMEI validation system
- [x] Dashboard UI with Material 3
- [x] Basic transaction management
- [x] Unit tests

### Phase 2 (Upcoming)
- [ ] Barcode scanning integration
- [ ] Thermal printer support
- [ ] Auto-save enhancement
- [ ] Google Drive backup
- [ ] Advanced UI refinements

### Phase 3 (Future)
- [ ] Multi-language support
- [ ] Dark mode enhancement
- [ ] Cloud sync
- [ ] Analytics integration
- [ ] Offline multi-device sync

## 📄 Additional Resources

- [Android Developer Guide](https://developer.android.com/guide)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://dagger.dev/hilt)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

---

**Maintained by**: [@avinaxhroy](https://github.com/avinaxhroy)  
**Last Updated**: November 2025  
**Project Status**: Active Development ✨

**If you find this project helpful, please consider giving it a ⭐ on [GitHub](https://github.com/avinaxhroy/BillMe)!**