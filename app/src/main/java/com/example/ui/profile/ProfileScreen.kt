package com.example.ui.profile

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.repository.DownloadProgressState
import com.example.data.repository.TtsStatus
import com.example.domain.model.AppSettings
import com.example.domain.model.DistanceUnit
import com.example.domain.model.LocationState
import com.example.domain.model.OfflineCityPreset
import com.example.domain.model.OfflineRegion
import com.example.domain.model.ReportType
import com.example.domain.model.StreetReport
import com.example.domain.model.ThemePreference
import com.example.domain.model.TravelMode
import com.example.ui.components.RahnamaLogoBadge
import com.example.ui.components.ReportIssueDialog
import com.example.util.GeoUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    settings: AppSettings,
    locationState: LocationState,
    ttsStatus: TtsStatus,
    offlineRegions: List<OfflineRegion>,
    downloadProgress: DownloadProgressState,
    cacheSizeBytes: Long,
    allReports: List<StreetReport>,
    savedPlacesCount: Int,
    searchHistoryCount: Int,
    onSetTheme: (ThemePreference) -> Unit,
    onSetVoiceEnabled: (Boolean) -> Unit,
    onSetVoiceVolume: (Float) -> Unit,
    onSetSelectedVoice: (String) -> Unit,
    onTestVoice: () -> Unit,
    onSetDefaultTravelMode: (TravelMode) -> Unit,
    onSetDistanceUnit: (DistanceUnit) -> Unit,
    onSetLocationSharing: (Boolean) -> Unit,
    onSetOfflineMode: (Boolean) -> Unit,
    onSetProfile: (String, String?) -> Unit,
    onClearSearchHistory: () -> Unit,
    onOpenSavedTab: () -> Unit,
    onDownloadCity: (OfflineCityPreset) -> Unit,
    onDownloadCurrentArea: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteOfflineRegion: (String) -> Unit,
    onClearAllMapCache: () -> Unit,
    onConfirmReport: (Long) -> Unit,
    onDeleteReport: (Long) -> Unit,
    onSubmitReport: (ReportType, String) -> Unit,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOfflineSheet by remember { mutableStateOf(false) }
    var showReportsSheet by remember { mutableStateOf(false) }
    var showNewReportDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onSetProfile(settings.profileName, uri.toString())
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Optional Local Profile Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(60.dp)
                        ) {
                            if (settings.profileAvatarUri != null) {
                                AsyncImage(
                                    model = Uri.parse(settings.profileAvatarUri),
                                    contentDescription = "تصویر پروفایل",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Person,
                                    contentDescription = "انتخاب تصویر پروفایل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings.profileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "استفاده کامل از تمام امکانات بدون نیاز به ثبت‌نام اجباری",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "ویرایش نام پروفایل")
                        }
                    }
                }
            }

            // Quick Feature Cards: Offline Maps & Street Reports
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        onClick = { showOfflineSheet = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_offline_maps_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("نقشه آفلاین", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${GeoUtils.toPersianDigits(offlineRegions.size.toString())} منطقه • ${GeoUtils.formatFileSize(cacheSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        onClick = { showReportsSheet = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_reports_manager_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text("گزارش‌های خیابان", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            val activeCount = allReports.count { !it.isExpired() }
                            Text(
                                text = "${GeoUtils.toPersianDigits(activeCount.toString())} گزارش فعال",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Appearance & Language
            item {
                SettingsSectionCard(title = "ظاهر و زبان برنامه") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("حالت نمایش", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = settings.themePreference == pref,
                                onClick = { onSetTheme(pref) },
                                label = { Text(pref.labelFa) }
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("زبان برنامه", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("فارسی (راست‌به‌چپ)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Voice Guidance Settings
            item {
                SettingsSectionCard(title = "تنظیمات راهنمای صوتی فارسی") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("پخش راهنمای صوتی", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(checked = settings.voiceEnabled, onCheckedChange = onSetVoiceEnabled)
                    }

                    Text(
                        text = "بلندی صدا: ${GeoUtils.toPersianDigits((settings.voiceVolume * 100).toInt().toString())}٪",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = settings.voiceVolume,
                        onValueChange = onSetVoiceVolume,
                        valueRange = 0f..1f,
                        enabled = settings.voiceEnabled
                    )

                    Text(
                        text = ttsStatus.statusMessageFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ttsStatus.isPersianSupported || ttsStatus.usingOnlinePersianVoice) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )

                    if (ttsStatus.availableVoices.isNotEmpty()) {
                        Text("انتخاب صدای راهنما:", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ttsStatus.availableVoices.forEach { voice ->
                                FilterChip(
                                    selected = settings.selectedVoiceName == voice.name,
                                    onClick = { onSetSelectedVoice(voice.name) },
                                    label = { Text(voice.displayLabelFa) }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onTestVoice, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تست صدای راهنما")
                        }
                        if (!ttsStatus.isPersianSupported) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                                    }
                                }
                            ) {
                                Text("تنظیمات TTS")
                            }
                        }
                    }
                }
            }

            // Navigation Preferences
            item {
                SettingsSectionCard(title = "مسیریابی و واحدها") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("نوع وسیله نقلیه پیش‌فرض", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TravelMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.defaultTravelMode == mode,
                                onClick = { onSetDefaultTravelMode(mode) },
                                label = { Text(mode.labelFa) }
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("واحد اندازه‌گیری فاصله", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DistanceUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = settings.distanceUnit == unit,
                                onClick = { onSetDistanceUnit(unit) },
                                label = { Text(unit.labelFa) }
                            )
                        }
                    }
                }
            }

            // Privacy & Data Management
            item {
                SettingsSectionCard(title = "حریم خصوصی و مدیریت داده‌ها") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("دسترسی مکانیابی درون برنامه", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (locationState.hasPermission) "مجوز سیستم صادر شده است" else "مجوز سیستم داده نشده است",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(checked = settings.locationSharingEnabled, onCheckedChange = onSetLocationSharing)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!locationState.hasPermission) {
                            OutlinedButton(onClick = onRequestLocationPermission, modifier = Modifier.weight(1f)) {
                                Text("درخواست مجوز GPS")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تنظیمات دسترسی سیستم")
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Bookmark, contentDescription = null)
                            Text("مکان‌های ذخیره‌شده (${GeoUtils.toPersianDigits(savedPlacesCount.toString())})")
                        }
                        TextButton(onClick = onOpenSavedTab) {
                            Text("مدیریت")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                            Text("تاریخچه جستوجو و مکان‌ها (${GeoUtils.toPersianDigits(searchHistoryCount.toString())})")
                        }
                        TextButton(onClick = onClearSearchHistory) {
                            Text("پاک کردن")
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Rounded.PrivacyTip, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "موقعیت مکانی شما تنها با رضایت خودتان در دستگاه استفاده می‌شود، به هیچ شخص دیگری نمایش داده نمی‌شود و داده‌های ذخیرهشده در حافظه امن دستگاه باقی می‌مانند.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // About Rahnama
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RahnamaLogoBadge(sizeDp = 48)
                            Column {
                                Text(
                                    text = "${stringResource(R.string.app_name)} | ${stringResource(R.string.app_name_en)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.app_slogan),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "نسخه برنامه: ${GeoUtils.toPersianDigits(BuildConfig.VERSION_NAME)} • داده‌های نقشه از OpenStreetMap، مسیریابی OSRM و جستوجوی Nominatim/Photon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showOfflineSheet) {
        OfflineMapsSheet(
            offlineModeEnabled = settings.offlineModeEnabled,
            downloadedRegions = offlineRegions,
            downloadProgress = downloadProgress,
            cacheSizeBytes = cacheSizeBytes,
            onToggleOfflineMode = onSetOfflineMode,
            onDownloadCity = onDownloadCity,
            onDownloadCurrentArea = onDownloadCurrentArea,
            onCancelDownload = onCancelDownload,
            onDeleteRegion = onDeleteOfflineRegion,
            onClearAllCache = onClearAllMapCache,
            onDismiss = { showOfflineSheet = false }
        )
    }

    if (showReportsSheet) {
        ReportsManagerSheet(
            reports = allReports,
            onConfirmReport = onConfirmReport,
            onDeleteReport = onDeleteReport,
            onNewReport = {
                showReportsSheet = false
                showNewReportDialog = true
            },
            onDismiss = { showReportsSheet = false }
        )
    }

    if (showNewReportDialog) {
        ReportIssueDialog(
            onDismiss = { showNewReportDialog = false },
            onSubmit = { type, desc ->
                onSubmitReport(type, desc)
                showNewReportDialog = false
            }
        )
    }

    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(settings.profileName) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("نام نمایشی محلی") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("نام شما") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSetProfile(nameInput.trim(), settings.profileAvatarUri)
                    showEditProfileDialog = false
                }) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
