package com.example

import com.example.data.model.GeoPoint
import com.example.util.GeometryUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  @Test
  fun `test indexToLetter alphabetical sequence`() {
    assertEquals("A", GeometryUtils.indexToLetter(0))
    assertEquals("B", GeometryUtils.indexToLetter(1))
    assertEquals("C", GeometryUtils.indexToLetter(2))
    assertEquals("D", GeometryUtils.indexToLetter(3))
    assertEquals("Z", GeometryUtils.indexToLetter(25))
    assertEquals("AA", GeometryUtils.indexToLetter(26))
  }

  @Test
  fun `test meters to cm conversion`() {
    assertEquals(500L, GeometryUtils.metersToCm(5.0))
    assertEquals(1025L, GeometryUtils.metersToCm(10.25))
    assertEquals(850L, GeometryUtils.metersToCm(8.5))
  }

  @Test
  fun `test haversine distance calculation`() {
    // 1 meter difference approximately
    val dist = GeometryUtils.calculateDistance(19.432608, -99.133209, 19.432617, -99.133209)
    assertTrue(dist > 0.5 && dist < 1.5)
  }

  @Test
  fun `test polygon area calculation`() {
    // Triangular polygon points
    val p1 = GeoPoint("A", 19.432608, -99.133209)
    val p2 = GeoPoint("B", 19.432708, -99.133209)
    val p3 = GeoPoint("C", 19.432708, -99.133109)
    val area = GeometryUtils.calculatePolygonArea(listOf(p1, p2, p3))
    assertTrue(area > 0.0)
  }

  @Test
  fun `test json serialization roundtrip`() {
    val points = listOf(
      GeoPoint("A", 19.432608, -99.133209, 0.0, 0L),
      GeoPoint("B", 19.432708, -99.133209, 11.1, 1110L),
      GeoPoint("C", 19.432708, -99.133109, 10.5, 1050L)
    )
    val json = GeometryUtils.pointsToJson(points)
    val parsed = GeometryUtils.jsonToPoints(json)
    assertEquals(3, parsed.size)
    assertEquals("A", parsed[0].letter)
    assertEquals("B", parsed[1].letter)
    assertEquals("C", parsed[2].letter)
  }
}
