package com.tsing.localcardwallet

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    currentCount: Int,
    cardLimit: Int,
    onBack: () -> Unit,
    onSave: (CardEntity) -> Unit,
) {
    val context = LocalContext.current
    var pan by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var autoFilledName by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var virtual by remember { mutableStateOf(false) }

    val digits = remember(pan) { CardValidator.digits(pan) }
    val outcome = remember(digits, bankName) { CardValidator.validate(context, digits, bankName) }

    LaunchedEffect(digits) {
        val bank = if (digits.length >= 6) BankBinRepository.lookup(digits) else null
        if (bank != null && (bankName.isEmpty() || bankName == autoFilledName)) {
            bankName = bank.name
            autoFilledName = bank.name
        }
    }

    val now = remember { Calendar.getInstance() }
    val monthValue = month.toIntOrNull()
    val yearValue = year.toIntOrNull()
    val expiryValid = month.length == 2 && year.length == 2 &&
        monthValue != null && monthValue in 1..12 &&
        yearValue != null && (2000 + yearValue) * 12 + monthValue >= (now.get(Calendar.YEAR)) * 12 + (now.get(Calendar.MONTH) + 1)
    val cvvValid = cvv.isEmpty() || cvv.length in 3..4
    val canSave = outcome.accepted && expiryValid && cvvValid && currentCount < cardLimit

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_cancel)) }
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
            Text(
                text = stringResource(R.string.add_pan),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = digits.chunked(4).joinToString(" "),
                onValueChange = { pan = it.filter { c -> c.isDigit() }.take(19) },
                placeholder = { Text("0000 0000 0000 0000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (outcome.type == "UNKNOWN") {
                        stringResource(R.string.add_network_unknown)
                    } else {
                        stringResource(R.string.add_network_prefix) + outcome.type
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (digits.length >= 6) {
                    Text(
                        text = if (outcome.bank != null) {
                            stringResource(R.string.add_bin_prefix) + outcome.bank.name
                        } else {
                            stringResource(R.string.add_bin_missing)
                        },
                        fontSize = 12.sp,
                        color = if (outcome.bank != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                if (digits.length >= 6) {
                    Text(
                        text = stringResource(R.string.add_kind_prefix) +
                            cardKindLabel(context, BankBinRepository.lookupKind(digits)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.add_bank_name),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = bankName,
                onValueChange = { if (outcome.bank == null) bankName = it },
                readOnly = outcome.bank != null,
                placeholder = { Text(stringResource(R.string.add_bank_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (outcome.bank != null) {
                Text(
                    text = stringResource(R.string.add_bank_locked_hint),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.add_expiry_month),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it.filter { c -> c.isDigit() }.take(2) },
                        placeholder = { Text("MM") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.add_expiry_year),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it.filter { c -> c.isDigit() }.take(2) },
                        placeholder = { Text("YY") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.add_cvv_optional),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { cvv = it.filter { c -> c.isDigit() }.take(4) },
                        placeholder = { Text("***") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.add_holder_optional),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = holder,
                onValueChange = { holder = it.take(40) },
                placeholder = { Text(stringResource(R.string.add_holder_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.add_virtual), fontSize = 15.sp)
                    Text(
                        text = stringResource(R.string.add_virtual_desc),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = virtual, onCheckedChange = { virtual = it })
            }

            Spacer(modifier = Modifier.height(12.dp))
            val errors = ArrayList<String>(outcome.errors)
            if (digits.isNotEmpty() && !expiryValid) errors.add(stringResource(R.string.add_error_expiry))
            if (!cvvValid) errors.add(stringResource(R.string.cvv_invalid))
            if (currentCount >= cardLimit) errors.add(stringResource(R.string.add_error_limit, cardLimit))

            if (errors.isNotEmpty() && (digits.isNotEmpty() || bankName.isNotEmpty())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        for (e in errors) {
                            Text(
                                text = "• " + e,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val card = CardEntity(
                        pan = outcome.pan,
                        expiry = month + "/" + year,
                        type = outcome.type,
                        bankCode = outcome.bank?.code,
                        bankName = bankName.trim().ifBlank { null },
                        holderName = holder.trim().ifBlank { null },
                        cvv = cvv.ifBlank { null },
                        isVirtual = virtual,
                        bankManual = outcome.bank == null,
                        cardKind = BankBinRepository.lookupKind(outcome.pan),
                        source = CardEntity.SOURCE_MANUAL,
                    )
                    onSave(card)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_submit))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.add_count, currentCount, cardLimit),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
