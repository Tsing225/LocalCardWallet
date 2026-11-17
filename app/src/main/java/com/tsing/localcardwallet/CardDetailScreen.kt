package com.tsing.localcardwallet

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    card: CardEntity,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUpdateFace: (CardEntity, String, String?) -> Unit,
    onUpdateBank: (CardEntity, String?) -> Unit,
    onUpdateInfo: (CardEntity, String?, String?, String?) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    var revealed by remember { mutableStateOf(false) }
    var cvvRevealed by remember { mutableStateOf(false) }
    var facePickerOpen by remember { mutableStateOf(false) }
    var bankPickerOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = copyCoverPhoto(context, uri, card.id)
                if (path != null) {
                    onUpdateFace(card, FaceStyle.PHOTO, path)
                    facePickerOpen = false
                }
            }
        }
    }

    fun requireBiometric(onGranted: () -> Unit) {
        if (!AppSettings.biometricForPan) {
            onGranted()
            return
        }
        if (activity == null || !BiometricGate.isAvailable(context)) {
            onGranted()
            return
        }
        BiometricGate.authenticate(
            activity = activity,
            title = context.getString(R.string.verify_title),
            subtitle = context.getString(R.string.verify_subtitle),
            onSuccess = onGranted,
            onFail = {
                Toast.makeText(context, context.getString(R.string.verify_failed) + it, Toast.LENGTH_SHORT).show()
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    BounceClick(onClick = onBack, pressedScale = 0.85f) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("<", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                actions = {
                    BounceClick(onClick = { editOpen = true }, pressedScale = 0.85f) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.action_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.shadow(24.dp, RoundedCornerShape(16.dp))) {
                WalletCard(
                    card = card,
                    showFullPan = revealed,
                    onLongClick = { facePickerOpen = true },
                    onClick = { facePickerOpen = true },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            InfoGroup(stringResource(R.string.detail_group_basic)) {
                InfoLine(
                    stringResource(R.string.detail_issuer),
                    card.bankName ?: BankBinRepository.byCode(card.bankCode)?.name
                    ?: stringResource(R.string.value_not_recognized),
                )
                SoftDivider()
                InfoLine(stringResource(R.string.detail_network), card.type)
                SoftDivider()
                InfoLine(stringResource(R.string.detail_kind), cardKindLabel(context, card.cardKind))
                SoftDivider()
                InfoLine(stringResource(R.string.detail_expiry), card.expiry)
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoGroup(stringResource(R.string.detail_group_security)) {
                InfoLine(
                    label = stringResource(R.string.detail_pan),
                    value = if (revealed) card.pan.chunked(4).joinToString(" ") else maskedPan(card.pan),
                    monospace = true,
                    trailing = {
                        Row {
                            if (revealed) {
                                TextButton(onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                        as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(
                                        android.content.ClipData.newPlainText("card number", card.pan)
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_pan_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }) {
                                    Text(stringResource(R.string.action_copy), fontSize = 12.sp)
                                }
                            }
                            TextButton(onClick = {
                                if (revealed) revealed = false
                                else requireBiometric { revealed = true }
                            }) {
                                Text(
                                    stringResource(if (revealed) R.string.action_hide else R.string.action_show),
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    },
                )
                SoftDivider()
                InfoLine(
                    label = stringResource(R.string.detail_cvv),
                    value = when {
                        card.cvv.isNullOrBlank() -> stringResource(R.string.value_not_saved)
                        cvvRevealed -> card.cvv ?: ""
                        else -> "###"
                    },
                    monospace = true,
                    trailing = {
                        if (!card.cvv.isNullOrBlank()) {
                            TextButton(onClick = {
                                if (cvvRevealed) cvvRevealed = false
                                else requireBiometric { cvvRevealed = true }
                            }) {
                                Text(
                                    stringResource(if (cvvRevealed) R.string.action_hide else R.string.action_show),
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoGroup(stringResource(R.string.detail_group_other)) {
                InfoLine(stringResource(R.string.detail_holder), card.holderName ?: stringResource(R.string.value_not_set))
                SoftDivider()
                InfoLine(
                    stringResource(R.string.detail_virtual),
                    stringResource(if (card.isVirtual) R.string.value_yes else R.string.value_no),
                )
                SoftDivider()
                InfoLine(
                    stringResource(R.string.detail_source),
                    stringResource(
                        if (card.source == CardEntity.SOURCE_NFC) R.string.source_nfc else R.string.source_manual
                    ),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ActionButton(Icons.Filled.Edit, stringResource(R.string.action_edit), false) { editOpen = true }
                ActionButton(Icons.Filled.Refresh, stringResource(R.string.action_change_face), false) { facePickerOpen = true }
                ActionButton(Icons.Filled.Search, stringResource(R.string.action_issuer), false) { bankPickerOpen = true }
                ActionButton(Icons.Filled.Delete, stringResource(R.string.action_delete), true) { confirmDelete = true }
            }
            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.action_delete_card)) },
            text = { Text(stringResource(R.string.delete_confirm_message, card.pan.takeLast(4))) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (editOpen) {
        EditCardInfoDialog(
            card = card,
            onDismiss = { editOpen = false },
            onSave = { holder, cvv, kind ->
                onUpdateInfo(card, holder, cvv, kind)
                editOpen = false
            },
        )
    }

    if (facePickerOpen) {
        FacePickerSheet(
            card = card,
            onDismiss = { facePickerOpen = false },
            onSelectStyle = { style ->
                onUpdateFace(card, style, null)
                facePickerOpen = false
            },
            onResetAuto = {
                onUpdateFace(card, FaceStyle.AUTO, null)
                facePickerOpen = false
            },
            onPickPhoto = {
                photoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickBank = {
                facePickerOpen = false
                bankPickerOpen = true
            },
            onAutoDetectBank = {
                onUpdateBank(card, null)
                facePickerOpen = false
            },
        )
    }

    if (bankPickerOpen) {
        BankPickerDialog(
            onDismiss = { bankPickerOpen = false },
            onPick = { bank ->
                onUpdateBank(card, bank.code)
                bankPickerOpen = false
            },
        )
    }
}

@Composable
private fun InfoGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                .padding(horizontal = 16.dp),
            content = content,
        )
    }
}

@Composable
private fun SoftDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        thickness = 0.6.dp,
    )
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
    monospace: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(4.dp))
            trailing()
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    danger: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BounceClick(onClick = onClick, pressedScale = 0.85f) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 10.sp, color = tint)
    }
}

@Composable
private fun EditCardInfoDialog(
    card: CardEntity,
    onDismiss: () -> Unit,
    onSave: (String?, String?, String?) -> Unit,
) {
    val context = LocalContext.current
    var holder by remember { mutableStateOf(card.holderName ?: "") }
    var cvv by remember { mutableStateOf(card.cvv ?: "") }
    var kind by remember { mutableStateOf(card.cardKind ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_edit_title)) },
        text = {
            Column {
                if (card.source == CardEntity.SOURCE_NFC) {
                    Text(
                        text = stringResource(R.string.detail_edit_nfc_hint),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = holder,
                    onValueChange = { holder = it.take(40); error = null },
                    placeholder = { Text(stringResource(R.string.edit_holder_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cvv,
                    onValueChange = { cvv = it.filter { c -> c.isDigit() }.take(4); error = null },
                    placeholder = { Text(stringResource(R.string.edit_cvv_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.detail_kind),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == "DC",
                        onClick = { kind = "DC" },
                        label = { Text(stringResource(R.string.kind_debit), fontSize = 12.sp) },
                    )
                    FilterChip(
                        selected = kind == "CC",
                        onClick = { kind = "CC" },
                        label = { Text(stringResource(R.string.kind_credit), fontSize = 12.sp) },
                    )
                    FilterChip(
                        selected = kind == "",
                        onClick = { kind = "" },
                        label = { Text(stringResource(R.string.kind_unknown), fontSize = 12.sp) },
                    )
                }
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (cvv.isNotEmpty() && cvv.length !in 3..4) {
                    error = context.getString(R.string.cvv_invalid)
                } else {
                    onSave(
                        holder.trim().ifBlank { null },
                        cvv.ifBlank { null },
                        kind.ifBlank { null },
                    )
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun maskedPan(pan: String): String {
    if (pan.isEmpty()) return ""
    val masked = pan.mapIndexed { i, c ->
        if (i < 4 || i >= pan.length - 4) c else '*'
    }.joinToString("")
    return masked.chunked(4).joinToString(" ")
}
