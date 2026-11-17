package com.tsing.localcardwallet

import android.content.Context
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cards", indices = [Index(value = ["pan"], unique = true)])
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 卡号，纯数字 */
    val pan: String,
    /** MM/yy */
    val expiry: String,
    /** VISA / MASTER CARD / UNIONPAY / AMERICAN EXPRESS ... */
    val type: String,
    /** BIN 识别或手动指定的银行代码 */
    val bankCode: String? = null,
    /** 完整银行名称（识别结果，或用户手动填写） */
    val bankName: String? = null,
    val holderName: String? = null,
    /** 可选，仅本机加密库内保存 */
    val cvv: String? = null,
    /** 借记 / 贷记：DC 借记卡，CC 贷记卡，null 未知 */
    val cardKind: String? = null,
    /** 录入来源：nfc 表示芯片读取，manual 表示手工录入 */
    val source: String = SOURCE_MANUAL,
    /** 卡面风格：auto / g1..g8 / photo */
    val faceStyle: String = FaceStyle.AUTO,
    /** 用户自选照片相对 filesDir 的路径 */
    val coverPath: String? = null,
    /** 虚拟卡（无实体卡面） */
    val isVirtual: Boolean = false,
    /** 发卡行由用户手动指定，自动识别不会覆盖 */
    val bankManual: Boolean = false,
) {
    companion object {
        const val SOURCE_NFC = "nfc"
        const val SOURCE_MANUAL = "manual"
    }
}

/** 卡片类型展示文案 */
fun cardKindLabel(context: Context, kind: String?): String = when (kind) {
    "DC" -> context.getString(R.string.kind_debit)
    "CC" -> context.getString(R.string.kind_credit)
    else -> context.getString(R.string.kind_unknown)
}
