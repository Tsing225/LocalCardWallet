package com.tsing.localcardwallet

import android.content.Context
import android.widget.Toast
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.github.devnied.emvnfccard.parser.EmvTemplate
import com.github.devnied.emvnfccard.parser.IProvider
import com.tsing.localcardwallet.ui.theme.LocalCardWalletTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : FragmentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var db: AppDatabase

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BankBinRepository.init(this)
        AppSettings.init(this)
        // 先同步加载 BIN 表（约 260KB），避免首帧渲染时在主线程解析造成卡顿
        runCatching { BankBinRepository.preload() }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        db = AppDatabase.getDatabase(this)

        // 预加载 BIN 表，并给迁移前录入的历史卡片补齐发卡行信息
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                BankBinRepository.preload()
            } catch (e: Exception) {
                Log.e("BIN_SELFTEST", "加载失败: " + e.message, e)
            }
            // 只补没有发卡行的卡；用户手动指定过的不会被覆盖
            for (card in db.cardDao().getCardsWithoutBank()) {
                val bank = BankBinRepository.lookup(card.pan) ?: continue
                db.cardDao().updateBankInfo(card.id, bank.code, bank.name, false)
            }
            // 老版本只存了 bankCode，补上完整银行名
            for (card in db.cardDao().getAllCardsOnce()) {
                if (card.bankName.isNullOrBlank() && card.bankCode != null) {
                    val name = BankBinRepository.byCode(card.bankCode)?.name
                    if (name != null) {
                        db.cardDao().updateBankInfo(card.id, card.bankCode, name, card.bankManual)
                    }
                }
            }
        }

        setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Wallet) }
            val savedCards by db.cardDao().getAllCards().collectAsState(initial = emptyList())

            BackHandler(enabled = screen != Screen.Wallet) { screen = Screen.Wallet }

            LocalCardWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (!AuthManager.unlocked) {
                        LockScreen(
                            onResetData = {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    for (c in db.cardDao().getAllCardsOnce()) {
                                        c.coverPath?.let { path ->
                                            runCatching { File(filesDir, path).delete() }
                                        }
                                        db.cardDao().deleteCardById(c.id)
                                    }
                                    AuthManager.resetAccount(this@MainActivity)
                                }
                            },
                        )
                    } else {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(initialScale = 0.97f, animationSpec = tween(200)))
                                .togetherWith(fadeOut(tween(120)))
                        },
                        label = "screen",
                    ) { s ->
                    when (s) {
                        Screen.Wallet -> WalletScreen(
                            cards = savedCards,
                            onOpenSettings = { screen = Screen.Settings },
                            onAddCard = { screen = Screen.AddCard },
                            onOpenDetail = { card -> screen = Screen.Detail(card.id) },
                            onDelete = { card ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    card.coverPath?.let { path ->
                                        runCatching { File(filesDir, path).delete() }
                                    }
                                    db.cardDao().deleteCardById(card.id)
                                }
                            },
                            onUpdateFace = { card, style, coverPath ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    db.cardDao().updateCardFace(card.id, style, coverPath)
                                }
                            },
                            onUpdateBank = { card, code ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val finalCode = code ?: BankBinRepository.lookup(card.pan)?.code
                                    db.cardDao().updateBankInfo(
                                        card.id,
                                        finalCode,
                                        BankBinRepository.byCode(finalCode)?.name,
                                        code != null
                                    )
                                }
                            },
                        )
                        Screen.Settings -> SettingsScreen(
                            cardCount = savedCards.size,
                            cardLimit = CARD_LIMIT,
                            onBack = { screen = Screen.Wallet },
                            onClearAll = {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    for (c in db.cardDao().getAllCardsOnce()) {
                                        c.coverPath?.let { path ->
                                            runCatching { File(filesDir, path).delete() }
                                        }
                                        db.cardDao().deleteCardById(c.id)
                                    }
                                }
                            },
                        )
                        Screen.AddCard -> AddCardScreen(
                            currentCount = savedCards.size,
                            cardLimit = CARD_LIMIT,
                            onBack = { screen = Screen.Wallet },
                            onSave = { card ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    if (db.cardDao().countCards() < CARD_LIMIT) {
                                        db.cardDao().insertCard(card)
                                    }
                                }
                                screen = Screen.Wallet
                            },
                        )
                        is Screen.Detail -> {
                            val detailCard = savedCards.firstOrNull { it.id == s.cardId }
                            if (detailCard != null) {
                                CardDetailScreen(
                                    card = detailCard,
                                    onBack = { screen = Screen.Wallet },
                                    onDelete = {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            detailCard.coverPath?.let { p ->
                                                runCatching { File(filesDir, p).delete() }
                                            }
                                            db.cardDao().deleteCardById(detailCard.id)
                                        }
                                        screen = Screen.Wallet
                                    },
                                    onUpdateFace = { c, style, path ->
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            db.cardDao().updateCardFace(c.id, style, path)
                                        }
                                    },
                                    onUpdateBank = { c, code ->
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val finalCode = code ?: BankBinRepository.lookup(c.pan)?.code
                                            db.cardDao().updateBankInfo(
                                                c.id,
                                                finalCode,
                                                BankBinRepository.byCode(finalCode)?.name,
                                                code != null
                                            )
                                        }
                                    },
                                    onUpdateInfo = { c, holder, cvv, kind ->
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            db.cardDao().updateCardInfo(c.id, holder, cvv, kind)
                                        }
                                    },
                                )
                            }
                        }
                    }
                    }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(this, { tag ->
            try {
                val isoDep = IsoDep.get(tag)
                if (isoDep != null) {
                    isoDep.connect()
                    isoDep.timeout = 5000

                    val provider = IsoDepProvider(isoDep)
                    val config = EmvTemplate.Config()
                        .setContactLess(true)
                        .setReadAllAids(true)
                        .setReadTransactions(false)
                        .setRemoveDefaultParsers(false)
                        .setReadAt(true)

                    val parser = EmvTemplate.Builder()
                        .setProvider(provider)
                        .setConfig(config)
                        .build()

                    val card = parser.readEmvCard()

                    val dateFormatter = SimpleDateFormat("MM/yy", Locale.getDefault())
                    val formattedExpiry = card.expireDate?.let { dateFormatter.format(it) } ?: "未知"

                    val pan = card.cardNumber.orEmpty()
                    val bank = BankBinRepository.lookup(pan)
                    // ★ 统一转为大写，方便后面匹配颜色和Logo
                    val newCard = CardEntity(
                        pan = pan,
                        expiry = formattedExpiry,
                        type = card.type?.name?.replace("_", " ")?.uppercase() ?: "UNKNOWN",
                        bankCode = bank?.code,
                        bankName = bank?.name,
                        cardKind = BankBinRepository.lookupKind(pan),
                        source = CardEntity.SOURCE_NFC,
                    )

                    if (newCard.pan.isEmpty()) {
                        Log.w("NFC_READ_ERROR", "卡片未返回卡号，已跳过保存")
                    } else {
                        lifecycleScope.launch(Dispatchers.IO) {
                            if (db.cardDao().countCards() < CARD_LIMIT) {
                                db.cardDao().insertCard(newCard) // DAO 里设置了 IGNORE，不会重复插入
                            }
                        }
                    }

                    isoDep.close()
                }
            } catch (e: Exception) {
                Log.e("NFC_READ_ERROR", "读取失败: " + e.message)
            }
        }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }
}

// 列表界面
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    cards: List<CardEntity>,
    onOpenSettings: () -> Unit,
    onAddCard: () -> Unit,
    onOpenDetail: (CardEntity) -> Unit,
    onDelete: (CardEntity) -> Unit,
    onUpdateFace: (CardEntity, String, String?) -> Unit,
    onUpdateBank: (CardEntity, String?) -> Unit,
) {
    var showFullPan by remember { mutableStateOf(false) }
    var actionCard by remember { mutableStateOf<CardEntity?>(null) }   // 长按操作菜单
    var pickerCard by remember { mutableStateOf<CardEntity?>(null) }   // 换卡面面板
    var cardToDelete by remember { mutableStateOf<CardEntity?>(null) } // 删除确认
    var pendingPhotoCard by remember { mutableStateOf<CardEntity?>(null) }
    var bankPickerCard by remember { mutableStateOf<CardEntity?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingPhotoCard
        pendingPhotoCard = null
        if (uri != null && target != null) {
            scope.launch {
                val path = copyCoverPhoto(context, uri, target.id)
                if (path != null) {
                    onUpdateFace(target, FaceStyle.PHOTO, path)
                    pickerCard = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    )
                )
            )
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 顶部标题栏和开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_title),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.home_subtitle, cards.size),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.4.sp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BounceClick(onClick = onOpenSettings, pressedScale = 0.85f) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                BounceClick(onClick = onAddCard, pressedScale = 0.85f) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_add_card),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.show_full_pan), fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
                checked = showFullPan,
                onCheckedChange = { checked ->
                    if (!checked) {
                        showFullPan = false
                    } else {
                        val hostActivity = context as? FragmentActivity
                        if (!AppSettings.biometricForPan ||
                            hostActivity == null ||
                            !BiometricGate.isAvailable(context)
                        ) {
                            showFullPan = true
                        } else {
                            BiometricGate.authenticate(
                                activity = hostActivity,
                                title = "验证身份",
                                subtitle = "验证后显示完整卡号",
                                onSuccess = { showFullPan = true },
                                onFail = {
                                    Toast.makeText(context, "验证未通过：" + it, Toast.LENGTH_SHORT).show()
                                },
                            )
                        }
                    }
                },
                modifier = Modifier.scale(0.8f),
            )
        }

        if (cards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.empty_waiting), color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            // 一摞卡：卡片本身完整绘制，靠 zIndex 让后面的卡压住前面的卡的下半部分，
            // 每张只露出上半部分；最后一张完整可见。
            var previewCardId by remember { mutableStateOf<Long?>(null) }
            val overlap = 122.dp
            val cardHeight = 200.dp
            val totalHeight = cardHeight + overlap * (cards.size - 1).coerceAtLeast(0).toFloat()
            val blankInteraction = remember { MutableInteractionSource() }
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
                    .verticalScroll(scrollState)
                    .clickable(
                        interactionSource = blankInteraction,
                        indication = null,
                    ) { previewCardId = null },
            ) {
                cards.forEachIndexed { index, card ->
                    val isPreview = previewCardId == card.id
                    val lift by animateDpAsState(
                        targetValue = if (isPreview) (-12).dp else 0.dp,
                        label = "lift",
                    )
                    val previewScale by animateFloatAsState(
                        targetValue = if (isPreview) 1.02f else 1f,
                        label = "previewScale",
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = overlap * index.toFloat() + lift)
                            .zIndex(if (isPreview) 1000f else index.toFloat())
                            .graphicsLayer {
                                scaleX = previewScale
                                scaleY = previewScale
                            }
                    ) {
                        WalletCard(
                            card = card,
                            showFullPan = showFullPan,
                            onLongClick = { actionCard = card },
                            onClick = {
                                // 第一次点击：预览这张完整卡面；再点一次：进详情
                                if (isPreview) onOpenDetail(card) else previewCardId = card.id
                            },
                        )
                    }
                }
            }

            // 卡片下方的空白：点这里收起预览
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = blankInteraction,
                        indication = null,
                    ) { previewCardId = null },
            )
        }
    }

    // ★ 长按后的操作菜单
    actionCard?.let { card ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { actionCard = null }, sheetState = sheetState) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text = stringResource(R.string.face_tail, card.pan.takeLast(4)),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SheetActionRow(stringResource(R.string.action_change_face), MaterialTheme.colorScheme.onSurface) {
                    actionCard = null
                    pickerCard = card
                }
                SheetActionRow(stringResource(R.string.action_delete_card), MaterialTheme.colorScheme.error) {
                    actionCard = null
                    cardToDelete = card
                }
            }
        }
    }

    // ★ 换卡面面板
    pickerCard?.let { card ->
        FacePickerSheet(
            card = card,
            onDismiss = { pickerCard = null },
            onSelectStyle = { style ->
                onUpdateFace(card, style, null)
                pickerCard = null
            },
            onResetAuto = {
                onUpdateFace(card, FaceStyle.AUTO, null)
                pickerCard = null
            },
            onPickPhoto = {
                pendingPhotoCard = card
                photoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickBank = {
                pickerCard = null
                bankPickerCard = card
            },
            onAutoDetectBank = {
                onUpdateBank(card, null)
                pickerCard = null
            },
        )
    }

    // ★ 手动指定发卡行
    bankPickerCard?.let { card ->
        BankPickerDialog(
            onDismiss = { bankPickerCard = null },
            onPick = { bank ->
                onUpdateBank(card, bank.code)
                bankPickerCard = null
            },
        )
    }

    // ★ 删除确认弹窗
    if (cardToDelete != null) {
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text(stringResource(R.string.action_delete_card)) },
            text = { Text(stringResource(R.string.delete_confirm_message, cardToDelete!!.pan.takeLast(4))) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(cardToDelete!!)
                    cardToDelete = null
                }) {
                    Text(stringResource(R.string.action_delete), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SheetActionRow(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        color = color,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

/** 单机卡包最多保存 100 张卡 */
private const val CARD_LIMIT = 100

sealed class Screen {
    object Wallet : Screen()
    object Settings : Screen()
    object AddCard : Screen()
    data class Detail(val cardId: Long) : Screen()
}

class IsoDepProvider(private val isoDep: IsoDep) : IProvider {
    override fun transceive(command: ByteArray): ByteArray = isoDep.transceive(command)
    override fun getAt(): ByteArray? = isoDep.historicalBytes
}
