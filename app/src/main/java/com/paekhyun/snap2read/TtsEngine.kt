package com.paekhyun.snap2read

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Android 시스템 TTS 래퍼 — 한국어/영어 읽어주기 (오프라인, 시스템 음성 데이터 사용). */
class TtsEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    fun init(onReady: (Boolean) -> Unit) {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                // 언어는 speak()에서 설정 — 여기선 기본값 확인만
                onReady(true)
            } else {
                onReady(false)
            }
        }
    }

    fun speak(text: String) {
        val t = tts ?: return
        if (!ready) return
        // 한국어가 포함되면 한국어 음성, 아니면 영어 음성
        val hasHangul = text.any { it in '\uAC00'..'\uD7A3' }
        val locale = if (hasHangul) Locale.KOREAN else Locale.US
        t.language = locale
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "snap2read")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
