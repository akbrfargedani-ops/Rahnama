package com.example.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AppSettings
import com.example.domain.model.LocationState
import com.example.domain.model.OfflineCityPreset
import com.example.domain.model.PlaceResult
import com.example.domain.model.SavedPlace
import com.example.domain.model.StreetReport
import com.example.ui.components.CompassButton
import com.example.ui.components.IranCityPickerDialog
import com.example.ui.components.MapLayersBottomSheet
import com.example.ui.components.RahnamaLogoBadge
import com.example.ui.components.ReportIssueDialog
import com.example.ui.components.SavePlaceDialog
import com.example.ui.components.ShareHelpers
import com.example.ui.components.TrafficInfoDialog
import com.example.ui.viewmodel.MapUiState
import com.example.ui.viewmodel.RoutingUiState
import com.example.util.GeoUtils

@Composable
fun MapScreen(
    mapState: MapUiState,
    locationState: LocationState,
    settings: AppSettings,
    routingState: RoutingUiState,
    savedPlaces: List<SavedPlace>,
    activeReports: List<StreetReport>,
    onOpenSearch: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRecenter: () -> Unit,
    onResetNorth: () -> Unit,
    onViewportChanged: (com.example.domain.model.GeoPointModel, Double, Float, com.example.ui.viewmodel.MapBoundingBox?) -> Unit,
    onUserInteracted: () -> Unit,
    onMapLongPress: (com.example.domain.model.GeoPointModel) -> Unit,
    onSavedPlacePinClick: (SavedPlace) -> Unit,
    onClearSelectedPlace: () -> Unit,
    onOpenRouteToPlace: (PlaceResult) -> Unit,
    onOpenRoutePlanner: () -> Unit,
    onCloseRouteDetails: () -> Unit,
    onPickRouteField: (com.example.ui.viewmodel.RouteFieldTarget) -> Unit,
    onUseCurrentOrigin: () -> Unit,
    onSwapOriginDest: () -> Unit,
    onSelectTravelMode: (com.example.domain.model.TravelMode) -> Unit,
    onSelectRouteOption: (Int) -> Unit,
    onRetryRoute: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onToggleVoice: () -> Unit,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onToggleLayersSheet: (Boolean) -> Unit,
    onSelectLayer: (com.example.domain.model.MapLayerType) -> Unit,
    onToggleSavedPins: (Boolean) -> Unit,
    onToggleReportPins: (Boolean) -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit,
    onToggleTrafficDialog: (Boolean) -> Unit,
    onToggleReportDialog: (Boolean) -> Unit,
    onSubmitReport: (com.example.domain.model.ReportType, String) -> Unit,
    onSavePlace: (String, String, com.example.domain.model.SavedCategory, Double, Double) -> Unit,
    onSelectIranCity: (OfflineCityPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSaveDialogForPlace by remember { mutableStateOf<PlaceResult?>(null) }
    var showCityPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Real Interactive OpenStreetMap View
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            initialCenter = mapState.center,
            initialZoom = mapState.zoomLevel,
            mapLayer = mapState.layer,
            offlineMode = settings.offlineModeEnabled,
            userLocation = if (settings.locationSharingEnabled) locationState.currentPoint else null,
            userHeadingDeg = locationState.headingDegrees,
            isFollowingUser = mapState.isFollowingUser,
            isNavigating = routingState.isNavigating,
            selectedPlace = mapState.selectedPlace ?: routingState.destination,
            savedPlaces = savedPlaces,
            showSavedPins = settings.showSavedOnMap,
            reports = activeReports,
            showReportPins = settings.showReportsOnMap,
            routes = routingState.routes,
            selectedRouteIndex = routingState.selectedRouteIndex,
            cameraCommand = mapState.cameraCommand,
            onViewportChanged = onViewportChanged,
            onUserInteracted = onUserInteracted,
            onMapLongPress = onMapLongPress,
            onSavedPlaceClick = onSavedPlacePinClick,
            onRouteSelect = onSelectRouteOption
        )

        if (routingState.isNavigating) {
            NavigationHud(
                routingState = routingState,
                distanceUnit = settings.distanceUnit,
                speedMps = locationState.speedMetersPerSec,
                voiceEnabled = settings.voiceEnabled,
                onToggleVoice = onToggleVoice,
                onPrevStep = onPrevStep,
                onNextStep = onNextStep,
                onStopNavigation = onStopNavigation
            )
        } else {
            // Top Floating Search Bar
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onOpenSearch,
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("map_search_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RahnamaLogoBadge(sizeDp = 34)
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "جستوجو",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Status chips row (Iran City Picker, Offline notice, or GPS prompt)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { showCityPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        shadowElevation = 3.dp,
                        modifier = Modifier.testTag("iran_city_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Rounded.LocationCity,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ایران • ${mapState.selectedCityNameFa}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (settings.offlineModeEnabled) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("حالت آفلاین فعال است", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (!locationState.hasPermission && settings.locationSharingEnabled) {
                        Surface(
                            onClick = onRequestLocationPermission,
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 2.dp,
                            modifier = Modifier.testTag("grant_location_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.MyLocation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "فعال‌سازی موقعیت GPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Floating Map Action Controls (Compass, Layers, Traffic, Report, My Location, Route)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 14.dp,
                        bottom = if (mapState.selectedPlace != null || routingState.showRouteDetails) 230.dp else 20.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompassButton(
                    orientationDeg = mapState.orientationDeg,
                    onClick = onResetNorth
                )

                SmallFloatingActionButton(
                    onClick = { onToggleLayersSheet(true) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_layers_button")
                ) {
                    Icon(Icons.Rounded.Layers, contentDescription = stringResource(R.string.cd_map_layers))
                }

                SmallFloatingActionButton(
                    onClick = { onToggleTrafficDialog(true) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_traffic_button")
                ) {
                    Icon(Icons.Rounded.Traffic, contentDescription = stringResource(R.string.cd_traffic_layer))
                }

                SmallFloatingActionButton(
                    onClick = { onToggleReportDialog(true) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_report_button")
                ) {
                    Icon(Icons.Rounded.ReportProblem, contentDescription = stringResource(R.string.cd_report_issue))
                }

                SmallFloatingActionButton(
                    onClick = {
                        if (!locationState.hasPermission && settings.locationSharingEnabled) {
                            onRequestLocationPermission()
                        } else {
                            onRecenter()
                        }
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    contentColor = if (mapState.isFollowingUser && locationState.currentPoint != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("my_location_button")
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.cd_my_location))
                }

                if (!routingState.showRouteDetails) {
                    FloatingActionButton(
                        onClick = onOpenRoutePlanner,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("open_route_planner_fab")
                    ) {
                        Icon(Icons.Rounded.Directions, contentDescription = stringResource(R.string.cd_route_button))
                    }
                }
            }

            // Selected Place Bottom Sheet Card
            AnimatedVisibility(
                visible = mapState.selectedPlace != null && !routingState.showRouteDetails,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                mapState.selectedPlace?.let { place ->
                    SelectedPlaceCard(
                        place = place,
                        isResolving = mapState.isResolvingPoint,
                        distanceUnit = settings.distanceUnit,
                        onClose = onClearSelectedPlace,
                        onRouteHere = { onOpenRouteToPlace(place) },
                        onSave = { showSaveDialogForPlace = place },
                        onShare = { ShareHelpers.sharePlace(context, place) }
                    )
                }
            }

            // Route Details & Alternatives Sheet
            AnimatedVisibility(
                visible = routingState.showRouteDetails,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                RouteDetailsSheet(
                    routingState = routingState,
                    distanceUnit = settings.distanceUnit,
                    activeReports = activeReports,
                    onPickField = onPickRouteField,
                    onUseCurrentOrigin = onUseCurrentOrigin,
                    onSwap = onSwapOriginDest,
                    onSelectTravelMode = onSelectTravelMode,
                    onSelectRoute = onSelectRouteOption,
                    onRetry = onRetryRoute,
                    onStartNavigation = onStartNavigation,
                    onClose = onCloseRouteDetails
                )
            }
        }
    }

    if (mapState.showLayersSheet) {
        MapLayersBottomSheet(
            currentLayer = mapState.layer,
            showSavedPins = settings.showSavedOnMap,
            showReportPins = settings.showReportsOnMap,
            offlineModeEnabled = settings.offlineModeEnabled,
            onSelectLayer = onSelectLayer,
            onToggleSavedPins = onToggleSavedPins,
            onToggleReportPins = onToggleReportPins,
            onToggleOfflineMode = onToggleOfflineMode,
            onDismiss = { onToggleLayersSheet(false) }
        )
    }

    if (mapState.showTrafficDialog) {
        TrafficInfoDialog(
            activeReports = activeReports,
            onDismiss = { onToggleTrafficDialog(false) },
            onOpenReportDialog = { onToggleReportDialog(true) }
        )
    }

    if (mapState.showReportDialog) {
        ReportIssueDialog(
            onDismiss = { onToggleReportDialog(false) },
            onSubmit = onSubmitReport
        )
    }

    showSaveDialogForPlace?.let { place ->
        SavePlaceDialog(
            initialName = place.name,
            initialAddress = place.address ?: "اطلاعات در دسترس نیست",
            onDismiss = { showSaveDialogForPlace = null },
            onConfirm = { name, address, cat ->
                onSavePlace(name, address, cat, place.lat, place.lon)
                showSaveDialogForPlace = null
            }
        )
    }

    if (showCityPicker) {
        IranCityPickerDialog(
            selectedCityNameFa = mapState.selectedCityNameFa,
            onSelectCity = onSelectIranCity,
            onDismiss = { showCityPicker = false }
        )
    }
}

@Composable
private fun SelectedPlaceCard(
    place: PlaceResult,
    isResolving: Boolean,
    distanceUnit: com.example.domain.model.DistanceUnit,
    onClose: () -> Unit,
    onRouteHere: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
        modifier = Modifier
            .widthIn(max = 580.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = place.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isResolving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }
                    Text(
                        text = place.address ?: stringResource(R.string.data_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = listOfNotNull(
                        place.category,
                        place.distanceMeters?.let { "فاصله: ${GeoUtils.formatDistance(it, distanceUnit)}" }
                    ).joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRouteHere,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("place_card_route_button")
                ) {
                    Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("مسیریابی")
                }
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("place_card_save_button")
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = "ذخیره مکان", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ذخیره")
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "اشتراک‌گذاری", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
