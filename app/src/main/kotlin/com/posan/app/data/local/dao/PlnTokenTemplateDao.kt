package com.posan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.posan.app.data.local.entity.PlnTokenTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlnTokenTemplateDao {
    @Query("SELECT * FROM pln_token_templates ORDER BY tarif ASC, daya ASC, nominalRupiah ASC")
    fun observeAll(): Flow<List<PlnTokenTemplateEntity>>

    @Query("SELECT * FROM pln_token_templates WHERE tarif = :tarif AND daya = :daya ORDER BY nominalRupiah ASC")
    fun observeForTariffDaya(tarif: String, daya: String): Flow<List<PlnTokenTemplateEntity>>

    @Query("SELECT * FROM pln_token_templates WHERE tarif = :tarif AND daya = :daya AND nominalRupiah = :nominal LIMIT 1")
    suspend fun findExact(tarif: String, daya: String, nominal: Int): PlnTokenTemplateEntity?

    @Query("SELECT * FROM pln_token_templates WHERE id = :id")
    suspend fun findById(id: Long): PlnTokenTemplateEntity?

    @Query("SELECT COUNT(*) FROM pln_token_templates")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: PlnTokenTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(templates: List<PlnTokenTemplateEntity>)

    @Update
    suspend fun update(template: PlnTokenTemplateEntity)

    @Delete
    suspend fun delete(template: PlnTokenTemplateEntity)

    @Query("DELETE FROM pln_token_templates")
    suspend fun deleteAll()
}
