package com.myos.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Note(val id: String = UUID.randomUUID().toString(), val title: String, val body: String)
data class Task(val id: String = UUID.randomUUID().toString(), val title: String, val done: Boolean = false)
data class Chat(val id: String = UUID.randomUUID().toString(), val role: String, val content: String)

data class UserStyle(val paletteId:Int=0,val radius:Int=24,val opacity:Float=0.66f,val compact:Boolean=false)

class LocalStore(context: Context) {
    private val p = context.getSharedPreferences("myos_local", Context.MODE_PRIVATE)
    fun loadStyle():UserStyle=UserStyle(
        paletteId=p.getInt("palette_id",0).coerceIn(0,3),
        radius=p.getInt("card_radius",24).coerceIn(10,36),
        opacity=p.getFloat("card_opacity",0.66f).coerceIn(.30f,.98f),
        compact=p.getBoolean("compact_ui",false)
    )
    fun saveStyle(s:UserStyle){
        p.edit().putInt("palette_id",s.paletteId).putInt("card_radius",s.radius)
            .putFloat("card_opacity",s.opacity).putBoolean("compact_ui",s.compact).apply()
    }
    var textModel: String
        get() = p.getString("text_model", "openai/gpt-oss-120b") ?: "openai/gpt-oss-120b"
        set(value) { p.edit().putString("text_model", value).apply() }
    var speechModel: String
        get() = p.getString("speech_model", "whisper-large-v3") ?: "whisper-large-v3"
        set(value) { p.edit().putString("speech_model", value).apply() }
    private fun array(key: String): JSONArray =
        runCatching { JSONArray(p.getString(key, "[]") ?: "[]") }.getOrDefault(JSONArray())
    private fun save(key: String, items: List<JSONObject>) {
        val a = JSONArray()
        items.forEach(a::put)
        p.edit().putString(key, a.toString()).apply()
    }
    fun notes(): List<Note> {
        val a = array("notes")
        return (0 until a.length()).mapNotNull { i ->
            a.optJSONObject(i)?.let { Note(it.optString("id"), it.optString("title"), it.optString("body")) }
        }
    }
    fun saveNotes(items: List<Note>) = save("notes", items.map {
        JSONObject().put("id", it.id).put("title", it.title).put("body", it.body)
    })
    fun tasks(): List<Task> {
        val a = array("tasks")
        return (0 until a.length()).mapNotNull { i ->
            a.optJSONObject(i)?.let { Task(it.optString("id"), it.optString("title"), it.optBoolean("done")) }
        }
    }
    fun saveTasks(items: List<Task>) = save("tasks", items.map {
        JSONObject().put("id", it.id).put("title", it.title).put("done", it.done)
    })
    fun chats(): List<Chat> {
        val a = array("chat")
        return (0 until a.length()).mapNotNull { i ->
            a.optJSONObject(i)?.let { Chat(it.optString("id"), it.optString("role"), it.optString("content")) }
        }.takeLast(100)
    }
    fun saveChats(items: List<Chat>) = save("chat", items.takeLast(100).map {
        JSONObject().put("id", it.id).put("role", it.role).put("content", it.content)
    })
}
