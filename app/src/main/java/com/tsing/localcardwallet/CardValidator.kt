package com.tsing.localcardwallet

import android.content.Context

/**
 * 添加卡片时的严格校验：卡号 Luhn、卡号长度、卡组织、BIN 与银行名一致性。
 */
object CardValidator {
    data class Outcome(
        val pan: String,
        val type: String,
        val bank: BankInfo?,
        val errors: List<String>,
    ) {
        val accepted: Boolean get() = errors.isEmpty()
    }

    fun digits(raw: String): String = raw.filter { it.isDigit() }

    fun luhn(pan: String): Boolean {
        if (pan.length < 12) return false
        var sum = 0
        var double = false
        for (i in pan.length - 1 downTo 0) {
            var d = pan[i] - '0'
            if (d < 0 || d > 9) return false
            if (double) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            double = !double
        }
        return sum % 10 == 0
    }

    /** 与 EMV 的 CardType 命名保持一致，方便 CardLogo 复用 */
    fun detectType(pan: String): String {
        if (pan.isEmpty()) return "UNKNOWN"
        val p2 = if (pan.length >= 2) pan.substring(0, 2).toIntOrNull() ?: -1 else -1
        val p4 = if (pan.length >= 4) pan.substring(0, 4).toIntOrNull() ?: -1 else -1
        return when {
            pan.startsWith("4") -> "VISA"
            p2 in 51..55 -> "MASTER CARD"
            p4 in 2221..2720 -> "MASTER CARD"
            pan.startsWith("34") || pan.startsWith("37") -> "AMERICAN EXPRESS"
            pan.startsWith("35") -> "JCB"
            pan.startsWith("6011") || p2 == 65 -> "DISCOVER"
            pan.startsWith("62") -> "UNIONPAY"
            pan.startsWith("3") -> "DINERS CLUB"
            else -> "UNKNOWN"
        }
    }

    private fun typeAllowsLength(type: String, length: Int): Boolean = when (type) {
        "AMERICAN EXPRESS" -> length == 15
        "VISA" -> length == 13 || length == 16 || length == 19
        "DINERS CLUB" -> length == 14
        else -> length in 16..19
    }

    private fun normalizeName(s: String): String = s.replace("\\s".toRegex(), "")

    fun validate(context: Context, panInput: String, bankNameInput: String): Outcome {
        val pan = digits(panInput)
        val errors = ArrayList<String>()
        val type = detectType(pan)
        val bank = if (pan.length >= 6) BankBinRepository.lookup(pan) else null

        if (pan.isEmpty()) {
            errors.add(context.getString(R.string.add_error_pan_empty))
        } else {
            if (pan.length !in 12..19) {
                errors.add(context.getString(R.string.add_error_pan_length))
            } else if (!typeAllowsLength(type, pan.length)) {
                errors.add(context.getString(R.string.add_error_pan_unsupported, pan.length))
            }
            if (!luhn(pan)) errors.add(context.getString(R.string.add_error_luhn))
        }

        val name = bankNameInput.trim()
        if (name.isEmpty()) {
            errors.add(context.getString(R.string.add_error_bank_empty))
        } else if (bank != null) {
            val a = normalizeName(name)
            val b = normalizeName(bank.name)
            if (!(a.contains(b) || b.contains(a))) {
                errors.add(context.getString(R.string.add_error_bank_mismatch, bank.name, name))
            }
        }

        return Outcome(pan, type, bank, errors)
    }
}
