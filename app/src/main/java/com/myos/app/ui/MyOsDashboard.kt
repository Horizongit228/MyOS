package com.myos.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myos.app.data.Task
import com.myos.app.data.UserStyle

@Composable
fun MyOsDashboard(
    notesCount:Int,tasks:List<Task>,style:UserStyle,apps:List<PhoneApp>,
    onOpen:(Int)->Unit,onAiAction:(String)->Unit,onApp:(PhoneApp)->Unit
){
    val p=style.palette()
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.bgTop,p.bgBottom)))
        .verticalScroll(rememberScrollState())
        .padding(horizontal=16.dp,vertical=16.dp),
        verticalArrangement=Arrangement.spacedBy(if(style.compact)11.dp else 15.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("✦",fontSize=26.sp,color=p.primary)
            Spacer(Modifier.width(9.dp))
            Text("MyOS",fontWeight=FontWeight.Bold,fontSize=25.sp,color=p.text)
            Spacer(Modifier.weight(1f))
            Text("Мой помощник",fontSize=12.sp,color=p.muted)
        }
        FrostedPanel(style,Modifier.fillMaxWidth().clickable{onOpen(1)}){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("✦",fontSize=31.sp,color=p.primary)
                Spacer(Modifier.width(12.dp))
                Text("Спросите ИИ...",modifier=Modifier.weight(1f),fontSize=17.sp,color=p.text)
                Text("🎙",fontSize=22.sp)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            DashboardChip("✎ Написать",style){onAiAction("Помоги написать ")}
            DashboardChip("⌕ Найти",style){onAiAction("Помоги найти ")}
            DashboardChip("▤ Кратко",style){onAiAction("Кратко перескажи: ")}
            DashboardChip("✧ Создать",style){onAiAction("Придумай ")}
        }
        FrostedPanel(style,Modifier.fillMaxWidth()){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("Добро пожаловать ✨",fontSize=13.sp,color=p.muted)
                    Spacer(Modifier.height(8.dp))
                    Text("Чем могу\nпомочь сегодня?",color=p.text,
                        fontSize=28.sp,lineHeight=32.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("MyAI · GPT-OSS 120B",fontSize=12.sp,color=p.muted)
                }
                Box(Modifier.size(88.dp).clip(RoundedCornerShape(44.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF65CFF8),p.primary,Color(0xFF383A82)))),
                    contentAlignment=Alignment.Center){
                    Text("✦",fontSize=43.sp,color=Color.White)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
                modifier=Modifier.horizontalScroll(rememberScrollState())){
                DashboardChip("Мой день",style){onAiAction("Помоги составить план на сегодня")}
                DashboardChip("Заметка",style){onOpen(2)}
                DashboardChip("Мои задачи",style){onOpen(3)}
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Box(Modifier.weight(1f)){
                FrostedPanel(style,Modifier.fillMaxWidth().clickable{onOpen(2)}){
                    Text("Заметки",color=p.muted,fontSize=13.sp)
                    Text(notesCount.toString(),color=p.text,fontSize=31.sp,fontWeight=FontWeight.Bold)
                    Text("Сохранено",color=p.muted,fontSize=12.sp)
                }
            }
            Box(Modifier.weight(1f)){
                FrostedPanel(style,Modifier.fillMaxWidth().clickable{onOpen(3)}){
                    Text("Задачи",color=p.muted,fontSize=13.sp)
                    Text(tasks.count{!it.done}.toString(),color=p.text,fontSize=31.sp,fontWeight=FontWeight.Bold)
                    Text("Активные",color=p.muted,fontSize=12.sp)
                }
            }
        }
        FrostedPanel(style,Modifier.fillMaxWidth()){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("Приложения телефона",color=p.text,fontSize=17.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick={onOpen(5)}){Text("Все ›")}
            }
            Spacer(Modifier.height(12.dp))
            if(apps.isEmpty()){
                Text("Разрешённые приложения появятся здесь.",color=p.muted)
            }else{
                apps.take(8).chunked(4).forEach{rowApps->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        rowApps.forEach{app->
                            PhoneAppTile(app,style,Modifier.weight(1f)){onApp(app)}
                        }
                        repeat(4-rowApps.size){Spacer(Modifier.weight(1f))}
                    }
                }
            }
        }
        FrostedPanel(style,Modifier.fillMaxWidth()){
            Text("Умные рекомендации",color=p.text,fontSize=17.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            ActionRow("✧","План на сегодня","Попросить ИИ составить план",style){
                onAiAction("Помоги составить план дел на сегодня")
            }
            Spacer(Modifier.height(8.dp))
            ActionRow("☑","Текущие задачи","Открыть список задач",style){onOpen(3)}
            Spacer(Modifier.height(8.dp))
            ActionRow("▤","Новая заметка","Записать идею",style){onOpen(2)}
        }
    }
}

@Composable private fun DashboardChip(label:String,style:UserStyle,onClick:()->Unit){
    val p=style.palette()
    Box(Modifier.clip(RoundedCornerShape(20.dp))
        .background(p.text.copy(alpha=.12f))
        .clickable(onClick=onClick)
        .padding(horizontal=14.dp,vertical=10.dp)){
        Text(label,color=p.text,fontSize=12.sp)
    }
}
@Composable private fun ActionRow(icon:String,title:String,description:String,style:UserStyle,onClick:()->Unit){
    val p=style.palette()
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(p.text.copy(alpha=.1f))
        .clickable(onClick=onClick).padding(12.dp),
        verticalAlignment=Alignment.CenterVertically){
        Text(icon,fontSize=23.sp,color=p.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)){
            Text(title,color=p.text,fontWeight=FontWeight.Medium,fontSize=14.sp)
            Text(description,color=p.muted,fontSize=11.sp,
                maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Text("›",color=p.muted,fontSize=24.sp)
    }
}
