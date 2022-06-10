package com.example.navigation_routing_fe_poc

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.here.sdk.core.Color
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.Point2D
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.mapview.*
import com.here.sdk.routing.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class RoutingProvider {
    private val TAG: String = "Routing Provider"
    private var context: Context? = null
    private var mapView: MapView? = null
    lateinit var locationProvider:LocationProvider
    private val mapMarkerList: MutableList<MapMarker> = ArrayList()
    private val mapPolylines: MutableList<MapPolyline> = ArrayList()
    private var routingEngine: RoutingEngine? = null
    private var startGeoCoordinates: GeoCoordinates? = null
    private var destinationGeoCoordinates: GeoCoordinates? = null

    fun setContext_MapView(contextRP: Context?,mapViewRP: MapView){
        context = contextRP
        mapView= mapViewRP
        locationProvider= LocationProvider()
        val camera = mapView!!.camera
        val distanceInMeters = (1000 * 10).toDouble()
        camera.lookAt(GeoCoordinates(52.520798, 13.409408), distanceInMeters)

        routingEngine = try {
            RoutingEngine()
        } catch (e: InstantiationErrorException) {
            throw java.lang.RuntimeException("Initialization of RoutingEngine failed: " + e.error.name)
        }
    }


    fun addRoute() {
//        startGeoCoordinates = locationProvider.getLastKnownLocation()?.coordinates
        startGeoCoordinates = createRandomGeoCoordinatesAroundMapCenter()
        destinationGeoCoordinates = createRandomGeoCoordinatesAroundMapCenter()
        val startWaypoint = Waypoint(startGeoCoordinates!!)
        val destinationWaypoint = Waypoint(destinationGeoCoordinates!!)
        val waypoints: List<Waypoint> = ArrayList(Arrays.asList(startWaypoint, destinationWaypoint))
        routingEngine!!.calculateRoute(
            waypoints,
            CarOptions()
        ) { routingError, routes ->
            if (routingError == null) {
                val route = routes!![0]
                showRouteDetails(route)
                showRouteOnMap(route)
                logRouteSectionDetails(route)
                logRouteViolations(route)
            } else {
                showDialog("Error while calculating a route:", routingError.toString())
            }
        }
    }

    // A route may contain several warnings, for example, when a certain route option could not be fulfilled.
    // An implementation may decide to reject a route if one or more violations are detected.
    private fun logRouteViolations(route: Route) {
        for (section in route.sections) {
            for (notice in section.sectionNotices) {
                Log.e(TAG, "This route contains the following warning: " + notice.code.toString())
            }
        }
    }

    private fun logRouteSectionDetails(route: Route) {
        val dateFormat: DateFormat = SimpleDateFormat("HH:mm")
        for (i in route.sections.indices) {
            val section = route.sections[i]
            Log.d(TAG, "Route Section : " + (i + 1))
            Log.d(TAG, "Route Section Departure Time : " + dateFormat.format(section.departureTime))
            Log.d(TAG, "Route Section Arrival Time : " + dateFormat.format(section.arrivalTime))
            Log.d(TAG, "Route Section length : " + section.lengthInMeters + " m")
            Log.d(TAG, "Route Section duration : " + section.duration.seconds + " s")
        }
    }

    private fun showRouteDetails(route: Route) {
        val estimatedTravelTimeInSeconds = route.duration.seconds
        val lengthInMeters = route.lengthInMeters
        val routeDetails = ("Travel Time: " + formatTime(estimatedTravelTimeInSeconds)
                + ", Length: " + formatLength(lengthInMeters))
        showDialog("Route Details", routeDetails)
    }

    private fun formatTime(sec: Long): String {
        val hours = (sec / 3600).toInt()
        val minutes = (sec % 3600 / 60).toInt()
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }

    private fun formatLength(meters: Int): String {
        val kilometers = meters / 1000
        val remainingMeters = meters % 1000
        return String.format(Locale.getDefault(), "%02d.%02d km", kilometers, remainingMeters)
    }

    private fun showRouteOnMap(route: Route) {
        // Optionally, clear any previous route.
        clearMap()

        // Show route as polyline.
        val routeGeoPolyline = route.geometry
        val widthInPixels = 20f
        val routeMapPolyline = MapPolyline(
            routeGeoPolyline,
            widthInPixels.toDouble(),
            Color.valueOf(0f, 0.56f, 0.54f, 0.63f)
        ) // RGBA
        mapView!!.mapScene.addMapPolyline(routeMapPolyline)
        mapPolylines.add(routeMapPolyline)
        val startPoint = route.sections[0].departurePlace.mapMatchedCoordinates
        val destination = route.sections[route.sections.size - 1].arrivalPlace.mapMatchedCoordinates

        // Draw a circle to indicate starting point and destination.
        addCircleMapMarker(startPoint, R.drawable.green_dot)
        addCircleMapMarker(destination, R.drawable.green_dot)

        // Log maneuver instructions per route section.
        val sections = route.sections
        for (section in sections) {
            logManeuverInstructions(section)
        }
    }

    private fun logManeuverInstructions(section: Section) {
        Log.d(TAG, "Log maneuver instructions per route section:")
        val maneuverInstructions = section.maneuvers
        for (maneuverInstruction in maneuverInstructions) {
            val maneuverAction = maneuverInstruction.action
            val maneuverLocation = maneuverInstruction.coordinates
            val maneuverInfo = (maneuverInstruction.text
                    + ", Action: " + maneuverAction.name
                    + ", Location: " + maneuverLocation.toString())
            Log.d(TAG, maneuverInfo)
        }
    }

    fun clearMap() {
        clearWaypointMapMarker()
        clearRoute()
    }

    private fun clearWaypointMapMarker() {
        for (mapMarker in mapMarkerList) {
            mapView!!.mapScene.removeMapMarker(mapMarker)
        }
        mapMarkerList.clear()
    }

    private fun clearRoute() {
        for (mapPolyline in mapPolylines) {
            mapView!!.mapScene.removeMapPolyline(mapPolyline)
        }
        mapPolylines.clear()
    }

    private fun createRandomGeoCoordinatesAroundMapCenter(): GeoCoordinates? {
        val centerGeoCoordinates = mapView!!.viewToGeoCoordinates(
            Point2D(
                (mapView!!.width / 2).toDouble(),
                (mapView!!.height / 2).toDouble()
            )
        )
            ?: // Should never happen for center coordinates.
            throw RuntimeException("CenterGeoCoordinates are null")
        val lat = centerGeoCoordinates.latitude
        val lon = centerGeoCoordinates.longitude
        return GeoCoordinates(
            getRandom(lat - 0.02, lat + 0.02),
            getRandom(lon - 0.02, lon + 0.02)
        )
    }

    private fun getRandom(min: Double, max: Double): Double {
        return min + Math.random() * (max - min)
    }

    private fun addCircleMapMarker(geoCoordinates: GeoCoordinates, resourceId: Int) {
        val mapImage = MapImageFactory.fromResource(context!!.resources, resourceId)
        val mapMarker = MapMarker(geoCoordinates, mapImage)
        mapView!!.mapScene.addMapMarker(mapMarker)
        mapMarkerList.add(mapMarker)
    }

    private fun showDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(
            context!!
        )
        builder.setTitle(title)
        builder.setMessage(message)
        builder.show()
    }
}