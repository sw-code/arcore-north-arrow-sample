package io.swcode.samples.northarrow.location

import io.swcode.samples.northarrow.math.degreeToRad
import io.swcode.samples.northarrow.math.radianToDegree
import java.io.Serializable
import kotlin.math.*

data class Location(
    val latitude: Double,
    val longitude: Double
) : Serializable {
    fun angleTo(location: Location): Int {
        val dX = location.latitude - latitude
        val dY = location.longitude - longitude

        val tanPhi = abs(dY / dX)
        val radAngle = atan(tanPhi)

        // int, do not care about decimals
        val degreeAngle = radianToDegree(radAngle).toInt()

        if (dX > 0 && dY > 0) { // I quarter
            return degreeAngle
        }
        if (dX < 0 && dY > 0) { // II
            return 180 - degreeAngle
        }
        if (dX < 0 && dY < 0) { // III
            return 180 + degreeAngle
        }
        return if (dX > 0 && dY < 0) { // IV
            360 - degreeAngle
        } else 0
    }

    fun distanceTo(other: Location): Int {
        val r = 6371000 // Radius of the earth (meter)
        val latDistance = degreeToRad(other.latitude - latitude)
        val lonDistance = degreeToRad(other.longitude - longitude)
        val a =
            sin(latDistance / 2) * sin(latDistance / 2) + cos(degreeToRad(latitude)) * cos(
                degreeToRad(other.latitude)
            ) * sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (r * c).toInt()
    }
}