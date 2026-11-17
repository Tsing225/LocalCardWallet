package com.tsing.localcardwallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity

@Composable
fun LockScreen(onResetData: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var hasAccount by remember { mutableStateOf(AuthManager.hasAccount(context)) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showReset by remember { mutableStateOf(false) }

    val biometricReady = remember { BiometricGate.isAvailable(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(34.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(stringResource(R.string.app_name), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(if (hasAccount) R.string.lock_unlock_subtitle else R.string.lock_create_subtitle),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it.take(64); error = null },
                placeholder = { Text(stringResource(R.string.lock_password_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                // 用 Text 类型而非 Password，避免系统「账号密码自动填充」往里塞内容
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!hasAccount) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.take(64); error = null },
                    placeholder = { Text(stringResource(R.string.lock_confirm_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = error ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            BounceClick(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.primary),
                pressedScale = 0.95f,
                onClick = {
                    if (hasAccount) {
                        if (AuthManager.verify(context, password)) {
                            // unlocked 状态变化会自动切换到主界面
                        } else {
                            error = context.getString(R.string.lock_error_wrong)
                            password = ""
                        }
                    } else {
                        if (password != confirm) {
                            error = context.getString(R.string.lock_error_mismatch)
                        } else {
                            error = AuthManager.createAccount(context, password)
                        }
                    }
                },
            ) {
                Text(
                    text = stringResource(if (hasAccount) R.string.lock_unlock else R.string.lock_create),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (hasAccount && biometricReady && activity != null) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = {
                    BiometricGate.authenticate(
                        activity = activity,
                        title = "解锁卡包",
                        subtitle = "使用指纹或面容",
                        onSuccess = { AuthManager.unlockWithBiometric() },
                        onFail = { error = it },
                    )
                }) {
                    Text(stringResource(R.string.lock_biometric))
                }
            }

            if (hasAccount) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { showReset = true }) {
                    Text(stringResource(R.string.lock_forgot), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.lock_privacy),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.lock_reset_title)) },
            text = {
                Text(stringResource(R.string.lock_reset_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showReset = false
                    onResetData()
                    hasAccount = AuthManager.hasAccount(context)
                    password = ""
                    confirm = ""
                    error = null
                }) {
                    Text(stringResource(R.string.lock_reset_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text(stringResource(R.string.lock_reset_cancel)) }
            },
        )
    }
}
