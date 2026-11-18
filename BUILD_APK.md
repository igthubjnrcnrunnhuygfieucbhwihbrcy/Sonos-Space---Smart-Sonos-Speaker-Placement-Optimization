# How to Build the APK

Since we cannot build the APK directly in this environment (no Java/Gradle available), here are the steps to build it on your local machine:

## Prerequisites

1. **Java JDK 17 or 18**
   - Download from: https://www.oracle.com/java/technologies/javase-downloads.html
   - Or use OpenJDK: https://adoptium.net/

2. **Android Studio** (or just Android SDK)
   - Download from: https://developer.android.com/studio

3. **Git**
   - Download from: https://git-scm.com/

## Steps to Build

### 1. Clone the repository
```bash
git clone https://github.com/igthubjnrcnrunnhuygfieucbhwihbrcy/Sonos-Space---Smart-Sonos-Speaker-Placement-Optimization.git
cd Sonos-Space---Smart-Sonos-Speaker-Placement-Optimization
```

### 2. Open in Android Studio
- Launch Android Studio
- Click "Open an Existing Project"
- Select the cloned repository folder
- Android Studio will automatically sync Gradle

### 3. Build the APK

#### Option A: Using Android Studio GUI
1. Click **Build > Build Bundle(s) / APK(s) > Build APK**
2. Wait for the build to complete
3. Android Studio will show a notification with a link to the APK

#### Option B: Using Command Line
```bash
# On Linux/Mac
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug
```

The APK will be created at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 4. Install the APK

#### On Android Device:
1. Copy the APK to your Android device
2. Open the file manager on your device
3. Navigate to the APK file
4. Tap to install
5. If prompted, enable "Unknown Sources" in Settings

#### Using ADB:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### Error: Gradle not found
Run: `./gradlew assembleDebug` from the project root

### Error: Java not found
- Make sure Java JDK is installed
- Set JAVA_HOME environment variable
- Example: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`

### Error: SDK not found
- Open Android Studio
- Go to SDK Manager
- Install the required SDK packages

### Error: Build failed
- Check the error message in the terminal
- Make sure you have internet connection (Gradle needs to download dependencies)
- Try: `./gradlew clean` then `./gradlew assembleDebug`

## Build Variants

### Debug APK (for development)
```bash
./gradlew assembleDebug
```
- File: `app/build/outputs/apk/debug/app-debug.apk`
- Can be installed on any device
- Has debug symbols

### Release APK (for distribution)
```bash
./gradlew assembleRelease
```
- File: `app/build/outputs/apk/release/app-release.apk`
- Optimized for distribution
- Needs signing for installation

### Signed Release APK
To create a signed APK for the Play Store:

1. Create a keystore (only do this once and keep it safe!):
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
```

2. Add signing config to `app/build.gradle`:
```groovy
android {
    signingConfigs {
        release {
            storeFile file('my-release-key.jks')
            storePassword 'yourpassword'
            keyAlias 'my-alias'
            keyPassword 'yourpassword'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

3. Build the signed APK:
```bash
./gradlew assembleRelease
```

The signed APK will be at: `app/build/outputs/apk/release/app-release.apk`

## Notes

- The first build may take several minutes as Gradle downloads all dependencies
- Make sure your device has at least Android 7.0 (API 24) or higher
- The app requires accelerometer and magnetometer sensors for angle measurement
