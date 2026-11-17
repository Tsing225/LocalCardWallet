package com.tsing.localcardwallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    cardCount: Int,
    cardLimit: Int,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
    var showChangePassword by remember { mutableStateOf(false) }
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "-"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back)) }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            SectionTitle(stringResource(R.string.settings_language))
            LocaleOption(stringResource(R.string.language_system), LocaleHelper.SYSTEM, context)
            LocaleOption(stringResource(R.string.language_zh), LocaleHelper.ZH, context)
            LocaleOption(stringResource(R.string.language_en), LocaleHelper.EN, context)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.settings_appearance))
            ThemeOption(stringResource(R.string.settings_theme_system), AppSettings.THEME_SYSTEM, context)
            ThemeOption(stringResource(R.string.settings_theme_light), AppSettings.THEME_LIGHT, context)
            ThemeOption(stringResource(R.string.settings_theme_dark), AppSettings.THEME_DARK, context)

            Spacer(modifier = Modifier.height(8.dp))
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_desc),
                checked = AppSettings.dynamicColor,
                onChange = { AppSettings.setDynamicColor(context, it) },
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.settings_security))
            SwitchRow(
                title = stringResource(R.string.settings_biometric),
                subtitle = stringResource(R.string.settings_biometric_desc),
                checked = AppSettings.biometricForPan,
                onChange = { AppSettings.setBiometricForPan(context, it) },
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.settings_account))
            InfoRow(stringResource(R.string.settings_account_type), stringResource(R.string.settings_account_local))
            InfoRow(stringResource(R.string.settings_account_password), stringResource(R.string.settings_account_password_desc))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { showChangePassword = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_change_password))
                }
                TextButton(onClick = { AuthManager.lock() }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_lock_now))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.settings_wallet))
            InfoRow(stringResource(R.string.settings_card_count), cardCount.toString() + " / " + cardLimit)
            InfoRow(stringResource(R.string.settings_bin_lib), stringResource(R.string.settings_bin_lib_desc))
            InfoRow(stringResource(R.string.settings_database), stringResource(R.string.settings_database_desc))

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.settings_about))
            InfoRow(stringResource(R.string.settings_version), versionName)
            InfoRow(stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_desc))

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onClearAll) {
                Text(stringResource(R.string.settings_clear_all), color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showChangePassword) {
        ChangePasswordDialog(onDismiss = { showChangePassword = false })
    }
}

@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.password_change_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it; error = null },
                    placeholder = { Text(stringResource(R.string.password_old)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    placeholder = {
                        Text(stringResource(R.string.password_new, AuthManager.MIN_PASSWORD_LENGTH))
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    placeholder = { Text(stringResource(R.string.password_confirm)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newPassword != confirmPassword) {
                    error = context.getString(R.string.password_mismatch)
                } else {
                    val result = AuthManager.changePassword(context, oldPassword, newPassword)
                    if (result == null) onDismiss() else error = result
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun LocaleOption(label: String, tag: String, context: android.content.Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                LocaleHelper.setTag(context, tag)
                LocaleHelper.findActivity(context)?.recreate()
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = LocaleHelper.currentTag(context) == tag, onClick = {
            LocaleHelper.setTag(context, tag)
            LocaleHelper.findActivity(context)?.recreate()
        })
        Text(text = label, fontSize = 15.sp)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ThemeOption(label: String, mode: Int, context: android.content.Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { AppSettings.setThemeMode(context, mode) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = AppSettings.themeMode == mode, onClick = { AppSettings.setThemeMode(context, mode) })
        Text(text = label, fontSize = 15.sp)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 14.sp)
        Text(text = value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
