<div align="center">
<h1>智慧海釣與海洋氣象觀測APP</h1>
<img width="1200" height="475" alt="GHBanner" src="Gemini_Generated_Image_gy2mxsgy2mxsgy2m.png" />
</div>

# 系統功能

## 1 海洋氣象觀測中心

<div align="center">
<img src="1.png" />
</div>


## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
