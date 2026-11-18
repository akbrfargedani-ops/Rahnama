package com.example.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.GeoPointModel
import com.example.domain.model.PlaceResult
import com.example.domain.model.SavedCategory
import com.example.domain.model.SavedPlace
import com.example.ui.components.SavePlaceDialog
import com.example.ui.components.savedCategoryIcon

@Composable
fun SavedScreen(
    savedPlaces: List<SavedPlace>,
    showSavedOnMap: Boolean,
    referencePoint: GeoPointModel,
    onToggleShowOnMap: (Boolean) -> Unit,
    onAddPlace: (String, String, SavedCategory, Double, Double) -> Unit,
    onUpdatePlace: (SavedPlace) -> Unit,
    onDeletePlace: (Long) -> Unit,
    onViewOnMap: (PlaceResult) -> Unit,
    onRouteToPlace: (PlaceResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<SavedCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlace by remember { mutableStateOf<SavedPlace?>(null) }

    val filtered = remember(savedPlaces, selectedCategory) {
        if (selectedCategory == null) savedPlaces else savedPlaces.filter { it.category == selectedCategory }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("مکان‌های ذخیره‌شده", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "اطلاعات در حافظه دستگاه نگهداری می‌شود",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("نمایش روی نقشه", style = MaterialTheme.typography.labelMedium)
                    Switch(checked = showSavedOnMap, onCheckedChange = onToggleShowOnMap)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("همه") }
                    )
                }
                items(SavedCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.labelFa) },
                        leadingIcon = {
                            Icon(savedCategoryIcon(cat), contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("هنوز مکانی در این بخش ذخیره نشده است", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "از دکمه + پایین صفحه یا از روی نقشه و جستوجو مکان‌های خود را اضافه کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    items(filtered, key = { it.id }) { place ->
                        val asPlaceResult = PlaceResult(
                            id = "saved_${place.id}",
                            name = place.name,
                            address = place.address,
                            category = place.category.labelFa,
                            lat = place.lat,
                            lon = place.lon
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = savedCategoryIcon(place.category),
                                            contentDescription = place.category.labelFa,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = place.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = place.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = place.category.labelFa,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { editingPlace = place }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "ویرایش نام و نشانی")
                                    }
                                    IconButton(onClick = { onDeletePlace(place.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف مکان")
                                    }
                                }
                                HorizontalDivider()
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onViewOnMap(asPlaceResult) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("نمایش روی نقشه")
                                    }
                                    Button(
                                        onClick = { onRouteToPlace(asPlaceResult) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("مسیریابی")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_saved_place_fab")
        ) {
            Icon(Icons.Rounded.AddLocationAlt, contentDescription = "افزودن مکان جدید")
        }
    }

    if (showAddDialog) {
        SavePlaceDialog(
            initialName = "",
            initialAddress = "${String.format("%.5f", referencePoint.lat)}, ${String.format("%.5f", referencePoint.lon)}",
            initialCategory = selectedCategory ?: SavedCategory.HOME,
            confirmLabel = "افزودن مکان",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, address, cat ->
                onAddPlace(name, address, cat, referencePoint.lat, referencePoint.lon)
                showAddDialog = false
            }
        )
    }

    editingPlace?.let { existing ->
        SavePlaceDialog(
            initialName = existing.name,
            initialAddress = existing.address,
            initialCategory = existing.category,
            confirmLabel = "ذخیره تغییرات",
            onDismiss = { editingPlace = null },
            onConfirm = { newName, newAddr, newCat ->
                onUpdatePlace(
                    existing.copy(
                        name = newName,
                        address = newAddr,
                        category = newCat
                    )
                )
                editingPlace = null
            }
        )
    }
}
