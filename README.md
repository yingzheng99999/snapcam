# SnapCam — Android Camera App

## Build APK via GitHub Actions (no local SDK required)

### 1. Create a GitHub repository

```bash
# Create a repo named "SnapCam" on github.com, then:
cd /home/huazhuo/SnapCam
git remote add origin https://github.com/YOUR_USERNAME/SnapCam.git
git push -u origin main
```

### 2. Wait for build

Go to your repo → **Actions** tab → The workflow will auto-start.
After ~5 minutes, the APK will be ready under **Artifacts**:

```
snapcam-debug-apk.zip → app-debug.apk
```

### 3. Install on phone

Download the APK from GitHub Actions artifacts, transfer to phone, and install.

---

## Local Build (requires Android SDK)

```bash
# Install Android SDK first, then:
cd SnapCam
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
SnapCam/
├── app/
│   ├── src/main/java/com/snapcam/
│   │   ├── app/              # MainActivity + Application
│   │   ├── data/             # CameraX, Room, Repository
│   │   ├── domain/           # Models, Repository interfaces
│   │   ├── di/               # Hilt DI modules
│   │   ├── navigation/       # NavGraph (camera↔gallery↔editor)
│   │   └── presentation/     # Camera, Gallery, Editor screens
│   └── src/main/assets/filters/  # 6 AGSL shader filters
├── .github/workflows/        # GitHub Actions CI
└── build.gradle.kts
```

## Features

- CameraX preview + photo (ZSL) + video (1080p)
- Front/back lens switch + flash + zoom
- 6 real-time AGSL filters (Grayscale → Film)
- Gallery with grid view
- Image editor (brightness/contrast/saturation/rotate)
- Room database for media metadata
- Clean Architecture + Hilt DI
