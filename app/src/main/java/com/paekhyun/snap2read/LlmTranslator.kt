package com.paekhyun.snap2read

import android.content.Context
import java.io.File

/**
 * 온디바이스 LLM 번역 — LiteRT-LM (Gemma 4 E4B).
 *
 * 모델 파일: app/src/main/assets/models/
 * 파일이 없으면 번역 기능만 비활성 (OCR·읽어주기는 정상 동작).
 *
 * ⚠️ LiteRT-LM API는 버전에 따라 시그니처가 다릅니다. 이 MVP는 구조와 흐름을
 *    보여주는 스켈레톤이며, 실제 API 연결은 안드로이드 스튜디오에서
 *    litertlm-android 최신 버전의 공식 예제(Google AI Edge)를 참고해 붙이세요.
 *    TODO로 표시된 부분이 그 지점입니다.
 */
class LlmTranslator(private val context: Context) {

    private var ready = false
    private var reason = "초기화 중…"

    fun init(onReady: (Boolean, String) -> Unit) {
        val modelFile = try {
            copyModelFromAssets()
        } catch (e: Exception) {
            onReady(false, "모델 파일 없음: ${e.message}")
            return
        }
        if (modelFile == null || !modelFile.exists()) {
            onReady(false, " 없음 — README 참고해 assets에 넣어 빌드")
            return
        }
        try {
            // TODO: LiteRT-LM 공식 API로 엔진 초기화
            //   val engine = GenAI.create(context, modelFile.absolutePath, ...)
            ready = true
            reason = ""
            onReady(true, "")
        } catch (e: Exception) {
            ready = false
            reason = "LLM 로드 실패: ${e.message}"
            onReady(false, reason)
        }
    }

    /** 스트리밍 번역 — partial로 실시간 표시, done=true로 종료, error로 실패. */
    fun translateStream(text: String, onUpdate: (partial: String, done: Boolean, error: String?) -> Unit) {
        if (!ready) {
            onUpdate("", true, "LLM이 준비되지 않았습니다: $reason")
            return
        }
        val prompt = buildString {
            append("/no_think 다음 텍스트를 한국어로 번역하세요. ")
            append("번역문만 출력하고 설명·사고과정은 출력하지 마세요.\n\n")
            append(text)
            append("\n\n한국어 번역:")
        }
        Thread {
            var acc = ""
            try {
                // TODO: LiteRT-LM 스트리밍 생성 호출
                //   engine.generate(prompt) { token -> acc += token; onUpdate(acc, false, null) }
                acc = "(LiteRT-LM 연결 필요 — TODO 참고)"
                onUpdate(acc, true, null)
            } catch (e: Exception) {
                onUpdate(acc, true, "번역 실패: ${e.message}")
            }
        }.start()
    }

    /** assets에서 모델 파일을 캐시 디렉터리로 복사 (LiteRT-LM은 파일 경로를 요구). */
    private fun copyModelFromAssets(): File? {
        val name = "models/"
        return try {
            context.assets.open(name).use { input ->
                val out = File(context.cacheDir, "")
                if (!out.exists() || out.length() != input.available().toLong()) {
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                out
            }
        } catch (e: Exception) {
            null // assets에 파일이 없는 경우
        }
    }
}
