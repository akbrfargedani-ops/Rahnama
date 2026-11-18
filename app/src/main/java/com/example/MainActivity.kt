package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.PlaceResult
import com.example.ui.components.RahnamaLogoBadge
import com.example.ui.map.MapScreen
import com.example.ui.nearby.NearbyScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.saved.SavedScreen
import com.example.ui.search.SearchScreen
import com.example.ui.theme.RahnamaTheme
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.RahnamaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RahnamaViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            RahnamaTheme(themePreference = settings.themePreference) {
                RahnamaRootApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RahnamaRootApp(viewModel: RahnamaViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val routingState by viewModel.routingState.collectAsStateWithLifecycle()
    val nearbyState by viewModel.nearbyState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val activeReports by viewModel.activeReports.collectAsStateWithLifecycle()
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()
    val offlineRegions by viewModel.offlineRegions.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val ttsStatus by viewModel.ttsStatus.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onLocationPermissionResult()
    }

    val requestLocationPermission = {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Request foreground location permission on first launch when enabled in privacy settings
    LaunchedEffect(Unit) {
        if (!locationState.hasPermission && settings.locationSharingEnabled) {
            requestLocationPermission()
        }
    }

    val hideNavChrome = routingState.isNavigating || (activeTab == MainTab.MAP && routingState.showRouteDetails)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useNavigationRail && !hideNavChrome) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.labelFa
                                    )
                                },
                                label = { Text(tab.labelFa, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && !hideNavChrome) {
                    NavigationRail(
                        header = {
                            RahnamaLogoBadge(
                                sizeDp = 40,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.labelFa
                                    )
                                },
                                label = { Text(tab.labelFa, maxLines = 1) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (activeTab) {
                        MainTab.MAP -> {
                            MapScreen(
                                mapState = mapState,
                                locationState = locationState,
                                settings = settings,
                                routingState = routingState,
                                savedPlaces = savedPlaces,
                                activeReports = activeReports,
                                onOpenSearch = { viewModel.selectTab(MainTab.SEARCH) },
                                onRequestLocationPermission = requestLocationPermission,
                                onRecenter = viewModel::recenterOnUserLocation,
                                onResetNorth = viewModel::resetMapNorth,
                                onViewportChanged = viewModel::onMapViewportChanged,
                                onUserInteracted = { viewModel.setFollowingUser(false) },
                                onMapLongPress = viewModel::onMapLongPress,
                                onSavedPlacePinClick = { saved ->
                                    viewModel.focusPlaceOnMap(
                                        PlaceResult(
                                            id = "saved_${saved.id}",
                                            name = saved.name,
                                            address = saved.address,
                                            category = saved.category.labelFa,
                                            lat = saved.lat,
                                            lon = saved.lon
                                        )
                                    )
                                },
                                onClearSelectedPlace = viewModel::clearSelectedPlace,
                                onOpenRouteToPlace = viewModel::openRouteToPlace,
                                onOpenRoutePlanner = viewModel::openRoutePlanner,
                                onCloseRouteDetails = viewModel::closeRouteDetails,
                                onPickRouteField = viewModel::startPickingForRoute,
                                onUseCurrentOrigin = viewModel::setOriginToCurrentGps,
                                onSwapOriginDest = viewModel::swapOriginDestination,
                                onSelectTravelMode = viewModel::setTravelMode,
                                onSelectRouteOption = viewModel::selectRouteOption,
                                onRetryRoute = viewModel::calculateRoutesIfReady,
                                onStartNavigation = viewModel::startNavigation,
                                onStopNavigation = viewModel::stopNavigation,
                                onToggleVoice = { viewModel.setVoiceEnabled(!settings.voiceEnabled) },
                                onPrevStep = viewModel::previewPrevStep,
                                onNextStep = viewModel::previewNextStep,
                                onToggleLayersSheet = viewModel::toggleLayersSheet,
                                onSelectLayer = viewModel::setMapLayer,
                                onToggleSavedPins = viewModel::setShowSavedOnMap,
                                onToggleReportPins = viewModel::setShowReportsOnMap,
                                onToggleOfflineMode = viewModel::setOfflineMode,
                                onToggleTrafficDialog = viewModel::toggleTrafficDialog,
                                onToggleReportDialog = viewModel::toggleReportDialog,
                                onSubmitReport = viewModel::submitStreetReport,
                                onSavePlace = viewModel::savePlace,
                                onSelectIranCity = viewModel::selectIranCity
                            )
                        }

                        MainTab.SEARCH -> {
                            SearchScreen(
                                searchState = searchState,
                                history = searchHistory,
                                distanceUnit = settings.distanceUnit,
                                onQueryChange = viewModel::onSearchQueryChanged,
                                onSubmitSearch = viewModel::submitSearch,
                                onCancelRoutePick = {
                                    viewModel.cancelRoutePicking()
                                    viewModel.selectTab(MainTab.MAP)
                                },
                                onSelectResult = viewModel::onSearchResultSelected,
                                onRouteToPlace = viewModel::openRouteToPlace,
                                onSavePlace = viewModel::savePlace,
                                onSelectHistoryItem = viewModel::onHistoryItemSelected,
                                onDeleteHistoryItem = viewModel::deleteSearchHistoryItem,
                                onClearHistory = viewModel::clearSearchHistory
                            )
                        }

                        MainTab.NEARBY -> {
                            NearbyScreen(
                                nearbyState = nearbyState,
                                distanceUnit = settings.distanceUnit,
                                selectedCityNameFa = mapState.selectedCityNameFa,
                                onSelectIranCity = viewModel::selectIranCity,
                                onSelectCategory = viewModel::selectNearbyCategory,
                                onRefresh = { viewModel.loadNearbyPlaces() },
                                onViewOnMap = { viewModel.focusPlaceOnMap(it) },
                                onRouteToPlace = viewModel::openRouteToPlace
                            )
                        }

                        MainTab.SAVED -> {
                            SavedScreen(
                                savedPlaces = savedPlaces,
                                showSavedOnMap = settings.showSavedOnMap,
                                referencePoint = viewModel.currentReferencePoint(),
                                onToggleShowOnMap = viewModel::setShowSavedOnMap,
                                onAddPlace = viewModel::savePlace,
                                onUpdatePlace = viewModel::updateSavedPlace,
                                onDeletePlace = viewModel::deleteSavedPlace,
                                onViewOnMap = { viewModel.focusPlaceOnMap(it) },
                                onRouteToPlace = viewModel::openRouteToPlace
                            )
                        }

                        MainTab.PROFILE -> {
                            ProfileScreen(
                                settings = settings,
                                locationState = locationState,
                                ttsStatus = ttsStatus,
                                offlineRegions = offlineRegions,
                                downloadProgress = downloadProgress,
                                cacheSizeBytes = cacheSizeBytes,
                                allReports = allReports,
                                savedPlacesCount = savedPlaces.size,
                                searchHistoryCount = searchHistory.size,
                                onSetTheme = viewModel::setTheme,
                                onSetVoiceEnabled = viewModel::setVoiceEnabled,
                                onSetVoiceVolume = viewModel::setVoiceVolume,
                                onSetSelectedVoice = viewModel::setSelectedVoice,
                                onTestVoice = viewModel::testVoiceGuidance,
                                onSetDefaultTravelMode = viewModel::setDefaultTravelMode,
                                onSetDistanceUnit = viewModel::setDistanceUnit,
                                onSetLocationSharing = viewModel::setLocationSharingEnabled,
                                onSetOfflineMode = viewModel::setOfflineMode,
                                onSetProfile = viewModel::setProfile,
                                onClearSearchHistory = viewModel::clearSearchHistory,
                                onOpenSavedTab = { viewModel.selectTab(MainTab.SAVED) },
                                onDownloadCity = viewModel::downloadOfflineCity,
                                onDownloadCurrentArea = viewModel::downloadCurrentVisibleArea,
                                onCancelDownload = viewModel::cancelOfflineDownload,
                                onDeleteOfflineRegion = viewModel::deleteOfflineRegion,
                                onClearAllMapCache = viewModel::clearAllMapCache,
                                onConfirmReport = viewModel::confirmStreetReport,
                                onDeleteReport = viewModel::deleteStreetReport,
                                onSubmitReport = viewModel::submitStreetReport,
                                onRequestLocationPermission = requestLocationPermission
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: MainTab, selected: Boolean): ImageVector {
    return when (tab) {
        MainTab.MAP -> if (selected) Icons.Rounded.Map else Icons.Outlined.Map
        MainTab.SEARCH -> if (selected) Icons.Rounded.Search else Icons.Outlined.Search
        MainTab.NEARBY -> if (selected) Icons.Rounded.Explore else Icons.Outlined.Explore
        MainTab.SAVED -> if (selected) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder
        MainTab.PROFILE -> if (selected) Icons.Rounded.Person else Icons.Outlined.Person
    }
}
