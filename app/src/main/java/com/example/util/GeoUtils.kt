package com.example.util

import com.example.domain.model.DistanceUnit
import com.example.domain.model.GeoPointModel
import com.example.domain.model.OfflineCityPreset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    val DEFAULT_TEHRAN_CENTER = GeoPointModel(35.6892, 51.3890)

    fun isInIran(lat: Double, lon: Double): Boolean {
        return lat in 24.5..40.0 && lon in 44.0..63.6
    }

    val IRAN_OFFLINE_CITIES = listOf(
        OfflineCityPreset("tehran", "تهران", "تهران", 35.58, 35.82, 51.18, 51.58),
        OfflineCityPreset("mashhad", "مشهد", "خراسان رضوی", 36.20, 36.40, 59.45, 59.72),
        OfflineCityPreset("isfahan", "اصفهان", "اصفهان", 32.57, 32.74, 51.57, 51.78),
        OfflineCityPreset("shiraz", "شیراز", "فارس", 29.54, 29.70, 52.45, 52.63),
        OfflineCityPreset("tabriz", "تبریز", "آذربایجان شرقی", 38.01, 38.14, 46.20, 46.39),
        OfflineCityPreset("karaj", "کرج", "البرز", 35.76, 35.88, 50.88, 51.06),
        OfflineCityPreset("qom", "قم", "قم", 34.59, 34.69, 50.82, 50.94),
        OfflineCityPreset("ahvaz", "اهواز", "خوزستان", 31.26, 31.38, 48.60, 48.75),
        OfflineCityPreset("rasht", "رشت", "گیلان", 37.23, 37.33, 49.53, 49.64),
        OfflineCityPreset("yazd", "یزد", "یزد", 31.84, 31.94, 54.30, 54.42),
        OfflineCityPreset("kerman", "کرمان", "کرمان", 30.24, 30.33, 57.01, 57.14),
        OfflineCityPreset("sari", "ساری", "مازندران", 36.52, 36.60, 53.01, 53.11)
    )

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun remainingRouteDistanceMeters(point: GeoPointModel, polyline: List<GeoPointModel>): Double {
        if (polyline.size < 2) return 0.0
        var closestIdx = 0
        var closestDist = Double.MAX_VALUE
        for (i in polyline.indices) {
            val d = distanceMeters(point.lat, point.lon, polyline[i].lat, polyline[i].lon)
            if (d < closestDist) {
                closestDist = d
                closestIdx = i
            }
        }
        var total = closestDist
        for (i in closestIdx until polyline.size - 1) {
            total += distanceMeters(
                polyline[i].lat, polyline[i].lon,
                polyline[i + 1].lat, polyline[i + 1].lon
            )
        }
        return total
    }

    fun formatArrivalTimeTehran(durationSeconds: Double, nowMillis: Long = System.currentTimeMillis()): String {
        val arrivalMillis = nowMillis + (durationSeconds * 1000.0).toLong()
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tehran"))
        cal.timeInMillis = arrivalMillis
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val min = cal.get(java.util.Calendar.MINUTE)
        return toPersianDigits(String.format("%02d:%02d", hour, min))
    }

    fun distanceToPolylineMeters(point: GeoPointModel, polyline: List<GeoPointModel>): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        if (polyline.size == 1) return distanceMeters(point.lat, point.lon, polyline[0].lat, polyline[0].lon)
        var minDist = Double.MAX_VALUE
        for (i in 0 until polyline.size - 1) {
            val d = pointToSegmentDistanceMeters(point, polyline[i], polyline[i + 1])
            if (d < minDist) minDist = d
        }
        return minDist
    }

    private fun pointToSegmentDistanceMeters(
        p: GeoPointModel,
        a: GeoPointModel,
        b: GeoPointModel
    ): Double {
        val latScale = 111320.0
        val lonScale = 111320.0 * cos(Math.toRadians(p.lat))
        val px = (p.lon - a.lon) * lonScale
        val py = (p.lat - a.lat) * latScale
        val bx = (b.lon - a.lon) * lonScale
        val by = (b.lat - a.lat) * latScale
        val lenSq = bx * bx + by * by
        if (lenSq == 0.0) return sqrt(px * px + py * py)
        val t = ((px * bx + py * by) / lenSq).coerceIn(0.0, 1.0)
        val projX = t * bx
        val projY = t * by
        val dx = px - projX
        val dy = py - projY
        return sqrt(dx * dx + dy * dy)
    }

    fun normalizePersianQuery(input: String): String {
        return input
            .trim()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('\u200C', ' ')
            .replace(Regex("\\s+"), " ")
    }

    fun levenshteinDistance(a: String, b: String): Int {
        val s1 = normalizePersianQuery(a).lowercase()
        val s2 = normalizePersianQuery(b).lowercase()
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length
        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val temp = dp[j]
                dp[j] = if (s1[i - 1] == s2[j - 1]) {
                    prev
                } else {
                    minOf(prev, dp[j], dp[j - 1]) + 1
                }
                prev = temp
            }
        }
        return dp[s2.length]
    }

    fun toPersianDigits(text: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatDistance(meters: Double, unit: DistanceUnit = DistanceUnit.KM): String {
        val raw = when (unit) {
            DistanceUnit.KM -> {
                if (meters < 1000.0) {
                    "${(meters / 10.0).roundToInt() * 10} متر"
                } else {
                    val km = meters / 1000.0
                    if (km < 10.0) String.format("%.1f کیلومتر", km)
                    else "${km.roundToInt()} کیلومتر"
                }
            }
            DistanceUnit.MILE -> {
                val miles = meters / 1609.344
                if (miles < 0.2) {
                    val feet = (meters * 3.28084).roundToInt()
                    "$feet فوت"
                } else {
                    String.format("%.1f مایل", miles)
                }
            }
        }
        return toPersianDigits(raw.replace('.', '٫'))
    }

    fun formatDuration(seconds: Double): String {
        val totalMinutes = (seconds / 60.0).roundToInt().coerceAtLeast(1)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val raw = when {
            hours == 0 -> "$totalMinutes دقیقه"
            mins == 0 -> "$hours ساعت"
            else -> "$hours ساعت و $mins دقیقه"
        }
        return toPersianDigits(raw)
    }

    fun formatSpeedKmh(speedMps: Float, unit: DistanceUnit): String {
        val raw = if (unit == DistanceUnit.KM) {
            "${(speedMps * 3.6f).roundToInt()} کیلومتر/ساعت"
        } else {
            "${(speedMps * 2.23694f).roundToInt()} مایل/ساعت"
        }
        return toPersianDigits(raw)
    }

    fun formatFileSize(bytes: Long): String {
        val raw = when {
            bytes < 1024 -> "$bytes بایت"
            bytes < 1024 * 1024 -> "${(bytes / 1024.0).roundToInt()} کیلوبایت"
            else -> String.format("%.1f مگابایت", bytes / (1024.0 * 1024.0))
        }
        return toPersianDigits(raw.replace('.', '٫'))
    }

    fun formatRemainingTime(expiresAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = expiresAtMillis - nowMillis
        if (diff <= 0) return "منقضی شده"
        val mins = (diff / 60000L).toInt()
        val raw = if (mins < 60) {
            "$mins دقیقه دیگر منقضی می‌شود"
        } else {
            val hours = mins / 60
            "$hours ساعت دیگر منقضی می‌شود"
        }
        return toPersianDigits(raw)
    }

    fun formatTimeAgo(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffMins = ((nowMillis - timestampMillis) / 60000L).coerceAtLeast(0L)
        val raw = when {
            diffMins < 1 -> "هم‌اکنون"
            diffMins < 60 -> "$diffMins دقیقه پیش"
            diffMins < 1440 -> "${diffMins / 60} ساعت پیش"
            else -> "${diffMins / 1440} روز پیش"
        }
        return toPersianDigits(raw)
    }

    fun estimateTileCount(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        minZoom: Int,
        maxZoom: Int
    ): Int {
        var total = 0
        for (z in minZoom..maxZoom) {
            val xMin = lonToTileX(minLon, z)
            val xMax = lonToTileX(maxLon, z)
            val yMin = latToTileY(maxLat, z)
            val yMax = latToTileY(minLat, z)
            total += ((xMax - xMin + 1).coerceAtLeast(1)) * ((yMax - yMin + 1).coerceAtLeast(1))
        }
        return total
    }

    private fun lonToTileX(lon: Double, zoom: Int): Int {
        return ((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
    }

    private fun latToTileY(lat: Double, zoom: Int): Int {
        val rad = lat * PI / 180.0
        val n = (1 - kotlin.math.ln(kotlin.math.tan(rad) + 1 / cos(rad)) / PI) / 2
        return (n * (1 shl zoom)).toInt()
    }
}
