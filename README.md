![Visits](https://api.visitorbadge.io/api/VisitorHit?user=Vitgracer&repo=android-local-llm&countColor=%237B1E7A&style=flat-square)
![Last Commit](https://img.shields.io/github/last-commit/Vitgracer/android-local-llm?color=4C6EF5&label=Last%20Commit&style=flat-square)
![Repo Size](https://img.shields.io/github/repo-size/Vitgracer/android-local-llm?color=2FBF71&label=Repo%20Size&style=flat-square)
![Stars](https://img.shields.io/github/stars/Vitgracer/android-local-llm?color=F59F00&label=Stars&style=flat-square)
![Forks](https://img.shields.io/github/forks/Vitgracer/android-local-llm?color=E03131&label=Forks&style=flat-square)

# LocaLLM 

LocaLLM is an Android application designed for real-time speech-to-text processing and intelligent response generation using local Large Language Models (LLMs). It serves as a helpful assistant during live conversations.

## Features
- **Voice Recognition**: Real-time transcription using Android's Speech-to-Text API.
- **Local LLM Integration**: Connects to your own local AI server (e.g., LM Studio).
- **Bilingual Support**: Toggle between Russian and English for both speech recognition and AI instructions.
- **Privacy Focused**: All LLM processing happens on your local machine.

## How to Run

### 1. Set up your Local LLM Server
1. Download and install [LM Studio](https://lmstudio.ai/) or a similar tool.
2. Download a model (e.g., Llama 3, Mistral, or Gemma).
3. Start the **Local Inference Server**. Make it visible within your network.
4. Make sure your PC and Android device are on the **same Wi-Fi network**.
5. Note your PC's local IP address (e.g., `192.168.1.10`).

### 2. Configure the Android Project
1. Open the project in Android Studio.
2. Locate the template file: `app/src/main/java/com/example/locallm/AppConfig.kt.example`.
3. **Copy and rename** it to `AppConfig.kt` in the same directory.
4. Edit `AppConfig.kt` and replace `YOUR_LOCAL_IP` with your PC's IP address:
   ```kotlin
   const val LM_STUDIO_URL = "http://192.168.x.x:1234/v1/chat/completions"
   ```
5. (Optional) Customize the `SYSTEM_PROMPT` to change how the AI behaves.

### 3. Build and Launch
1. Enable Developer Options and USB Debugging on your Android device.
2. Connect your device and click **Run** in Android Studio.
3. Grant Microphone permissions when prompted.
4. Use the switch to select your language and press **GENERATE ANSWER** to get AI insights based on the live transcript.

## Tech Stack
- **Kotlin**: Core language.
- **OkHttp**: For network requests to the local server.
- **Gson**: For JSON parsing.
- **Android Speech API**: For real-time voice recognition.

## License
Apache 2.0 License. See `LICENSE` for more details.

