package com.tsing.localcardwallet

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

@Database(entities = [CardEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v2：BIN 识别结果、卡面风格、用户照片 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN bankCode TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN faceStyle TEXT NOT NULL DEFAULT 'auto'")
                db.execSQL("ALTER TABLE cards ADD COLUMN coverPath TEXT")
            }
        }

        /** v3：银行全名、持卡人、CVV、虚拟卡、手动指定标记 */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN bankName TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN holderName TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN cvv TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN isVirtual INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cards ADD COLUMN bankManual INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v4：借记/贷记类型、录入来源 */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN cardKind TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN source TEXT NOT NULL DEFAULT 'manual'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                // 0. 必须先加载 SQLCipher 的 native 库（.so），
                //    否则第一次建库会直接 UnsatisfiedLinkError 崩溃
                SQLiteDatabase.loadLibs(appContext)

                val passphrase: ByteArray = SQLiteDatabase.getBytes(getOrCreatePassphrase(appContext).toCharArray())
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "wallet_database"
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun getOrCreatePassphrase(context: Context): String {
            val prefs = context.getSharedPreferences("db_secure_prefs", Context.MODE_PRIVATE)
            var passphrase = prefs.getString("db_passphrase", null)
            if (passphrase == null) {
                val random = SecureRandom()
                val bytes = ByteArray(32)
                random.nextBytes(bytes)
                passphrase = bytes.joinToString("") { "%02x".format(it) }
                prefs.edit().putString("db_passphrase", passphrase).apply()
            }
            return passphrase
        }
    }
}
