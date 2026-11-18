package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.RahnamaDao
import com.example.data.local.SavedPlaceEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.StreetReportEntity
import com.example.data.remote.RemoteMapServices
import com.example.domain.model.DistanceUnit
import com.example.domain.model.GeoPointModel
import com.example.domain.model.NearbyCategory
import com.example.domain.model.PlaceResult
import com.example.domain.model.ReportType
import com.example.domain.model.RouteOption
import com.example.domain.model.SavedCategory
import com.example.domain.model.SavedPlace
import com.example.domain.model.SearchHistoryItem
import com.example.domain.model.StreetReport
import com.example.domain.model.TravelMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MapDataRepository(
    private val context: Context,
    private val dao: RahnamaDao,
    private val remote: RemoteMapServices
) {

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Saved Places
    val savedPlaces: Flow<List<SavedPlace>> = dao.observeSavedPlaces().map { list ->
        list.map { e ->
            SavedPlace(
                id = e.id,
                name = e.name,
                address = e.address,
                category = runCatching { SavedCategory.valueOf(e.categoryName) }
                    .getOrDefault(SavedCategory.FAVORITES),
                lat = e.lat,
                lon = e.lon,
                createdAt = e.createdAt
            )
        }
    }

    suspend fun addSavedPlace(
        name: String,
        address: String,
        category: SavedCategory,
        lat: Double,
        lon: Double
    ) {
        dao.insertSavedPlace(
            SavedPlaceEntity(
                name = name.trim(),
                address = address.trim(),
                categoryName = category.name,
                lat = lat,
                lon = lon,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSavedPlace(place: SavedPlace) {
        dao.updateSavedPlace(
            SavedPlaceEntity(
                id = place.id,
                name = place.name.trim(),
                address = place.address.trim(),
                categoryName = place.category.name,
                lat = place.lat,
                lon = place.lon,
                createdAt = place.createdAt
            )
        )
    }

    suspend fun deleteSavedPlace(id: Long) {
        dao.deleteSavedPlace(id)
    }

    // Search History
    val searchHistory: Flow<List<SearchHistoryItem>> = dao.observeSearchHistory().map { list ->
        list.map {
            SearchHistoryItem(
                id = it.id,
                query = it.query,
                displayName = it.displayName,
                address = it.address,
                lat = it.lat,
                lon = it.lon,
                searchedAt = it.searchedAt
            )
        }
    }

    suspend fun recordSearchSelection(query: String, place: PlaceResult) {
        dao.deleteSearchHistoryByName(place.name)
        dao.insertSearchHistory(
            SearchHistoryEntity(
                query = query,
                displayName = place.name,
                address = place.address ?: "اطلاعات در دسترس نیست",
                lat = place.lat,
                lon = place.lon,
                searchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSearchHistoryItem(id: Long) {
        dao.deleteSearchHistoryById(id)
    }

    suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }

    // Street Reports
    val streetReports: Flow<List<StreetReport>> = dao.observeStreetReports().map { list ->
        list.map { e ->
            StreetReport(
                id = e.id,
                type = runCatching { ReportType.valueOf(e.typeName) }.getOrDefault(ReportType.HAZARD),
                description = e.description,
                lat = e.lat,
                lon = e.lon,
                addressLabel = e.addressLabel,
                createdAt = e.createdAt,
                expiresAt = e.expiresAt,
                isVerified = e.isVerified,
                confirmations = e.confirmations
            )
        }
    }

    suspend fun addStreetReport(
        type: ReportType,
        description: String,
        lat: Double,
        lon: Double,
        addressLabel: String
    ) {
        val now = System.currentTimeMillis()
        val expiresAt = now + type.ttlHours * 3600_000L
        dao.insertStreetReport(
            StreetReportEntity(
                typeName = type.name,
                description = description.trim(),
                lat = lat,
                lon = lon,
                addressLabel = addressLabel.ifBlank { "اطلاعات در دسترس نیست" },
                createdAt = now,
                expiresAt = expiresAt,
                isVerified = false, // Always marked unverified per policy
                confirmations = 1
            )
        )
    }

    suspend fun confirmStreetReport(id: Long) {
        dao.confirmStreetReport(id)
    }

    suspend fun deleteStreetReport(id: Long) {
        dao.deleteStreetReport(id)
    }

    suspend fun purgeExpiredReports() {
        dao.deleteExpiredReports(System.currentTimeMillis())
    }

    // Remote queries
    suspend fun searchPlaces(query: String, biasCenter: GeoPointModel?): Result<List<PlaceResult>> {
        if (!isNetworkAvailable()) {
            return Result.failure(IllegalStateException("اتصال اینترنت برقرار نیست."))
        }
        return runCatching { remote.searchPlaces(query, biasCenter) }
    }

    suspend fun reverseGeocode(point: GeoPointModel): PlaceResult? {
        if (!isNetworkAvailable()) return null
        return remote.reverseGeocode(point)
    }

    suspend fun fetchNearby(
        center: GeoPointModel,
        category: NearbyCategory,
        radiusMeters: Int = 2500
    ): Result<List<PlaceResult>> {
        if (!isNetworkAvailable()) {
            return Result.failure(IllegalStateException("برای دریافت مکان‌های اطراف، اتصال اینترنت لازم است."))
        }
        return runCatching { remote.fetchNearbyPlaces(center, category, radiusMeters) }
    }

    suspend fun fetchRoutes(
        origin: GeoPointModel,
        destination: GeoPointModel,
        mode: TravelMode,
        unit: DistanceUnit
    ): Result<List<RouteOption>> {
        if (!isNetworkAvailable()) {
            return Result.failure(
                IllegalStateException("مسیریابی نیازمند اتصال اینترنت است. در حالت آفلاین فقط مشاهده نقشه دانلودشده ممکن است.")
            )
        }
        return runCatching { remote.fetchRoutes(origin, destination, mode, unit) }
    }
}
