package io.swcode.samples.northarrow.arcore

import io.spotar.tour.filament.sample.orientation.Orientation
import io.swcode.samples.northarrow.location.Location

data class LocationNode(val destinationLocation: Location, val orientation: Orientation, val originLocation: Location) {

    fun angleToDestination(): Int {
//        var angle = orientation.azimuth + originLocation.angleTo(destinationLocation)
//        this.orientation = this.orientation % 360;
//
//        if (this.orientation < 0)
//        {
//            this.orientation += 360;
//        }

        return originLocation.angleTo(destinationLocation)
    }
}