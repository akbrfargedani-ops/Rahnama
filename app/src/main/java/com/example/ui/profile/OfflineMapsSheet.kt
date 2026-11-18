package com.example.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.DownloadProgressState
import com.example.domain.model.OfflineCityPreset
import com.example.domain.model.OfflineRegion
import com.example.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapsSheet(
    offlineModeEnabled: Boolean,
    downloadedRegions: List<OfflineRegion>,
    downloadProgress: DownloadProgressState,
    cacheSizeBytes: Long,
    onToggleOfflineMode: (Boolean) -> Unit,
    onDownloadCity: (OfflineCityPreset) -> Unit,
    onDownloadCurrentArea: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteRegion: (String) -> Unit,
    onClearAllCache: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("نقشه‌های آفلاین", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "فضای مصرف‌شده: ${GeoUtils.formatFileSize(cacheSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "بستن")
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (offlineModeEnabled) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("استفاده بدون اینترنت (حالت آفلاین)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "نقشه فقط از کاشی‌های ذخیره‌شده نمایش داده می‌شود",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(checked = offlineModeEnabled, onCheckedChange = onToggleOfflineMode)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "نمایش، زوم و مرور نقشه در مناطق دانلودشده کاملاً بدون اینترنت انجام می‌شود. مسیریابی خودکار OSRM به اتصال اینترنت نیاز دارد و به‌صورت آفلاین ارائه نمی‌شود.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (downloadProgress.isDownloading) {
                val pct = if (downloadProgress.totalTiles > 0) {
                    downloadProgress.completedTiles.toFloat() / downloadProgress.totalTiles.toFloat()
                } else 0f
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "در حال دانلود ${downloadProgress.regionNameFa}...",
                                style = MaterialTheme.typography.titleSmall
                            )
                            TextButton(onClick = onCancelDownload) {
                                Text("لغو")
                            }
                        }
                        LinearProgressIndicator(
                            progress = { pct.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${GeoUtils.toPersianDigits(downloadProgress.completedTiles.toString())} از ${GeoUtils.toPersianDigits(downloadProgress.totalTiles.toString())} کاشی نقشه",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDownloadCurrentArea,
                    enabled = !downloadProgress.isDownloading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("دانلود محدوده فعلی نقشه")
                }
                if (cacheSizeBytes > 0) {
                    OutlinedButton(onClick = onClearAllCache) {
                        Text("پاک‌سازی کش")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (downloadedRegions.isNotEmpty()) {
                    item {
                        Text("مناطق دانلودشده", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    items(downloadedRegions, key = { it.id }) { region ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(region.nameFa, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${GeoUtils.toPersianDigits(region.tileCount.toString())} کاشی • ${GeoUtils.formatFileSize(region.sizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteRegion(region.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف نقشه")
                                }
                            }
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                }

                item {
                    Text("انتخاب شهر برای دانلود نقشه", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                items(GeoUtils.IRAN_OFFLINE_CITIES, key = { it.id }) { city ->
                    val estTiles = GeoUtils.estimateTileCount(
                        city.minLat, city.maxLat, city.minLon, city.maxLon, city.minZoom, city.maxZoom
                    )
                    val estBytes = estTiles * 14_000L
                    val isDownloaded = downloadedRegions.any { it.id == city.id }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("${city.nameFa} (${city.provinceFa})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "حجم تقریبی: ${GeoUtils.formatFileSize(estBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { onDownloadCity(city) },
                                enabled = !downloadProgress.isDownloading
                            ) {
                                Text(if (isDownloaded) "به‌روزرسانی" else "دانلود")
                            }
                        }
                    }
                }
            }
        }
    }
}
