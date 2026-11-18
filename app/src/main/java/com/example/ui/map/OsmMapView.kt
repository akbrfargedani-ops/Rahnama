package com.example.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.GeoPointModel
import com.example.domain.model.MapLayerType
import com.example.domain.model.PlaceResult
import com.example.domain.model.RouteOption
import com.example.domain.model.SavedPlace
import com.example.domain.model.StreetReport
import com.example.ui.viewmodel.MapBoundingBox
import com.example.ui.viewmodel.MapCameraCommand
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

private val SATELLITE_TILE_SOURCE: OnlineTileSourceBase = object : XYTileSource(
    "EsriWorldImagery",
    0,
    18,
    256,
    ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$baseUrl$z/$y/$x"
    }
}

private val DARK_TILE_SOURCE: OnlineTileSourceBase = XYTileSource(
    "CartoDarkMatter",
    0,
    18,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/"
    )
)

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    initialCenter: GeoPointModel,
    initialZoom: Double,
    mapLayer: MapLayerType,
    offlineMode: Boolean,
    userLocation: GeoPointModel?,
    userHeadingDeg: Float,
    isFollowingUser: Boolean,
    isNavigating: Boolean,
    selectedPlace: PlaceResult?,
    savedPlaces: List<SavedPlace>,
    showSavedPins: Boolean,
    reports: List<StreetReport>,
    showReportPins: Boolean,
    routes: List<RouteOption>,
    selectedRouteIndex: Int,
    cameraCommand: MapCameraCommand?,
    onViewportChanged: (GeoPointModel, Double, Float, MapBoundingBox?) -> Unit,
    onUserInteracted: () -> Unit,
    onMapLongPress: (GeoPointModel) -> Unit,
    onSavedPlaceClick: (SavedPlace) -> Unit,
    onRouteSelect: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val onViewportLatest by rememberUpdatedState(onViewportChanged)
    val onUserInteractedLatest by rememberUpdatedState(onUserInteracted)
    val onLongPressLatest by rememberUpdatedState(onMapLongPress)
    val onSavedClickLatest by rememberUpdatedState(onSavedPlaceClick)
    val onRouteSelectLatest by rememberUpdatedState(onRouteSelect)

    var lastAppliedLayer by remember { mutableStateOf<MapLayerType?>(null) }
    var lastAppliedCommandId by remember { mutableStateOf<Long?>(null) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 3.0
            maxZoomLevel = 19.5
            isTilesScaledToDpi = true
            controller.setZoom(initialZoom)
            controller.setCenter(GeoPoint(initialCenter.lat, initialCenter.lon))

            // Two-finger rotation support
            val rotationOverlay = RotationGestureOverlay(this).apply {
                isEnabled = true
            }
            overlays.add(rotationOverlay)

            // Scale bar overlay
            val scaleBar = ScaleBarOverlay(this).apply {
                setCentred(false)
                setScaleBarOffset(32, 32)
            }
            overlays.add(scaleBar)

            // Long press & tap handler
            val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    p?.let { onLongPressLatest(GeoPointModel(it.latitude, it.longitude)) }
                    return true
                }
            })
            overlays.add(eventsOverlay)

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_MOVE) {
                    onUserInteractedLatest()
                }
                false
            }

            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    reportViewport(this@apply, onViewportLatest)
                    return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean {
                    reportViewport(this@apply, onViewportLatest)
                    return false
                }
            })
        }
    }

    // Lifecycle binding for OSMDroid
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Execute explicit camera commands
    LaunchedEffect(cameraCommand?.id) {
        val cmd = cameraCommand ?: return@LaunchedEffect
        if (lastAppliedCommandId != cmd.id) {
            lastAppliedCommandId = cmd.id
            cmd.zoom?.let { mapView.controller.setZoom(it) }
            cmd.orientationDeg?.let { mapView.mapOrientation = it }
            mapView.controller.animateTo(GeoPoint(cmd.target.lat, cmd.target.lon))
        }
    }

    // Follow user location smoothly when active
    LaunchedEffect(userLocation, userHeadingDeg, isFollowingUser, isNavigating) {
        if (isFollowingUser && userLocation != null) {
            mapView.controller.animateTo(GeoPoint(userLocation.lat, userLocation.lon))
            if (isNavigating) {
                mapView.mapOrientation = -userHeadingDeg
            }
        }
    }

    val userDrawable = remember(context) { createUserLocationDrawable(context) }
    val destDrawable = remember(context) { createPinDrawable(context, 0xFFE02424.toInt(), "م") }
    val savedDrawable = remember(context) { createPinDrawable(context, 0xFF0E9F6E.toInt(), "★") }
    val reportDrawable = remember(context) { createPinDrawable(context, 0xFFD97706.toInt(), "!") }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.setUseDataConnection(!offlineMode)

            if (lastAppliedLayer != mapLayer) {
                lastAppliedLayer = mapLayer
                val source = when (mapLayer) {
                    MapLayerType.STANDARD -> TileSourceFactory.MAPNIK
                    MapLayerType.SATELLITE -> SATELLITE_TILE_SOURCE
                    MapLayerType.TOPO -> TileSourceFactory.OpenTopo
                    MapLayerType.DARK -> DARK_TILE_SOURCE
                }
                view.setTileSource(source)
            }

            // Preserve persistent base overlays (Rotation, ScaleBar, MapEvents) and rebuild dynamic ones
            val baseOverlays = view.overlays.filter {
                it is RotationGestureOverlay || it is ScaleBarOverlay || it is MapEventsOverlay
            }
            view.overlays.clear()
            view.overlays.addAll(baseOverlays)

            // 1. Route polylines (draw alternatives first, then selected on top)
            routes.forEachIndexed { idx, route ->
                if (idx != selectedRouteIndex) {
                    val poly = Polyline(view).apply {
                        setPoints(route.geometryPoints.map { GeoPoint(it.lat, it.lon) })
                        outlinePaint.color = 0x9964748B.toInt()
                        outlinePaint.strokeWidth = 11f
                        setOnClickListener { _, _, _ ->
                            onRouteSelectLatest(idx)
                            true
                        }
                    }
                    view.overlays.add(poly)
                }
            }
            routes.getOrNull(selectedRouteIndex)?.let { selectedRoute ->
                val outlinePoly = Polyline(view).apply {
                    setPoints(selectedRoute.geometryPoints.map { GeoPoint(it.lat, it.lon) })
                    outlinePaint.color = 0xFF043A8C.toInt()
                    outlinePaint.strokeWidth = 18f
                }
                val mainPoly = Polyline(view).apply {
                    setPoints(selectedRoute.geometryPoints.map { GeoPoint(it.lat, it.lon) })
                    outlinePaint.color = 0xFF1D7AFC.toInt()
                    outlinePaint.strokeWidth = 12f
                }
                view.overlays.add(outlinePoly)
                view.overlays.add(mainPoly)
            }

            // 2. Saved places markers
            if (showSavedPins) {
                savedPlaces.forEach { saved ->
                    val marker = Marker(view).apply {
                        position = GeoPoint(saved.lat, saved.lon)
                        title = saved.name
                        snippet = saved.category.labelFa
                        icon = savedDrawable
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            onSavedClickLatest(saved)
                            true
                        }
                    }
                    view.overlays.add(marker)
                }
            }

            // 3. Active Street Reports markers
            if (showReportPins) {
                reports.filter { !it.isExpired() }.forEach { report ->
                    val marker = Marker(view).apply {
                        position = GeoPoint(report.lat, report.lon)
                        title = "${report.type.labelFa} (تأییدنشده)"
                        snippet = report.addressLabel
                        icon = reportDrawable
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    view.overlays.add(marker)
                }
            }

            // 4. Selected / Destination Pin
            selectedPlace?.let { place ->
                val marker = Marker(view).apply {
                    position = GeoPoint(place.lat, place.lon)
                    title = place.name
                    snippet = place.address ?: ""
                    icon = destDrawable
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                view.overlays.add(marker)
            }

            // 5. User Location + Heading Arrow Marker
            userLocation?.let { loc ->
                val userMarker = Marker(view).apply {
                    position = GeoPoint(loc.lat, loc.lon)
                    title = "موقعیت فعلی شما"
                    icon = userDrawable
                    rotation = -userHeadingDeg
                    isFlat = true
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                view.overlays.add(userMarker)
            }

            view.invalidate()
        }
    )
}

private fun reportViewport(
    mapView: MapView,
    onViewportChanged: (GeoPointModel, Double, Float, MapBoundingBox?) -> Unit
) {
    val c = mapView.mapCenter
    val bb = mapView.boundingBox
    val bounds = if (bb != null) {
        MapBoundingBox(
            minLat = bb.latSouth,
            maxLat = bb.latNorth,
            minLon = bb.lonWest,
            maxLon = bb.lonEast
        )
    } else null
    onViewportChanged(
        GeoPointModel(c.latitude, c.longitude),
        mapView.zoomLevelDouble,
        mapView.mapOrientation,
        bounds
    )
}

private fun createUserLocationDrawable(context: Context): Drawable {
    val size = 84
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f

    // Accuracy halo
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x330A66E6
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 38f, haloPaint)

    // Direction cone arrow pointing up (rotated by marker.rotation)
    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0A66E6.toInt()
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        moveTo(cx, 6f)
        lineTo(cx + 15f, 34f)
        lineTo(cx, 28f)
        lineTo(cx - 15f, 34f)
        close()
    }
    canvas.drawPath(path, arrowPaint)

    // White ring + blue core
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy + 4f, 16f, whitePaint)
    canvas.drawCircle(cx, cy + 4f, 11f, arrowPaint)

    return BitmapDrawable(context.resources, bmp)
}

private fun createPinDrawable(context: Context, fillColor: Int, symbol: String): Drawable {
    val width = 76
    val height = 96
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = width / 2f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44000000
        style = Paint.Style.FILL
    }
    canvas.drawOval(cx - 14f, height - 12f, cx + 14f, height - 2f, shadowPaint)

    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    canvas.drawCircle(cx, 32f, 26f, pinPaint)
    val tipPath = Path().apply {
        moveTo(cx - 19f, 46f)
        lineTo(cx, height - 6f)
        lineTo(cx + 19f, 46f)
        close()
    }
    canvas.drawPath(tipPath, pinPaint)
    canvas.drawCircle(cx, 32f, 26f, strokePaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    canvas.drawText(symbol, cx, 40f, textPaint)

    return BitmapDrawable(context.resources, bmp)
}
