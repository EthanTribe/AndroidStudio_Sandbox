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
import android.util.Log
import androidx.core.app.ActivityCompat

class DistanceTravelledService : Service() {
    private var locationListener: LocationListener? = null
    private var locationManager: LocationManager? = null
    private var distTravelBinder: DistanceTravelBinder = DistanceTravelBinder()
    private var distanceChanged = 0f
    private var lastLocation: Location? = null
    val intent = Intent(DISTANCE_UPDATED)
    private val pollPeriod : Long = 5 // time in seconds between location update requests

    override fun onCreate() {
        Log.v("DistanceTravelledService", "onCreate")
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.v("DistanceTravelledService", "location = $location")
                Log.v("DistanceTravelledService", "lastLocation = $lastLocation")
                if (lastLocation == null) {
                    lastLocation = location
                }
                distanceChanged = location.distanceTo(lastLocation!!)
                lastLocation = location

                intent.putExtra(DISTANCE_EXTRA, distanceChanged)
                if (distanceChanged == 0f)
                {
                    intent.putExtra(SPEED_EXTRA, 0.0)
                    Log.d("DistanceTravelledService", "Not moved")
                }
                else
                {
                    val speed = 1000f * pollPeriod.toFloat() / distanceChanged
                    intent.putExtra(SPEED_EXTRA, speed)
                    Log.d("DistanceTravelledService", "moved $distanceChanged in $pollPeriod s at a pace of $speed")
                }
                sendBroadcast(intent)
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            Log.v("DistanceTravelledService", "No permissions")
            return
        }

        locationManager!!.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            pollPeriod * 1000,
            1f,
            locationListener!!)
        Log.v("DistanceTravelledService", "Permissioned")
    }

    fun reset() {
        if (locationManager != null)
        {
            locationManager!!.removeUpdates(locationListener!!)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return distTravelBinder
    }

    inner class DistanceTravelBinder : Binder() {
        val binder: DistanceTravelledService
            get() = this@DistanceTravelledService
    }

    companion object {
        const val DISTANCE_UPDATED = "distanceUpdated"
        const val DISTANCE_EXTRA = "distanceExtra"
        const val SPEED_EXTRA = "speedExtra"
    }
}