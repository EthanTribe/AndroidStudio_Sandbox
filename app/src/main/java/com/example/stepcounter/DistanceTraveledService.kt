package com.example.stepcounter

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.ActivityCompat

class DistanceTraveledService : Service() {
    private var distTravelBinder: DistanceTravelBinder = DistanceTravelBinder()
    private var distanceTraveledInMetres = 0.0
    private var lastLocation: Location? = null

    override fun onCreate() {
        val locationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (lastLocation == null) {
                    lastLocation = location
                }
                distanceTraveledInMetres += location.distanceTo(lastLocation!!).toDouble()
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            return
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            1f,
            locationListener)
    }

    override fun onBind(intent: Intent): IBinder? {
        return distTravelBinder
    }

    inner class DistanceTravelBinder : Binder() {
        val binder: DistanceTraveledService
            get() = this@DistanceTraveledService
    }

    fun getDistanceTraveled(): Double {
        return distanceTraveledInMetres
    }
}