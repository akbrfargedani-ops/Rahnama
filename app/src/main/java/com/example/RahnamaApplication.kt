package com.example

import android.app.Application
import com.example.data.local.RahnamaDatabase
import com.example.data.prefs.SettingsRepository
import com.example.data.remote.RemoteMapServices
import com.example.data.repository.LocationRepository
import com.example.data.repository.MapDataRepository
import com.example.data.repository.OfflineMapRepository
import com.example.data.repository.VoiceGuidanceManager
import org.osmdroid.config.Configuration
import java.io.File

class RahnamaApplication : Application() {

    lateinit var database: RahnamaDatabase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var locationRepository: LocationRepository
        private set
    lateinit var voiceGuidanceManager: VoiceGuidanceManager
        private set
    lateinit var offlineMapRepository: OfflineMapRepository
        private set
    lateinit var mapDataRepository: MapDataRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Configure OSMDroid to use app-private storage (zero storage permissions needed)
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = "${packageName}/1.0 (RahnamaMaps)"
        val basePath = File(filesDir, "osmdroid").apply { mkdirs() }
        val tileCache = File(cacheDir, "osmdroid_tiles").apply { mkdirs() }
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = tileCache
        osmConfig.tileFileSystemCacheMaxBytes = 250L * 1024L * 1024L
        osmConfig.tileFileSystemCacheTrimBytes = 200L * 1024L * 1024L

        database = RahnamaDatabase.getInstance(this)
        settingsRepository = SettingsRepository(this)
        locationRepository = LocationRepository(this)
        voiceGuidanceManager = VoiceGuidanceManager(this)
        offlineMapRepository = OfflineMapRepository(this, database.rahnamaDao())
        mapDataRepository = MapDataRepository(
            context = this,
            dao = database.rahnamaDao(),
            remote = RemoteMapServices()
        )
    }
}
