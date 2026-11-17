package com.tsing.localcardwallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 换卡面面板。所有模板都是纯代码绘制，预览与真实卡面用同一套绘制逻辑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacePickerSheet(
    card: CardEntity,
    onDismiss: () -> Unit,
    onSelectStyle: (String) -> Unit,
    onResetAuto: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickBank: () -> Unit,
    onAutoDetectBank: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = androidx.compose.ui.platform.LocalContext.current
    val base = remember(card.bankCode, card.type) { cardBaseColor(card) }
    val accent = remember(base) { cardAccentColor(base) }
    val bankName = remember(card.bankCode) { BankBinRepository.byCode(card.bankCode)?.name }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.face_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = (bankName ?: stringResource(R.string.face_unrecognized)) + " · " + stringResource(R.string.face_tail, card.pan.takeLast(4)),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.face_templates),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            FaceStyle.TEMPLATES.chunked(4).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (style in row) {
                        TemplateThumb(
                            style = style,
                            base = base,
                            accent = accent,
                            selected = card.faceStyle == style,
                            onClick = { onSelectStyle(style) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size < 4) {
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                if (rowIndex < 1) Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onPickPhoto,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.face_pick_photo))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.face_photo_note),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onResetAuto, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.face_reset_auto))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.face_issuer),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onPickBank, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.face_pick_issuer))
                }
                TextButton(onClick = onAutoDetectBank, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.face_auto_detect))
                }
            }
        }
    }
}

@Composable
private fun TemplateThumb(
    style: String,
    base: Color,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable(onClick = onClick)
        ) {
            CardFaceBackground(style, base, accent, null, Modifier.matchParentSize())
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = FaceStyle.label(context, style),
            fontSize = 10.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
