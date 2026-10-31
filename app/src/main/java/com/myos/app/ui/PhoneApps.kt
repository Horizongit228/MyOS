package com.myos.app.ui
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myos.app.data.UserStyle
import java.util.Locale

data class PhoneApp(val label:String,val packageName:String,val activity:String)
object PhoneApps{
    fun discover(context:Context):List<PhoneApp>{
        val pm=context.packageManager
        val filter=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities=if(Build.VERSION.SDK_INT>=33){
            pm.queryIntentActivities(filter,PackageManager.ResolveInfoFlags.of(0L))
        }else{
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(filter,0)
        }
        return activities.mapNotNull{resolved->
            val info=resolved.activityInfo?:return@mapNotNull null
            if(info.packageName==context.packageName)return@mapNotNull null
            PhoneApp(resolved.loadLabel(pm).toString().ifBlank{info.packageName},info.packageName,info.name)
        }.distinctBy{it.packageName}.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER){it.label})
    }
    fun open(context:Context,app:PhoneApp):Boolean=try{
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.packageName,app.activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }catch(_:Exception){false}
    fun requestedName(message:String):String?{
        val normalized=message.trim().replace(Regex("(?i)^(ии[,! ]+|myai[,! ]+)"),"")
        return Regex("(?i)^(открой|открыть|запусти|запустить|open|launch)\\s+(.+)$")
            .find(normalized)?.groupValues?.getOrNull(2)?.trim()?.trim('.','!','?',' ')
    }
    fun match(apps:List<PhoneApp>,query:String):PhoneApp?{
        val aliases=mapOf("телеграм" to "telegram","ватсап" to "whatsapp",
            "вацап" to "whatsapp","ютуб" to "youtube","хром" to "chrome",
            "браузер" to "chrome","дискорд" to "discord","камера" to "camera")
        val key=query.lowercase(Locale.ROOT).trim()
        val q=aliases.entries.firstOrNull{key==it.key||key.startsWith(it.key+" ")}?.value?:key
        return apps.firstOrNull{it.label.lowercase(Locale.ROOT)==q}
            ?:apps.firstOrNull{it.label.lowercase(Locale.ROOT).startsWith(q)}
            ?:apps.firstOrNull{it.label.lowercase(Locale.ROOT).contains(q)}
    }
}
private fun iconBitmap(context:Context,app:PhoneApp):Bitmap?=runCatching{
    val d:Drawable=context.packageManager.getApplicationIcon(app.packageName)
    val bitmap=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(bitmap)
    d.setBounds(0,0,96,96)
    d.draw(canvas)
    bitmap
}.getOrNull()

@Composable fun PhoneAppTile(app:PhoneApp,style:UserStyle,modifier:Modifier=Modifier,onClick:()->Unit){
    val ctx=LocalContext.current
    val bitmap=remember(app.packageName){iconBitmap(ctx,app)?.asImageBitmap()}
    val color=style.palette().text
    Column(modifier.clickable(onClick=onClick).padding(5.dp),horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(7.dp)){
        if(bitmap!=null)Image(bitmap=bitmap,contentDescription=app.label,
            modifier=Modifier.size(if(style.compact)46.dp else 56.dp).clip(RoundedCornerShape(14.dp)))
        else Surface(color=style.palette().primary.copy(alpha=.3f),shape=RoundedCornerShape(14.dp),
            modifier=Modifier.size(56.dp)){
            Box(contentAlignment=Alignment.Center){Text(app.label.take(1),fontSize=22.sp,color=color)}
        }
        Text(app.label,fontSize=11.sp,color=color,maxLines=1,
            overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center)
    }
}
@Composable fun PhoneAppsScreen(style:UserStyle,apps:List<PhoneApp>,onOpen:(PhoneApp)->Unit){
    var query by remember{mutableStateOf("")}
    val filtered=remember(apps,query){apps.filter{it.label.contains(query.trim(),ignoreCase=true)}}
    Column(Modifier.fillMaxSize().padding(horizontal=14.dp,vertical=10.dp)){
        Text("Установленные приложения · "+apps.size,color=style.palette().muted,
            style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value=query,onValueChange={query=it},label={Text("Найти приложение")},
            singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(style.radius.dp))
        Spacer(Modifier.height(12.dp))
        if(filtered.isEmpty())Text("Приложения не найдены",color=style.palette().muted)
        else LazyVerticalGrid(columns=GridCells.Fixed(4),
            contentPadding=PaddingValues(bottom=16.dp),
            horizontalArrangement=Arrangement.spacedBy(2.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)){
            items(filtered,key={it.packageName}){app->
                PhoneAppTile(app,style,Modifier.fillMaxWidth()){onOpen(app)}
            }
        }
    }
}
