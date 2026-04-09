package com.alittleapp.data.db.dao

import androidx.room.*
import com.alittleapp.data.db.entity.ClipboardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, createdAt DESC")
    fun getAllItems(): Flow<List<ClipboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardEntity): Long

    @Update
    suspend fun update(item: ClipboardEntity)

    @Delete
    suspend fun delete(item: ClipboardEntity)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun clearUnpinned()

    @Query("SELECT COUNT(*) FROM clipboard_items WHERE isPinned = 0")
    suspend fun unpinnedCount(): Int
}
