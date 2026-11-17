package com.tsing.localcardwallet

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** banks.json 里的一行银行信息 */
data class BankInfo(
    val code: String,
    val name: String,
    val country: String,
    val colorHex: String?,
) {
    /** 解析失败或数据缺失时为 null，调用方自行兜底 */
    val brandColor: Color?
        get() = colorHex?.let { hex ->
            runCatching { AndroidColor.parseColor(hex) }.getOrNull()?.let { Color(it) }
        }
}

/**
 * 本地 BIN 前缀查询。数据来自 assets/banks.json（banks-db 的裁剪快照）。
 * 全程离线，绝不发起网络请求。
 */
object BankBinRepository {
    private const val ASSET_NAME = "banks.json"

    @Volatile private var appContext: Context? = null
    @Volatile private var banks: Map<String, BankInfo> = emptyMap()
    @Volatile private var prefixes: Map<String, String> = emptyMap()
    @Volatile private var rules: List<Pair<String, String>> = emptyList()
    @Volatile private var kinds: Map<String, String> = emptyMap()
    @Volatile private var loaded = false

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    /** 在后台线程调用一次，避免首次渲染卡面时同步解析造成掉帧 */
    fun preload() {
        ensureLoaded()
    }

    private fun ensureLoaded(): Boolean {
        if (loaded) return true
        val ctx = appContext ?: return false
        synchronized(this) {
            if (loaded) return true
            val text = ctx.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val root = JSONObject(text)

            val bankMap = HashMap<String, BankInfo>()
            val bankObj = root.getJSONObject("banks")
            for (code in bankObj.keys()) {
                val o = bankObj.getJSONObject(code)
                bankMap[code] = BankInfo(
                    code = code,
                    name = o.optString("n", code),
                    country = o.optString("c", ""),
                    colorHex = o.optString("col", "").ifBlank { null },
                )
            }

            val prefixMap = HashMap<String, String>()
            val prefixObj = root.getJSONObject("prefixes")
            for (key in prefixObj.keys()) {
                val v = prefixObj.optString(key, "")
                if (v.isNotEmpty() && bankMap.containsKey(v)) prefixMap[key] = v
            }

            val kindMap = HashMap<String, String>()
            root.optJSONObject("types")?.let { t ->
                for (key in t.keys()) {
                    val v = t.optString(key, "")
                    if (v == "DC" || v == "CC") kindMap[key] = v
                }
            }

            val ruleList = ArrayList<Pair<String, String>>()
            root.optJSONObject("rules")?.let { r ->
                for (key in r.keys()) {
                    val v = r.optString(key, "")
                    if (v.isNotEmpty() && bankMap.containsKey(v)) ruleList.add(key to v)
                }
            }
            ruleList.sortByDescending { it.first.length }

            banks = bankMap
            prefixes = prefixMap
            rules = ruleList
            kinds = kindMap
            loaded = true
        }
        return true
    }

    /** 按卡号识别发卡行；识别失败返回 null */
    fun lookup(pan: String): BankInfo? {
        if (!ensureLoaded()) return null
        val digits = pan.filter { it.isDigit() }
        if (digits.length < 4) return null

        // 1. 精确前缀表（最长匹配，数据里是 5~8 位）
        val maxLen = minOf(8, digits.length)
        for (len in maxLen downTo 4) {
            val code = prefixes[digits.substring(0, len)]
            if (code != null) return banks[code]
        }
        // 2. 号段规则（如 Amex 的 34 / 37）
        for ((prefix, code) in rules) {
            if (digits.startsWith(prefix)) return banks[code]
        }
        return null
    }

    /** 供"手动指定发卡行"使用，按名称排序 */
    fun allBanks(): List<BankInfo> {
        if (!ensureLoaded()) return emptyList()
        return banks.values.sortedBy { it.name }
    }

    /** 借记/贷记：返回 "DC" 或 "CC"，未知返回 null */
    fun lookupKind(pan: String): String? {
        if (!ensureLoaded()) return null
        val digits = pan.filter { it.isDigit() }
        if (digits.length < 4) return null
        val maxLen = minOf(8, digits.length)
        for (len in maxLen downTo 4) {
            kinds[digits.substring(0, len)]?.let { return it }
        }
        return null
    }

    fun byCode(code: String?): BankInfo? {
        if (code == null) return null
        if (!ensureLoaded()) return null
        return banks[code]
    }
}
