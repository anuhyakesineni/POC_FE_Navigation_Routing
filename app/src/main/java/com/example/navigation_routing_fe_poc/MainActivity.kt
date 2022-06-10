package com.example.navigation_routing_fe_poc

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    lateinit var permissionsRequestor:PermissionsRequester
    lateinit var mapView: MapView
    lateinit var routingProvider: RoutingProvider
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get a MapView instance from layout.
        mapView = findViewById<MapView>(R.id.map_view)
        mapView.onCreate(savedInstanceState)

        handleAndroidPermissions()

    }

    private fun handleAndroidPermissions() {
        permissionsRequestor = PermissionsRequester(this)
        permissionsRequestor.request(object :PermissionsRequester.ResultListener{
            override fun permissionsGranted() {
                loadMapScene()
            }

            override fun permissionsDenied() {
                Log.e(TAG, "Permissions denied by user.")
            }

        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsRequestor.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun loadMapScene() {
        mapView.mapScene.loadScene(
            MapScheme.NORMAL_DAY
        ) { mapError ->
            if (mapError == null) {
                routingProvider= RoutingProvider()
                routingProvider.setContext_MapView(this,mapView)
            } else {
                Log.d(
                    TAG,
                    "Loading map failed: mapErrorCode: " + mapError.name
                )
            }
        }
    }
    fun addRouteButtonClicked(view: View?) {
        routingProvider.addRoute()
    }


    fun clearMapButtonClicked(view: View?) {
        routingProvider.clearMap()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

}