package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val categoryName: String,
    val lat: Double,
    val lon: Double,
    val createdAt: Long
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val displayName: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val searchedAt: Long
)

@Entity(tableName = "street_reports")
data class StreetReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val typeName: String,
    val description: String,
    val lat: Double,
    val lon: Double,
    val addressLabel: String,
    val createdAt: Long,
    val expiresAt: Long,
    val isVerified: Boolean,
    val confirmations: Int
)

@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey val id: String,
    val nameFa: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val tileCount: Int,
    val sizeBytes: Long,
    val downloadedAt: Long
)
