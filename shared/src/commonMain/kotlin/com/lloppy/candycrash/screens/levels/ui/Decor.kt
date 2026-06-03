package com.lloppy.candycrash.screens.levels.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Золотой акцент для звёзд (общий по всему приложению). */
val Gold = Color(0xFFFFC107)

/**
 * «Наклеечный» заголовок в стиле casual-игр: жирный текст с тёмной обводкой
 * и золотисто-кремовым градиентом. Хорошо читается на любом фоне (обе темы).
 */
@Composable
fun GameTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    fillTop: Color = Color(0xFFFFF6C0),
    fillBottom: Color = Color(0xFFFFC107),
    outlineColor: Color = Color(0xFF4A2A7A),
) {
    val outlinePx = with(LocalDensity.current) { fontSize.toPx() } * 0.16f
    val base = TextStyle(fontSize = fontSize, fontWeight = FontWeight.ExtraBold)
    Box(modifier, contentAlignment = Alignment.Center) {
        // мягкая тень
        Text(
            text = text,
            style = base.copy(color = Color.Black.copy(alpha = 0.25f)),
            modifier = Modifier.offset(y = 2.dp),
        )
        // обводка
        Text(
            text = text,
            style = base.copy(
                color = outlineColor,
                drawStyle = Stroke(width = outlinePx, join = StrokeJoin.Round),
            ),
        )
        // заливка-градиент
        Text(
            text = text,
            style = base.copy(brush = Brush.verticalGradient(listOf(fillTop, fillBottom))),
        )
    }
}

/** Фон приложения: космический градиент + звёзды в тёмной теме, конфетный — в светлой. */
@Composable
fun GameBackground(modifier: Modifier = Modifier) {
    val dark = _root_ide_package_.com.lloppy.candycrash.screens.levels.theme.LocalIsDarkTheme.current
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (dark) listOf(Color(0xFF140C2A), Color(0xFF1A0A2E), Color(0xFF0B0818))
                    else listOf(Color(0xFFFFE5F4), Color(0xFFF3E8FF), Color(0xFFE1F0FF))
                )
            )
    ) {
        if (dark) _root_ide_package_.com.lloppy.candycrash.screens.levels.theme.StarryBackground(
            Modifier.fillMaxSize()
        )
    }
}

/** Круглая глянцевая кнопка. */
@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dark = _root_ide_package_.com.lloppy.candycrash.screens.levels.theme.LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .size(46.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    if (dark) listOf(Color(0xFF3A2E66), Color(0xFF241C49))
                    else listOf(Color.White, Color(0xFFE7DBFA))
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Глянцевая панель-карточка с градиентом и тенью. */
@Composable
fun GlossyCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = _root_ide_package_.com.lloppy.candycrash.screens.levels.theme.LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    if (dark) listOf(Color(0xFF302659), Color(0xFF201A42))
                    else listOf(Color.White, Color(0xFFF2EAFC))
                )
            ),
        content = content,
    )
}

/** Диалог в едином стиле игры. */
@Composable
fun GameDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        GlossyCard(Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}
