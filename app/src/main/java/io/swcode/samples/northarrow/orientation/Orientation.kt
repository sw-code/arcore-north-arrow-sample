package io.spotar.tour.filament.sample.orientation

data class Orientation constructor(val azimuth: Int, val pitch: Int, val roll: Int) {
    companion object {
        fun ofZero() : Orientation {
            return Orientation(0, 0, 0)
        }
    }
}