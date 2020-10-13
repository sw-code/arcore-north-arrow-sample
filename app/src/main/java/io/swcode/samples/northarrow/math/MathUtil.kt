package io.swcode.samples.northarrow.math

fun radianToDegree(angle: Double): Double {
    return angle * (180.0 / Math.PI)
}

fun degreeToRad(degree: Double): Double {
    return degree * Math.PI / 180
}