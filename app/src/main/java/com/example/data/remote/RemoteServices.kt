package com.example.data.remote

import com.example.domain.model.DistanceUnit
import com.example.domain.model.GeoPointModel
import com.example.domain.model.NearbyCategory
import com.example.domain.model.PlaceResult
import com.example.domain.model.RouteOption
import com.example.domain.model.RouteStep
import com.example.domain.model.TravelMode
import com.example.util.GeoUtils
import com.example.util.PersianManeuverTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RemoteMapServices {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "RahnamaMaps/1.0 (Android; OpenStreetMap Navigation)")
                .header("Accept-Language", "fa,en;q=0.7")
                .build()
            chain.proceed(req)
        }
        .build()

    private val genericNames = setOf(
        "رستوران", "کافه", "کارگاه", "تعمیرگاه", "داروخانه", "بیمارستان",
        "سوپرمارکت", "نانوایی", "بانک", "خودپرداز", "پمپ بنزین", "پارک", "هتل",
        "restaurant", "cafe", "shop", "workshop", "pharmacy", "hospital"
    )

    suspend fun searchPlaces(
        query: String,
        biasCenter: GeoPointModel?
    ): List<PlaceResult> = withContext(Dispatchers.IO) {
        val cleanQuery = GeoUtils.normalizePersianQuery(query)
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val iranCenter = if (biasCenter != null && GeoUtils.isInIran(biasCenter.lat, biasCenter.lon)) {
            biasCenter
        } else {
            GeoUtils.DEFAULT_TEHRAN_CENTER
        }

        // If query matches or starts with a Persian category (e.g., رستوران، کارگاه، کافه، تعمیرگاه),
        // fetch real named Iranian places in that category via Overpass as well!
        val matchedCategory = matchCategoryFromQuery(cleanQuery)
        val overpassCategoryResults = if (matchedCategory != null) {
            runCatching {
                fetchNearbyPlaces(iranCenter, matchedCategory, radiusMeters = 6000)
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        // Query Nominatim restricted strictly to Iran (countrycodes=ir)
        val nominatimResults = runCatching { queryNominatimIran(cleanQuery, iranCenter) }
            .getOrDefault(emptyList())

        // Query Photon biased to Iran and filtered strictly inside Iran bounds
        val photonResults = runCatching { queryPhotonIran(cleanQuery, iranCenter) }
            .getOrDefault(emptyList())

        val merged = mutableListOf<PlaceResult>()
        val combined = if (matchedCategory != null && cleanQuery.split(" ").size == 1) {
            // Pure category search ("رستوران", "کارگاه"): prioritize real named Overpass POIs first!
            overpassCategoryResults + nominatimResults + photonResults
        } else {
            nominatimResults + photonResults + overpassCategoryResults
        }

        for (candidate in combined) {
            if (!GeoUtils.isInIran(candidate.lat, candidate.lon)) continue
            val duplicate = merged.any { existing ->
                GeoUtils.distanceMeters(existing.lat, existing.lon, candidate.lat, candidate.lon) < 70.0 &&
                    existing.name.equals(candidate.name, ignoreCase = true)
            }
            if (!duplicate) merged.add(candidate)
        }

        // Typo/partial-token fallback inside Iran if empty
        if (merged.isEmpty() && cleanQuery.contains(" ")) {
            val tokens = cleanQuery.split(" ").filter { it.length >= 2 }
            if (tokens.size >= 2) {
                val relaxed = tokens.take(2).joinToString(" ")
                val retry = runCatching { queryNominatimIran(relaxed, iranCenter) }.getOrDefault(emptyList()) +
                    runCatching { queryPhotonIran(relaxed, iranCenter) }.getOrDefault(emptyList())
                merged.addAll(retry.filter { GeoUtils.isInIran(it.lat, it.lon) })
            }
        }

        merged.map { p ->
            p.copy(distanceMeters = GeoUtils.distanceMeters(iranCenter.lat, iranCenter.lon, p.lat, p.lon))
        }.sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
    }

    private fun matchCategoryFromQuery(query: String): NearbyCategory? {
        return when {
            query.contains("رستوران") || query.contains("فست فود") || query.contains("کباب") -> NearbyCategory.RESTAURANT
            query.contains("کافه") || query.contains("قهوه") -> NearbyCategory.CAFE
            query.contains("کارگاه") || query.contains("تولیدی") || query.contains("صنعت") -> NearbyCategory.WORKSHOP
            query.contains("تعمیرگاه") || query.contains("مکانیک") -> NearbyCategory.CAR_REPAIR
            query.contains("سوپرمارکت") || query.contains("فروشگاه") -> NearbyCategory.SUPERMARKET
            query.contains("نانوایی") -> NearbyCategory.BAKERY
            query.contains("بیمارستان") || query.contains("درمانگاه") -> NearbyCategory.HOSPITAL
            query.contains("داروخانه") -> NearbyCategory.PHARMACY
            query.contains("پمپ بنزین") || query.contains("سوخت") -> NearbyCategory.FUEL
            query.contains("بانک") -> NearbyCategory.BANK
            query.contains("خودپرداز") || query.contains("عابربانک") -> NearbyCategory.ATM
            query.contains("مترو") -> NearbyCategory.METRO
            query.contains("پارکینگ") -> NearbyCategory.PARKING
            query.contains("پارک") || query.contains("بوستان") -> NearbyCategory.PARK
            query.contains("مرکز خرید") || query.contains("پاساژ") -> NearbyCategory.MALL
            query.contains("هتل") -> NearbyCategory.HOTEL
            else -> null
        }
    }

    private fun queryPhotonIran(query: String, bias: GeoPointModel): List<PlaceResult> {
        val url = "https://photon.komoot.io/api/".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("lat", bias.lat.toString())
            .addQueryParameter("lon", bias.lon.toString())
            .addQueryParameter("limit", "15")
            .build()
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val root = JSONObject(body)
            val features = root.optJSONArray("features") ?: return emptyList()
            val list = mutableListOf<PlaceResult>()
            for (i in 0 until features.length()) {
                val feat = features.optJSONObject(i) ?: continue
                val geom = feat.optJSONObject("geometry") ?: continue
                val coords = geom.optJSONArray("coordinates") ?: continue
                if (coords.length() < 2) continue
                val lon = coords.optDouble(0)
                val lat = coords.optDouble(1)
                if (!GeoUtils.isInIran(lat, lon)) continue

                val props = feat.optJSONObject("properties") ?: JSONObject()
                val countryCode = props.optString("countrycode")
                if (countryCode.isNotBlank() && !countryCode.equals("IR", ignoreCase = true)) continue

                val name = props.optString("name").ifBlank {
                    props.optString("street").ifBlank {
                        props.optString("district").ifBlank { props.optString("city") }
                    }
                }
                if (name.isBlank()) continue
                val addressParts = listOfNotNull(
                    props.optString("street").takeIf { it.isNotBlank() },
                    props.optString("district").takeIf { it.isNotBlank() },
                    props.optString("city").takeIf { it.isNotBlank() },
                    props.optString("state").takeIf { it.isNotBlank() }
                ).distinct()
                val address = addressParts.joinToString("، ").ifBlank { null }
                val cat = translateOsmCategory(props.optString("osm_value").ifBlank { props.optString("osm_key") })
                list.add(
                    PlaceResult(
                        id = "photon_${props.optLong("osm_id", i.toLong())}_$i",
                        name = name,
                        address = address,
                        category = cat,
                        lat = lat,
                        lon = lon,
                        source = "Photon / OpenStreetMap ایران"
                    )
                )
            }
            return list
        }
    }

    private fun queryNominatimIran(query: String, bias: GeoPointModel): List<PlaceResult> {
        val d = 0.45
        val url = "https://nominatim.openstreetmap.org/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("countrycodes", "ir")
            .addQueryParameter("format", "jsonv2")
            .addQueryParameter("addressdetails", "1")
            .addQueryParameter("extratags", "1")
            .addQueryParameter("accept-language", "fa")
            .addQueryParameter("viewbox", "${bias.lon - d},${bias.lat + d},${bias.lon + d},${bias.lat - d}")
            .addQueryParameter("limit", "15")
            .build()
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val arr = JSONArray(body)
            val list = mutableListOf<PlaceResult>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                if (!GeoUtils.isInIran(lat, lon)) continue

                val displayName = obj.optString("display_name")
                val shortName = obj.optString("name").ifBlank {
                    displayName.substringBefore(",").trim()
                }
                if (shortName.isBlank()) continue
                val extratags = obj.optJSONObject("extratags")
                val phone = extratags?.optString("phone")?.takeIf { it.isNotBlank() }
                    ?: extratags?.optString("contact:phone")?.takeIf { it.isNotBlank() }
                val hours = extratags?.optString("opening_hours")?.takeIf { it.isNotBlank() }
                val cat = translateOsmCategory(obj.optString("type").ifBlank { obj.optString("category") })
                list.add(
                    PlaceResult(
                        id = "nom_${obj.optLong("place_id", i.toLong())}",
                        name = shortName,
                        address = displayName.takeIf { it.isNotBlank() },
                        category = cat,
                        lat = lat,
                        lon = lon,
                        phone = phone,
                        openingHours = hours,
                        source = "Nominatim / OpenStreetMap ایران"
                    )
                )
            }
            return list
        }
    }

    suspend fun reverseGeocode(point: GeoPointModel): PlaceResult? = withContext(Dispatchers.IO) {
        val url = "https://nominatim.openstreetmap.org/reverse".toHttpUrl().newBuilder()
            .addQueryParameter("lat", point.lat.toString())
            .addQueryParameter("lon", point.lon.toString())
            .addQueryParameter("format", "jsonv2")
            .addQueryParameter("addressdetails", "1")
            .addQueryParameter("accept-language", "fa")
            .build()
        val req = Request.Builder().url(url).get().build()
        runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val obj = JSONObject(body)
                if (obj.has("error")) return@use null
                val displayName = obj.optString("display_name")
                val addr = obj.optJSONObject("address")
                val name = obj.optString("name").ifBlank {
                    addr?.optString("road")?.takeIf { it.isNotBlank() }
                        ?: addr?.optString("neighbourhood")?.takeIf { it.isNotBlank() }
                        ?: addr?.optString("suburb")?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBefore(",").trim()
                }.ifBlank { "نقطه انتخاب‌شده روی نقشه" }
                PlaceResult(
                    id = "rev_${point.lat}_${point.lon}",
                    name = name,
                    address = displayName.takeIf { it.isNotBlank() },
                    category = translateOsmCategory(obj.optString("type")),
                    lat = point.lat,
                    lon = point.lon,
                    source = "Nominatim / OpenStreetMap ایران"
                )
            }
        }.getOrNull()
    }

    suspend fun fetchNearbyPlaces(
        center: GeoPointModel,
        category: NearbyCategory,
        radiusMeters: Int = 3500
    ): List<PlaceResult> = withContext(Dispatchers.IO) {
        val iranCenter = if (GeoUtils.isInIran(center.lat, center.lon)) center else GeoUtils.DEFAULT_TEHRAN_CENTER

        // First try strictly named Iranian places within radiusMeters
        val namedFirst = executeOverpassCategoryQuery(
            center = iranCenter,
            category = category,
            radiusMeters = radiusMeters,
            requireName = true
        )

        // If too few named places found, expand radius up to 8km inside the city
        val expanded = if (namedFirst.size < 6 && radiusMeters < 8000) {
            executeOverpassCategoryQuery(
                center = iranCenter,
                category = category,
                radiusMeters = 8000,
                requireName = true
            )
        } else {
            namedFirst
        }

        // Only if zero named places exist (e.g. ATM/parking sometimes lack names), include unnamed ones
        if (expanded.isNotEmpty()) {
            expanded
        } else {
            executeOverpassCategoryQuery(
                center = iranCenter,
                category = category,
                radiusMeters = 5000,
                requireName = false
            )
        }
    }

    private fun executeOverpassCategoryQuery(
        center: GeoPointModel,
        category: NearbyCategory,
        radiusMeters: Int,
        requireName: Boolean
    ): List<PlaceResult> {
        val nameCondition = if (requireName) "[\"name\"]" else ""
        val extraCraftFallback = if (category == NearbyCategory.WORKSHOP) {
            """
            node["industrial"]$nameCondition(around:$radiusMeters,${center.lat},${center.lon});
            way["industrial"]$nameCondition(around:$radiusMeters,${center.lat},${center.lon});
            """.trimIndent()
        } else ""

        val query = """
            [out:json][timeout:18];
            (
              node${category.overpassFilter}$nameCondition(around:$radiusMeters,${center.lat},${center.lon});
              way${category.overpassFilter}$nameCondition(around:$radiusMeters,${center.lat},${center.lon});
              $extraCraftFallback
            );
            out center tags 60;
        """.trimIndent()

        val body = FormBody.Builder().add("data", query).build()
        val req = Request.Builder()
            .url("https://overpass-api.de/api/interpreter")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("خطای دریافت اطلاعات مکان‌های ایران (کد ${resp.code})")
            }
            val raw = resp.body?.string() ?: return emptyList()
            val root = JSONObject(raw)
            val elements = root.optJSONArray("elements") ?: return emptyList()
            val results = mutableListOf<PlaceResult>()

            for (i in 0 until elements.length()) {
                val el = elements.optJSONObject(i) ?: continue
                val centerObj = el.optJSONObject("center")
                val lat = if (el.has("lat")) el.optDouble("lat") else centerObj?.optDouble("lat") ?: continue
                val lon = if (el.has("lon")) el.optDouble("lon") else centerObj?.optDouble("lon") ?: continue
                if (!GeoUtils.isInIran(lat, lon)) continue

                val tags = el.optJSONObject("tags") ?: JSONObject()
                val rawName = tags.optString("name:fa")
                    .ifBlank { tags.optString("name") }
                    .trim()

                if (requireName && (rawName.isBlank() || genericNames.contains(rawName.lowercase()))) {
                    continue
                }

                val displayName = formatNamedPlaceTitle(rawName, category, tags)
                val street = tags.optString("addr:street")
                val alley = tags.optString("addr:place")
                val house = tags.optString("addr:housenumber")
                val district = tags.optString("addr:district").ifBlank { tags.optString("addr:suburb") }
                val city = tags.optString("addr:city")
                val fullAddr = tags.optString("addr:full")
                val builtAddress = fullAddr.ifBlank {
                    listOfNotNull(
                        city.takeIf { it.isNotBlank() },
                        district.takeIf { it.isNotBlank() },
                        street.takeIf { it.isNotBlank() },
                        alley.takeIf { it.isNotBlank() },
                        house.takeIf { it.isNotBlank() }?.let { "پلاک $it" }
                    ).joinToString("، ")
                }.takeIf { it.isNotBlank() }

                val phone = tags.optString("phone")
                    .ifBlank { tags.optString("contact:phone") }
                    .ifBlank { tags.optString("contact:mobile") }
                    .takeIf { it.isNotBlank() }
                val hours = tags.optString("opening_hours").takeIf { it.isNotBlank() }
                val image = tags.optString("image").takeIf { it.startsWith("http") }
                val dist = GeoUtils.distanceMeters(center.lat, center.lon, lat, lon)

                val duplicate = results.any {
                    it.name == displayName && GeoUtils.distanceMeters(it.lat, it.lon, lat, lon) < 40.0
                }
                if (!duplicate) {
                    results.add(
                        PlaceResult(
                            id = "ovp_${el.optLong("id", i.toLong())}",
                            name = displayName,
                            address = builtAddress,
                            category = translateSubCraftOrCategory(category, tags),
                            lat = lat,
                            lon = lon,
                            distanceMeters = dist,
                            phone = phone,
                            openingHours = hours,
                            rating = null,
                            imageUrl = image,
                            source = "OpenStreetMap ایران"
                        )
                    )
                }
            }
            return results.sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
        }
    }

    private fun formatNamedPlaceTitle(rawName: String, category: NearbyCategory, tags: JSONObject): String {
        if (rawName.isBlank()) {
            val brand = tags.optString("brand:fa").ifBlank { tags.optString("brand") }
            return if (brand.isNotBlank()) "$brand (${category.labelFa})" else "${category.labelFa} (بدون نام ثبت‌شده)"
        }
        return when (category) {
            NearbyCategory.RESTAURANT -> {
                if (rawName.contains("رستوران") || rawName.contains("کباب") || rawName.contains("فست") ||
                    rawName.contains("پیتزا") || rawName.contains("سفره‌خانه") || rawName.contains("طباخی") ||
                    rawName.contains("بریانی") || rawName.contains("اغذیه")
                ) rawName else "رستوران $rawName"
            }
            NearbyCategory.CAFE -> {
                if (rawName.contains("کافه") || rawName.contains("کافی") || rawName.contains("قهوه")) rawName else "کافه $rawName"
            }
            NearbyCategory.WORKSHOP -> {
                if (rawName.contains("کارگاه") || rawName.contains("تولیدی") || rawName.contains("صنایع")) rawName else "کارگاه $rawName"
            }
            NearbyCategory.PHARMACY -> {
                if (rawName.contains("داروخانه")) rawName else "داروخانه $rawName"
            }
            NearbyCategory.HOSPITAL -> {
                if (rawName.contains("بیمارستان") || rawName.contains("درمانگاه") || rawName.contains("کلینیک")) rawName else "مرکز درمانی $rawName"
            }
            NearbyCategory.BAKERY -> {
                if (rawName.contains("نان") || rawName.contains("شیرینی")) rawName else "نانوایی $rawName"
            }
            else -> rawName
        }
    }

    private fun translateSubCraftOrCategory(category: NearbyCategory, tags: JSONObject): String {
        val craft = tags.optString("craft")
        if (category == NearbyCategory.WORKSHOP && craft.isNotBlank()) {
            val craftFa = when (craft) {
                "carpenter" -> "کارگاه نجاری"
                "metal_construction", "blacksmith", "welder" -> "کارگاه فلزکاری و جوشکاری"
                "tailor" -> "کارگاه خیاطی"
                "shoemaker" -> "کارگاه کفاشی"
                "bakery" -> "کارگاه نان و شیرینی"
                "electronics_repair" -> "کارگاه تعمیرات الکترونیک"
                "upholsterer" -> "کارگاه رویه‌کوبی مبل"
                "glaziery" -> "کارگاه شیشه‌بری"
                "plumber" -> "کارگاه تأسیسات"
                else -> "کارگاه ($craft)"
            }
            return craftFa
        }
        return category.labelFa
    }

    suspend fun fetchRoutes(
        origin: GeoPointModel,
        destination: GeoPointModel,
        travelMode: TravelMode,
        unit: DistanceUnit
    ): List<RouteOption> = withContext(Dispatchers.IO) {
        val coords = "${origin.lon},${origin.lat};${destination.lon},${destination.lat}"
        val primaryUrl = when (travelMode) {
            TravelMode.CAR, TravelMode.MOTORCYCLE ->
                "https://routing.openstreetmap.de/routed-car/route/v1/driving/$coords"
            TravelMode.BIKE ->
                "https://routing.openstreetmap.de/routed-bike/route/v1/driving/$coords"
            TravelMode.WALK ->
                "https://routing.openstreetmap.de/routed-foot/route/v1/driving/$coords"
        }
        val fallbackUrl = "https://router.project-osrm.org/route/v1/${travelMode.osrmProfile}/$coords"

        val responseJson = runCatching { executeOsrmRequest(primaryUrl) }.getOrNull()
            ?: executeOsrmRequest(fallbackUrl)

        val routesArr = responseJson.optJSONArray("routes")
            ?: throw IllegalStateException("مسیری بین این دو نقطه یافت نشد.")

        if (routesArr.length() == 0) {
            throw IllegalStateException("مسیری بین این دو نقطه یافت نشد.")
        }

        val parsed = mutableListOf<RouteOption>()
        for (rIdx in 0 until routesArr.length()) {
            val routeObj = routesArr.optJSONObject(rIdx) ?: continue
            val totalDist = routeObj.optDouble("distance", 0.0)
            val totalDur = routeObj.optDouble("duration", 0.0)
            val geomObj = routeObj.optJSONObject("geometry") ?: continue
            val coordArr = geomObj.optJSONArray("coordinates") ?: continue
            val points = ArrayList<GeoPointModel>(coordArr.length())
            for (cIdx in 0 until coordArr.length()) {
                val pt = coordArr.optJSONArray(cIdx) ?: continue
                if (pt.length() >= 2) {
                    points.add(GeoPointModel(lat = pt.optDouble(1), lon = pt.optDouble(0)))
                }
            }

            val legs = routeObj.optJSONArray("legs")
            val stepsList = mutableListOf<RouteStep>()
            val streetsSet = linkedSetOf<String>()
            if (legs != null && legs.length() > 0) {
                val stepsArr = legs.optJSONObject(0)?.optJSONArray("steps")
                if (stepsArr != null) {
                    for (sIdx in 0 until stepsArr.length()) {
                        val stepObj = stepsArr.optJSONObject(sIdx) ?: continue
                        val streetName = stepObj.optString("name", "").trim()
                        if (streetName.isNotBlank() && streetName != "-") {
                            streetsSet.add(streetName)
                        }
                        val stepDist = stepObj.optDouble("distance", 0.0)
                        val stepDur = stepObj.optDouble("duration", 0.0)
                        val maneuver = stepObj.optJSONObject("maneuver") ?: JSONObject()
                        val mType = maneuver.optString("type", "continue")
                        val mModifier = maneuver.optString("modifier").takeIf { it.isNotBlank() }
                        val exitNum = if (maneuver.has("exit")) maneuver.optInt("exit") else null
                        val locArr = maneuver.optJSONArray("location")
                        val stepPt = if (locArr != null && locArr.length() >= 2) {
                            GeoPointModel(locArr.optDouble(1), locArr.optDouble(0))
                        } else {
                            points.firstOrNull() ?: origin
                        }
                        val instruction = PersianManeuverTranslator.translateStep(
                            maneuverType = mType,
                            modifier = mModifier,
                            streetName = streetName,
                            distanceMeters = stepDist,
                            exitNumber = exitNum,
                            unit = unit
                        )
                        stepsList.add(
                            RouteStep(
                                instructionFa = instruction,
                                streetName = streetName,
                                distanceMeters = stepDist,
                                durationSeconds = stepDur,
                                maneuverType = mType,
                                maneuverModifier = mModifier,
                                exitNumber = exitNum,
                                location = stepPt
                            )
                        )
                    }
                }
            }

            parsed.add(
                RouteOption(
                    id = "route_$rIdx",
                    travelMode = travelMode,
                    geometryPoints = points,
                    distanceMeters = totalDist,
                    durationSeconds = totalDur,
                    steps = stepsList,
                    mainStreets = streetsSet.take(6),
                    isMotorcycleFallbackNotice = (travelMode == TravelMode.MOTORCYCLE)
                )
            )
        }

        if (parsed.isEmpty()) throw IllegalStateException("مسیری یافت نشد.")

        val minDuration = parsed.minOf { it.durationSeconds }
        val minDistance = parsed.minOf { it.distanceMeters }
        var markedFastest = false
        var markedShortest = false

        parsed.map { r ->
            val fastest = !markedFastest && r.durationSeconds <= minDuration + 0.1
            if (fastest) markedFastest = true
            val shortest = !markedShortest && r.distanceMeters <= minDistance + 0.1
            if (shortest) markedShortest = true
            r.copy(isFastest = fastest, isShortest = shortest)
        }
    }

    private fun executeOsrmRequest(baseUrl: String): JSONObject {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addQueryParameter("alternatives", "3")
            .addQueryParameter("steps", "true")
            .addQueryParameter("overview", "full")
            .addQueryParameter("geometries", "geojson")
            .build()
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("خطا در سرویس مسیریابی (کد ${resp.code})")
            }
            val raw = resp.body?.string() ?: throw IllegalStateException("پاسخ خالی از سرور مسیریابی")
            val json = JSONObject(raw)
            if (json.optString("code") != "Ok") {
                throw IllegalStateException("مسیر قابل محاسبه نیست (${json.optString("code")})")
            }
            return json
        }
    }

    private fun translateOsmCategory(raw: String): String? {
        return when (raw.lowercase()) {
            "restaurant", "fast_food" -> "رستوران"
            "cafe" -> "کافه"
            "craft", "industrial", "workshop" -> "کارگاه / مرکز صنعتی"
            "hospital", "clinic" -> "بیمارستان / درمانگاه"
            "pharmacy" -> "داروخانه"
            "fuel" -> "پمپ بنزین"
            "bank" -> "بانک"
            "atm" -> "خودپرداز"
            "bakery" -> "نانوایی"
            "school" -> "مدرسه"
            "university", "college" -> "دانشگاه"
            "hotel", "guest_house" -> "هتل"
            "mall", "department_store", "supermarket" -> "مرکز خرید / فروشگاه"
            "park" -> "پارک"
            "car_repair" -> "تعمیرگاه"
            "subway", "station" -> "ایستگاه مترو"
            "residential", "primary", "secondary", "tertiary", "motorway", "trunk" -> "خیابان / معبر"
            "suburb", "neighbourhood", "quarter" -> "محله"
            "city", "town", "village" -> "شهر / روستا"
            else -> raw.takeIf { it.isNotBlank() && it != "yes" }
        }
    }
}
