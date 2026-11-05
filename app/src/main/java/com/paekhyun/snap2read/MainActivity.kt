package com.paekhyun.snap2read

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * snap2read 안드로이드 앱 — 온디바이스 파이프라인
 * 촬영(CameraX) → OCR(ML Kit) → 표시/수정 → 읽어주기(TTS) → 번역(LiteRT-LM)
 */
class MainActivity : ComponentActivity() {

    private val ocr = OcrEngine(this)
    private val tts = TtsEngine(this)
    private val llm = LlmTranslator(this)

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) uiState = uiState.copy(cameraReady = true)
        }

    private var uiState by mutableStateOf(UiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermission.launch(android.Manifest.permission.CAMERA)
        tts.init { ready -> uiState = uiState.copy(ttsReady = ready) }
        llm.init { ready, reason -> uiState = uiState.copy(llmReady = ready, llmReason = reason) }
        setContent { Snap2ReadScreen() }
    }

    @Composable
    private fun Snap2ReadScreen() {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📸 snap2read", fontSize = 22.sp)
            Text(
                when {
                    !uiState.cameraReady -> "카메라 권한 필요"
                    uiState.busy -> uiState.stage
                    else -> "페이지를 찍으면 읽어줍니다"
                },
                fontSize = 13.sp, color = MaterialTheme.colorScheme.primary
            )

            // 1) 촬영 버튼 (CameraX 프리뷰는 다음 단계에서 추가 — 지금은 시스템 카메라 인텐트로 MVP)
            Button(onClick = { launchCamera() }, enabled = uiState.cameraReady && !uiState.busy) {
                Text("📷 사진 찍기 / 선택")
            }

            // 2) OCR 결과 표시 + 편집
            if (uiState.text.isNotEmpty()) {
                OutlinedTextField(
                    value = uiState.text,
                    onValueChange = { uiState = uiState.copy(text = it) },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    label = { Text("인식된 텍스트 (수정 가능)") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { speak(uiState.text) }, enabled = uiState.ttsReady) {
                        Text("🔊 읽어주기")
                    }
                    Button(
                        onClick = { translate(uiState.text) },
                        enabled = uiState.llmReady && !uiState.busy
                    ) { Text("🌐 번역") }
                }
            }

            // 3) 번역 결과
            if (uiState.translation.isNotEmpty()) {
                Text("번역:", fontSize = 13.sp)
                Text(uiState.translation, fontSize = 15.sp)
            }

            // 4) 오류/상태
            uiState.error?.let { Text("⚠️ $it", color = MaterialTheme.colorScheme.error) }
            if (!uiState.llmReady && uiState.llmReason.isNotEmpty()) {
                Text("LLM: ${uiState.llmReason}", fontSize = 12.sp)
            }
        }
    }

    private fun launchCamera() {
        // MVP: 시스템 카메라 앱으로 촬영 → 결과 이미지로 OCR (CameraX 인앱 프리뷰는 후속 단계)
        val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, 1001)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 1001 || resultCode != RESULT_OK) return
        val bitmap = data?.extras?.get("data") as? android.graphics.Bitmap ?: return
        uiState = uiState.copy(busy = true, stage = "글자 인식 중…", error = null)
        ocr.recognize(bitmap) { text ->
            uiState = uiState.copy(busy = false, text = text, stage = "")
            if (text.isEmpty()) {
                uiState = uiState.copy(error = "글자를 인식하지 못했습니다. 더 가깝고 밝게 찍어보세요.")
            }
        }
    }

    private fun speak(text: String) {
        if (text.isBlank()) return
        tts.speak(text)
    }

    private fun translate(text: String) {
        if (!uiState.llmReady) return
        uiState = uiState.copy(busy = true, stage = "번역 중…", translation = "")
        llm.translateStream(text) { partial, done, error ->
            if (error != null) {
                uiState = uiState.copy(busy = false, error = error)
            } else {
                uiState = uiState.copy(translation = partial, busy = !done)
            }
        }
    }
}

data class UiState(
    val cameraReady: Boolean = false,
    val ttsReady: Boolean = false,
    val llmReady: Boolean = false,
    val llmReason: String = "",
    val busy: Boolean = false,
    val stage: String = "",
    val text: String = "",
    val translation: String = "",
    val error: String? = null
)
