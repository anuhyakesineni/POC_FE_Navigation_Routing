package com.example.navigation_routing_fe_poc

import android.util.Log
import com.here.sdk.consent.Consent
import com.here.sdk.consent.ConsentEngine
import com.here.sdk.core.Location
import com.here.sdk.core.LocationListener
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.location.*

class LocationProvider {
    private val LOG_TAG: String = "Location Provider"

    private var locationEngine: LocationEngine? = null
    private var updateListener: LocationListener? = null

    private val locationStatusListener: LocationStatusListener = object : LocationStatusListener {
        override fun onStatusChanged(locationEngineStatus: LocationEngineStatus) {
            Log.d(LOG_TAG, "Location engine status: " + locationEngineStatus.name)
        }

        override fun onFeaturesNotAvailable(features: List<LocationFeature>) {
            for (feature in features) {
                Log.d(LOG_TAG, "Location feature not available: " + feature.name)
            }
        }
    }

    fun HEREPositioningProvider() {
        val consentEngine: ConsentEngine
        try {
            consentEngine = ConsentEngine()
            locationEngine = LocationEngine()
        } catch (e: InstantiationErrorException) {
            throw RuntimeException("Initialization failed: " + e.message)
        }

        // Ask user to optionally opt in to HERE's data collection / improvement program.
        if (consentEngine.userConsentState == Consent.UserReply.NOT_HANDLED) {
            consentEngine.requestUserConsent()
        }
    }

    fun getLastKnownLocation(): Location? {
        return locationEngine!!.lastKnownLocation
    }

    // Does nothing when engine is already running.
    fun startLocating(updateListener: LocationListener?, accuracy: LocationAccuracy?) {
        if (locationEngine!!.isStarted) {
            return
        }
        this.updateListener = updateListener

        // Set listeners to get location updates.
        locationEngine!!.addLocationListener(updateListener!!)
        locationEngine!!.addLocationStatusListener(locationStatusListener)
        locationEngine!!.start(accuracy!!)
    }

    // Does nothing when engine is already stopped.
    fun stopLocating() {
        if (!locationEngine!!.isStarted) {
            return
        }

        // Remove listeners and stop location engine.
        locationEngine!!.removeLocationListener(updateListener!!)
        locationEngine!!.removeLocationStatusListener(locationStatusListener)
        locationEngine!!.stop()
    }
}