package com.measurepulse.ar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomScanDao {
    @Query("SELECT * FROM room_scans ORDER BY isFeatured DESC, createdAt DESC")
    fun observeAll(): Flow<List<RoomScanEntity>>

    @Query("SELECT * FROM room_scans WHERE id = :id")
    fun observeById(id: Long): Flow<RoomScanEntity?>

    @Query("SELECT * FROM room_scans ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(): RoomScanEntity?

    @Query("SELECT * FROM room_scans WHERE category IN ('FLOORPLAN','AR_TAPE') ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestRoom(): RoomScanEntity?

    @Query("SELECT * FROM room_scans WHERE id = :id")
    suspend fun byId(id: Long): RoomScanEntity?

    @Query("SELECT COUNT(*) FROM room_scans")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(scans: List<RoomScanEntity>)

    @Insert
    suspend fun insert(scan: RoomScanEntity): Long

    @Update
    suspend fun update(scan: RoomScanEntity)

    @Query("UPDATE room_scans SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("DELETE FROM room_scans WHERE id = :id")
    suspend fun delete(id: Long)
}
