package com.example.data.repository

import android.content.Context
import com.example.data.local.OfflineRegionEntity
import com.example.data.local.RahnamaDao
import com.example.domain.model.OfflineRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox

data class DownloadProgressState(
    val isDownloading: Boolean = false,
    val regionId: String? = null,
    val regionNameFa: String = "",
    val completedTiles: Int = 0,
    val totalTiles: Int = 0,
    val errorMessage: String? = null
)

class OfflineMapRepository(
    private val context: Context,
    private val dao: RahnamaDao
) {
    private val _downloadProgress = MutableStateFlow(DownloadProgressState())
    val downloadProgress: StateFlow<DownloadProgressState> = _downloadProgress.asStateFlow()

    private var activeTask: CacheManager.CacheManagerTask? = null

    val offlineRegions: Flow<List<OfflineRegion>> = dao.observeOfflineRegions().map { list ->
        list.map {
            OfflineRegion(
                id = it.id,
                nameFa = it.nameFa,
                minLat = it.minLat,
                maxLat = it.maxLat,
                minLon = it.minLon,
                maxLon = it.maxLon,
                tileCount = it.tileCount,
                sizeBytes = it.sizeBytes,
                downloadedAt = it.downloadedAt
            )
        }
    }

    fun getCacheDirectorySizeBytes(): Long {
        val cacheDir = Configuration.getInstance().osmdroidTileCache ?: return 0L
        return calculateFolderSize(cacheDir)
    }

    private fun calculateFolderSize(dir: java.io.File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        return dir.listFiles()?.sumOf { calculateFolderSize(it) } ?: 0L
    }

    fun downloadRegion(
        regionId: String,
        nameFa: String,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        minZoom: Int = 11,
        maxZoom: Int = 14,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (_downloadProgress.value.isDownloading) {
            onComplete(false, "یک دانلود در حال انجام است.")
            return
        }

        val box = BoundingBox(maxLat, maxLon, minLat, minLon)
        val tileProvider = MapTileProviderBasic(context.applicationContext, TileSourceFactory.MAPNIK)
        val cacheManager = CacheManager(tileProvider, SqlTileWriter(), minZoom, maxZoom)
        val total = cacheManager.possibleTilesInArea(box, minZoom, maxZoom).coerceAtLeast(1)

        val sizeBefore = getCacheDirectorySizeBytes()

        _downloadProgress.value = DownloadProgressState(
            isDownloading = true,
            regionId = regionId,
            regionNameFa = nameFa,
            completedTiles = 0,
            totalTiles = total
        )

        activeTask = cacheManager.downloadAreaAsync(
            context,
            box,
            minZoom,
            maxZoom,
            object : CacheManager.CacheManagerCallback {
                override fun onTaskComplete() {
                    val sizeAfter = getCacheDirectorySizeBytes()
                    val deltaBytes = (sizeAfter - sizeBefore).coerceAtLeast(total * 12_000L)
                    _downloadProgress.value = DownloadProgressState(
                        isDownloading = false,
                        regionId = regionId,
                        regionNameFa = nameFa,
                        completedTiles = total,
                        totalTiles = total
                    )
                    activeTask = null
                    // Caller suspends/saves in Room via coroutine
                    onRegionSavedCallback?.invoke(
                        OfflineRegionEntity(
                            id = regionId,
                            nameFa = nameFa,
                            minLat = minLat,
                            maxLat = maxLat,
                            minLon = minLon,
                            maxLon = maxLon,
                            tileCount = total,
                            sizeBytes = deltaBytes,
                            downloadedAt = System.currentTimeMillis()
                        )
                    )
                    onComplete(true, null)
                }

                override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
                    _downloadProgress.value = _downloadProgress.value.copy(
                        completedTiles = progress.coerceAtMost(total)
                    )
                }

                override fun downloadStarted() {}

                override fun setPossibleTilesInArea(totalTiles: Int) {
                    _downloadProgress.value = _downloadProgress.value.copy(
                        totalTiles = totalTiles.coerceAtLeast(1)
                    )
                }

                override fun onTaskFailed(errors: Int) {
                    _downloadProgress.value = DownloadProgressState(
                        isDownloading = false,
                        errorMessage = "دانلود با خطا مواجه شد ($errors خطای شبکه)"
                    )
                    activeTask = null
                    onComplete(false, "خطا در دانلود کاشی‌های نقشه")
                }
            }
        )
    }

    var onRegionSavedCallback: ((OfflineRegionEntity) -> Unit)? = null

    fun cancelDownload() {
        runCatching { activeTask?.cancel(true) }
        activeTask = null
        _downloadProgress.value = DownloadProgressState(isDownloading = false)
    }

    suspend fun saveRegionMetadata(entity: OfflineRegionEntity) {
        dao.insertOfflineRegion(entity)
    }

    suspend fun deleteRegion(id: String) {
        dao.deleteOfflineRegion(id)
    }

    suspend fun clearAllTileCache() = withContext(Dispatchers.IO) {
        dao.clearOfflineRegions()
        val cacheDir = Configuration.getInstance().osmdroidTileCache
        cacheDir?.listFiles()?.forEach { file ->
            file.deleteRecursively()
        }
    }
}
