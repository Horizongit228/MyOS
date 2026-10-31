package com.myos.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myos.app.data.*
import kotlinx.coroutines.launch
import java.io.File

private val blue = Color(0xFF777BF2)
private val background = Color(0xFFF8FAFC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOsApp(store:LocalStore,secrets:SecureKeyStore){
    val context=LocalContext.current
    var page by rememberSaveable{mutableStateOf(0)}
    var token by remember{mutableStateOf(secrets.load())}
    var textModel by remember{mutableStateOf(store.textModel)}
    var speechModel by remember{mutableStateOf(store.speechModel)}
    var style by remember{mutableStateOf(store.loadStyle())}
    val palette=style.palette()
    val notes=remember{mutableStateListOf<Note>().apply{addAll(store.notes())}}
    val tasks=remember{mutableStateListOf<Task>().apply{addAll(store.tasks())}}
    val messages=remember{mutableStateListOf<Chat>().apply{addAll(store.chats())}}
    var sending by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf("")}
    var starterPrompt by remember{mutableStateOf("")}
    var pendingApp by remember{mutableStateOf<PhoneApp?>(null)}
    val apps=remember(page){PhoneApps.discover(context)}
    val scope=rememberCoroutineScope()
    val pages=listOf(0,1,5,4)
    val symbols=listOf("⌂","✦","▦","⚙")
    val names=listOf("Главная","ИИ","Приложения","Настройки")
    val titles=listOf("Главная","MyAI","Заметки","Задачи","Настройки","Приложения")
    val colors=if(style.paletteId==3)lightColorScheme(
        primary=palette.primary,background=palette.bgBottom,surface=Color.White,
        onSurface=Color(0xFF213051),onBackground=Color(0xFF213051))
    else darkColorScheme(primary=palette.primary,background=palette.bgBottom,
        surface=Color(0xFF324666),onSurface=palette.text,onBackground=palette.text)
    MaterialTheme(colorScheme=colors){
        Scaffold(
            containerColor=palette.bgBottom,
            topBar={
                if(page!=0)TopAppBar(
                    title={Text(titles.getOrElse(page){"MyOS"},fontWeight=FontWeight.Bold)},
                    actions={
                        if(page==1&&messages.isNotEmpty())TextButton(onClick={
                            messages.clear();store.saveChats(messages);error=""
                        },enabled=!sending){Text("Очистить")}
                    },
                    colors=TopAppBarDefaults.topAppBarColors(containerColor=palette.bgTop)
                )
            },
            bottomBar={
                Row(Modifier.fillMaxWidth().background(palette.bgBottom)
                    .padding(horizontal=12.dp,vertical=7.dp)
                    .clip(RoundedCornerShape(style.radius.dp))
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(palette.surface.copy(alpha=.95f),palette.primary.copy(alpha=.48f))))
                    .border(1.dp,palette.text.copy(alpha=.20f),RoundedCornerShape(style.radius.dp))
                    .padding(vertical=7.dp),
                    verticalAlignment=Alignment.CenterVertically){
                    pages.forEachIndexed{i,target->
                        Column(Modifier.weight(1f).clickable{page=target}
                            .padding(vertical=5.dp),
                            horizontalAlignment=Alignment.CenterHorizontally){
                            Text(symbols[i],fontSize=23.sp,
                                color=if(page==target)palette.primary else palette.text.copy(alpha=.75f))
                            Text(names[i],fontSize=11.sp,
                                color=if(page==target)palette.text else palette.muted,maxLines=1)
                        }
                    }
                }
            }
        ){insets->
            Box(Modifier.fillMaxSize().background(palette.bgBottom).padding(insets)){
                when(page){
                    0->MyOsDashboard(notes.size,tasks,style,apps,onOpen={page=it},
                        onAiAction={starterPrompt=it;page=1},
                        onApp={PhoneApps.open(context,it)})
                    1->ChatUi(messages,token.isNotBlank(),sending,error,textModel,speechModel,token,
                        prefill=starterPrompt,onOpenSettings={page=4},
                        onSend={input->
                            if(input.isNotBlank()&&!sending){
                                val message=input.trim()
                                val requested=PhoneApps.requestedName(message)
                                if(requested!=null){
                                    messages.add(Chat(role="user",content=message))
                                    val app=PhoneApps.match(apps,requested)
                                    if(app!=null){
                                        messages.add(Chat(role="assistant",content="Найдено "+app.label+". Подтвердите запуск приложения."))
                                        pendingApp=app
                                    }else{
                                        messages.add(Chat(role="assistant",content="Не нашёл приложение «"+requested+"» на телефоне. Откройте раздел «Приложения»."))
                                    }
                                    store.saveChats(messages)
                                    starterPrompt=""
                                }else if(token.isBlank()){
                                    error="Для общения с Groq добавьте ключ в настройках. Команды «открой ...» работают без ключа."
                                }else{
                                    messages.add(Chat(role="user",content=message))
                                    store.saveChats(messages)
                                    error=""
                                    starterPrompt=""
                                    sending=true
                                    scope.launch{
                                        try{
                                            val answer=GroqClient.chat(token,textModel,messages.toList())
                                            messages.add(Chat(role="assistant",content=answer))
                                            store.saveChats(messages)
                                        }catch(e:Exception){error=e.localizedMessage?:"Ошибка подключения к Groq."}
                                        finally{sending=false}
                                    }
                                }
                            }
                        })
                    2->NotesUi(notes,onSave={note->
                        val pos=notes.indexOfFirst{it.id==note.id}
                        if(pos>=0)notes[pos]=note else notes.add(0,note)
                        store.saveNotes(notes)
                    },onDelete={id->notes.removeAll{it.id==id};store.saveNotes(notes)})
                    3->TasksUi(tasks,onAdd={tasks.add(0,Task(title=it));store.saveTasks(tasks)},
                        onToggle={id->
                            val pos=tasks.indexOfFirst{it.id==id}
                            if(pos>=0){tasks[pos]=tasks[pos].copy(done=!tasks[pos].done);store.saveTasks(tasks)}
                        },onDelete={id->tasks.removeAll{it.id==id};store.saveTasks(tasks)})
                    4->SettingsUi(token.isNotBlank(),textModel,speechModel,style,
                        onStyle={newStyle->style=newStyle;store.saveStyle(newStyle)},
                        onKey={key->
                            runCatching{secrets.save(key);token=key.trim()}.exceptionOrNull()?.localizedMessage
                        },
                        onTextModel={textModel=it;store.textModel=it},
                        onSpeechModel={speechModel=it;store.speechModel=it})
                    5->PhoneAppsScreen(style,apps){PhoneApps.open(context,it)}
                }
            }
        }
        if(pendingApp!=null){
            val app=pendingApp!!
            AlertDialog(onDismissRequest={pendingApp=null},
                title={Text("Открыть приложение?")},
                text={Text("MyAI предлагает открыть «"+app.label+"». Запустить?")},
                confirmButton={
                    Button(onClick={
                        val ok=PhoneApps.open(context,app)
                        messages.add(Chat(role="assistant",content=if(ok)"Открываю "+app.label+"." else "Не удалось открыть "+app.label+"."))
                        store.saveChats(messages)
                        pendingApp=null
                    }){Text("Открыть")}
                },
                dismissButton={TextButton(onClick={pendingApp=null}){Text("Отмена")}})
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
                Column(Modifier.padding(horizontal=24.dp),horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✦ MyAI", style = MaterialTheme.typography.headlineMedium, color = blue)
                    Text(textModel, textAlign=TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.6f))
                    Text("Напишите вопрос или используйте микрофон", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.7f))
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
                            color = if (msg.role == "user") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            Text(msg.content, Modifier.padding(14.dp),
                                color = if (msg.role == "user") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
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
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha=.38f)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(), enabled = !sending,
                placeholder = { Text("Сообщение...") }, maxLines = 4,
                shape = RoundedCornerShape(14.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(Modifier.width(10.dp))
                Button(onClick = { val text = draft.trim(); if (text.isNotEmpty()) { onSend(text); draft = "" } },
                    enabled = draft.isNotBlank() && !sending,
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
    hasKey:Boolean,textModel:String,speechModel:String,style:UserStyle,
    onStyle:(UserStyle)->Unit,onKey:(String)->String?,
    onTextModel:(String)->Unit,onSpeechModel:(String)->Unit
){
    var entered by remember{mutableStateOf("")}
    var visible by remember{mutableStateOf(false)}
    var result by remember{mutableStateOf("")}
    val p=style.palette()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)){
        FrostedPanel(style,Modifier.fillMaxWidth()){
            Text("Оформление",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge,color=p.text)
            Spacer(Modifier.height(6.dp))
            Text("Все изменения видны сразу и сохраняются на устройстве.",color=p.muted)
            Spacer(Modifier.height(14.dp))
            Text("Цветовая тема",color=p.text,fontWeight=FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            val palettes=listOf("Ночь","Океан","Фиолетовый","Светлая")
            palettes.chunked(2).forEachIndexed{row,items->
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    items.forEachIndexed{col,name->
                        val idx=row*2+col
                        if(style.paletteId==idx)Button(onClick={onStyle(style.copy(paletteId=idx))},
                            modifier=Modifier.weight(1f)){Text(name)}
                        else OutlinedButton(onClick={onStyle(style.copy(paletteId=idx))},
                            modifier=Modifier.weight(1f)){Text(name)}
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Прозрачность карточек · "+(style.opacity*100).toInt()+"%",color=p.text)
            Slider(value=style.opacity,onValueChange={onStyle(style.copy(opacity=it))},
                valueRange=.30f..98f/100f)
            Text("Скругление · "+style.radius+" dp",color=p.text)
            Slider(value=style.radius.toFloat(),onValueChange={
                onStyle(style.copy(radius=it.toInt()))
            },valueRange=10f..36f,steps=25)
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("Компактное отображение",modifier=Modifier.weight(1f),color=p.text)
                Switch(checked=style.compact,onCheckedChange={onStyle(style.copy(compact=it))})
            }
            OutlinedButton(onClick={onStyle(UserStyle())},modifier=Modifier.fillMaxWidth()){
                Text("Сбросить оформление")
            }
        }
        FrostedPanel(style,Modifier.fillMaxWidth()){
            Text("Groq API",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge,color=p.text)
            Text(if(hasKey)"Ключ сохранён ✓" else "Ключ не добавлен",color=p.muted)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(entered,{entered=it},Modifier.fillMaxWidth(),
                label={Text("API-ключ")},singleLine=true,
                visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon={TextButton(onClick={visible=!visible}){Text(if(visible)"Скрыть" else "Показать")}})
            Spacer(Modifier.height(8.dp))
            Button(onClick={
                val err=onKey(entered)
                result=err?:"Ключ успешно сохранён."
                if(err==null)entered=""
            },enabled=entered.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Сохранить ключ")}
            if(hasKey)OutlinedButton(onClick={result=onKey("")?:"Ключ удалён."},
                modifier=Modifier.fillMaxWidth()){Text("Удалить ключ")}
            if(result.isNotBlank())Text(result,color=p.muted)
            Spacer(Modifier.height(12.dp))
            Text("Модель чата",color=p.text,fontWeight=FontWeight.Medium)
            GroqModelPicker(textModel,GroqClient.textModels,onTextModel)
            Text("Распознавание голоса",color=p.text,fontWeight=FontWeight.Medium)
            GroqModelPicker(speechModel,GroqClient.speechModels,onSpeechModel)
        }
        FrostedPanel(style,Modifier.fillMaxWidth()){
            Text("Запуск приложений через MyAI",fontWeight=FontWeight.Bold,color=p.text)
            Text("Например, напишите: «Открой Telegram». MyOS ищет программу на телефоне и спрашивает подтверждение перед запуском. Список приложений не отправляется в Groq.",color=p.muted)
            Spacer(Modifier.height(8.dp))
            Text("MyOS 1.3 · GPT-OSS 120B · Whisper Large V3",color=p.muted)
        }
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
