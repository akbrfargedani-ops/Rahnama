package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RahnamaApplication
import com.example.data.local.OfflineRegionEntity
import com.example.data.repository.DownloadProgressState
import com.example.data.repository.TtsStatus
import com.example.domain.model.AppSettings
import com.example.domain.model.DistanceUnit
import com.example.domain.model.GeoPointModel
import com.example.domain.model.LocationState
import com.example.domain.model.MapLayerType
import com.example.domain.model.NearbyCategory
import com.example.domain.model.OfflineCityPreset
import com.example.domain.model.OfflineRegion
import com.example.domain.model.PlaceResult
import com.example.domain.model.ReportType
import com.example.domain.model.RouteOption
import com.example.domain.model.SavedCategory
import com.example.domain.model.SavedPlace
import com.example.domain.model.SearchHistoryItem
import com.example.domain.model.StreetReport
import com.example.domain.model.ThemePreference
import com.example.domain.model.TravelMode
import com.example.util.GeoUtils
import com.example.util.PersianManeuverTranslator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab(val route: String, val labelFa: String) {
    MAP("map", "نقشه"),
    SEARCH("search", "جستوجو"),
    NEARBY("nearby", "اطراف من"),
    SAVED("saved", "ذخیره‌شده‌ها"),
    PROFILE("profile", "حساب کاربری")
}

enum class RouteFieldTarget {
    ORIGIN,
    DESTINATION
}

data class MapBoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)

data class MapCameraCommand(
    val id: Long = System.currentTimeMillis(),
    val target: GeoPointModel,
    val zoom: Double? = null,
    val orientationDeg: Float? = null
)

data class MapUiState(
    val center: GeoPointModel = GeoUtils.DEFAULT_TEHRAN_CENTER,
    val selectedCityNameFa: String = "تهران",
    val isGpsOutsideIran: Boolean = false,
    val zoomLevel: Double = 14.5,
    val orientationDeg: Float = 0f,
    val layer: MapLayerType = MapLayerType.STANDARD,
    val showTrafficDialog: Boolean = false,
    val showLayersSheet: Boolean = false,
    val showReportDialog: Boolean = false,
    val isFollowingUser: Boolean = false,
    val selectedPlace: PlaceResult? = null,
    val isResolvingPoint: Boolean = false,
    val visibleBounds: MapBoundingBox? = null,
    val cameraCommand: MapCameraCommand? = null
)

data class SearchUiState(
    val query: String = "",
    val suggestions: List<PlaceResult> = emptyList(),
    val results: List<PlaceResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pickingForRouteField: RouteFieldTarget? = null
)

data class RoutingUiState(
    val showRouteDetails: Boolean = false,
    val origin: PlaceResult? = null, // null means Current GPS Location
    val destination: PlaceResult? = null,
    val travelMode: TravelMode = TravelMode.CAR,
    val routes: List<RouteOption> = emptyList(),
    val selectedRouteIndex: Int = 0,
    val isCalculating: Boolean = false,
    val errorMessage: String? = null,
    val isNavigating: Boolean = false,
    val currentStepIndex: Int = 0,
    val distanceToNextTurnMeters: Double = 0.0,
    val remainingDistanceMeters: Double = 0.0,
    val remainingDurationSeconds: Double = 0.0,
    val isRerouting: Boolean = false,
    val hasArrived: Boolean = false
) {
    val selectedRoute: RouteOption?
        get() = routes.getOrNull(selectedRouteIndex)
}

data class NearbyUiState(
    val selectedCategory: NearbyCategory = NearbyCategory.RESTAURANT,
    val places: List<PlaceResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val radiusMeters: Int = 2500
)

class RahnamaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RahnamaApplication
    private val settingsRepo = app.settingsRepository
    private val locationRepo = app.locationRepository
    private val voiceManager = app.voiceGuidanceManager
    private val offlineRepo = app.offlineMapRepository
    private val mapRepo = app.mapDataRepository

    private val _activeTab = MutableStateFlow(MainTab.MAP)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _mapState = MutableStateFlow(MapUiState())
    val mapState: StateFlow<MapUiState> = _mapState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _routingState = MutableStateFlow(RoutingUiState())
    val routingState: StateFlow<RoutingUiState> = _routingState.asStateFlow()

    private val _nearbyState = MutableStateFlow(NearbyUiState())
    val nearbyState: StateFlow<NearbyUiState> = _nearbyState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    val locationState: StateFlow<LocationState> = locationRepo.locationState
    val ttsStatus: StateFlow<TtsStatus> = voiceManager.status
    val downloadProgress: StateFlow<DownloadProgressState> = offlineRepo.downloadProgress

    val savedPlaces: StateFlow<List<SavedPlace>> = mapRepo.savedPlaces.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val searchHistory: StateFlow<List<SearchHistoryItem>> = mapRepo.searchHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allReports: StateFlow<List<StreetReport>> = mapRepo.streetReports.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeReports: StateFlow<List<StreetReport>> = mapRepo.streetReports
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { !it.isExpired(now) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val offlineRegions: StateFlow<List<OfflineRegion>> = offlineRepo.offlineRegions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var searchDebounceJob: Job? = null
    private var lastAutoCenteredGps = false
    private var lastAnnouncedStepIndex = -1
    private var lastAnnouncedApproachIndex = -1
    private var lastRerouteTime = 0L

    init {
        locationRepo.refreshPermissionState()
        refreshCacheSize()

        offlineRepo.onRegionSavedCallback = { entity: OfflineRegionEntity ->
            viewModelScope.launch {
                offlineRepo.saveRegionMetadata(entity)
                refreshCacheSize()
            }
        }

        // Sync settings to Voice, Location, and Map Defaults
        viewModelScope.launch {
            settings.collectLatest { s ->
                voiceManager.updateSettings(s.voiceEnabled, s.voiceVolume, s.selectedVoiceName)
                locationRepo.setPrivacyLocationEnabled(s.locationSharingEnabled)
                if (!_routingState.value.showRouteDetails && !_routingState.value.isNavigating) {
                    _routingState.update { it.copy(travelMode = s.defaultTravelMode) }
                }
            }
        }

        // React to real GPS updates: only auto-center if GPS is genuinely inside Iran
        viewModelScope.launch {
            locationState.collectLatest { loc ->
                val point = loc.currentPoint
                if (point != null) {
                    val inIran = GeoUtils.isInIran(point.lat, point.lon)
                    _mapState.update { it.copy(isGpsOutsideIran = !inIran) }
                    if (inIran && !lastAutoCenteredGps) {
                        lastAutoCenteredGps = true
                        _mapState.update {
                            it.copy(
                                center = point,
                                isFollowingUser = true,
                                cameraCommand = MapCameraCommand(target = point, zoom = 15.5)
                            )
                        }
                    }
                }
                if (point != null && _routingState.value.isNavigating) {
                    handleNavigationLocationUpdate(point)
                }
            }
        }

        // Purge expired reports periodically
        viewModelScope.launch {
            mapRepo.purgeExpiredReports()
        }
    }

    fun selectTab(tab: MainTab) {
        _activeTab.value = tab
        if (tab == MainTab.NEARBY && _nearbyState.value.places.isEmpty() && !_nearbyState.value.isLoading) {
            loadNearbyPlaces(_nearbyState.value.selectedCategory)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // Location & Permission
    fun onLocationPermissionResult() {
        locationRepo.refreshPermissionState()
        val hasPerm = locationRepo.hasLocationPermission()
        if (hasPerm) {
            locationRepo.startTracking()
        } else {
            showMessage("دسترسی موقعیت مکانی داده نشد. می‌توانید نقشه را به صورت دستی مرور کنید.")
        }
    }

    fun recenterOnUserLocation() {
        if (!settings.value.locationSharingEnabled) {
            showMessage("مکان‌یابی در تنظیمات حریم خصوصی غیرفعال شده است.")
            return
        }
        if (!locationRepo.hasLocationPermission()) {
            showMessage("لطفاً ابتدا مجوز موقعیت مکانی را تأیید کنید.")
            return
        }
        val current = locationState.value.currentPoint
        if (current != null && GeoUtils.isInIran(current.lat, current.lon)) {
            _mapState.update {
                it.copy(
                    isFollowingUser = true,
                    center = current,
                    cameraCommand = MapCameraCommand(target = current, zoom = 16.0)
                )
            }
        } else if (current != null) {
            showMessage("موقعیت شبیه‌ساز/GPS خارج از ایران است؛ نقشه روی ایران (${_mapState.value.selectedCityNameFa}) متمرکز ماند.")
            _mapState.update {
                it.copy(
                    isFollowingUser = false,
                    cameraCommand = MapCameraCommand(target = it.center, zoom = 14.0)
                )
            }
        } else {
            locationRepo.startTracking()
            showMessage("در حال دریافت موقعیت GPS...")
        }
    }

    fun selectIranCity(city: OfflineCityPreset) {
        val center = GeoPointModel((city.minLat + city.maxLat) / 2.0, (city.minLon + city.maxLon) / 2.0)
        _mapState.update {
            it.copy(
                selectedCityNameFa = city.nameFa,
                isFollowingUser = false,
                center = center,
                cameraCommand = MapCameraCommand(target = center, zoom = 13.5)
            )
        }
        if (_activeTab.value == MainTab.NEARBY || _nearbyState.value.places.isNotEmpty()) {
            loadNearbyPlaces(_nearbyState.value.selectedCategory)
        }
    }

    fun resetMapNorth() {
        _mapState.update {
            it.copy(
                orientationDeg = 0f,
                cameraCommand = MapCameraCommand(target = it.center, orientationDeg = 0f)
            )
        }
    }

    fun onMapViewportChanged(center: GeoPointModel, zoom: Double, orientationDeg: Float, bounds: MapBoundingBox?) {
        _mapState.update {
            it.copy(
                center = center,
                zoomLevel = zoom,
                orientationDeg = orientationDeg,
                visibleBounds = bounds
            )
        }
    }

    fun setFollowingUser(following: Boolean) {
        _mapState.update { it.copy(isFollowingUser = following) }
    }

    fun setMapLayer(layer: MapLayerType) {
        _mapState.update { it.copy(layer = layer, showLayersSheet = false) }
    }

    fun toggleLayersSheet(show: Boolean) {
        _mapState.update { it.copy(showLayersSheet = show) }
    }

    fun toggleTrafficDialog(show: Boolean) {
        _mapState.update { it.copy(showTrafficDialog = show) }
    }

    fun toggleReportDialog(show: Boolean) {
        _mapState.update { it.copy(showReportDialog = show) }
    }

    fun clearSelectedPlace() {
        _mapState.update { it.copy(selectedPlace = null) }
    }

    fun onMapLongPress(point: GeoPointModel) {
        _mapState.update {
            it.copy(
                isResolvingPoint = true,
                selectedPlace = PlaceResult(
                    id = "picked_${point.lat}_${point.lon}",
                    name = "نقطه انتخاب‌شده",
                    address = "${String.format("%.5f", point.lat)}, ${String.format("%.5f", point.lon)}",
                    category = "موقعیت روی نقشه",
                    lat = point.lat,
                    lon = point.lon,
                    distanceMeters = currentReferencePoint().let { ref ->
                        GeoUtils.distanceMeters(ref.lat, ref.lon, point.lat, point.lon)
                    }
                )
            )
        }
        viewModelScope.launch {
            val resolved = mapRepo.reverseGeocode(point)
            _mapState.update { state ->
                val ref = currentReferencePoint()
                val updated = resolved?.copy(
                    distanceMeters = GeoUtils.distanceMeters(ref.lat, ref.lon, resolved.lat, resolved.lon)
                ) ?: state.selectedPlace
                state.copy(isResolvingPoint = false, selectedPlace = updated)
            }
        }
    }

    fun focusPlaceOnMap(place: PlaceResult, zoom: Double = 16.5) {
        _activeTab.value = MainTab.MAP
        _mapState.update {
            it.copy(
                selectedPlace = place,
                isFollowingUser = false,
                center = GeoPointModel(place.lat, place.lon),
                cameraCommand = MapCameraCommand(
                    target = GeoPointModel(place.lat, place.lon),
                    zoom = zoom
                )
            )
        }
    }

    // Search
    fun startPickingForRoute(field: RouteFieldTarget) {
        _searchState.update { it.copy(pickingForRouteField = field, errorMessage = null) }
        _activeTab.value = MainTab.SEARCH
    }

    fun cancelRoutePicking() {
        _searchState.update { it.copy(pickingForRouteField = null) }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchState.update { it.copy(query = newQuery, errorMessage = null) }
        searchDebounceJob?.cancel()
        if (newQuery.trim().length < 2) {
            _searchState.update { it.copy(suggestions = emptyList(), isLoading = false) }
            return
        }
        searchDebounceJob = viewModelScope.launch {
            delay(350L)
            executeSearch(newQuery, isSuggestionOnly = true)
        }
    }

    fun submitSearch(queryOverride: String? = null) {
        val q = (queryOverride ?: _searchState.value.query).trim()
        if (q.isBlank()) return
        searchDebounceJob?.cancel()
        _searchState.update { it.copy(query = q) }
        viewModelScope.launch {
            executeSearch(q, isSuggestionOnly = false)
        }
    }

    private suspend fun executeSearch(query: String, isSuggestionOnly: Boolean) {
        _searchState.update { it.copy(isLoading = true, errorMessage = null) }
        val bias = currentReferencePoint()
        val result = mapRepo.searchPlaces(query, bias)
        result.fold(
            onSuccess = { list ->
                _searchState.update {
                    if (isSuggestionOnly) {
                        it.copy(
                            suggestions = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    } else {
                        it.copy(
                            suggestions = emptyList(),
                            results = list,
                            isLoading = false,
                            errorMessage = if (list.isEmpty()) "نتیجه‌ای یافت نشد. املای نام را بررسی کنید یا عبارت کوتاه‌تری جستجو کنید." else null
                        )
                    }
                }
            },
            onFailure = { err ->
                _searchState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "خطا در جستجوی مکان"
                    )
                }
            }
        )
    }

    fun onSearchResultSelected(place: PlaceResult) {
        val q = _searchState.value.query.ifBlank { place.name }
        viewModelScope.launch {
            mapRepo.recordSearchSelection(q, place)
        }

        val routeTarget = _searchState.value.pickingForRouteField
        if (routeTarget != null) {
            _searchState.update { it.copy(pickingForRouteField = null) }
            _activeTab.value = MainTab.MAP
            when (routeTarget) {
                RouteFieldTarget.ORIGIN -> {
                    _routingState.update { it.copy(origin = place, showRouteDetails = true) }
                }
                RouteFieldTarget.DESTINATION -> {
                    _routingState.update { it.copy(destination = place, showRouteDetails = true) }
                }
            }
            calculateRoutesIfReady()
        } else {
            focusPlaceOnMap(place)
        }
    }

    fun onHistoryItemSelected(item: SearchHistoryItem) {
        val place = PlaceResult(
            id = "hist_${item.id}",
            name = item.displayName,
            address = item.address,
            category = "جستوجوی اخیر",
            lat = item.lat,
            lon = item.lon,
            distanceMeters = currentReferencePoint().let {
                GeoUtils.distanceMeters(it.lat, it.lon, item.lat, item.lon)
            }
        )
        onSearchResultSelected(place)
    }

    fun deleteSearchHistoryItem(id: Long) {
        viewModelScope.launch { mapRepo.deleteSearchHistoryItem(id) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            mapRepo.clearSearchHistory()
            showMessage("تاریخچه جستجوها پاک شد.")
        }
    }

    // Nearby POIs
    fun selectNearbyCategory(category: NearbyCategory) {
        _nearbyState.update { it.copy(selectedCategory = category) }
        loadNearbyPlaces(category)
    }

    fun loadNearbyPlaces(category: NearbyCategory = _nearbyState.value.selectedCategory) {
        viewModelScope.launch {
            _nearbyState.update { it.copy(isLoading = true, errorMessage = null) }
            val center = currentReferencePoint()
            val res = mapRepo.fetchNearby(center, category, _nearbyState.value.radiusMeters)
            res.fold(
                onSuccess = { list ->
                    _nearbyState.update {
                        it.copy(
                            places = list,
                            isLoading = false,
                            errorMessage = if (list.isEmpty()) "در این محدوده مکانی برای این دسته‌بندی ثبت نشده است." else null
                        )
                    }
                },
                onFailure = { err ->
                    _nearbyState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "خطا در دریافت اطلاعات مکان‌های اطراف"
                        )
                    }
                }
            )
        }
    }

    // Routing & Turn-by-Turn Navigation
    fun openRouteToPlace(destination: PlaceResult) {
        _activeTab.value = MainTab.MAP
        _mapState.update { it.copy(selectedPlace = null) }
        _routingState.update {
            it.copy(
                showRouteDetails = true,
                destination = destination,
                travelMode = settings.value.defaultTravelMode,
                errorMessage = null,
                isNavigating = false,
                hasArrived = false
            )
        }
        calculateRoutesIfReady()
    }

    fun openRoutePlanner() {
        _activeTab.value = MainTab.MAP
        _routingState.update {
            it.copy(
                showRouteDetails = true,
                travelMode = settings.value.defaultTravelMode,
                errorMessage = null
            )
        }
        calculateRoutesIfReady()
    }

    fun closeRouteDetails() {
        _routingState.update {
            it.copy(
                showRouteDetails = false,
                routes = emptyList(),
                errorMessage = null
            )
        }
    }

    fun swapOriginDestination() {
        val currentOrigin = _routingState.value.origin ?: currentGpsAsPlace()
        val currentDest = _routingState.value.destination
        _routingState.update {
            it.copy(
                origin = currentDest,
                destination = currentOrigin
            )
        }
        calculateRoutesIfReady()
    }

    fun setOriginToCurrentGps() {
        _routingState.update { it.copy(origin = null) }
        calculateRoutesIfReady()
    }

    fun setTravelMode(mode: TravelMode) {
        _routingState.update { it.copy(travelMode = mode) }
        calculateRoutesIfReady()
    }

    fun selectRouteOption(index: Int) {
        _routingState.update { it.copy(selectedRouteIndex = index) }
    }

    fun calculateRoutesIfReady() {
        val dest = _routingState.value.destination ?: return
        val originPoint = _routingState.value.origin?.let { GeoPointModel(it.lat, it.lon) }
            ?: currentReferencePoint()
        val destPoint = GeoPointModel(dest.lat, dest.lon)
        val mode = _routingState.value.travelMode

        viewModelScope.launch {
            _routingState.update { it.copy(isCalculating = true, errorMessage = null) }
            val result = mapRepo.fetchRoutes(originPoint, destPoint, mode, settings.value.distanceUnit)
            result.fold(
                onSuccess = { list ->
                    _routingState.update {
                        it.copy(
                            routes = list,
                            selectedRouteIndex = 0,
                            isCalculating = false,
                            errorMessage = null
                        )
                    }
                    // Fit camera on origin or midpoint
                    list.firstOrNull()?.geometryPoints?.let { pts ->
                        if (pts.isNotEmpty()) {
                            val mid = pts[pts.size / 2]
                            _mapState.update { m ->
                                m.copy(
                                    isFollowingUser = false,
                                    cameraCommand = MapCameraCommand(target = mid, zoom = 13.0)
                                )
                            }
                        }
                    }
                },
                onFailure = { err ->
                    _routingState.update {
                        it.copy(
                            routes = emptyList(),
                            isCalculating = false,
                            errorMessage = err.message ?: "خطا در محاسبه مسیر"
                        )
                    }
                }
            )
        }
    }

    fun startNavigation() {
        val route = _routingState.value.selectedRoute ?: return
        lastAnnouncedStepIndex = -1
        lastAnnouncedApproachIndex = -1
        _routingState.update {
            it.copy(
                showRouteDetails = false,
                isNavigating = true,
                currentStepIndex = 0,
                distanceToNextTurnMeters = route.steps.firstOrNull()?.distanceMeters ?: route.distanceMeters,
                remainingDistanceMeters = route.distanceMeters,
                remainingDurationSeconds = route.durationSeconds,
                hasArrived = false
            )
        }
        val focusPt = locationState.value.currentPoint ?: route.geometryPoints.firstOrNull()
        if (focusPt != null) {
            _mapState.update {
                it.copy(
                    isFollowingUser = true,
                    cameraCommand = MapCameraCommand(target = focusPt, zoom = 17.5)
                )
            }
        }
        val firstStep = route.steps.firstOrNull()
        if (firstStep != null) {
            voiceManager.speak(firstStep.instructionFa, force = true)
            lastAnnouncedStepIndex = 0
        }
    }

    fun stopNavigation() {
        voiceManager.stop()
        _routingState.update {
            it.copy(
                isNavigating = false,
                isRerouting = false,
                hasArrived = false,
                routes = emptyList()
            )
        }
        resetMapNorth()
    }

    fun previewNextStep() {
        val state = _routingState.value
        val route = state.selectedRoute ?: return
        if (state.currentStepIndex + 1 < route.steps.size) {
            val nextIdx = state.currentStepIndex + 1
            val step = route.steps[nextIdx]
            _routingState.update {
                it.copy(
                    currentStepIndex = nextIdx,
                    distanceToNextTurnMeters = step.distanceMeters
                )
            }
            voiceManager.speak(step.instructionFa, force = true)
        }
    }

    fun previewPrevStep() {
        val state = _routingState.value
        val route = state.selectedRoute ?: return
        if (state.currentStepIndex > 0) {
            val prevIdx = state.currentStepIndex - 1
            val step = route.steps[prevIdx]
            _routingState.update {
                it.copy(
                    currentStepIndex = prevIdx,
                    distanceToNextTurnMeters = step.distanceMeters
                )
            }
        }
    }

    private fun handleNavigationLocationUpdate(currentPoint: GeoPointModel) {
        val state = _routingState.value
        val route = state.selectedRoute ?: return
        val dest = state.destination ?: return

        val distToDest = GeoUtils.distanceMeters(currentPoint.lat, currentPoint.lon, dest.lat, dest.lon)
        if (distToDest <= 30.0 && !state.hasArrived) {
            _routingState.update {
                it.copy(
                    hasArrived = true,
                    remainingDistanceMeters = 0.0,
                    remainingDurationSeconds = 0.0
                )
            }
            voiceManager.speak("به مقصد رسیدید", force = true)
            return
        }

        // Off-route detection (> 70 meters from polyline)
        val distFromRoute = GeoUtils.distanceToPolylineMeters(currentPoint, route.geometryPoints)
        val now = System.currentTimeMillis()
        if (distFromRoute > 70.0 && !state.isRerouting && (now - lastRerouteTime) > 10_000L) {
            lastRerouteTime = now
            triggerReroute(currentPoint, dest, state.travelMode)
            return
        }

        // Advance step when close to next maneuver
        val steps = route.steps
        var stepIdx = state.currentStepIndex
        if (stepIdx + 1 < steps.size) {
            val nextStepLoc = steps[stepIdx + 1].location
            val distToNext = GeoUtils.distanceMeters(
                currentPoint.lat, currentPoint.lon, nextStepLoc.lat, nextStepLoc.lon
            )
            if (distToNext < 35.0) {
                stepIdx++
            }
        }

        val targetStep = steps.getOrNull(stepIdx)
        val nextTurnLoc = steps.getOrNull(stepIdx + 1)?.location ?: GeoPointModel(dest.lat, dest.lon)
        val distToTurn = GeoUtils.distanceMeters(
            currentPoint.lat, currentPoint.lon, nextTurnLoc.lat, nextTurnLoc.lon
        )

        val remainingRouteDist = GeoUtils.remainingRouteDistanceMeters(currentPoint, route.geometryPoints)
        val ratio = if (route.distanceMeters > 0) (remainingRouteDist / route.distanceMeters).coerceIn(0.0, 1.0) else 0.0
        val remainingDur = route.durationSeconds * ratio

        _routingState.update {
            it.copy(
                currentStepIndex = stepIdx,
                distanceToNextTurnMeters = distToTurn,
                remainingDistanceMeters = remainingRouteDist,
                remainingDurationSeconds = remainingDur
            )
        }

        // Voice announcements at ~450m and step transitions
        if (targetStep != null) {
            if (stepIdx != lastAnnouncedStepIndex) {
                lastAnnouncedStepIndex = stepIdx
                voiceManager.speak(targetStep.instructionFa)
            } else if (distToTurn in 90.0..520.0 && stepIdx != lastAnnouncedApproachIndex) {
                val nextStep = steps.getOrNull(stepIdx + 1)
                if (nextStep != null) {
                    lastAnnouncedApproachIndex = stepIdx
                    val msg = PersianManeuverTranslator.buildVoiceAnnouncement(
                        maneuverType = nextStep.maneuverType,
                        modifier = nextStep.maneuverModifier,
                        streetName = nextStep.streetName,
                        distanceMeters = distToTurn,
                        exitNumber = nextStep.exitNumber,
                        unit = settings.value.distanceUnit
                    )
                    voiceManager.speak(msg)
                }
            }
        }
    }

    private fun triggerReroute(currentPoint: GeoPointModel, dest: PlaceResult, mode: TravelMode) {
        viewModelScope.launch {
            _routingState.update { it.copy(isRerouting = true) }
            voiceManager.speak("مسیر جدید در حال محاسبه است", force = true)
            val result = mapRepo.fetchRoutes(
                currentPoint,
                GeoPointModel(dest.lat, dest.lon),
                mode,
                settings.value.distanceUnit
            )
            result.fold(
                onSuccess = { newRoutes ->
                    lastAnnouncedStepIndex = -1
                    lastAnnouncedApproachIndex = -1
                    val first = newRoutes.firstOrNull()
                    _routingState.update {
                        it.copy(
                            routes = newRoutes,
                            selectedRouteIndex = 0,
                            isRerouting = false,
                            currentStepIndex = 0,
                            remainingDistanceMeters = first?.distanceMeters ?: 0.0,
                            remainingDurationSeconds = first?.durationSeconds ?: 0.0
                        )
                    }
                },
                onFailure = {
                    _routingState.update { it.copy(isRerouting = false) }
                }
            )
        }
    }

    // Saved Places
    fun savePlace(name: String, address: String, category: SavedCategory, lat: Double, lon: Double) {
        viewModelScope.launch {
            mapRepo.addSavedPlace(name, address, category, lat, lon)
            showMessage("«$name» در مکان‌های ذخیره‌شده ثبت شد.")
        }
    }

    fun updateSavedPlace(place: SavedPlace) {
        viewModelScope.launch {
            mapRepo.updateSavedPlace(place)
            showMessage("اطلاعات مکان به‌روزرسانی شد.")
        }
    }

    fun deleteSavedPlace(id: Long) {
        viewModelScope.launch {
            mapRepo.deleteSavedPlace(id)
            showMessage("مکان حذف شد.")
        }
    }

    // Street Reports
    fun submitStreetReport(type: ReportType, description: String) {
        val point = locationState.value.currentPoint ?: _mapState.value.center
        _mapState.update { it.copy(showReportDialog = false) }
        viewModelScope.launch {
            val rev = mapRepo.reverseGeocode(point)
            val label = rev?.name ?: rev?.address ?: "مختصات ثبت‌شده روی نقشه"
            mapRepo.addStreetReport(
                type = type,
                description = description,
                lat = point.lat,
                lon = point.lon,
                addressLabel = label
            )
            showMessage("گزارش «${type.labelFa}» با برچسب تأییدنشده ثبت شد.")
        }
    }

    fun confirmStreetReport(id: Long) {
        viewModelScope.launch { mapRepo.confirmStreetReport(id) }
    }

    fun deleteStreetReport(id: Long) {
        viewModelScope.launch { mapRepo.deleteStreetReport(id) }
    }

    // Offline Maps
    fun downloadOfflineCity(city: OfflineCityPreset) {
        if (!mapRepo.isNetworkAvailable()) {
            showMessage("برای دانلود نقشه آفلاین اتصال اینترنت لازم است.")
            return
        }
        offlineRepo.downloadRegion(
            regionId = city.id,
            nameFa = "${city.nameFa} (${city.provinceFa})",
            minLat = city.minLat,
            maxLat = city.maxLat,
            minLon = city.minLon,
            maxLon = city.maxLon,
            minZoom = city.minZoom,
            maxZoom = city.maxZoom
        ) { success, err ->
            if (success) {
                showMessage("نقشه آفلاین ${city.nameFa} با موفقیت دانلود شد.")
            } else {
                showMessage(err ?: "خطا در دانلود نقشه آفلاین")
            }
        }
    }

    fun downloadCurrentVisibleArea() {
        val bounds = _mapState.value.visibleBounds
        if (bounds == null) {
            showMessage("ابتدا نقشه را باز کنید تا محدوده قابل مشاهده مشخص شود.")
            return
        }
        if (!mapRepo.isNetworkAvailable()) {
            showMessage("برای دانلود نقشه آفلاین اتصال اینترنت لازم است.")
            return
        }
        val id = "custom_${System.currentTimeMillis()}"
        offlineRepo.downloadRegion(
            regionId = id,
            nameFa = "محدوده انتخابی نقشه",
            minLat = bounds.minLat,
            maxLat = bounds.maxLat,
            minLon = bounds.minLon,
            maxLon = bounds.maxLon,
            minZoom = 12,
            maxZoom = 15
        ) { success, err ->
            if (success) {
                showMessage("محدوده فعلی نقشه برای استفاده آفلاین ذخیره شد.")
            } else {
                showMessage(err ?: "خطا در دانلود نقشه")
            }
        }
    }

    fun cancelOfflineDownload() {
        offlineRepo.cancelDownload()
    }

    fun deleteOfflineRegion(id: String) {
        viewModelScope.launch {
            offlineRepo.deleteRegion(id)
            refreshCacheSize()
        }
    }

    fun clearAllMapCache() {
        viewModelScope.launch {
            offlineRepo.clearAllTileCache()
            refreshCacheSize()
            showMessage("حافظه کش و نقشه‌های آفلاین پاک شد.")
        }
    }

    fun refreshCacheSize() {
        _cacheSizeBytes.value = offlineRepo.getCacheDirectorySizeBytes()
    }

    // Settings
    fun setTheme(pref: ThemePreference) = viewModelScope.launch { settingsRepo.setTheme(pref) }
    fun setVoiceEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepo.setVoiceEnabled(enabled) }
    fun setVoiceVolume(vol: Float) = viewModelScope.launch { settingsRepo.setVoiceVolume(vol) }
    fun setSelectedVoice(name: String) = viewModelScope.launch { settingsRepo.setSelectedVoice(name) }
    fun setDefaultTravelMode(mode: TravelMode) = viewModelScope.launch { settingsRepo.setDefaultTravelMode(mode) }
    fun setDistanceUnit(unit: DistanceUnit) = viewModelScope.launch { settingsRepo.setDistanceUnit(unit) }
    fun setLocationSharingEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepo.setLocationEnabled(enabled) }
    fun setOfflineMode(enabled: Boolean) = viewModelScope.launch { settingsRepo.setOfflineMode(enabled) }
    fun setShowSavedOnMap(show: Boolean) = viewModelScope.launch { settingsRepo.setShowSavedOnMap(show) }
    fun setShowReportsOnMap(show: Boolean) = viewModelScope.launch { settingsRepo.setShowReportsOnMap(show) }
    fun setProfile(name: String, avatarUri: String?) = viewModelScope.launch { settingsRepo.setProfile(name, avatarUri) }

    fun testVoiceGuidance() {
        voiceManager.speak("راهنما آماده هدایت شماست. تا پانصد متر دیگر به راست بپیچید.", force = true)
    }

    fun currentReferencePoint(): GeoPointModel {
        val gps = locationState.value.currentPoint
        if (gps != null && GeoUtils.isInIran(gps.lat, gps.lon)) {
            return gps
        }
        val mapCenter = _mapState.value.center
        return if (GeoUtils.isInIran(mapCenter.lat, mapCenter.lon)) mapCenter else GeoUtils.DEFAULT_TEHRAN_CENTER
    }

    private fun currentGpsAsPlace(): PlaceResult? {
        val pt = locationState.value.currentPoint
        if (pt != null && GeoUtils.isInIran(pt.lat, pt.lon)) {
            return PlaceResult(
                id = "current_gps",
                name = "موقعیت فعلی من",
                address = null,
                category = "موقعیت GPS",
                lat = pt.lat,
                lon = pt.lon
            )
        }
        val center = currentReferencePoint()
        return PlaceResult(
            id = "current_city_center",
            name = "مرکز ${_mapState.value.selectedCityNameFa}",
            address = null,
            category = "نقطه مرجع",
            lat = center.lat,
            lon = center.lon
        )
    }

    override fun onCleared() {
        super.onCleared()
        locationRepo.stopTracking()
        voiceManager.shutdown()
    }
}
