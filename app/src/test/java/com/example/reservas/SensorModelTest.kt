package com.example.reservas

import com.example.reservas.data.model.SensorHistoryReading
import com.example.reservas.data.model.SensorLatestResponse
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SensorModelTest {
    private val gson = GsonBuilder().create()

    @Test
    fun latestSensorParsesNestedAndFlatVariables() {
        val sensor = gson.fromJson(
            """{"name":"greenhouse-1","temperature":"21.5","variables":{"co2":450}}""",
            SensorLatestResponse::class.java
        )

        assertEquals(21.5, sensor.getVariable("temp")!!, 0.001)
        assertEquals(450.0, sensor.getVariable("CO2")!!, 0.001)
        assertEquals("greenhouse-1", sensor.sensor_name)
    }

    @Test
    fun historyReadingParsesAliasesAndValues() {
        val reading = gson.fromJson(
            """{"ts":"2026-01-01T00:00:00Z","humidity":55,"values":{"ph":7.1}}""",
            SensorHistoryReading::class.java
        )

        assertNotNull(reading)
        assertEquals(55.0, reading.valueFor("humi")!!, 0.001)
        assertEquals(7.1, reading.valueFor("PH")!!, 0.001)
    }
}
