package com.tsing.localcardwallet

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 卡面风格常量与名称 */
object FaceStyle {
    const val AUTO = "auto"
    const val PHOTO = "photo"

    val TEMPLATES = listOf("g1", "g2", "g3", "g4", "g5", "g6", "g7", "g8")

    /** 把 auto / photo 解析成实际要绘制的模板；photo 会先判断图片是否还在 */
    fun resolve(style: String): String {
        if (style == AUTO || style == PHOTO) return "g1"
        return if (style in TEMPLATES) style else "g1"
    }

    fun label(context: android.content.Context, style: String): String = when (style) {
        AUTO -> context.getString(R.string.template_auto)
        PHOTO -> context.getString(R.string.template_photo)
        "g1" -> context.getString(R.string.template_g1)
        "g2" -> context.getString(R.string.template_g2)
        "g3" -> context.getString(R.string.template_g3)
        "g4" -> context.getString(R.string.template_g4)
        "g5" -> context.getString(R.string.template_g5)
        "g6" -> context.getString(R.string.template_g6)
        "g7" -> context.getString(R.string.template_g7)
        "g8" -> context.getString(R.string.template_g8)
        else -> style
    }
}

/** 识别失败时按卡组织兜底的颜色 */
fun fallbackColor(type: String): Color = when {
    type.contains("VISA") -> Color(0xFF1A1F71)
    type.contains("MASTER") -> Color(0xFF2B2B2B)
    type.contains("UNION") -> Color(0xFFC01E2D)
    type.contains("AMEX") || type.contains("AMERICAN") -> Color(0xFF006FCF)
    else -> Color(0xFF2C2C2C)
}

/** 卡面底色：优先银行主色，其次按卡组织兜底 */
fun cardBaseColor(card: CardEntity): Color {
    val bank = BankBinRepository.byCode(card.bankCode)
    return bank?.brandColor ?: fallbackColor(card.type)
}

fun cardAccentColor(base: Color): Color = lerp(base, Color.White, 0.55f)

/**
 * 卡面背景。photo 非空时铺用户照片并压一层暗色蒙版，
 * 否则用纯代码绘制的模板。整个卡面不含任何图片资源文件。
 */
@Composable
fun CardFaceBackground(
    style: String,
    base: Color,
    accent: Color,
    photo: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    if (photo != null) {
        Box(modifier) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.58f),
                                Color.Black.copy(alpha = 0.12f),
                                Color.Black.copy(alpha = 0.70f),
                            )
                        )
                    )
            )
        }
    } else {
        Canvas(modifier) { drawFace(style, base, accent) }
    }
}

private fun DrawScope.drawFace(style: String, base: Color, accent: Color) {
    val w = size.width
    val h = size.height
    when (style) {
        "g2" -> {
            drawRect(
                Brush.linearGradient(
                    listOf(lerp(base, Color.Black, 0.12f), lerp(base, Color.Black, 0.62f)),
                    start = Offset(0f, 0f), end = Offset(w, h),
                )
            )
            rotate(-18f) {
                drawRect(
                    Color.White.copy(alpha = 0.10f),
                    topLeft = Offset(w * 0.52f, -h),
                    size = Size(w * 0.38f, h * 3f),
                )
            }
        }
        "g3" -> {
            drawRect(lerp(base, Color.Black, 0.32f))
            val center = Offset(w * 0.86f, h * 1.12f)
            for (i in 0 until 9) {
                drawCircle(
                    Color.White.copy(alpha = 0.035f + i * 0.004f),
                    radius = h * (0.30f + i * 0.16f),
                    center = center,
                )
            }
        }
        "g4" -> {
            drawRect(
                Brush.linearGradient(
                    listOf(lerp(base, Color.Black, 0.08f), lerp(base, Color.Black, 0.48f)),
                    start = Offset(0f, 0f), end = Offset(w, h),
                )
            )
            val step = 14.dp.toPx()
            val radius = 1.3.dp.toPx()
            var y = step / 2f
            while (y < h) {
                var x = step / 2f
                while (x < w) {
                    drawCircle(Color.White.copy(alpha = 0.075f), radius = radius, center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        "g5" -> {
            drawRect(lerp(base, Color.Black, 0.58f))
            val c1 = Offset(w * 0.20f, h * 0.10f)
            drawCircle(
                brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), Color.Transparent), center = c1, radius = w * 0.80f),
                radius = w * 0.80f, center = c1,
            )
            val c2 = Offset(w * 0.92f, h * 1.15f)
            drawCircle(
                brush = Brush.radialGradient(listOf(base.copy(alpha = 0.90f), Color.Transparent), center = c2, radius = w * 0.85f),
                radius = w * 0.85f, center = c2,
            )
            val c3 = Offset(w * 0.66f, h * 0.20f)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), center = c3, radius = w * 0.42f),
                radius = w * 0.42f, center = c3,
            )
        }
        "g6" -> {
            drawRect(
                Brush.linearGradient(
                    listOf(base, lerp(base, Color.Black, 0.38f)),
                    start = Offset(0f, 0f), end = Offset(w, h),
                )
            )
            val barWidth = 6.dp.toPx()
            val gap = 20.dp.toPx()
            var x = 0f
            while (x < w) {
                drawRect(Color.White.copy(alpha = 0.05f), topLeft = Offset(x, 0f), size = Size(barWidth, h))
                x += gap
            }
        }
        "g7" -> {
            drawRect(
                Brush.linearGradient(
                    listOf(
                        lerp(base, Color.Black, 0.55f),
                        lerp(base, Color.White, 0.18f),
                        lerp(base, Color.Black, 0.62f),
                    ),
                    start = Offset(0f, 0f), end = Offset(w, h),
                )
            )
            rotate(-24f) {
                drawRect(
                    Color.White.copy(alpha = 0.11f),
                    topLeft = Offset(w * 0.12f, -h * 0.6f),
                    size = Size(w * 0.10f, h * 2.2f),
                )
            }
        }
        "g8" -> {
            drawRect(lerp(base, Color.Black, 0.42f))
            val spots = listOf(
                Triple(0.22f, 0.28f, 0.42f),
                Triple(0.74f, 0.78f, 0.46f),
                Triple(0.52f, 0.04f, 0.30f),
            )
            for (spot in spots) {
                val c = Offset(w * spot.first, h * spot.second)
                val radius = w * spot.third
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(lerp(accent, Color.White, 0.2f).copy(alpha = 0.30f), Color.Transparent),
                        center = c, radius = radius,
                    ),
                    radius = radius, center = c,
                )
            }
        }
        else -> {
            drawRect(
                Brush.linearGradient(
                    listOf(
                        lerp(base, Color.White, 0.22f),
                        base,
                        lerp(base, Color.Black, 0.50f),
                    ),
                    start = Offset(0f, 0f), end = Offset(w, h),
                )
            )
        }
    }
}

private suspend fun loadCover(path: String?, context: android.content.Context): ImageBitmap? {
    if (path.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val file = File(context.filesDir, path)
            if (!file.exists()) null else BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalletCard(
    card: CardEntity,
    showFullPan: Boolean,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val bank = remember(card.bankCode) { BankBinRepository.byCode(card.bankCode) }
    val base = remember(card.bankCode, card.type) { cardBaseColor(card) }
    val accent = remember(base) { cardAccentColor(base) }

    val photo by produceState<ImageBitmap?>(initialValue = null, card.coverPath) {
        value = loadCover(card.coverPath, context)
    }
    val style = if (photo != null) FaceStyle.PHOTO else FaceStyle.resolve(card.faceStyle)

    val displayPan = if (showFullPan) {
        card.pan.chunked(4).joinToString("  ")
    } else {
        card.pan.mapIndexed { index, c ->
            if (index < 4 || index >= card.pan.length - 4) c else '*'
        }.joinToString("").chunked(4).joinToString("  ")
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "cardPress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        CardFaceBackground(style, base, accent, photo, Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = bank?.name ?: card.type.ifBlank { "BANK CARD" },
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
                CardLogo(card.type)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = displayPan,
                color = Color.White,
                fontSize = 22.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "VALID THRU",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                    )
                    Text(card.expiry, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                if (bank != null && bank.country.isNotBlank()) {
                    Text(
                        text = bank.country.uppercase(),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

/** 纯代码绘制卡组织标识，不使用任何官方 logo 图片 */
@Composable
fun CardLogo(type: String) {
    if (type.contains("VISA")) {
        Text(
            "VISA",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
        )
    } else if (type.contains("MASTER")) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).background(Color(0xFFEB001B), shape = CircleShape))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = (-10).dp)
                    .background(Color(0xFFF79E1B).copy(alpha = 0.9f), shape = CircleShape)
            )
        }
    } else if (type.contains("UNION")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(width = 10.dp, height = 24.dp).background(Color(0xFFE21836), shape = RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(2.dp))
            Box(modifier = Modifier.size(width = 10.dp, height = 24.dp).offset(y = (-4).dp).background(Color(0xFF00447C), shape = RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(2.dp))
            Box(modifier = Modifier.size(width = 10.dp, height = 24.dp).background(Color(0xFF007B84), shape = RoundedCornerShape(2.dp)))
        }
    } else if (type.contains("AMEX") || type.contains("AMERICAN")) {
        Text("AMEX", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
    } else {
        Text("CARD", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
