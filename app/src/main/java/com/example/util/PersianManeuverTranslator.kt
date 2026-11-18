package com.example.util

import com.example.domain.model.DistanceUnit

object PersianManeuverTranslator {

    fun translateStep(
        maneuverType: String,
        modifier: String?,
        streetName: String,
        distanceMeters: Double,
        exitNumber: Int?,
        unit: DistanceUnit = DistanceUnit.KM
    ): String {
        val cleanStreet = streetName.trim()
        val onStreet = if (cleanStreet.isNotEmpty() && cleanStreet != "-") " به سمت $cleanStreet" else ""
        val distText = GeoUtils.formatDistance(distanceMeters, unit)

        return when (maneuverType) {
            "depart" -> {
                if (cleanStreet.isNotEmpty()) {
                    "حرکت را در مسیر $cleanStreet آغاز کنید"
                } else {
                    "حرکت را آغاز کنید"
                }
            }
            "arrive" -> {
                when (modifier) {
                    "right" -> "مقصد شما در سمت راست قرار دارد"
                    "left" -> "مقصد شما در سمت چپ قرار دارد"
                    else -> "به مقصد رسیدید"
                }
            }
            "roundabout", "rotary" -> {
                val exitText = exitNumber?.let { "خروجی ${ordinalFa(it)}" } ?: "خروجی مشخص‌شده"
                "در میدان وارد $exitText شوید$onStreet"
            }
            "turn", "end of road", "fork", "off ramp", "on ramp" -> {
                val turnAction = translateModifier(modifier)
                if (distanceMeters >= 80) {
                    "تا $distText دیگر $turnAction$onStreet"
                } else {
                    "در تقاطع بعدی $turnAction$onStreet"
                }
            }
            "merge" -> "به مسیر اصلی وارد شوید$onStreet"
            "new name", "continue" -> {
                when (modifier) {
                    "left", "sharp left", "slight left" -> "به چپ متمایل شوید$onStreet"
                    "right", "sharp right", "slight right" -> "به راست متمایل شوید$onStreet"
                    "uturn" -> "دور بزنید$onStreet"
                    else -> if (cleanStreet.isNotEmpty()) "مستقیم در مسیر $cleanStreet ادامه دهید" else "مستقیم ادامه دهید"
                }
            }
            else -> {
                val turnAction = translateModifier(modifier)
                if (turnAction.isNotEmpty()) "$turnAction$onStreet" else "مسیر را ادامه دهید$onStreet"
            }
        }
    }

    fun buildVoiceAnnouncement(
        maneuverType: String,
        modifier: String?,
        streetName: String,
        distanceMeters: Double,
        exitNumber: Int?,
        unit: DistanceUnit
    ): String {
        val dist = GeoUtils.formatDistance(distanceMeters, unit)
        return when (maneuverType) {
            "arrive" -> {
                when {
                    distanceMeters > 40 -> "به مقصد نزدیک شده‌اید"
                    modifier == "right" -> "مقصد شما در سمت راست قرار دارد"
                    modifier == "left" -> "مقصد شما در سمت چپ قرار دارد"
                    else -> "به مقصد خود رسیدید"
                }
            }
            "roundabout", "rotary" -> {
                val exitText = exitNumber?.let { "خروجی ${ordinalFa(it)}" } ?: "خروجی"
                "تا $dist دیگر در میدان وارد $exitText شوید"
            }
            else -> {
                val action = translateModifier(modifier)
                if (distanceMeters > 120) {
                    "تا $dist دیگر $action"
                } else {
                    "در تقاطع بعدی $action"
                }
            }
        }
    }

    private fun translateModifier(modifier: String?): String {
        return when (modifier) {
            "right" -> "به راست بپیچید"
            "sharp right" -> "به راستِ تند بپیچید"
            "slight right" -> "کمی به راست متمایل شوید"
            "left" -> "به چپ بپیچید"
            "sharp left" -> "به چپِ تند بپیچید"
            "slight left" -> "کمی به چپ متمایل شوید"
            "uturn" -> "دور برگردان بزنید"
            "straight" -> "مستقیم ادامه دهید"
            else -> "مسیر را ادامه دهید"
        }
    }

    private fun ordinalFa(num: Int): String {
        return when (num) {
            1 -> "اول"
            2 -> "دوم"
            3 -> "سوم"
            4 -> "چهارم"
            5 -> "پنجم"
            6 -> "ششم"
            else -> GeoUtils.toPersianDigits(num.toString())
        }
    }
}
