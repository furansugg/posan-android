package com.posan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.posan.app.data.local.entity.PrintSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintSettingsDao {
    @Query("SELECT * FROM print_settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<PrintSettingsEntity?>

    @Query("SELECT * FROM print_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): PrintSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: PrintSettingsEntity)

    @Update
    suspend fun update(settings: PrintSettingsEntity)
}
