# Attendix

Attendix is an Android attendance app that allows students to check in for lectures by snapping a quick selfie and verifying their Wi-Fi connection. The app uses Firebase Authentication, Firestore and Storage for user management and attendance records, and CameraX for seamless camera integration.

## Features

- User sign-up & login via Firebase Auth  
- Select your lecture slot (morning/afternoon/evening)  
- Auto-validate campus Wi-Fi (using [`AppConstants.ALLOWED_WIFI_SSIDS`](app/src/main/java/com/fpt/attendix/util/AppConstants.java))  
- Capture a photo with CameraX ([`CameraActivity`](app/src/main/java/com/fpt/attendix/ui/main/CameraActivity.java))  
- Preview & confirm your selfie before uploading ([`ImagePreviewActivity`](app/src/main/java/com/fpt/attendix/ui/main/ImagePreviewActivity.java))  
- Upload photo to Firebase Storage ([`AttendanceRepository.uploadAttendanceImage`](app/src/main/java/com/fpt/attendix/data/repository/AttendanceRepository.java))  
- Save attendance record in Firestore with timestamp ([`AttendanceViewModel.saveRecordToFirestore`](app/src/main/java/com/fpt/attendix/ui/main/AttendanceViewModel.java))  
- Logout from the attendance screen

## Tech Stack

- Java on Android SDK 24+  
- AndroidX + Material3 (`material:1.12.0`)  
- Firebase: Auth, Firestore, Storage, Config  
- CameraX (`androidx.camera:*:1.4.2`)  
- Glide (for image loading)  
- MVVM with LiveData & ViewModel (`lifecycle-livedata:2.9.1`, `lifecycle-viewmodel:2.9.1`)

## Prerequisites

- Android Studio Arctic Fox or newer  
- Java 11+ SDK  
- A Firebase project with Auth, Firestore & Storage enabled  
- Add your `google-services.json` to `app/`

## Getting Started

1. Clone the repo  
   ```bash
   git clone https://github.com/your-org/attendix.git
   cd attendix
   ```
2. Open in Android Studio  
3. Sync Gradle (uses [libs.versions.toml](gradle/libs.versions.toml))  
4. Run on an Android device/emulator

## Project Structure

```
app/
 ├─ src/main/java/com/fpt/attendix/ui/auth/
 │    ├─ LoginFragment.java        # User login flow
 │    ├─ RegisterFragment.java     # User sign-up flow
 │    └─ SplashActivity.java       # Entry splash screen
 ├─ src/main/java/com/fpt/attendix/ui/main/
 │    ├─ MainActivity.java
 │    ├─ AttendanceFragment.java   # Select slot & launch camera
 │    ├─ CameraActivity.java       # CameraX capture
 │    └─ ImagePreviewActivity.java # Preview & confirm photo
 ├─ src/main/java/com/fpt/attendix/ui/main/AttendanceViewModel.java
 ├─ src/main/java/com/fpt/attendix/data/repository/AttendanceRepository.java
 └─ src/main/java/com/fpt/attendix/util/AppConstants.java
```

## Usage

1. **Sign up / Login**  
2. **Grant location & camera permissions**  
3. **Select your slot** from the dropdown in [`AttendanceFragment`](app/src/main/java/com/fpt/attendix/ui/main/AttendanceFragment.java)  
4. **Tap “Mở Camera và Điểm Danh”**  
5. **Review your photo**, then confirm upload  
6. **Logout** via the button at the bottom  

## License

This project is licensed under the [Apache-2.0 License](LICENSE)