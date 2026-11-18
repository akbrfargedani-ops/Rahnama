package com.example.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.DistanceUnit
import com.example.domain.model.StreetReport
import com.example.domain.model.TravelMode
import com.example.ui.components.ShareHelpers
import com.example.ui.theme.RahnamaEmerald
import com.example.ui.theme.TrafficUnknownSlate
import com.example.ui.viewmodel.RouteFieldTarget
import com.example.ui.viewmodel.RoutingUiState
import com.example.util.GeoUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RouteDetailsSheet(
    routingState: RoutingUiState,
    distanceUnit: DistanceUnit,
    activeReports: List<StreetReport>,
    onPickField: (RouteFieldTarget) -> Unit,
    onUseCurrentOrigin: () -> Unit,
    onSwap: () -> Unit,
    onSelectTravelMode: (TravelMode) -> Unit,
    onSelectRoute: (Int) -> Unit,
    onRetry: () -> Unit,
    onStartNavigation: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    var showStepsList by remember { mutableStateOf(false) }

    val originName = routingState.origin?.name ?: "موقعیت فعلی من"
    val destName = routingState.destination?.name ?: "مقصد را انتخاب کنید"
    val selectedRoute = routingState.selectedRoute

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 12.dp,
        modifier = modifier
            .widthIn(max = 620.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .heightIn(max = 540.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("جزئیات و انتخاب مسیر", style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onClose, modifier = Modifier.testTag("close_route_details_button")) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن")
                }
            }

            // Origin & Destination selector box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = { onPickField(RouteFieldTarget.ORIGIN) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Rounded.MyLocation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "مبدأ: $originName",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (routingState.origin != null) {
                                    TextButton(
                                        onClick = onUseCurrentOrigin,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("GPS", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        Surface(
                            onClick = { onPickField(RouteFieldTarget.DESTINATION) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "مقصد: $destName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onSwap,
                        modifier = Modifier.testTag("swap_origin_dest_button")
                    ) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = "جابه‌جایی مبدأ و مقصد")
                    }
                }
            }

            // Travel modes
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(TravelMode.entries) { _, mode ->
                    val icon = when (mode) {
                        TravelMode.CAR -> Icons.Rounded.DirectionsCar
                        TravelMode.WALK -> Icons.AutoMirrored.Rounded.DirectionsWalk
                        TravelMode.BIKE -> Icons.AutoMirrored.Rounded.DirectionsBike
                        TravelMode.MOTORCYCLE -> Icons.Rounded.TwoWheeler
                    }
                    FilterChip(
                        selected = routingState.travelMode == mode,
                        onClick = { onSelectTravelMode(mode) },
                        label = { Text(mode.labelFa) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            if (routingState.travelMode == TravelMode.MOTORCYCLE) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "موتورسیکلت: موتور مسیریابی عمومی پروفایل اختصاصی موتور ندارد؛ مسیر بر اساس شبکه خودرو محاسبه شده است.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            when {
                routingState.destination == null -> {
                    Text(
                        text = "برای محاسبه مسیر، مقصد را جستوجو کنید یا روی نقشه انگشت خود را نگه دارید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                routingState.isCalculating -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("در حال محاسبه مسیرهای واقعی از OSRM...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                routingState.errorMessage != null -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = routingState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            OutlinedButton(onClick = onRetry) {
                                Text("تلاش مجدد")
                            }
                        }
                    }
                }
                routingState.routes.isNotEmpty() -> {
                    // Alternative Routes horizontal selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(routingState.routes) { idx, route ->
                            val isSelected = idx == routingState.selectedRouteIndex
                            Card(
                                onClick = { onSelectRoute(idx) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    }
                                ),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.widthIn(min = 155.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (route.isFastest) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = RahnamaEmerald.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "سریع‌ترین",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = RahnamaEmerald,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (route.isShortest) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "کوتاه‌ترین",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (!route.isFastest && !route.isShortest) {
                                            Text(
                                                text = "مسیر جایگزین ${GeoUtils.toPersianDigits((idx + 1).toString())}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = GeoUtils.formatDuration(route.durationSeconds),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${GeoUtils.formatDistance(route.distanceMeters, distanceUnit)} • رسیدن ${GeoUtils.formatArrivalTimeTehran(route.durationSeconds)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Detailed Route Summary (Section 10)
                    if (selectedRoute != null) {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "تعداد پیچ‌ها: ${GeoUtils.toPersianDigits(selectedRoute.steps.size.toString())} مرحله",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Traffic,
                                                    contentDescription = null,
                                                    tint = TrafficUnknownSlate,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "ترافیک آنلاین: نامشخص",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        val nearbyReports = activeReports.filter { rep ->
                                            !rep.isExpired() && GeoUtils.distanceToPolylineMeters(
                                                com.example.domain.model.GeoPointModel(rep.lat, rep.lon),
                                                selectedRoute.geometryPoints
                                            ) < 250.0
                                        }
                                        if (nearbyReports.isNotEmpty()) {
                                            Text(
                                                text = "هشدار کاربر در مسیر (تأییدنشده): ${nearbyReports.joinToString("، ") { it.type.labelFa }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }

                                        Text("خیابان‌های اصلی مسیر:", style = MaterialTheme.typography.labelMedium)
                                        if (selectedRoute.mainStreets.isEmpty()) {
                                            Text(
                                                text = "اطلاعات در دسترس نیست",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                selectedRoute.mainStreets.forEach { st ->
                                                    AssistChip(
                                                        onClick = {},
                                                        label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                                                    )
                                                }
                                            }
                                        }

                                        TextButton(
                                            onClick = { showStepsList = !showStepsList },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (showStepsList) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (showStepsList) "پنهان کردن جزئیات پیچ‌ها" else "مشاهده گام‌به‌گام مسیر")
                                        }

                                        if (showStepsList) {
                                            HorizontalDivider()
                                            selectedRoute.steps.forEachIndexed { idx, step ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "${GeoUtils.toPersianDigits((idx + 1).toString())}. ${step.instructionFa}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = GeoUtils.formatDistance(step.distanceMeters, distanceUnit),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Action Buttons: Start Navigation & Share Route
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onStartNavigation,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("start_navigation_button")
                            ) {
                                Icon(Icons.Rounded.Navigation, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("شروع مسیریابی")
                            }
                            OutlinedButton(
                                onClick = {
                                    ShareHelpers.shareRoute(
                                        context = context,
                                        originName = originName,
                                        destinationName = destName,
                                        route = selectedRoute,
                                        unit = distanceUnit
                                    )
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("share_route_button")
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "اشتراک‌گذاری مسیر")
                                Spacer(Modifier.width(6.dp))
                                Text("اشتراک‌گذاری")
                            }
                        }
                    }
                }
            }
        }
    }
}
