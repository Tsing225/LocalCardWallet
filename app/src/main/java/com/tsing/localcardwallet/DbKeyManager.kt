package com.tsing.localcardwallet

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 数据库口令的保管者。
 *
 * SQLCipher 的口令是一串随机字符串，绝不能明文存放：它先经 Android Keystore
 * 里的 AES 密钥（GCM 模式，密钥本身不可导出）加密，才写入 SharedPreferences。
 * 旧版本曾把口令明文存在同一份 prefs 里，首次启动时会自动迁移并删除明文，
 * 迁移时原样复用旧口令（它是旧库的钥匙，绝不能换），已存卡片不受影响。
 *
 * 极端情况下 Keystore 密钥丢失（例如系统异常导致密钥失效），旧库将无法解密，
 * 此时只能清空数据库重建——这正是备份规则必须排除数据库的原因：
 * 备份出去的数据库在新设备上没有密钥，同样打不开。
 */
object DbKeyManager {
    private const val TAG = "DB_KEY"
    private const val PREF_NAME = "db_secure_prefs"
    private const val KEY_ENCRYPTED = "db_passphrase_enc"
    private const val KEY_LEGACY_PLAIN = "db_passphrase"
    private const val KEYSTORE_ALIAS = "lcw_db_key"
    private const val DB_NAME = "wallet_database"
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    fun getOrCreatePassphrase(context: Context): String {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // 1. 已有加密口令：解密后返回
        prefs.getString(KEY_ENCRYPTED, null)?.let { blob ->
            try {
                return decrypt(blob)
            } catch (e: Exception) {
                // 密钥丢失或密文损坏：旧库已无法打开，只能清空重建
                Log.e(TAG, "Stored database key unreadable, recreating database", e)
                wipeDatabase(appContext)
                prefs.edit().remove(KEY_ENCRYPTED).remove(KEY_LEGACY_PLAIN).apply()
                deleteKeystoreKey()
            }
        }

        // 2. 旧版明文口令：原样复用，加密后存回并删除明文
        prefs.getString(KEY_LEGACY_PLAIN, null)?.let { legacy ->
            val blob = encrypt(legacy)
            prefs.edit().putString(KEY_ENCRYPTED, blob).remove(KEY_LEGACY_PLAIN).apply()
            return legacy
        }

        // 3. 全新安装：生成随机口令并加密保存
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val fresh = bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        prefs.edit().putString(KEY_ENCRYPTED, encrypt(fresh)).apply()
        return fresh
    }

    private fun keystore(): KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        val ks = keystore()
        (ks.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(spec)
        return gen.generateKey()
    }

    private fun deleteKeystoreKey() {
        runCatching { keystore().deleteEntry(KEYSTORE_ALIAS) }
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    private fun decrypt(blobB64: String): String {
        val blob = Base64.decode(blobB64, Base64.NO_WRAP)
        require(blob.size > GCM_IV_BYTES) { "key blob too short" }
        val iv = blob.copyOfRange(0, GCM_IV_BYTES)
        val cipherText = blob.copyOfRange(GCM_IV_BYTES, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    /** 删除数据库文件与封面照片（仅在密钥不可恢复时调用）。 */
    private fun wipeDatabase(context: Context) {
        runCatching { context.deleteDatabase(DB_NAME) }
        runCatching {
            val covers = File(context.filesDir, "covers")
            covers.listFiles()?.forEach { it.delete() }
        }
    }
}
