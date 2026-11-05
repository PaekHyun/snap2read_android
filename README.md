# snap2read_android 📱→🔊

**폰 단독으로 동작하는 snap2read 안드로이드 앱.** 사진 찍기 → 온디바이스 OCR → 텍스트 표시·수정 → 읽어주기 → (온디바이스 LLM) 한국어 번역.

## 구성

| 기능 | 구현 | 오프라인 |
|------|------|---------|
| 사진 촬영 | CameraX | ✅ |
| OCR | ML Kit Text Recognition v2 (라틴+한국어) | ✅ 온디바이스 |
| 읽어주기 | Android TextToSpeech (시스템) | ✅ |
| 번역 | **Gemma 4 E4B** — LiteRT-LM (`` 번들) | ✅ 온디바이스 |
| (선택) 서버 연동 | 같은 WiFi의 snap2read PC 서버 사용 — GPU OCR·자연 음성·qwen3 번역 | WiFi |

메모리: Gemma 4 E4B ≈ 3GB + ML Kit ≈ 수십 MB → 12GB RAM 폰에서 여유 있음.

## 사전 준비 (개발 PC)

1. **JDK 17** — `winget install EclipseAdoptium.Temurin.17.JDK` (또는 안드로이드 스튜디오 내장 JDK 사용)
2. **Android Studio** (권장) — SDK·에뮬레이터 관리 포함. 명령줄만으로도 가능: `sdkmanager`로 platform-tools, platforms;android-34, build-tools 설치
3. `local.properties` 생성 (자동 생성 안 될 때):
   ```
   sdk.dir=C:\\Users\\<사용자>\\AppData\\Local\\Android\\Sdk
   ```

## 빌드·실행

```bash
cd snap2read_android
gradlew.bat assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
gradlew.bat installDebug           # 연결된 폰에 바로 설치
```

- 폰에서 "출처를 알 수 없는 앱" 설치 허용 1회 (사이드로딩 — Play 스토어 불필요, 0원)
- 첫 실행 시 ML Kit 한국어 모델 다운로드(수십 MB, 1회) — 이후 오프라인

## 모델 파일

- `app/src/main/assets/models/` — LiteRT-LM 형식.
  **APK에 포함하면 앱이 커지므로**(수 GB), 다운로드 후 이 위치에 직접 넣어 빌드하세요.
  (파일이 없으면 번역 기능만 비활성되고 OCR·읽어주기는 동작)

## 구조

```
app/src/main/java/com/paekhyun/snap2read/
├─ MainActivity.kt        # 화면: 카메라 → 결과 → 편집 → 재생
├─ OcrEngine.kt           # ML Kit 래퍼
├─ TtsEngine.kt           # Android TTS 래퍼
└─ LlmTranslator.kt       # LiteRT-LM (Gemma 4 E4B) 래퍼 — 파일 있을 때만 활성
```

## 서버 연동 모드 (선택)

설정에서 서버 주소(`https://<PC IP>:9000`)를 넣으면, 사진을 PC로 보내
GPU OCR(PP-OCRv6)·edge-tts 자연 음성·qwen3 번역을 사용할 수 있습니다.
오프라인 모드와 서버 모드를 설정에서 전환.

## 트러블슈팅

| 증상 | 해결 |
|------|------|
| `SDK location not found` | `local.properties`에 `sdk.dir` 지정 |
| `Unsupported class file major version` | JDK 17 사용 확인 (`java -version`) |
| ML Kit 한국어 인식 실패 | 첫 실행 시 모델 다운로드 필요 — WiFi에서 1회 실행 |
| LLM 로드 실패/OOM | litertlm 파일 위치 확인, 다른 앱 종료 후 재시도 |
