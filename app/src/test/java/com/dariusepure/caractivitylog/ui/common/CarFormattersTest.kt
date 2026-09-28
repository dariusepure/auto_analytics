package com.dariusepure.caractivitylog.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CarFormattersTest {

    @Test
    fun testDistanceConversion() {
        // When usesMiles is false, distance is unchanged
        val km = 100.0
        assertEquals(100.0, CarFormatters.toCanonicalDistance(km, false), 0.001)
        assertEquals(100.0, CarFormatters.fromCanonicalDistance(km, false), 0.001)

        // When usesMiles is true, distance is multiplied by MILE_RATIO (1.609344)
        val miles = 10.0
        val expectedKm = 10.0 * 1.609344
        assertEquals(expectedKm, CarFormatters.toCanonicalDistance(miles, true), 0.001)
        assertEquals(miles, CarFormatters.fromCanonicalDistance(expectedKm, true), 0.001)
    }

    @Test
    fun testVolumeConversion() {
        val liters = 50.0
        assertEquals(50.0, CarFormatters.toCanonicalVolume(liters, false), 0.001)
        assertEquals(50.0, CarFormatters.fromCanonicalVolume(liters, false), 0.001)

        // UK Gallon conversion: 1 UK Gallon = 4.54609 Liters
        val ukGallons = 10.0
        val expectedLiters = 10.0 * 4.54609
        assertEquals(expectedLiters, CarFormatters.toCanonicalVolume(ukGallons, true), 0.001)
        assertEquals(ukGallons, CarFormatters.fromCanonicalVolume(expectedLiters, true), 0.001)
    }

    @Test
    fun testConsumptionCalculation() {
        // 7 liters for 100 km -> 7.0 L/100km
        val consumptionL100 = CarFormatters.calculateConsumption(7.0, 100.0, false)
        assertEquals(7.0, consumptionL100, 0.001)

        // Zero distance should return 0.0
        val zeroDist = CarFormatters.calculateConsumption(5.0, 0.0, false)
        assertEquals(0.0, zeroDist, 0.001)
    }
}
