# BillMe Android App - Complete Setup Guide

Welcome to BillMe! This guide will help you set up the development environment and get the app running.

## 📋 Prerequisites

Before you begin, ensure you have:
- **macOS**, **Linux**, or **Windows** with WSL2
- **Internet connection** (at least 5GB for SDK downloads)
- **10GB free disk space** for Android SDK
- **Administrator access** for system-level installations

## 🔧 Step-by-Step Setup

### 1. Install Java Development Kit (JDK)

BillMe requires **JDK 11 or higher**.

#### Option A: macOS (Homebrew - Recommended)

```bash
# Install Homebrew if needed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install OpenJDK 17 (recommended, but 11+ works)
brew install openjdk@17

# Add to shell profile
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
echo 'export PATH="${JAVA_HOME}/bin:$PATH"' >> ~/.zshrc

# Apply changes
source ~/.zshrc

# Verify installation
java -version
javac -version
```

#### Option B: Linux (apt - Ubuntu/Debian)

```bash
sudo apt update
sudo apt install openjdk-17-jdk

# Verify
java -version
```

#### Option C: Windows (Chocolatey)

```powershell
choco install openjdk17

# Verify
java -version
```

#### Option D: Manual Installation

1. Visit [Adoptium](https://adoptium.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
2. Download JDK 11+ for your OS
3. Follow installation instructions
4. Set `JAVA_HOME` environment variable:
   - **macOS/Linux**: Add to `~/.zshrc` or `~/.bash_profile`:
     ```bash
     export JAVA_HOME=/path/to/jdk
     export PATH="$JAVA_HOME/bin:$PATH"
     ```
   - **Windows**: Set system environment variable `JAVA_HOME` to JDK installation path

### 2. Install Android Studio

1. Download [Android Studio](https://developer.android.com/studio) (latest stable)
2. Follow installation wizard for your OS
3. Complete the initial setup and data migration
4. Accept Android SDK licenses

### 3. Install Android SDK Components

**Through Android Studio UI:**

1. Open Android Studio
2. Go to `Tools → SDK Manager`
3. Select the **SDK Platforms** tab:
   - ✅ Check **Android 14 (API level 34)** (required)
   - ✅ Check **Android 13 (API level 33)** (recommended)
   - ✅ Check **Android 12 (API level 31)** (optional)
4. Select the **SDK Tools** tab:
   - ✅ **Android SDK Build-Tools 34.0.0+**
   - ✅ **Android Emulator**
   - ✅ **Android SDK Platform-Tools**
   - ✅ **Google Play services**
5. Click **Apply** and wait for installation

**Verify SDK Installation:**

```bash
# Check Android SDK location
~/Library/Android/sdk/  # macOS
~/Android/Sdk/          # Linux
%APPDATA%\Local\Android\Sdk\ # Windows

# List installed packages
sdkmanager --list_installed
```

### 4. Configure Android Emulator (Optional but Recommended)

```bash
# Create a virtual device
avdmanager create avd \
  -n Pixel_5_API_34 \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d "Pixel 5"

# List available devices
emulator -list-avds

# Launch emulator
emulator -avd Pixel_5_API_34
```

### 5. Clone and Set Up BillMe Repository

```bash
# Clone the repository
git clone https://github.com/avinaxhroy/BillMe.git
cd BillMe

# Verify directory structure
ls -la
# Should see: build.gradle.kts, settings.gradle.kts, app/, gradlew, etc.
```

### 6. Open Project in Android Studio

1. Launch **Android Studio**
2. Select **File → Open** (or **Open Project** on welcome screen)
3. Navigate to the `BillMe` folder and select it
4. Click **Open**
5. Wait for Gradle to sync (check status bar at bottom)
6. If prompted, accept missing SDK components installations

### 7. Build and Run

```bash
# Build debug APK
./gradlew assembleDebug

# Run on connected device or emulator
./gradlew installDebug

# Build and run immediately
./gradlew run

# Build release APK (requires keystore)
./gradlew assembleRelease
```

## 📱 Running on Different Targets

### Physical Android Device

1. **Enable Developer Mode:**
   - Go to `Settings → About phone`
   - Tap `Build number` 7 times
   - Go to `Settings → Developer options`
   - Enable `USB Debugging`

2. **Connect via USB:**
   ```bash
   # Check connected devices
   adb devices

   # Should show your device with "device" status
   ```

3. **Install and Run:**
   ```bash
   ./gradlew installDebug
   adb shell am start -n com.billme.app/.MainActivity
   ```

### Android Emulator

```bash
# Start emulator (if not already running)
emulator -avd Pixel_5_API_34 &

# Check connected emulators
adb devices

# Install and run
./gradlew installDebug
```

## 🧪 Running Tests

```bash
# Run all unit tests
./gradlew test

# Run tests with output
./gradlew test --info

# Run specific test class
./gradlew test --tests com.billme.app.core.util.ImeiValidatorTest

# Generate test report (view in browser)
./gradlew test
# Report: app/build/reports/tests/testDebugUnitTest/index.html
```

## 🛠️ Build Variants and Tasks

```bash
# Build debug and release
./gradlew build

# Only debug build
./gradlew assembleDebug

# Only release build (requires signing config)
./gradlew assembleRelease

# Clean build
./gradlew clean build

# Build with verbose output
./gradlew build --info

# Check available tasks
./gradlew tasks
```

## 📊 Project Structure

```
BillMe/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml        # App configuration
│   │   │   ├── assets/                    # Static files
│   │   │   ├── res/                       # Resources (drawable, values, xml)
│   │   │   └── java/com/billme/app/
│   │   │       ├── BillMeApplication.kt
│   │   │       ├── MainActivity.kt
│   │   │       ├── core/                  # Core utilities
│   │   │       ├── data/                  # Data layer (database, repos)
│   │   │       ├── di/                    # Dependency injection
│   │   │       ├── hardware/              # Hardware interfaces
│   │   │       ├── service/               # Services
│   │   │       └── ui/                    # UI (Compose screens)
│   │   └── test/
│   │       └── java/com/billme/app/       # Unit tests
│   ├── build.gradle.kts                   # App-level dependencies
│   ├── lint.xml                           # Lint rules
│   └── proguard-rules.pro                 # ProGuard configuration
├── android/                               # Android resources
├── .github/                               # GitHub workflows & templates
├── build.gradle.kts                       # Project configuration
├── settings.gradle.kts                    # Module configuration
├── gradle.properties                      # Gradle settings
└── gradlew                                # Gradle wrapper
```

## 🔧 Configuration

### Environment Variables

Set these for optimal development:

```bash
# Add to ~/.zshrc (macOS) or ~/.bashrc (Linux)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
export ANDROID_HOME=$HOME/Android/Sdk          # Linux
export ANDROID_HOME=$APPDATA/Local/Android/Sdk # Windows
export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH"
```

### Gradle Configuration

Edit `gradle.properties` to customize:

```properties
# JVM configuration
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m

# Parallel build
org.gradle.parallel=true

# Daemon configuration
org.gradle.daemon=true
```

## ⚠️ Troubleshooting

### Gradle Sync Issues

```bash
# Clean gradle cache
./gradlew clean

# Sync with output
./gradlew sync --info

# Full rebuild
./gradlew clean build
```

### JDK/Java Issues

```bash
# Check current Java version
java -version
javac -version

# Check JAVA_HOME
echo $JAVA_HOME

# Should point to JDK 11+ installation
```

### Android SDK Issues

```bash
# List installed SDK versions
sdkmanager --list_installed

# Update SDK tools
sdkmanager --update

# Install missing platform
sdkmanager "platforms;android-34"

# Verify licenses are accepted
sdkmanager --licenses
```

### Device Connection Issues

```bash
# List connected devices
adb devices

# Restart ADB daemon
adb kill-server
adb start-server

# Check device logs
adb logcat

# Enable verbose mode
adb devices -l
```

### Emulator Issues

```bash
# List available virtual devices
emulator -list-avds

# Start emulator with verbose output
emulator -avd Pixel_5_API_34 -verbose

# Cold boot (clear state)
emulator -avd Pixel_5_API_34 -cold-boot-now
```

### Common Build Errors

| Error | Solution |
|-------|----------|
| `ANDROID_HOME not set` | Set ANDROID_HOME environment variable |
| `Java version incompatible` | Install JDK 11 or higher |
| `SDK platform not found` | Install Android SDK 34 via SDK Manager |
| `Gradle daemon crashed` | Run `./gradlew --stop` then rebuild |
| `Permission denied on gradlew` | Run `chmod +x gradlew` |

## 📝 Development Workflow

1. **Open in Android Studio**
   ```bash
   open -a "Android Studio" .
   ```

2. **Run in IDE:**
   - Click **Run** (Shift + F10) or ▶️ button
   - Select target device/emulator
   - Wait for build and installation

3. **Debug:**
   - Set breakpoints (click line number)
   - Click **Debug** (Shift + F9)
   - Use Variables panel to inspect state

4. **View Logs:**
   - Open **Logcat** tab in Android Studio
   - Filter by app name: `com.billme.app`

## 🚀 Next Steps

1. ✅ Complete setup steps above
2. ✅ Run `./gradlew build` to verify everything works
3. ✅ Open project in Android Studio
4. ✅ Explore the codebase in `app/src/main/java/com/billme/app/`
5. ✅ Read [CONTRIBUTING.md](CONTRIBUTING.md) to start contributing
6. ✅ Check [README.md](README.md) for project overview

## 🆘 Getting Help

- 📖 **Documentation**: Read [README.md](README.md) and [CONTRIBUTING.md](CONTRIBUTING.md)
- 🐛 **Report Issues**: [GitHub Issues](https://github.com/avinaxhroy/BillMe/issues)
- 💬 **Ask Questions**: Create a discussion or issue with details
- 🔗 **Resources**: 
  - [Android Developer](https://developer.android.com/)
  - [Kotlin Documentation](https://kotlinlang.org/docs/)
  - [Jetpack Compose](https://developer.android.com/jetpack/compose)

## ✨ Tips for Success

- 🎯 Keep SDK and Android Studio updated
- 💾 Allocate sufficient RAM to Gradle (4GB+)
- 🔌 Use wired USB connection for device debugging
- 📱 Test on physical device when possible
- 🧹 Run `./gradlew clean` if experiencing weird build issues
- 📚 Read build error messages carefully - they're usually helpful!

---

**Setup complete! Happy coding! 🚀**