package com.tsing.localcardwallet

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 把用户选中的照片复制到 App 私有目录。
 * Photo Picker 返回的 URI 重启后可能失效，所以必须落盘一份。
 * 返回相对 filesDir 的路径，失败返回 null。
 */
internal suspend fun copyCoverPhoto(context: Context, uri: Uri, cardId: Long): String? =
    withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "covers")
            if (!dir.exists() && !dir.mkdirs()) return@withContext null
            val prefix = cardId.toString() + "_"
            dir.listFiles()?.forEach { old -> if (old.name.startsWith(prefix)) old.delete() }
            val target = File(dir, prefix + System.currentTimeMillis() + ".img")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            "covers/" + target.name
        } catch (e: Exception) {
            Log.e("CARD_FACE", "复制卡面照片失败: " + e.message)
            null
        }
    }
