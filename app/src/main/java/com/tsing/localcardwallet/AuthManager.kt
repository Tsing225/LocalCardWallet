package com.tsing.localcardwallet

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 离线本地账号。密码用 PBKDF2 加盐哈希后存本机，绝不出设备。
 * 注意：这是 App 级的访问门禁；数据库本身另有 SQLCipher 加密。
 * 忘记密码只能清空数据重置。
 */
object AuthManager {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_SALT = "salt"
    private const val KEY_HASH = "hash"
    private const val KEY_FAILS = "fails"
    private const val KEY_LOCKOUT_UNTIL = "lockout_until"
    private const val MAX_FAILS_BEFORE_LOCKOUT = 5
    private const val BASE_LOCKOUT_MS = 30_000L
    private const val MAX_LOCKOUT_MS = 300_000L
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    const val MIN_PASSWORD_LENGTH = 8

    /** 返回 null 表示密码达标 */
    fun validatePassword(context: Context, password: String): String? {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return context.getString(R.string.password_error_short, MIN_PASSWORD_LENGTH)
        }
        if (!password.any { it.isDigit() }) return context.getString(R.string.password_error_digit)
        if (!password.any { it.isLowerCase() }) return context.getString(R.string.password_error_lower)
        if (!password.any { it.isUpperCase() }) return context.getString(R.string.password_error_upper)
        if (Regex("(.)\\1{2,}").containsMatchIn(password)) {
            return context.getString(R.string.password_error_repeat)
        }
        return null
    }

    var unlocked by mutableStateOf(false)
        private set

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun hasAccount(context: Context): Boolean {
        val p = prefs(context)
        return p.contains(KEY_SALT) && p.contains(KEY_HASH)
    }

    /** 返回 null 表示成功 */
    fun createAccount(context: Context, password: String): String? {
        validatePassword(context, password)?.let { return it }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt)
        prefs(context).edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
        unlocked = true
        return null
    }

    /** 锁屏剩余锁定毫秒数；0 表示未锁定。锁定状态存进 prefs，杀进程也绕不过。 */
    fun lockoutRemainingMs(context: Context): Long {
        val until = prefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun verify(context: Context, password: String): Boolean {
        if (lockoutRemainingMs(context) > 0) return false
        val ok = checkPassword(context, password)
        if (ok) {
            clearFailures(context)
            unlocked = true
        } else {
            recordFailure(context)
        }
        return ok
    }

    private fun recordFailure(context: Context) {
        val p = prefs(context)
        val fails = p.getInt(KEY_FAILS, 0) + 1
        val edit = p.edit().putInt(KEY_FAILS, fails)
        if (fails >= MAX_FAILS_BEFORE_LOCKOUT) {
            // 30 秒起步，每多错一次翻倍，上限 5 分钟
            val shift = (fails - MAX_FAILS_BEFORE_LOCKOUT).coerceAtMost(4)
            val lockMs = (BASE_LOCKOUT_MS shl shift).coerceAtMost(MAX_LOCKOUT_MS)
            edit.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + lockMs)
        }
        edit.apply()
    }

    private fun clearFailures(context: Context) {
        prefs(context).edit().remove(KEY_FAILS).remove(KEY_LOCKOUT_UNTIL).apply()
    }

    private fun checkPassword(context: Context, password: String): Boolean {
        val p = prefs(context)
        val saltB64 = p.getString(KEY_SALT, null) ?: return false
        val hashB64 = p.getString(KEY_HASH, null) ?: return false
        return try {
            val salt = Base64.decode(saltB64, Base64.NO_WRAP)
            val expected = Base64.decode(hashB64, Base64.NO_WRAP)
            val actual = derive(password, salt)
            MessageDigest.isEqual(expected, actual)
        } catch (e: Exception) {
            false
        }
    }

    fun changePassword(context: Context, oldPassword: String, newPassword: String): String? {
        if (!checkPassword(context, oldPassword)) return context.getString(R.string.password_error_old)
        validatePassword(context, newPassword)?.let { return it }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(newPassword, salt)
        prefs(context).edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
        return null
    }

    fun unlockWithBiometric() {
        unlocked = true
    }

    fun lock() {
        unlocked = false
    }

    fun resetAccount(context: Context) {
        prefs(context).edit().clear().apply()
        unlocked = false
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val factory = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (e: Exception) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return factory.generateSecret(spec).encoded
    }
}
