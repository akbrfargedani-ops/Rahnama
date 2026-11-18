package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.RahnamaDatabase
import com.example.data.local.SavedPlaceEntity
import com.example.data.local.StreetReportEntity
import com.example.domain.model.DistanceUnit
import com.example.domain.model.GeoPointModel
import com.example.domain.model.ReportType
import com.example.domain.model.SavedCategory
import com.example.util.GeoUtils
import com.example.util.PersianManeuverTranslator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RahnamaCoreTest {

    private lateinit var db: RahnamaDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RahnamaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `persian maneuver translator formats turn right and roundabout`() {
        val turnRight = PersianManeuverTranslator.translateStep(
            maneuverType = "turn",
            modifier = "right",
            streetName = "خیابان ولیعصر",
            distanceMeters = 500.0,
            exitNumber = null,
            unit = DistanceUnit.KM
        )
        assertTrue(turnRight.contains("۵۰۰ متر"))
        assertTrue(turnRight.contains("به راست بپیچید"))
        assertTrue(turnRight.contains("خیابان ولیعصر"))

        val roundabout = PersianManeuverTranslator.translateStep(
            maneuverType = "roundabout",
            modifier = null,
            streetName = "بزرگراه چمران",
            distanceMeters = 120.0,
            exitNumber = 2,
            unit = DistanceUnit.KM
        )
        assertTrue(roundabout.contains("خروجی دوم"))
    }

    @Test
    fun `geoutils calculates distance and polyline deviation accurately`() {
        val azadi = GeoPointModel(35.6997, 51.3380)
        val valiasr = GeoPointModel(35.7002, 51.4050)
        val dist = GeoUtils.distanceMeters(azadi.lat, azadi.lon, valiasr.lat, valiasr.lon)
        assertTrue(dist in 5500.0..6600.0)

        val offRoutePoint = GeoPointModel(35.7100, 51.3700)
        val deviation = GeoUtils.distanceToPolylineMeters(offRoutePoint, listOf(azadi, valiasr))
        assertTrue(deviation > 900.0)

        assertTrue(GeoUtils.isInIran(35.6892, 51.3890))
        assertTrue(!GeoUtils.isInIran(37.4220, -122.0840))

        val remaining = GeoUtils.remainingRouteDistanceMeters(azadi, listOf(azadi, valiasr))
        assertTrue(remaining > 5500.0)
        val arrival = GeoUtils.formatArrivalTimeTehran(900.0)
        assertTrue(arrival.contains(":"))
    }

    @Test
    fun `room saved places and report expiration work correctly`() = runBlocking {
        val dao = db.rahnamaDao()
        dao.insertSavedPlace(
            SavedPlaceEntity(
                name = "خانه",
                address = "تهران، خیابان انقلاب",
                categoryName = SavedCategory.HOME.name,
                lat = 35.701,
                lon = 51.391,
                createdAt = System.currentTimeMillis()
            )
        )
        val saved = dao.observeSavedPlaces().first()
        assertEquals(1, saved.size)
        assertEquals("خانه", saved[0].name)

        val now = System.currentTimeMillis()
        dao.insertStreetReport(
            StreetReportEntity(
                typeName = ReportType.ACCIDENT.name,
                description = "تصادف جزئی",
                lat = 35.70,
                lon = 51.39,
                addressLabel = "میدان انقلاب",
                createdAt = now - 10_000_000L,
                expiresAt = now - 1000L,
                isVerified = false,
                confirmations = 1
            )
        )
        dao.deleteExpiredReports(now)
        val remaining = dao.observeStreetReports().first()
        assertTrue(remaining.isEmpty())
    }
}
