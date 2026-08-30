package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

data class TransactionWithItems(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transaction_items WHERE transactionInvoiceNo = :invoiceNo")
    suspend fun getItemsForInvoice(invoiceNo: String): List<TransactionItemEntity>

    @Query("SELECT * FROM transactions WHERE invoiceNo = :invoiceNo LIMIT 1")
    suspend fun getTransactionByInvoice(invoiceNo: String): TransactionEntity?

    @Query("SELECT SUM(finalAmount) FROM transactions WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    suspend fun getTotalSalesBetween(startTimestamp: Long, endTimestamp: Long): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    suspend fun getTransactionCountBetween(startTimestamp: Long, endTimestamp: Long): Int

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET isSynced = 1 WHERE invoiceNo = :invoiceNo")
    suspend fun markAsSynced(invoiceNo: String)

    @Query("DELETE FROM transactions WHERE invoiceNo = :invoiceNo")
    suspend fun deleteTransaction(invoiceNo: String)

    @Query("DELETE FROM transaction_items WHERE transactionInvoiceNo = :invoiceNo")
    suspend fun deleteTransactionItems(invoiceNo: String)
}
