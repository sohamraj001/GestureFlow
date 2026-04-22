# GestureFLOW ✋🌊

**GestureFLOW** is a professional-grade, hands-free control engine for Android and Web. It leverages AI-powered hand landmark tracking to enable seamless navigation of short-form video content like **Instagram Reels** and **YouTube Shorts** using simple hand gestures.

---

## 🚀 Key Features

- **AI-Powered Navigation**: Use your front camera to scroll through videos.
  - **Index Up/Down**: Scroll to next/previous video.
  - **Thumb Up**: Like the video (Double-tap simulation).
  - **Open Palm**: Pause/Play toggle.
- **System-Wide Accessibility**: Works on top of native apps using Android Accessibility Services.
- **Premium UI/UX**: Includes a high-tech floating overlay with real-time feedback and a pulsing status indicator.
- **Low-End Optimized**: Built for performance with frame-skipping and resolution scaling to support i3-equivalent hardware.
- **Privacy First**: All gesture processing happens locally on the device using MediaPipe. No biometric data is ever uploaded.

---

## 📂 Project Structure

- `/android-app`: Full Android Studio project (Kotlin, CameraX, MediaPipe).
- `/web-version`: Browser-based implementation for testing and demonstration.
- `implementation_plan.md`: Technical architecture and build guide.
- `walkthrough.md`: Calibration and setup instructions.

---

## 🛠️ Setup Instructions (Android)

1. **Clone the Repo**:
   ```bash
   git clone https://github.com/sohamraj001/GestureFlow.git
   ```
2. **Download AI Model**: 
   Download the `hand_landmarker.task` from [MediaPipe](https://developers.google.com/mediapipe/solutions/vision/hand_landmarker#models) and place it in:
   `android-app/app/src/main/assets/`
3. **Build & Install**:
   Open the `android-app` folder in Android Studio and deploy to your device.
4. **Permissions**:
   - Grant **Camera** permission.
   - Grant **Overlay** (Display over other apps) permission.
   - Enable **GestureFLOW Gestures** in Accessibility Settings.

---

## 🧪 Testing the Web Version

1. Open `index.html` in a modern browser.
2. Grant camera access.
3. Wait for "AI Engine Active".
4. Position your hand in the preview and start flowing!

---

## ⚖️ Disclaimer

This project is for educational and assistive purposes. It uses the Android Accessibility API to simulate touch events. It is not affiliated with, endorsed by, or integrated via official APIs with Instagram or YouTube.

---

**Built with ❤️ by [Soham Raj](https://github.com/sohamraj001)**
