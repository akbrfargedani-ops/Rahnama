package com.example.ui.nearby

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Atm
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Subway
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.DistanceUnit
import com.example.domain.model.NearbyCategory
import com.example.domain.model.OfflineCityPreset
import com.example.domain.model.PlaceResult
import com.example.ui.components.IranCityPickerDialog
import com.example.ui.viewmodel.NearbyUiState
import com.example.util.GeoUtils

@Composable
fun NearbyScreen(
    nearbyState: NearbyUiState,
    distanceUnit: DistanceUnit,
    selectedCityNameFa: String,
    onSelectIranCity: (OfflineCityPreset) -> Unit,
    onSelectCategory: (NearbyCategory) -> Unit,
    onRefresh: () -> Unit,
    onViewOnMap: (PlaceResult) -> Unit,
    onRouteToPlace: (PlaceResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCityPicker by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text("مکان‌های واقعی اطراف در ایران", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "محدوده جستوجو: $selectedCityNameFa (OpenStreetMap ایران)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { showCityPicker = true },
                        label = { Text(selectedCityNameFa) },
                        leadingIcon = { Icon(Icons.Rounded.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("nearby_refresh_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NearbyCategory.entries) { cat ->
                    FilterChip(
                        selected = nearbyState.selectedCategory == cat,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat.labelFa) },
                        leadingIcon = {
                            Icon(
                                imageVector = nearbyCategoryIcon(cat),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            when {
                nearbyState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("در حال دریافت مکان‌های اطراف از Overpass...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                nearbyState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = nearbyState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = onRefresh) {
                                Text("تلاش مجدد")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(nearbyState.places, key = { it.id }) { place ->
                            NearbyPlaceCard(
                                place = place,
                                category = nearbyState.selectedCategory,
                                distanceUnit = distanceUnit,
                                onViewOnMap = { onViewOnMap(place) },
                                onRoute = { onRouteToPlace(place) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCityPicker) {
        IranCityPickerDialog(
            selectedCityNameFa = selectedCityNameFa,
            onSelectCity = onSelectIranCity,
            onDismiss = { showCityPicker = false }
        )
    }
}

@Composable
private fun NearbyPlaceCard(
    place: PlaceResult,
    category: NearbyCategory,
    distanceUnit: DistanceUnit,
    onViewOnMap: () -> Unit,
    onRoute: () -> Unit
) {
    val context = LocalContext.current
    val unavailable = stringResource(R.string.data_unavailable)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (place.imageUrl != null) {
                    AsyncImage(
                        model = place.imageUrl,
                        contentDescription = place.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = nearbyCategoryIcon(category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = place.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        place.distanceMeters?.let { dist ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = GeoUtils.formatDistance(dist, distanceUnit),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "نشانی: ${place.address ?: unavailable}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Real optional OSM metadata (phone, hours, rating)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "ساعت کاری: ${place.openingHours ?: unavailable}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "تلفن: ${place.phone ?: unavailable}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (place.phone != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.StarOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "امتیاز: ${place.rating ?: unavailable}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onViewOnMap,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مشاهده روی نقشه")
                }
                Button(
                    onClick = onRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مسیریابی")
                }
                if (place.phone != null) {
                    IconButton(
                        onClick = {
                            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${place.phone}"))
                            context.startActivity(dial)
                        }
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = "تماس با مکان", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun nearbyCategoryIcon(cat: NearbyCategory): ImageVector = when (cat) {
    NearbyCategory.RESTAURANT -> Icons.Rounded.Restaurant
    NearbyCategory.CAFE -> Icons.Rounded.LocalCafe
    NearbyCategory.WORKSHOP -> Icons.Rounded.Engineering
    NearbyCategory.CAR_REPAIR -> Icons.Rounded.Build
    NearbyCategory.SUPERMARKET -> Icons.Rounded.ShoppingCart
    NearbyCategory.BAKERY -> Icons.Rounded.BakeryDining
    NearbyCategory.HOSPITAL -> Icons.Rounded.LocalHospital
    NearbyCategory.PHARMACY -> Icons.Rounded.LocalPharmacy
    NearbyCategory.FUEL -> Icons.Rounded.LocalGasStation
    NearbyCategory.BANK -> Icons.Rounded.AccountBalance
    NearbyCategory.ATM -> Icons.Rounded.Atm
    NearbyCategory.METRO -> Icons.Rounded.Subway
    NearbyCategory.PARKING -> Icons.Rounded.LocalParking
    NearbyCategory.PARK -> Icons.Rounded.Park
    NearbyCategory.MALL -> Icons.Rounded.ShoppingBag
    NearbyCategory.HOTEL -> Icons.Rounded.Hotel
}
