# Sonos Space - Smart Sonos Speaker Placement & Optimization

![Sonos Space Logo](https://via.placeholder.com/150/4A90E2/FFFFFF?text=Sonos+Space)

**Sonos Space** is a complete alternative to SonosSequencer with advanced features for optimal speaker placement and sound optimization. The app combines modern design with powerful functionality to help you get the best sound experience from your Sonos speakers.

## Features

### 📱 Smart Speaker Placement
- **Phone-on-Speaker Angle Measurement**: Place your phone on the speaker with fronts aligned to automatically measure the speaker angle using your phone's sensors (accelerometer + gyroscope)
- **Precise Position Input**: Enter exact height, distance from wall, and distance from corner in centimeters
- **Visual Guides**: Clear visual instructions for proper phone placement

### 🏠 Room Configuration
- **Room Dimensions**: Enter length, width, and height of your room
- **Room Shape**: Select from rectangular, square, L-shaped, or open plan
- **Wall Materials**: Choose from concrete, brick, drywall, wood, or glass
- **Furniture Level**: Select furniture density (minimal, light, medium, heavy)
- **3D Room Preview**: Visual representation of your room with speaker placement

### 🎵 Sound Optimization
- **Automatic EQ Calculation**: Optimizes bass, treble, loudness, and balance based on room acoustics and speaker position
- **Room Mode Analysis**: Calculates standing waves and suggests optimal EQ settings
- **Listening Position Finder**: Recommends the best spot to listen based on your speaker setup

### 🔍 Sonos Integration
- **Automatic Speaker Discovery**: Finds all Sonos speakers on your local network via UPnP/SSDP
- **Speaker Management**: Add, edit, and remove speakers with their exact positions
- **Connection Status**: Real-time monitoring of speaker connectivity
- **Multi-Speaker Support**: Configure and optimize multiple speakers in the same room

### ⚙️ Settings & Preferences
- **Dark Mode**: Enable dark theme for comfortable nighttime use
- **Auto Updates**: Automatically check for and install app updates
- **Unit Preferences**: Choose between centimeters or inches
- **Persistent Settings**: All your data and preferences are saved

### 📲 Modern Design
- **Material Design 3**: Clean, modern interface following Google's latest design guidelines
- **Apple-like Feel**: Smooth animations, intuitive navigation, and polished UI
- **Bottom Navigation**: Easy access to all main features
- **Responsive Layouts**: Works perfectly on all Android devices

## Screenshots

| Main Screen | Speaker Setup | Room Configuration |
|-------------|---------------|-------------------|
| ![Main](https://via.placeholder.com/300x600/4A90E2/FFFFFF?text=Main) | ![Setup](https://via.placeholder.com/300x600/4A90E2/FFFFFF?text=Setup) | ![Room](https://via.placeholder.com/300x600/4A90E2/FFFFFF?text=Room) |

## Installation

### From GitHub

1. Clone this repository:
```bash
git clone https://github.com/igthubjnrcnrunnhuygfieucbhwihbrcy/Sonosequencer.new.android.beta.git
```

2. Open the project in Android Studio

3. Build and run on your Android device (minimum SDK 24)

### APK Download

You can download the latest APK from the [Releases](https://github.com/igthubjnrcnrunnhuygfieucbhwihbrcy/Sonosequencer.new.android.beta/releases) page.

## Usage

### Quick Start

1. **Discover Speakers**: Tap the search icon to find all Sonos speakers on your network
2. **Add Speaker**: Select a speaker and follow the 3-step setup:
   - Step 1: Select your speaker from the list
   - Step 2: Place your phone on the speaker with fronts aligned, then tap "Start Measurement"
   - Step 3: Enter precise position details and save
3. **Configure Room**: Go to Settings > Room Configuration to set up your room dimensions and characteristics
4. **Optimize**: The app will automatically calculate the best EQ settings for your setup

### Speaker Placement Tips

- **Height**: For best results, place speakers at ear level when seated (typically 90-120cm from floor)
- **Distance from Wall**: Keep at least 30-50cm from walls to avoid excessive bass buildup
- **Angle**: For stereo speakers, angle them slightly inward (15-30 degrees) toward the listening position
- **Symmetry**: For stereo setups, place speakers equidistant from the center line

## Technical Details

### Architecture

- **MVVM Pattern**: Clean separation of concerns with ViewModel, Repository, and Data layers
- **Room Database**: Local database for storing speaker and room configurations
- **Retrofit**: REST API client for Sonos API integration
- **Coroutines**: Asynchronous programming for smooth UI performance
- **View Binding**: Type-safe view access without boilerplate

### Dependencies

- **Kotlin**: 1.9.22
- **Android Gradle Plugin**: 8.3.2
- **Room**: 2.6.1
- **Retrofit**: 2.11.0
- **OkHttp**: 4.12.0
- **Coroutines**: 1.7.3
- **Material Components**: 1.11.0
- **Play Core**: 2.1.0 (for auto-updates)

### Sensors Used

- **Accelerometer**: Measures device acceleration and tilt
- **Magnetometer**: Measures magnetic field for compass direction
- **Gyroscope**: Measures rotation rate (used for sensor fusion)

### Permissions Required

- **Internet**: For Sonos API communication
- **Network State**: To check network connectivity
- **WiFi State**: To access WiFi information
- **Bluetooth**: For potential future Bluetooth speaker support
- **Location**: For fine location (required for WiFi scanning on some devices)

## How It Works

### Angle Measurement

The app uses your phone's accelerometer and magnetometer to calculate the orientation. When you place your phone on the speaker with fronts aligned:

1. The app reads the accelerometer to detect the device's tilt
2. The magnetometer provides compass direction
3. Sensor fusion combines these readings to calculate the exact angle
4. The angle is displayed in real-time and can be saved to your speaker profile

### Acoustic Calculation

The app uses room dimensions and speaker positions to:

1. Calculate room volume and identify potential standing waves (room modes)
2. Analyze wall material absorption characteristics
3. Consider furniture density for sound absorption
4. Determine optimal EQ settings to compensate for room acoustics
5. Suggest the best listening position for your speaker setup

### Automatic Updates

The app uses Google Play Core's App Update library to:
- Check for new versions
- Download updates in the background (flexible updates)
- Prompt users to install updates when available
- Support immediate updates for critical fixes

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by SonosSequencer
- Built with love for audio enthusiasts
- Thanks to the Android and Kotlin communities

## Contact

For questions, suggestions, or feedback, please open an issue on GitHub.

---

**Sonos Space** - Optimize Your Sound Experience

*Made with ❤️ and Kotlin*
