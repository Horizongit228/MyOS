package com.myos.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myos.app.data.*
import kotlinx.coroutines.launch
import java.io.File

private val blue = Color(0xFF777BF2)
private val background = Color(0xFFF8FAFC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOsApp(store: LocalStore, secrets: SecureKeyStore) {
    var page by rememberSaveable { mutableStateOf(0) }
    var token by remember { mutableStateOf(secrets.load()) }
    var textModel by remember { mutableStateOf(store.textModel) }
    var speechModel by remember { mutableStateOf(store.speechModel) }
    val notes = remember { mutableStateListOf<Note>().apply { addAll(store.notes()) } }
    val tasks = remember { mutableStateListOf<Task>().apply { addAll(store.tasks()) } }
    val messages = remember { mutableStateListOf<Chat>().apply { addAll(store.chats()) } }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val labels = listOf("Главная", "ИИ", "Заметки", "Задачи", "Настройки")
    val navPages = listOf(0, 1, 3, 4)
    val navLabels = listOf("Главная", "ИИ", "Задачи", "Профиль")
    val navSymbols = listOf("⌂", "✦", "☑", "♙")
    var starterPrompt by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = blue, background = background, surface = Color.White,
        onSurface = Color(0xFF172033), surfaceVariant = Color(0xFFF1F5F9)
    )) {
        Scaffold(
            containerColor = if (page == 0) Color(0xFF172C50) else background,
            topBar = {
                if (page != 0) TopAppBar(
                    title = { Text(if (page == 0) "MyOS" else labels[page], fontWeight = FontWeight.Bold) },
                    actions = {
                        if (page == 1 && messages.isNotEmpty()) {
                            TextButton(onClick = { messages.clear(); store.saveChats(messages); error = "" },
                                enabled = !sending) { Text("Очистить") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = background)
                )
            },
            bottomBar = {
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (page == 0) Color(0xFF172C50) else background)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFF445A8C), Color(0xFF596FAD), Color(0xFF3A568C))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(26.dp))
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navPages.forEachIndexed { i, target ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                                .clickable { page = target }.padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(navSymbols[i], color = if (page == target) Color(0xFFB9ACFF) else Color(0xFFC9D5F2),
                                fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text(navLabels[i], color = if (page == target) Color.White else Color(0xFFD7DEEE),
                                fontSize = 11.sp, fontWeight = if (page == target) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        ) { insets ->
            Box(Modifier.fillMaxSize().padding(insets)) {
                when (page) {
                    0 -> MyOsDashboard(notes.size, tasks, onOpen = { page = it }, onAiAction = { starterPrompt = it; page = 1 })
                    1 -> ChatUi(
                        messages, token.isNotBlank(), sending, error,
                        textModel, speechModel, token,
                        prefill = starterPrompt,
                        onOpenSettings = { page = 4 },
                        onSend = { input ->
                            if (token.isNotBlank() && input.isNotBlank() && !sending) {
                                messages.add(Chat(role = "user", content = input.trim()))
                                store.saveChats(messages)
                                error = ""
                                sending = true
                                scope.launch {
                                    try {
                                        val answer = GroqClient.chat(token, textModel, messages.toList())
                                        messages.add(Chat(role = "assistant", content = answer))
                                        store.saveChats(messages)
                                    } catch (e: Exception) {
                                        error = e.localizedMessage ?: "Не удалось связаться с Groq."
                                    } finally { sending = false }
                                }
                            }
                        }
                    )
                    2 -> NotesUi(notes, onSave = { note ->
                        val pos = notes.indexOfFirst { it.id == note.id }
                        if (pos >= 0) notes[pos] = note else notes.add(0, note)
                        store.saveNotes(notes)
                    }, onDelete = { id ->
                        notes.removeAll { it.id == id }
                        store.saveNotes(notes)
                    })
                    3 -> TasksUi(tasks, onAdd = {
                        tasks.add(0, Task(title = it)); store.saveTasks(tasks)
                    }, onToggle = { id ->
                        val pos = tasks.indexOfFirst { it.id == id }
                        if (pos >= 0) { tasks[pos] = tasks[pos].copy(done = !tasks[pos].done); store.saveTasks(tasks) }
                    }, onDelete = { id ->
                        tasks.removeAll { it.id == id }; store.saveTasks(tasks)
                    })
                    4 -> SettingsUi(token.isNotBlank(), textModel, speechModel,
                        onKey = { newKey ->
                            runCatching { secrets.save(newKey); token = newKey.trim() }
                                .exceptionOrNull()?.localizedMessage
                        },
                        onTextModel = { textModel = it; store.textModel = it },
                        onSpeechModel = { speechModel = it; store.speechModel = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeUi(notes: Int, tasks: Int, onOpen: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Добро пожаловать 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Простое место для ваших дел и ИИ", color = Color.Gray)
        HomeCard("✦", "MyAI", "GPT-OSS 120B + Whisper Large V3", 1, onOpen)
        HomeCard("▤", "Заметки", "Заметок: " + notes, 2, onOpen)
        HomeCard("✓", "Задачи", "Осталось выполнить: " + tasks, 3, onOpen)
        HomeCard("⚙", "Настройки", "Groq API и выбор моделей", 4, onOpen)
        Text("MyOS 1.1 · светлый дизайн", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HomeCard(icon: String, title: String, subtitle: String, page: Int, onOpen: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(page) },
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(icon, color = blue, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = Color.Gray)
        }
    }
}

@Composable
private fun ChatUi(
    messages: List<Chat>, hasKey: Boolean, sending: Boolean, error: String,
    textModel: String, speechModel: String, key: String,
    prefill: String,
    onOpenSettings: () -> Unit, onSend: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(prefill) { if (prefill.isNotBlank()) draft = prefill }
    var recording by remember { mutableStateOf(false) }
    var processingAudio by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf("") }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    fun startRecording() {
        try {
            val file = File.createTempFile("myos-voice-", ".m4a", context.cacheDir)
            val deviceRecorder = MediaRecorder()
            deviceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            deviceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            deviceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            deviceRecorder.setOutputFile(file.absolutePath)
            deviceRecorder.prepare()
            deviceRecorder.start()
            audioFile = file
            recorder = deviceRecorder
            recording = true
            voiceError = ""
        } catch (e: Exception) {
            runCatching { recorder?.release() }
            recorder = null
            audioFile?.delete()
            audioFile = null
            recording = false
            voiceError = "Ошибка микрофона: " + (e.localizedMessage ?: "неизвестная ошибка")
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) startRecording() else voiceError = "Для голосового ввода нужно разрешение на микрофон."
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { recorder?.release() }; audioFile?.delete() }
    }
    Column(Modifier.fillMaxSize()) {
        if (!hasKey) {
            Card(Modifier.padding(16.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Подключите Groq", fontWeight = FontWeight.Bold)
                    Text("Введите личный API-ключ в настройках.")
                    Button(onClick = onOpenSettings) { Text("Открыть настройки") }
                }
            }
        }
        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✦ MyAI", style = MaterialTheme.typography.headlineMedium, color = blue)
                    Text(textModel, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("Напишите вопрос или используйте микрофон", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(messages, key = { it.id }) { msg ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (msg.role == "user") blue else Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            Text(msg.content, Modifier.padding(14.dp),
                                color = if (msg.role == "user") Color.White else Color.Black)
                        }
                    }
                }
                if (sending) item { Text("MyAI отвечает…", color = Color.Gray) }
            }
        }
        if (error.isNotBlank()) Text(error, Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
        if (voiceError.isNotBlank()) Text(voiceError, Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        if (processingAudio) LinearProgressIndicator(Modifier.fillMaxWidth())
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().background(Color.White).padding(10.dp),
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it },
                modifier = Modifier.weight(1f), enabled = hasKey && !sending,
                placeholder = { Text("Сообщение...") }, maxLines = 4,
                shape = RoundedCornerShape(14.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    enabled = hasKey && !processingAudio,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    onClick = {
                        if (!recording) {
                            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                                startRecording()
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            val captured = audioFile
                            try {
                                recorder?.stop()
                                recorder?.release()
                                recorder = null
                                recording = false
                                if (captured != null) {
                                    processingAudio = true
                                    scope.launch {
                                        try {
                                            val transcript = GroqClient.transcribe(key, speechModel, captured)
                                            draft = if (draft.isBlank()) transcript else draft.trim() + " " + transcript
                                            voiceError = ""
                                        } catch (e: Exception) {
                                            voiceError = e.localizedMessage ?: "Ошибка распознавания речи"
                                        } finally { processingAudio = false; captured.delete(); audioFile = null }
                                    }
                                }
                            } catch (e: Exception) {
                                recorder?.release()
                                recorder = null
                                recording = false
                                captured?.delete()
                                audioFile = null
                                voiceError = "Не удалось завершить запись."
                            }
                        }
                    }
                ) { Text(if (recording) "■ Стоп" else "🎙") }
                Button(onClick = { val text = draft.trim(); if (text.isNotEmpty()) { onSend(text); draft = "" } },
                    enabled = hasKey && draft.isNotBlank() && !sending,
                    contentPadding = PaddingValues(horizontal = 10.dp)) { Text("→") }
            }
        }
    }
}

@Composable
private fun NotesUi(notes: List<Note>, onSave: (Note) -> Unit, onDelete: (String) -> Unit) {
    var edit by remember { mutableStateOf<Note?>(null) }
    var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { edit = null; show = true }, Modifier.fillMaxWidth()) { Text("+ Новая заметка") }
        if (notes.isEmpty()) Text("Заметок пока нет", color = Color.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes, key = { it.id }) { note ->
                Card(Modifier.fillMaxWidth().clickable { edit = note; show = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(note.title, fontWeight = FontWeight.Bold)
                        if (note.body.isNotBlank()) Text(note.body, maxLines = 3, color = Color.Gray)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onDelete(note.id) }) { Text("Удалить") }
                            TextButton(onClick = { edit = note; show = true }) { Text("Изменить") }
                        }
                    }
                }
            }
        }
    }
    if (show) {
        var title by remember(edit?.id) { mutableStateOf(edit?.title.orEmpty()) }
        var body by remember(edit?.id) { mutableStateOf(edit?.body.orEmpty()) }
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(if (edit == null) "Новая заметка" else "Редактирование") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true)
                    OutlinedTextField(body, { body = it }, label = { Text("Текст") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSave(edit?.copy(title = title.trim(), body = body.trim())
                        ?: Note(title = title.trim(), body = body.trim()))
                    show = false
                }, enabled = title.isNotBlank()) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun TasksUi(tasks: List<Task>, onAdd: (String) -> Unit, onToggle: (String) -> Unit, onDelete: (String) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(draft, { draft = it }, Modifier.weight(1f),
                label = { Text("Новая задача") }, singleLine = true)
            Button(onClick = { if (draft.isNotBlank()) { onAdd(draft.trim()); draft = "" } },
                enabled = draft.isNotBlank()) { Text("+") }
        }
        if (tasks.isEmpty()) Text("Задач пока нет", color = Color.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.id }) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(task.done, onCheckedChange = { onToggle(task.id) })
                        Text(task.title, Modifier.weight(1f),
                            textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None)
                        TextButton(onClick = { onDelete(task.id) }) { Text("✕") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsUi(
    hasKey: Boolean, textModel: String, speechModel: String,
    onKey: (String) -> String?, onTextModel: (String) -> Unit, onSpeechModel: (String) -> Unit
) {
    var entered by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Groq API", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (hasKey) "Ключ сохранён ✓" else "Ключ не добавлен",
            color = if (hasKey) Color(0xFF15803D) else Color.Gray)
        OutlinedTextField(entered, { entered = it }, Modifier.fillMaxWidth(),
            label = { Text("API-ключ") }, singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "Скрыть" else "Показать")
            } }
        )
        Button(onClick = {
            val error = onKey(entered)
            result = error ?: "Ключ успешно сохранён."
            if (error == null) entered = ""
        }, enabled = entered.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Сохранить ключ") }
        if (hasKey) OutlinedButton(onClick = {
            result = onKey("") ?: "Ключ удалён."
        }, modifier = Modifier.fillMaxWidth()) { Text("Удалить ключ") }
        if (result.isNotBlank()) Text(result, color = blue)
        HorizontalDivider()
        Text("Модель чата", fontWeight = FontWeight.SemiBold)
        GroqModelPicker(textModel, GroqClient.textModels, onTextModel)
        Text("Распознавание голоса", fontWeight = FontWeight.SemiBold)
        GroqModelPicker(speechModel, GroqClient.speechModels, onSpeechModel)
        HorizontalDivider()
        Text("MyOS 1.1.0", fontWeight = FontWeight.Bold)
        Text("Светлый интерфейс · GPT-OSS · Whisper", color = Color.Gray)
        Text("Заметки и задачи — офлайн. Чат и распознавание речи — через Groq API.",
            color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        Text("Ключ шифруется на устройстве. Аудио отправляется на Groq только после записи.",
            color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GroqModelPicker(selected: String, choices: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected, modifier = Modifier.weight(1f))
            Text("⌄")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(text = { Text(choice) }, onClick = {
                    onSelect(choice); expanded = false
                })
            }
        }
    }
}
