package com.example.domain.model

data class GeoPointModel(
    val lat: Double,
    val lon: Double
)

enum class MapLayerType(val labelFa: String) {
    STANDARD("نقشه استاندارد"),
    SATELLITE("ماهواره‌ای"),
    TOPO("توپوگرافی"),
    DARK("حالت شب")
}

enum class TravelMode(val labelFa: String, val osrmProfile: String) {
    CAR("خودرو", "driving"),
    WALK("پیاده‌روی", "foot"),
    BIKE("دوچرخه", "bike"),
    MOTORCYCLE("موتورسیکلت", "driving")
}

enum class DistanceUnit(val labelFa: String) {
    KM("کیلومتر"),
    MILE("مایل")
}

enum class ThemePreference(val labelFa: String) {
    SYSTEM("هماهنگ با سیستم"),
    LIGHT("حالت روشن"),
    DARK("حالت تاریک")
}

enum class SavedCategory(val labelFa: String) {
    HOME("خانه"),
    WORK("محل کار"),
    SCHOOL("مدرسه"),
    FRIENDS("دوستان"),
    FAVORITES("مکان‌های موردعلاقه")
}

enum class ReportType(
    val labelFa: String,
    val ttlHours: Int
) {
    ACCIDENT("تصادف", 3),
    HEAVY_TRAFFIC("ترافیک سنگین", 2),
    ROAD_CLOSED("بسته بودن مسیر", 12),
    ROADWORKS("عملیات عمرانی", 24),
    BROKEN_VEHICLE("خرابی خودرو", 2),
    HAZARD("خطر در مسیر", 4),
    OBSTACLE("مانع در خیابان", 4)
}

enum class NearbyCategory(
    val id: String,
    val labelFa: String,
    val overpassFilter: String
) {
    RESTAURANT("restaurant", "رستوران‌ها", "[\"amenity\"~\"restaurant|fast_food\"]"),
    CAFE("cafe", "کافه‌ها", "[\"amenity\"=\"cafe\"]"),
    WORKSHOP("workshop", "کارگاه‌ها", "[\"craft\"]"),
    CAR_REPAIR("car_repair", "تعمیرگاه‌ها", "[\"shop\"=\"car_repair\"]"),
    SUPERMARKET("supermarket", "سوپرمارکت‌ها", "[\"shop\"~\"supermarket|convenience\"]"),
    BAKERY("bakery", "نانوایی‌ها", "[\"shop\"=\"bakery\"]"),
    HOSPITAL("hospital", "بیمارستان‌ها", "[\"amenity\"~\"hospital|clinic\"]"),
    PHARMACY("pharmacy", "داروخانه‌ها", "[\"amenity\"=\"pharmacy\"]"),
    FUEL("fuel", "جایگاه‌های سوخت", "[\"amenity\"=\"fuel\"]"),
    BANK("bank", "بانک‌ها", "[\"amenity\"=\"bank\"]"),
    ATM("atm", "خودپردازها", "[\"amenity\"=\"atm\"]"),
    METRO("metro", "ایستگاه مترو", "[\"station\"=\"subway\"]"),
    PARKING("parking", "پارکینگ‌ها", "[\"amenity\"=\"parking\"]"),
    PARK("park", "پارک‌ها", "[\"leisure\"=\"park\"]"),
    MALL("mall", "مراکز خرید", "[\"shop\"~\"mall|department_store\"]"),
    HOTEL("hotel", "هتل‌ها", "[\"tourism\"~\"hotel|guest_house\"]")
}

data class PlaceResult(
    val id: String,
    val name: String,
    val address: String?,
    val category: String?,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Double? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val rating: String? = null,
    val imageUrl: String? = null,
    val source: String = "OpenStreetMap"
)

data class RouteStep(
    val instructionFa: String,
    val streetName: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuverType: String,
    val maneuverModifier: String?,
    val exitNumber: Int? = null,
    val location: GeoPointModel
)

data class RouteOption(
    val id: String,
    val travelMode: TravelMode,
    val geometryPoints: List<GeoPointModel>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<RouteStep>,
    val mainStreets: List<String>,
    val isFastest: Boolean = false,
    val isShortest: Boolean = false,
    val isMotorcycleFallbackNotice: Boolean = false
)

data class SavedPlace(
    val id: Long = 0,
    val name: String,
    val address: String,
    val category: SavedCategory,
    val lat: Double,
    val lon: Double,
    val createdAt: Long = System.currentTimeMillis()
)

data class SearchHistoryItem(
    val id: Long = 0,
    val query: String,
    val displayName: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val searchedAt: Long = System.currentTimeMillis()
)

data class StreetReport(
    val id: Long = 0,
    val type: ReportType,
    val description: String,
    val lat: Double,
    val lon: Double,
    val addressLabel: String,
    val createdAt: Long,
    val expiresAt: Long,
    val isVerified: Boolean = false,
    val confirmations: Int = 1
) {
    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean = nowMillis >= expiresAt
}

data class OfflineCityPreset(
    val id: String,
    val nameFa: String,
    val provinceFa: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val minZoom: Int = 11,
    val maxZoom: Int = 14
)

data class OfflineRegion(
    val id: String,
    val nameFa: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val tileCount: Int,
    val sizeBytes: Long,
    val downloadedAt: Long
)

data class AppSettings(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val voiceEnabled: Boolean = true,
    val voiceVolume: Float = 0.9f,
    val selectedVoiceName: String = "",
    val defaultTravelMode: TravelMode = TravelMode.CAR,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val locationSharingEnabled: Boolean = true,
    val offlineModeEnabled: Boolean = false,
    val showSavedOnMap: Boolean = true,
    val showReportsOnMap: Boolean = true,
    val profileName: String = "کاربر مهمان",
    val profileAvatarUri: String? = null
)

data class LocationState(
    val hasPermission: Boolean = false,
    val isGpsAvailable: Boolean = false,
    val currentPoint: GeoPointModel? = null,
    val speedMetersPerSec: Float = 0f,
    val headingDegrees: Float = 0f,
    val accuracyMeters: Float? = null
)
