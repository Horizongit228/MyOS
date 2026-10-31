package com.myos.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myos.app.data.Task
import java.time.LocalTime
import kotlin.math.min

private val ink = Color(0xFF102749)
private val white = Color(0xFFF9FCFF)
private val lavender = Color(0xFFB4B9FF)
private val glass = Color(0xFF526D9D).copy(alpha = 0.49f)

@Composable
fun MyOsDashboard(
    notesCount: Int,
    tasks: List<Task>,
    onOpen: (Int) -> Unit,
    onAiAction: (String) -> Unit
) {
    val ctx = LocalContext.current
    fun openExternal(type: String) {
        val intent = when(type) {
            "phone" -> Intent(Intent.ACTION_DIAL)
            "sms" -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
            "photos" -> Intent(Intent.ACTION_GET_CONTENT).apply { this.type = "image/*" }
            "camera" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            "maps" -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=nearby"))
            "settings" -> Intent(Settings.ACTION_SETTINGS)
            "weather" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather+near+me"))
            else -> return
        }
        runCatching { ctx.startActivity(intent) }.onFailure {
            Toast.makeText(ctx, "Приложение для этого действия не найдено", Toast.LENGTH_SHORT).show()
        }
    }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF132746), Color(0xFF213B65), Color(0xFF182C4C))))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF95B7FF).copy(alpha = .43f), Color.Transparent),
                    center = Offset(size.width * .68f, size.height * .12f),
                    radius = size.width * 1.1f
                ),
                radius = size.width * 1.1f, center = Offset(size.width * .68f, size.height * .12f)
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF6F4DCC).copy(alpha = .2f), Color.Transparent),
                    center = Offset(size.width * .12f, size.height * .67f),
                    radius = size.width * .8f
                ),
                radius = size.width * .8f, center = Offset(size.width * .12f, size.height * .67f)
            )
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp).padding(top = 15.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF8E8FF8), Color(0xFF66D3F8)))),
                    contentAlignment = Alignment.Center
                ) { Text("✦", fontSize = 20.sp, color = Color.White) }
                Spacer(Modifier.width(10.dp))
                Text("MyOS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = white)
                Spacer(Modifier.weight(1f))
                Text("Личный помощник", fontSize = 12.sp, color = Color(0xFFD7E5FF))
            }

            // Large frosted AI search bar, just like the reference.
            GlassPanel(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(1) },
                radius = 28.dp,
                colors = listOf(Color(0xFF7895CD).copy(alpha = .6f), Color(0xFF4D6AAA).copy(alpha = .52f)),
                padding = 10.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistantOrb(Modifier.size(48.dp))
                    Spacer(Modifier.width(11.dp))
                    Text("Спросите ИИ…", fontSize = 17.sp, color = Color(0xFFEDF1FF),
                        modifier = Modifier.weight(1f))
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = .19f))
                            .clickable { onOpen(1) },
                        contentAlignment = Alignment.Center
                    ) { Text("🎙", fontSize = 19.sp) }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = .16f))
                            .clickable { onAiAction("Помоги мне разобраться с фотографией или документом. Опиши, что можно сделать.") },
                        contentAlignment = Alignment.Center
                    ) { Text("▣", fontSize = 24.sp, color = white) }
                }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChip("✎", "Написать") { onAiAction("Помоги мне написать ") }
                ActionChip("⌕", "Найти") { onAiAction("Помоги мне найти информацию о ") }
                ActionChip("▤", "Суммаризовать") { onAiAction("Сделай краткое резюме следующего текста:\n") }
                ActionChip("✧", "Создать") { onAiAction("Придумай и создай ") }
            }

            HeroCard(onOpen = onOpen, onAiAction = onAiAction)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppShortcut("☎", "Телефон", listOf(Color(0xFF43DB9C), Color(0xFF0BAE73))) { openExternal("phone") }
                AppShortcut("●", "Сообщения", listOf(Color(0xFF3DB0FF), Color(0xFF155DDD))) { openExternal("sms") }
                AppShortcut("✿", "Фото", listOf(Color(0xFFFFC678), Color(0xFFDE78CF))) { openExternal("photos") }
                AppShortcut("◎", "Камера", listOf(Color(0xFFE6E9FF), Color(0xFFA8B6D6))) { openExternal("camera") }
                AppShortcut("➤", "Карты", listOf(Color(0xFF4CCC9E), Color(0xFF539AFF))) { openExternal("maps") }
                AppShortcut("⚙", "Настр.", listOf(Color(0xFFE5E7F6), Color(0xFF91A3C3))) { onOpen(4) }
            }

            Row(Modifier.fillMaxWidth().heightIn(min = 190.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPanel(
                    Modifier.weight(1f).height(203.dp).clickable { openExternal("weather") },
                    colors = listOf(Color(0xFF316CBA), Color(0xFF6383C4)),
                    padding = 14.dp
                ) {
                    Text("⌖  Погода", fontSize = 13.sp, color = white)
                    Spacer(Modifier.height(15.dp))
                    Text("☀", fontSize = 42.sp, color = Color(0xFFFFDC81))
                    Text("Посмотреть\nпрогноз", fontWeight = FontWeight.Bold,
                        fontSize = 19.sp, lineHeight = 23.sp, color = white)
                    Spacer(Modifier.weight(1f))
                    Text("Открыть прогноз  ›", fontSize = 11.sp, color = Color(0xFFE4ECFF))
                }
                GlassPanel(
                    Modifier.weight(1f).height(203.dp).clickable { onOpen(3) },
                    colors = listOf(Color(0xFFE2E8FF), Color(0xFFBBBFEF)),
                    padding = 13.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Мои задачи", fontWeight = FontWeight.Bold, color = ink, fontSize = 15.sp)
                        Spacer(Modifier.weight(1f))
                        Text("+", color = ink, fontSize = 22.sp)
                    }
                    Text("Активных: " + tasks.count { !it.done }, color = Color(0xFF50618D), fontSize = 11.sp)
                    Spacer(Modifier.height(7.dp))
                    tasks.filterNot { it.done }.take(3).forEachIndexed { index, task ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("●", color = listOf(Color(0xFF3A9BF5), Color(0xFFAA68EE), Color(0xFF3EBC85))[index], fontSize = 10.sp)
                            Spacer(Modifier.width(5.dp))
                            Text(task.title, color = ink, fontSize = 11.sp,
                                lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(7.dp))
                    }
                    if (tasks.none { !it.done }) {
                        Text("Пока нет задач.\nДобавьте первую!", fontSize = 13.sp, color = Color(0xFF57648C))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Открыть список  ›", color = Color(0xFF465988), fontSize = 11.sp)
                }
            }

            GlassPanel(
                Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFF456A9C).copy(alpha = .75f), Color(0xFF37527E).copy(alpha = .7f)),
                padding = 15.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Умные рекомендации", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = white)
                    Spacer(Modifier.weight(1f))
                    Text("MyAI ✦", fontSize = 12.sp, color = Color(0xFFD3D6FF))
                }
                Spacer(Modifier.height(10.dp))
                SuggestionRow("✧", "Составить план дня", "ИИ поможет расставить приоритеты") {
                    onAiAction("Помоги мне составить реалистичный план на день и задай уточняющие вопросы.")
                }
                Spacer(Modifier.height(7.dp))
                SuggestionRow("▤", "Мои заметки", "Сохранено: " + notesCount) { onOpen(2) }
                Spacer(Modifier.height(7.dp))
                SuggestionRow("✓", "Мои задачи", "Невыполненных: " + tasks.count { !it.done }) { onOpen(3) }
            }

            GlassPanel(
                Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFF344D7A).copy(alpha = .85f), Color(0xFF445889).copy(alpha = .7f)),
                padding = 15.dp
            ) {
                Text("Быстрые действия", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = white)
                Spacer(Modifier.height(10.dp))
                SuggestionRow("🎙", "Голосовой ввод", "Распознавание речи Groq Whisper") { onOpen(1) }
                Spacer(Modifier.height(7.dp))
                SuggestionRow("✎", "Новая заметка", "Запишите важную мысль") { onOpen(2) }
            }
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    colors: List<Color> = listOf(glass, Color(0xFF486394).copy(alpha = .56f)),
    padding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(colors))
            .border(1.dp, Color.White.copy(alpha = .25f), shape)
            .padding(padding),
        content = content
    )
}

@Composable
private fun ActionChip(icon: String, name: String, action: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        Modifier.clip(shape).background(Color.White.copy(alpha = .12f))
            .border(1.dp, Color.White.copy(alpha = .19f), shape)
            .clickable(onClick = action).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = Color(0xFFD9E2FF), fontSize = 17.sp)
        Spacer(Modifier.width(6.dp))
        Text(name, color = white, fontSize = 12.sp)
    }
}

@Composable
private fun HeroCard(onOpen: (Int) -> Unit, onAiAction: (String) -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when(hour) {
        in 5..11 -> "Доброе утро! ☀"
        in 12..17 -> "Добрый день! ☀"
        in 18..22 -> "Добрый вечер! ✨"
        else -> "Доброй ночи! ☾"
    }
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier.fillMaxWidth().height(240.dp).clip(shape)
            .background(Brush.linearGradient(listOf(
                Color(0xFFB5CFFA), Color(0xFFE7C7F3), Color(0xFF85CBE7)
            )))
            .border(1.dp, Color.White.copy(alpha = .56f), shape)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = .65f), Color.Transparent),
                    center = Offset(size.width * .92f, size.height * .28f),
                    radius = size.width * .72f
                ),
                radius = size.width * .72f,
                center = Offset(size.width * .92f, size.height * .28f)
            )
        }
        AssistantOrb(Modifier.align(Alignment.CenterEnd).offset(x = 15.dp, y = (-18).dp).size(146.dp))
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(greeting, color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(9.dp))
            Text("Чем могу\nпомочь сегодня?", color = ink,
                fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(max = 220.dp))
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroButton("✉", "Написать") { onAiAction("Помоги написать сообщение: ") }
                HeroButton("▦", "План дня") { onAiAction("Помоги составить расписание на день") }
                HeroButton("✈", "Идеи") { onAiAction("Предложи несколько интересных идей для ") }
                HeroButton("+", "Заметка") { onOpen(2) }
            }
        }
    }
}

@Composable
private fun HeroButton(symbol: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = .54f)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(symbol, color = Color(0xFF575DD8), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text(label, color = ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AppShortcut(symbol: String, label: String, colors: List<Color>, action: () -> Unit) {
    Column(
        Modifier.width(52.dp).clickable(onClick = action),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(colors))
                .border(1.dp, Color.White.copy(alpha = .32f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = if (label == "Камера" || label == "Настр.") ink else Color.White)
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = white, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun SuggestionRow(icon: String, label: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = .1f))
            .clickable(onClick = onClick).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(37.dp).clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF91B4FA).copy(alpha = .45f)), contentAlignment = Alignment.Center) {
            Text(icon, color = white, fontSize = 19.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = white, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = Color(0xFFD0E0FB), fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = Color(0xFFE5EDFF), fontSize = 20.sp)
    }
}

@Composable
fun AssistantOrb(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = min(size.width, size.height) * .45f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFEDF6FF), Color(0xFF7198FF), Color(0xFF373C9D), Color(0xFFB1A5F9)),
                center = Offset(c.x - r * .22f, c.y - r * .38f), radius = r * 1.85f
            ), radius = r, center = c
        )
        drawCircle(color = Color(0xFFC1F3FF).copy(alpha = .7f), radius = r * .97f,
            center = c, style = Stroke(width = r * .08f))
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF1AC8F5).copy(alpha = .7f), Color.Transparent),
                center = Offset(c.x + r * .48f, c.y + r * .56f), radius = r
            ),
            radius = r, center = c
        )
        drawCircle(color = Color.White.copy(alpha = .9f), radius = r * .075f,
            center = Offset(c.x - r * .25f, c.y + r * .02f))
        drawCircle(color = Color.White.copy(alpha = .9f), radius = r * .075f,
            center = Offset(c.x + r * .23f, c.y + r * .02f))
        drawArc(
            color = Color.White.copy(alpha = .9f),
            startAngle = 15f, sweepAngle = 145f, useCenter = false,
            topLeft = Offset(c.x - r * .2f, c.y + r * .04f),
            size = Size(r * .4f, r * .36f),
            style = Stroke(width = r * .035f)
        )
    }
}
