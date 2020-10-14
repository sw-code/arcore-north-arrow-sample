package io.swcode.samples.northarrow.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Criteria
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import io.swcode.samples.northarrow.eventbus.SimpleEventBus

class LiveLocationService constructor(context: Context) : LocationListener {

    private val locationManager : LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    init {
        setup()
    }

    @SuppressLint("MissingPermission")
    private fun setup() {
        val criteria = Criteria()
        val provider = locationManager.getBestProvider(criteria, false)
        locationManager.requestLocationUpdates(provider!!, 1000, 5.0f, this)
    }

    override fun onLocationChanged(location: android.location.Location) {
        SimpleEventBus.publish(Location(location.latitude, location.longitude))
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        // NOP
    }

    override fun onProviderEnabled(provider: String) {
        // NOP
    }

    override fun onProviderDisabled(provider: String) {
        // NOP
    }
}