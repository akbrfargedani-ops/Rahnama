package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MinorCrash
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DistanceUnit
import com.example.domain.model.MapLayerType
import com.example.domain.model.PlaceResult
import com.example.domain.model.ReportType
import com.example.domain.model.RouteOption
import com.example.domain.model.SavedCategory
import com.example.domain.model.StreetReport
import com.example.ui.theme.TrafficFreeGreen
import com.example.ui.theme.TrafficHeavyOrange
import com.example.ui.theme.TrafficModerateYellow
import com.example.ui.theme.TrafficSevereRed
import com.example.ui.theme.TrafficUnknownSlate
import com.example.util.GeoUtils

@Composable
fun RahnamaLogoBadge(
    modifier: Modifier = Modifier,
    sizeDp: Int = 36
) {
    Image(
        painter = painterResource(id = R.drawable.rahnama_logo),
        contentDescription = stringResource(R.string.cd_app_logo),
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}

@Composable
fun CompassButton(
    orientationDeg: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp,
        modifier = modifier
            .size(48.dp)
            .testTag("compass_button")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Navigation,
                contentDescription = stringResource(R.string.cd_compass),
                tint = if (kotlin.math.abs(orientationDeg) < 3f) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                modifier = Modifier
                    .size(24.dp)
                    .rotate(-orientationDeg)
            )
        }
    }
}

fun savedCategoryIcon(category: SavedCategory): ImageVector = when (category) {
    SavedCategory.HOME -> Icons.Rounded.Home
    SavedCategory.WORK -> Icons.Rounded.Work
    SavedCategory.SCHOOL -> Icons.Rounded.School
    SavedCategory.FRIENDS -> Icons.Rounded.People
    SavedCategory.FAVORITES -> Icons.Rounded.Favorite
}

fun reportTypeIcon(type: ReportType): ImageVector = when (type) {
    ReportType.ACCIDENT -> Icons.Rounded.CarCrash
    ReportType.HEAVY_TRAFFIC -> Icons.Rounded.Traffic
    ReportType.ROAD_CLOSED -> Icons.Rounded.DoNotDisturbOn
    ReportType.ROADWORKS -> Icons.Rounded.Construction
    ReportType.BROKEN_VEHICLE -> Icons.Rounded.MinorCrash
    ReportType.HAZARD -> Icons.Rounded.Warning
    ReportType.OBSTACLE -> Icons.Rounded.ReportProblem
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavePlaceDialog(
    initialName: String,
    initialAddress: String,
    initialCategory: SavedCategory = SavedCategory.FAVORITES,
    confirmLabel: String = "ذخیره مکان",
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String, category: SavedCategory) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var address by remember { mutableStateOf(initialAddress) }
    var category by remember { mutableStateOf(initialCategory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ذخیره در مکان‌های شخصی",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام مکان") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_place_name_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("نشانی") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_place_address_input")
                )
                Text(
                    text = "دسته‌بندی",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SavedCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.labelFa) },
                            leadingIcon = {
                                Icon(
                                    imageVector = savedCategoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), address.trim().ifBlank { "اطلاعات در دسترس نیست" }, category)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_save_place_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportIssueDialog(
    onDismiss: () -> Unit,
    onSubmit: (ReportType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(ReportType.HEAVY_TRAFFIC) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Text("گزارش وضعیت خیابان", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "گزارش شما با موقعیت جغرافیایی و زمان ثبت ذخیره می‌شود و پس از مدت مشخص به‌صورت خودکار منقضی خواهد شد. گزارش‌های کاربران به عنوان «تأییدنشده» نمایش داده می‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReportType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.labelFa) },
                            leadingIcon = {
                                Icon(
                                    imageVector = reportTypeIcon(type),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات تکمیلی (اختیاری)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedType, description) },
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("ثبت گزارش")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}

@Composable
fun TrafficInfoDialog(
    activeReports: List<StreetReport>,
    onDismiss: () -> Unit,
    onOpenReportDialog: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Traffic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("وضعیت ترافیک زنده", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TrafficUnknownSlate.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "وضعیت منبع ترافیک آنلاین: نامشخص",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اطلاعات جریان ترافیک زنده از منبع رسمی بدون کلید تجاری در دسترس نیست. راهنما از تولید رنگ یا ترافیک ساختگی خودداری می‌کند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text("راهنمای رنگ‌بندی استاندارد ترافیک:", style = MaterialTheme.typography.labelLarge)
                TrafficLegendRow(TrafficFreeGreen, "سبز: ترافیک روان")
                TrafficLegendRow(TrafficModerateYellow, "زرد: ترافیک نیمه‌سنگین")
                TrafficLegendRow(TrafficHeavyOrange, "نارنجی: ترافیک سنگین")
                TrafficLegendRow(TrafficSevereRed, "قرمز: ترافیک بسیار سنگین")

                HorizontalDivider()

                val heavyReports = activeReports.count { !it.isExpired() }
                Text(
                    text = "گزارش‌های فعال ثبت‌شده کاربران (تأییدنشده): ${GeoUtils.toPersianDigits(heavyReports.toString())} مورد",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                onOpenReportDialog()
            }) {
                Text("ثبت گزارش ترافیک")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}

@Composable
private fun TrafficLegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLayersBottomSheet(
    currentLayer: MapLayerType,
    showSavedPins: Boolean,
    showReportPins: Boolean,
    offlineModeEnabled: Boolean,
    onSelectLayer: (MapLayerType) -> Unit,
    onToggleSavedPins: (Boolean) -> Unit,
    onToggleReportPins: (Boolean) -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("لایه‌های نقشه و نمایش", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LayerOptionCard(
                    label = MapLayerType.STANDARD.labelFa,
                    icon = Icons.Rounded.Map,
                    selected = currentLayer == MapLayerType.STANDARD,
                    onClick = { onSelectLayer(MapLayerType.STANDARD) },
                    modifier = Modifier.weight(1f)
                )
                LayerOptionCard(
                    label = MapLayerType.SATELLITE.labelFa,
                    icon = Icons.Rounded.SatelliteAlt,
                    selected = currentLayer == MapLayerType.SATELLITE,
                    onClick = { onSelectLayer(MapLayerType.SATELLITE) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LayerOptionCard(
                    label = MapLayerType.TOPO.labelFa,
                    icon = Icons.Rounded.Terrain,
                    selected = currentLayer == MapLayerType.TOPO,
                    onClick = { onSelectLayer(MapLayerType.TOPO) },
                    modifier = Modifier.weight(1f)
                )
                LayerOptionCard(
                    label = MapLayerType.DARK.labelFa,
                    icon = Icons.Rounded.DarkMode,
                    selected = currentLayer == MapLayerType.DARK,
                    onClick = { onSelectLayer(MapLayerType.DARK) },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("نمایش مکان‌های ذخیره‌شده روی نقشه", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = showSavedPins, onCheckedChange = onToggleSavedPins)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("نمایش گزارش‌های خیابان روی نقشه", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = showReportPins, onCheckedChange = onToggleReportPins)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("استفاده از نقشه آفلاین (بدون اینترنت)", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "فقط از کاشی‌های ذخیره‌شده روی دستگاه استفاده می‌شود",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = offlineModeEnabled, onCheckedChange = onToggleOfflineMode)
            }
        }
    }
}

@Composable
private fun LayerOptionCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = modifier
            .height(74.dp)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IranCityPickerDialog(
    selectedCityNameFa: String,
    onSelectCity: (com.example.domain.model.OfflineCityPreset) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب شهر ایران", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "برای مشاهده مکان‌ها و مسیرهای واقعی در شهر مورد نظر خود انتخاب کنید:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(GeoUtils.IRAN_OFFLINE_CITIES.size) { index ->
                        val city = GeoUtils.IRAN_OFFLINE_CITIES[index]
                        val isSelected = city.nameFa == selectedCityNameFa
                        Surface(
                            onClick = {
                                onSelectCity(city)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = city.nameFa,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = city.provinceFa,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}

object ShareHelpers {
    fun sharePlace(context: Context, place: PlaceResult) {
        val text = buildString {
            appendLine("مکان اشتراک‌گذاری‌شده از اپلیکیشن راهنما:")
            appendLine(place.name)
            if (!place.address.isNullOrBlank()) appendLine("نشانی: ${place.address}")
            appendLine("مختصات: ${place.lat}, ${place.lon}")
            append("https://www.openstreetmap.org/?mlat=${place.lat}&mlon=${place.lon}#map=16/${place.lat}/${place.lon}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری مکان"))
    }

    fun shareRoute(
        context: Context,
        originName: String,
        destinationName: String,
        route: RouteOption,
        unit: DistanceUnit
    ) {
        val dist = GeoUtils.formatDistance(route.distanceMeters, unit)
        val dur = GeoUtils.formatDuration(route.durationSeconds)
        val streets = if (route.mainStreets.isNotEmpty()) route.mainStreets.joinToString(" ← ") else "اطلاعات در دسترس نیست"
        val text = buildString {
            appendLine("مسیر پیشنهادی راهنما (${route.travelMode.labelFa}):")
            appendLine("مبدأ: $originName")
            appendLine("مقصد: $destinationName")
            appendLine("فاصله کل: $dist")
            appendLine("زمان تقریبی: $dur")
            appendLine("خیابان‌های اصلی: $streets")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری مسیر"))
    }
}
