package com.paekhyun.snap2read

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** ML Kit 온디바이스 OCR 래퍼 — 한국어+라틴 인식 (완전 오프라인). */
class OcrEngine(private val context: Context) {

    private val recognizer by lazy {
        // 한국어 옵션은 라틴도 함께 인식 (ML Kit v2 다국어 모델)
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    /** 비트맵 → 텍스트. 콜백은 메인 스레드에서 호출됨. */
    fun recognize(bitmap: Bitmap, onResult: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                onResult(result.text.trim())
            }
            .addOnFailureListener {
                onResult("")
            }
    }
}
