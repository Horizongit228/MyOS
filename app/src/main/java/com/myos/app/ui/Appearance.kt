package com.myos.app.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myos.app.data.UserStyle

data class UiPalette(val bgTop:Color,val bgBottom:Color,val primary:Color,val surface:Color,val text:Color,val muted:Color)
fun UserStyle.palette():UiPalette=when(paletteId){
    1->UiPalette(Color(0xFF082D37),Color(0xFF174A59),Color(0xFF80E3E7),Color(0xFF477687),Color.White,Color(0xFFC8E7EC))
    2->UiPalette(Color(0xFF1A1038),Color(0xFF452A71),Color(0xFFCAA0FF),Color(0xFF745C9F),Color.White,Color(0xFFE1D4FA))
    3->UiPalette(Color(0xFFF2F5FC),Color(0xFFE3EBFA),Color(0xFF4969D8),Color.White,Color(0xFF1B2946),Color(0xFF61708E))
    else->UiPalette(Color(0xFF14223E),Color(0xFF273F70),Color(0xFF9FAEFF),Color(0xFF667DAB),Color.White,Color(0xFFD2DCF5))
}
@Composable fun FrostedPanel(style:UserStyle,modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    val p=style.palette()
    val shape=RoundedCornerShape(style.radius.dp)
    Column(modifier.clip(shape).background(p.surface.copy(alpha=if(style.paletteId==3)style.opacity.coerceAtLeast(.7f) else style.opacity))
        .border(1.dp,p.text.copy(alpha=.16f),shape).padding(if(style.compact)12.dp else 16.dp),content=content)
}
