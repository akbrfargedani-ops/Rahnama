package com.example.ui.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DistanceUnit
import com.example.domain.model.PlaceResult
import com.example.domain.model.SavedCategory
import com.example.domain.model.SearchHistoryItem
import com.example.ui.components.SavePlaceDialog
import com.example.ui.viewmodel.RouteFieldTarget
import com.example.ui.viewmodel.SearchUiState
import com.example.util.GeoUtils

private val QUICK_SEARCH_CATEGORIES = listOf(
    "رستوران",
    "کافه",
    "کارگاه",
    "تعمیرگاه",
    "بیمارستان",
    "داروخانه",
    "پمپ بنزین",
    "نانوایی",
    "بانک",
    "خودپرداز",
    "مترو",
    "مدرسه",
    "دانشگاه",
    "هتل",
    "مرکز خرید",
    "پارک"
)

@Composable
fun SearchScreen(
    searchState: SearchUiState,
    history: List<SearchHistoryItem>,
    distanceUnit: DistanceUnit,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String?) -> Unit,
    onCancelRoutePick: () -> Unit,
    onSelectResult: (PlaceResult) -> Unit,
    onRouteToPlace: (PlaceResult) -> Unit,
    onSavePlace: (String, String, SavedCategory, Double, Double) -> Unit,
    onSelectHistoryItem: (SearchHistoryItem) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var saveDialogPlace by remember { mutableStateOf<PlaceResult?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (searchState.pickingForRouteField != null) {
                val label = if (searchState.pickingForRouteField == RouteFieldTarget.ORIGIN) {
                    "در حال انتخاب مبدأ مسیر"
                } else {
                    "در حال انتخاب مقصد مسیر"
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = onCancelRoutePick) {
                            Text("انصراف")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchState.query,
                onValueChange = onQueryChange,
                placeholder = { Text("نام خیابان، کوچه، محله، شهر، روستا یا مکان...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchState.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "پاک کردن متن")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmitSearch(null) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input_field")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(QUICK_SEARCH_CATEGORIES) { cat ->
                    AssistChip(
                        onClick = { onSubmitSearch(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            if (searchState.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("در حال جستوجو در نقشه OpenStreetMap...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            searchState.errorMessage?.let { err ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }

            val displayList = when {
                searchState.suggestions.isNotEmpty() -> searchState.suggestions
                searchState.results.isNotEmpty() -> searchState.results
                else -> emptyList()
            }

            if (displayList.isNotEmpty()) {
                val heading = if (searchState.suggestions.isNotEmpty()) "پیشنهادهای خودکار" else "نتایج جستوجو"
                Text(heading, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(displayList, key = { it.id }) { place ->
                        SearchPlaceCard(
                            place = place,
                            distanceUnit = distanceUnit,
                            isRoutePicking = searchState.pickingForRouteField != null,
                            onSelectOnMap = { onSelectResult(place) },
                            onRoute = { onRouteToPlace(place) },
                            onSave = { saveDialogPlace = place }
                        )
                    }
                }
            } else {
                // Recent searches section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("جستوجوهای اخیر", style = MaterialTheme.typography.titleMedium)
                    if (history.isNotEmpty()) {
                        TextButton(
                            onClick = onClearHistory,
                            modifier = Modifier.testTag("clear_search_history_button")
                        ) {
                            Text("پاک کردن همه")
                        }
                    }
                }

                if (history.isEmpty()) {
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
                                Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "هنوز جستوجویی ثبت نشده است",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "خیابان، محله، شهر، داروخانه یا پمپ بنزین مورد نظر را جستوجو کنید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(history, key = { it.id }) { item ->
                            Surface(
                                onClick = { onSelectHistoryItem(item) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { onDeleteHistoryItem(item.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف از تاریخچه")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    saveDialogPlace?.let { place ->
        SavePlaceDialog(
            initialName = place.name,
            initialAddress = place.address ?: stringResource(R.string.data_unavailable),
            onDismiss = { saveDialogPlace = null },
            onConfirm = { name, address, cat ->
                onSavePlace(name, address, cat, place.lat, place.lon)
                saveDialogPlace = null
            }
        )
    }
}

@Composable
private fun SearchPlaceCard(
    place: PlaceResult,
    distanceUnit: DistanceUnit,
    isRoutePicking: Boolean,
    onSelectOnMap: () -> Unit,
    onRoute: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        onClick = onSelectOnMap,
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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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
                        Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onSave) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = "ذخیره مکان")
                }
            }

            if (!isRoutePicking) {
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSelectOnMap,
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
                }
            }
        }
    }
}
