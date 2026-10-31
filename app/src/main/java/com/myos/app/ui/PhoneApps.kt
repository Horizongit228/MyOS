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
    private fun normalized(value:String):String =
        value.lowercase(Locale.ROOT).replace('ё','е')
            .replace(Regex("[^\\p{L}\\p{N}]+")," ").trim()

    private fun aliases(q:String):String = when {
        Regex("телеграм|телега|tg|telegram").containsMatchIn(q)->"telegram"
        Regex("ютуб|ютюб|youtube|ютьюб").containsMatchIn(q)->"youtube"
        Regex("вотсап|ватсап|вацап|whatsapp|вацапп").containsMatchIn(q)->"whatsapp"
        Regex("хром|chrome|гугл хром").containsMatchIn(q)->"chrome"
        Regex("вк|вконтакте|vk").matches(q)->"vk"
        Regex("дискорд|discord").containsMatchIn(q)->"discord"
        Regex("тикток|tiktok").containsMatchIn(q)->"tiktok"
        Regex("инстаграм|instagram").containsMatchIn(q)->"instagram"
        Regex("яндекс музыка|яндекс мьюзик").containsMatchIn(q)->"музыка"
        Regex("галерея|фото|фотографии").matches(q)->"gallery"
        Regex("плей маркет|play market|google play").containsMatchIn(q)->"play"
        else->q
    }

    fun requestedName(message:String):String? {
        val text=normalized(message)
            .replace(Regex("^(ии |myai |эй ии |ассистент )"),"")
        val pattern=Regex("^(?:(?:можешь|мог бы|пожалуйста|плиз) )*(?:открой|открыть|запусти|запустить|открывай|открываем|включи|зайди в|перейди в|open|launch)(?: мне| пожалуйста| плиз)? (.+)")
        val result=pattern.find(text)?.groupValues?.getOrNull(1)?.trim()
        return result?.takeIf { it.length in 2..80 }?.replace(Regex(" пожалуйста$"),"")
    }

    private fun similarity(a:String,b:String):Double {
        if(a==b)return 1.0
        if(a.isEmpty()||b.isEmpty())return 0.0
        var previous=IntArray(b.length+1){it}
        for(i in a.indices){
            val next=IntArray(b.length+1)
            next[0]=i+1
            for(j in b.indices)next[j+1]=minOf(
                next[j]+1,previous[j+1]+1,
                previous[j]+if(a[i]==b[j])0 else 1)
            previous=next
        }
        return 1.0-previous[b.length].toDouble()/maxOf(a.length,b.length)
    }

    fun match(apps:List<PhoneApp>,query:String):PhoneApp? {
        val q=aliases(normalized(query))
        if(q.isBlank())return null
        val ranked=apps.map { app ->
            val label=normalized(app.label)
            val pkg=normalized(app.packageName.replace('.',' '))
            val best=when {
                label==q -> 1.0
                label.startsWith(q) -> .96
                label.contains(q) -> .90
                pkg.contains(q) -> .89
                else -> maxOf(similarity(q,label),
                    label.split(" ").filter{it.isNotBlank()}.maxOfOrNull{similarity(q,it)}?:0.0)
            }
            app to best
        }.sortedByDescending{it.second}
        return ranked.firstOrNull{it.second>=if(q.length<=3).94 else .66}?.first
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
@Composable fun PhoneAppsScreen(
    style:UserStyle,apps:List<PhoneApp>,favorites:Set<String>,
    onFavorite:(PhoneApp)->Unit,onOpen:(PhoneApp)->Unit
){
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) }
    val p=style.palette()
    val filtered=remember(apps,query,filter,favorites){
        apps.filter { it.label.contains(query.trim(),ignoreCase=true)||
            it.packageName.contains(query.trim(),ignoreCase=true) }
            .filter{filter!=1||it.packageName in favorites}
            .sortedWith(compareByDescending<PhoneApp>{it.packageName in favorites}
                .thenBy(String.CASE_INSENSITIVE_ORDER){it.label})
    }
    Column(Modifier.fillMaxSize().padding(horizontal=14.dp,vertical=10.dp)){
        Text("Приложения · "+apps.size+"   Избранное · "+favorites.size,
            color=p.muted,style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value=query,onValueChange={query=it},
            label={Text("Поиск по названию")},singleLine=true,
            modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(style.radius.dp))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            if(filter==0)Button(onClick={filter=0}){Text("Все")}
            else OutlinedButton(onClick={filter=0}){Text("Все")}
            if(filter==1)Button(onClick={filter=1}){Text("★ Избранное")}
            else OutlinedButton(onClick={filter=1}){Text("☆ Избранное")}
        }
        Spacer(Modifier.height(12.dp))
        if(filtered.isEmpty())Text("Приложения не найдены",color=p.muted)
        else LazyVerticalGrid(columns=GridCells.Fixed(if(style.compact)5 else 4),
            contentPadding=PaddingValues(bottom=16.dp),
            horizontalArrangement=Arrangement.spacedBy(3.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)){
            items(filtered,key={it.packageName}){app->
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    PhoneAppTile(app,style,Modifier.fillMaxWidth()){onOpen(app)}
                    TextButton(onClick={onFavorite(app)},
                        contentPadding=PaddingValues(0.dp),modifier=Modifier.height(30.dp)){
                        Text(if(app.packageName in favorites)"★" else "☆",
                            color=if(app.packageName in favorites)p.primary else p.muted,
                            fontSize=20.sp)
                    }
                }
            }
        }
    }
}
