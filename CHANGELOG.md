# Changelog

All notable changes to the BillMe project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### 🚧 Planned Features
- Multi-store support
- Web dashboard
- API integration for third-party services
- Tablet UI optimization
- Advanced ML-based predictive analytics

## [2.0.0] - 2024-11-04

### 🎉 Major Release - Enterprise-Grade Mobile Shop Management

#### ✨ Added - GST & Invoice Management
- **Complete GST System**
  - 4 GST modes: FULL_GST, PARTIAL_GST, GST_REFERENCE, NO_GST
  - Intrastate (CGST + SGST) and Interstate (IGST) calculations
  - HSN/SAC code support with multi-tier GST rates
  - GST configuration management with state codes
  - Comprehensive GST reports and analytics
  - GST return filing data export

- **Professional Invoice System**
  - Invoice generation with comprehensive GST breakdown
  - Multiple invoice types (REGULAR, PROFORMA, CREDIT_NOTE, DEBIT_NOTE, EXPORT, SEZ)
  - PDF generation using iText7 (v8.0.2)
  - Bilingual invoice support (English/Hindi)
  - Digital signature support with image embedding
  - Customer and Owner copy generation
  - Invoice history and search
  - Invoice cancellation and return invoice creation

#### ✨ Added - OCR & Automation
- **Advanced OCR System (ML Kit)**
  - Invoice/bill scanning with field extraction
  - Smart IMEI detection from camera
  - Product information extraction
  - Intelligent text filtering and preprocessing
  - Multi-page document support
  - Quality assessment and auto-correction
  - Template matching for various invoice formats
  - Confidence scoring and validation

- **Unified IMEI Scanner**
  - CameraX integration with ML Kit Text Recognition
  - Three scan modes: SINGLE, DUAL, BULK
  - Auto-detection of IMEI1 and IMEI2
  - Real-time duplicate checking
  - Luhn algorithm validation
  - Progressive scanning with confidence scoring
  - Manual entry fallback
  - Torch/flash support

#### ✨ Added - Data Management
- **Enhanced Database System**
  - 15 entities with comprehensive relationships
  - Database migrations (v1 to v11)
  - Entities: Product, ProductIMEI, Supplier, Transaction, TransactionLineItem, Customer, Invoice, InvoiceLineItem, GSTConfiguration, GSTRate, InvoiceGSTDetails, StockAdjustment, ProductCostHistory, Signature, AppSetting
  - Database health monitoring
  - Automatic integrity checks
  - Performance optimization with indices

- **Backup & Restore System**
  - Local backup with ZIP compression
  - Incremental and full backup modes
  - Backup scheduling with WorkManager
  - Database, invoices, and signatures backup
  - Metadata preservation
  - One-click restore functionality
  - Automatic backup corruption detection
  - Google Drive backup preparation (infrastructure ready)

#### ✨ Added - Business Features
- **Customer Management**
  - Customer profiles with GST details
  - Customer segmentation (VIP, REGULAR, OCCASIONAL, NEW)
  - Purchase history tracking
  - Loyalty points system
  - Customer lifetime value (LTV) analytics
  - Retention rate calculations
  - Top customers identification

- **Product Management**
  - Individual IMEI tracking per device
  - Product status management (IN_STOCK, SOLD, RETURNED, DAMAGED, RESERVED, OUT_OF_STOCK)
  - Multiple IMEI support per product
  - Stock adjustments with reason tracking
  - Product cost history for profit analysis
  - Warranty tracking (start date, expiry)
  - Variant and color management
  - Barcode support

- **Enhanced Billing**
  - Quick billing screen with cart management
  - IMEI-based product addition
  - Discount application (percentage/fixed)
  - Payment method selection (CASH, UPI, CARD, etc.)
  - Draft transaction auto-save
  - Transaction history with search
  - Profit calculations per transaction

#### ✨ Added - Printing & Sharing
- **Thermal Printing**
  - ESC/POS printer support via Bluetooth
  - Receipt builder with formatting
  - Text alignment and sizing
  - Line separators and QR codes
  - Printer connection management
  - Print queue handling

- **PDF & Sharing**
  - PDF invoice generation and storage
  - Share invoices via any app
  - Print invoices to PDF or printer
  - File manager integration
  - Invoice preview in app
  - Bulk invoice operations

#### ✨ Added - Analytics & Reporting
- **Business Metrics**
  - Daily, weekly, monthly sales analytics
  - Profit tracking and margins
  - Top-selling products
  - Customer analytics and segmentation
  - GST collection reports
  - Revenue trends and forecasting
  - Inventory valuation

- **Dashboard**
  - Real-time sales overview
  - Today's transactions count
  - Revenue and profit cards
  - Quick action buttons
  - Recent activity feed
  - Low stock alerts
  - GST summary

#### 🛠️ Technical Improvements
- **Architecture**
  - Clean Architecture with layered separation
  - MVVM pattern with StateFlow
  - Repository pattern for data access
  - Use case layer for business logic
  - Dependency Injection with Hilt
  - Coroutines and Flow for async operations

- **Core Services**
  - EnhancedBillingService for invoice creation
  - GSTCalculationService for tax calculations
  - AnalyticsService for business metrics
  - IMEIManagementService for device tracking
  - DatabaseInitializer with seeding
  - BackupScheduler with daily automation

- **UI/UX**
  - Material 3 design system
  - Dynamic theming
  - Edge-to-edge display support
  - Responsive layouts
  - Loading states and error handling
  - Pull-to-refresh functionality
  - Smooth animations and transitions

#### 📦 Dependencies & Libraries
- **Core**
  - Kotlin 2.0.21
  - Android Gradle Plugin 8.13.0
  - Jetpack Compose BOM 2024.12.01
  - Material 3 components

- **Database**
  - Room 2.6.1 with KSP
  - Kotlinx Serialization 1.7.1
  - Kotlinx DateTime 0.6.0

- **Dependency Injection**
  - Hilt 2.51.1
  - Hilt Navigation Compose

- **Camera & ML**
  - CameraX 1.4.0
  - ML Kit Text Recognition 16.0.0
  - ML Kit Barcode Scanning 17.3.0

- **PDF & Printing**
  - iText7 Core 8.0.2
  - ESC/POS Thermal Printer 3.3.0

- **Other**
  - WorkManager 2.8.1 with Hilt integration
  - DataStore Preferences 1.1.0
  - Security Crypto 1.1.0-alpha06
  - Accompanist Permissions 0.32.0
  - Coil Image Loading 2.5.0

#### 🔧 Configuration
- **Build Configuration**
  - Min SDK 24 (Android 7.0)
  - Target SDK 35 (Android 15)
  - Compile SDK 35
  - Version Code 2
  - Version Name 2.0.0
  - ProGuard rules for release builds
  - Lint configuration with suppression

- **App Configuration**
  - FileProvider for secure file sharing
  - WorkManager with Hilt integration
  - Permissions: Camera, Bluetooth, Storage, Location, Internet
  - Backup rules and data extraction rules

#### � Bug Fixes
- Fixed IMEI duplicate detection logic
- Improved OCR accuracy with text filtering
- Resolved database migration issues
- Fixed PDF generation for large invoices
- Corrected GST calculation edge cases
- Improved scanner performance on low-end devices

#### 🎯 Phase 2 Achievements
- ✅ Complete GST compliance system
- ✅ Professional invoice generation
- ✅ Advanced OCR with ML Kit
- ✅ Unified IMEI scanning system
- ✅ Comprehensive backup & restore
- ✅ Thermal printing support
- ✅ Customer analytics and LTV
- ✅ Enhanced database with 15 entities
- ✅ Digital signature support
- ✅ Bilingual invoices

---

## Version History

### Version Numbering
- **Major** (X.0.0): Breaking changes, major architectural updates
- **Minor** (0.X.0): New features, backwards compatible
- **Patch** (0.0.X): Bug fixes, minor improvements

### Release Notes

#### v2.0.0 (2024-11-04) - Enterprise-Grade Release
Major release with complete GST system, invoice management, OCR capabilities, advanced IMEI scanning, backup/restore, thermal printing, and comprehensive analytics.

**Key Features:**
- Complete GST compliance (4 modes)
- Professional invoice generation with PDF
- OCR-powered invoice scanning
- Unified IMEI scanner with ML Kit
- Enterprise backup & restore system
- Thermal receipt printing
- Customer lifetime value analytics

---

## Upcoming Releases

### v2.1.0 (Planned Q1 2025)
- Google Drive cloud backup integration
- Multi-language support (Hindi, Tamil, Bengali)
- Dark mode improvements
- Bulk import/export enhancements
- SMS notifications for customers

### v2.2.0 (Planned Q2 2025)
- Advanced analytics dashboard
- Predictive inventory management
- Multi-store support foundation
- Receipt customization templates
- WhatsApp Business integration

### v3.0.0 (Future)
- Multi-store management
- Web dashboard portal
- API for third-party integrations
- Tablet-optimized interface
- Advanced ML-based insights

---

## Migration Guides

### Migrating from v1.x to v2.0.0

**Database Changes:**
- Database will auto-migrate from v1-11
- New entities added: Invoice, InvoiceLineItem, GSTConfiguration, GSTRate, InvoiceGSTDetails, StockAdjustment, ProductCostHistory, Signature
- ProductIMEI entity added for individual device tracking

**New Features to Configure:**
1. Set up GST configuration in Settings → GST Settings
2. Add shop signature in Settings → Signature
3. Configure thermal printer if available
4. Enable backup scheduling

**Breaking Changes:**
- None - fully backwards compatible

---

## Breaking Changes

### v2.0.0
- No breaking changes from v1.x
- All previous data is preserved and migrated
- New features are optional

---

## Deprecations

### v2.0.0
- Legacy IMEI fields in Product table (replaced by ProductIMEI entity)
- Old backup format (replaced by enhanced ZIP-based backup)

**Note:** Deprecated features will be removed in v3.0.0

---

## Security Updates

### v2.0.0
- Enhanced encryption for sensitive data (Security Crypto 1.1.0)
- Secure file sharing with FileProvider
- Database encryption support
- Signature verification for backups
- Improved permission handling

### v1.0.0
- Initial security implementation
- BUSL-1.1 license for IP protection
- Security policy established
- Vulnerability reporting procedure defined

---

## Performance Improvements

### v2.0.0
- Database query optimization with proper indices
- Room Write-Ahead Logging (WAL) mode
- LazyColumn for efficient list rendering
- Image compression for OCR processing
- Background processing with WorkManager
- Memory leak prevention in camera operations

---

## Known Issues

### v2.0.0
- Excel export feature not yet implemented (use CSV instead)
- Google Drive backup UI ready but sync pending
- Some low-end devices may experience slower OCR processing
- Thermal printer compatibility limited to ESC/POS devices

---

## Notes

- 📅 **Current Version**: 2.0.0
- 🏷️ **Latest Stable**: 2.0.0
- 🚀 **Next Release**: 2.1.0 (Q1 2025)
- 📝 **License**: BUSL-1.1 (converts to MIT on 2028-11-04)
- 🔧 **Min Android Version**: 7.0 (API 24)
- 🎯 **Target Android Version**: 15 (API 35)

---

For more information:
- [README.md](README.md) - Project overview and features
- [SETUP.md](SETUP.md) - Complete setup instructions
- [CONTRIBUTING.md](CONTRIBUTING.md) - How to contribute
- [SECURITY.md](SECURITY.md) - Security policy and reporting
- [QUICK_REFERENCE.md](QUICK_REFERENCE.md) - Quick reference guide

---

**Last Updated**: November 4, 2024
**Maintainer**: @avinaxhroy
**Repository**: https://github.com/avinaxhroy/BillMe
