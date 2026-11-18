package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RahnamaDao {

    // Saved Places
    @Query("SELECT * FROM saved_places ORDER BY createdAt DESC")
    fun observeSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPlace(place: SavedPlaceEntity): Long

    @Update
    suspend fun updateSavedPlace(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deleteSavedPlace(id: Long)

    // Search History
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 30")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE displayName = :displayName")
    suspend fun deleteSearchHistoryByName(displayName: String)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchHistoryById(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // Street Reports
    @Query("SELECT * FROM street_reports ORDER BY createdAt DESC")
    fun observeStreetReports(): Flow<List<StreetReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreetReport(report: StreetReportEntity): Long

    @Query("UPDATE street_reports SET confirmations = confirmations + 1 WHERE id = :id")
    suspend fun confirmStreetReport(id: Long)

    @Query("DELETE FROM street_reports WHERE id = :id")
    suspend fun deleteStreetReport(id: Long)

    @Query("DELETE FROM street_reports WHERE expiresAt <= :nowMillis")
    suspend fun deleteExpiredReports(nowMillis: Long)

    // Offline Regions
    @Query("SELECT * FROM offline_regions ORDER BY downloadedAt DESC")
    fun observeOfflineRegions(): Flow<List<OfflineRegionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineRegion(region: OfflineRegionEntity)

    @Query("DELETE FROM offline_regions WHERE id = :id")
    suspend fun deleteOfflineRegion(id: String)

    @Query("DELETE FROM offline_regions")
    suspend fun clearOfflineRegions()
}
