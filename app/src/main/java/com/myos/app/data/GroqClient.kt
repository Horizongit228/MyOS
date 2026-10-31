package com.myos.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object GroqClient {
    const val CHAT_DEFAULT = "openai/gpt-oss-120b"
    const val SPEECH_DEFAULT = "whisper-large-v3"
    val textModels = listOf(CHAT_DEFAULT, "openai/gpt-oss-20b", "qwen/qwen3.8-27b")
    val speechModels = listOf(SPEECH_DEFAULT, "whisper-large-v3-turbo")

    private fun connect(path: String, key: String): HttpURLConnection =
        (URL("https://api.groq.com/openai/v1/" + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 90000
            doOutput = true
            setRequestProperty("Authorization", "Bearer " + key.trim())
        }

    private fun result(c: HttpURLConnection): String {
        val status = c.responseCode
        val source = if (status in 200..299) c.inputStream else c.errorStream
        val body = source?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            val reason = when(status) {
                401, 403 -> "Недействительный ключ Groq или нет доступа к модели."
                404 -> "Модель не найдена. Измените её в настройках."
                413 -> "Аудиофайл слишком большой."
                429 -> "Лимит Groq исчерпан. Попробуйте позднее."
                else -> "Ошибка Groq HTTP " + status
            }
            throw IllegalStateException(reason + " " + JSONObject(body.ifBlank { "{}" }).optJSONObject("error")?.optString("message").orEmpty().take(160))
        }
        return body
    }

    suspend fun chat(key: String, model: String, history: List<Chat>): String = withContext(Dispatchers.IO) {
        require(key.isNotBlank()) { "Сначала сохраните Groq API-ключ в настройках." }
        val items = JSONArray().put(JSONObject().put("role", "system")
            .put("content", "Ты MyAI, полезный русскоязычный помощник внутри MyOS. Отвечай понятно и точно."))
        history.takeLast(16).forEach {
            items.put(JSONObject().put("role", it.role).put("content", it.content))
        }
        val payload = JSONObject()
            .put("model", model)
            .put("messages", items)
            .put("max_completion_tokens", 2400)
            .put("stream", false)
        if (model.startsWith("openai/gpt-oss-")) {
            payload.put("reasoning_effort", "medium").put("include_reasoning", false)
        }
        val c = connect("chat/completions", key)
        try {
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val answer = JSONObject(result(c)).getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").optString("content").trim()
            check(answer.isNotBlank()) { "Groq вернул пустой ответ." }
            answer
        } finally { c.disconnect() }
    }

    suspend fun transcribe(key: String, model: String, audio: File): String = withContext(Dispatchers.IO) {
        require(key.isNotBlank()) { "Сначала сохраните Groq API-ключ в настройках." }
        val boundary = "----MyOS" + UUID.randomUUID().toString().replace("-", "")
        val c = connect("audio/transcriptions", key)
        try {
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
            c.outputStream.use { out ->
                fun write(s: String) { out.write(s.toByteArray(Charsets.UTF_8)) }
                write("--" + boundary + "\r\n")
                write("Content-Disposition: form-data; name=\"model\"\r\n\r\n")
                write(model + "\r\n")
                write("--" + boundary + "\r\n")
                write("Content-Disposition: form-data; name=\"file\"; filename=\"recording.m4a\"\r\n")
                write("Content-Type: audio/mp4\r\n\r\n")
                audio.inputStream().use { it.copyTo(out) }
                write("\r\n--" + boundary + "--\r\n")
            }
            JSONObject(result(c)).optString("text").trim().also {
                check(it.isNotBlank()) { "Речь не распознана." }
            }
        } finally { c.disconnect() }
    }
}
