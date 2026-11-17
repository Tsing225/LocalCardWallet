package com.tsing.localcardwallet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY id DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCard(card: CardEntity): Long

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun countCards(): Int

    @Query("UPDATE cards SET faceStyle = :style, coverPath = :coverPath WHERE id = :cardId")
    suspend fun updateCardFace(cardId: Long, style: String, coverPath: String?)

    @Query("UPDATE cards SET bankCode = :bankCode WHERE id = :cardId")
    suspend fun updateBankCode(cardId: Long, bankCode: String?)

    @Query("UPDATE cards SET bankCode = :code, bankName = :name, bankManual = :manual WHERE id = :cardId")
    suspend fun updateBankInfo(cardId: Long, code: String?, name: String?, manual: Boolean)

    @Query("UPDATE cards SET holderName = :holder, cvv = :cvv, cardKind = :kind WHERE id = :cardId")
    suspend fun updateCardInfo(cardId: Long, holder: String?, cvv: String?, kind: String?)

    @Query("SELECT * FROM cards WHERE bankCode IS NULL")
    suspend fun getCardsWithoutBank(): List<CardEntity>

    @Query("SELECT * FROM cards")
    suspend fun getAllCardsOnce(): List<CardEntity>
}
