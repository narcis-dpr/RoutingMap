package com.example.maprouting.view

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.carto.graphics.Color
import com.carto.styles.AnimationStyle
import com.carto.styles.AnimationStyleBuilder
import com.carto.styles.AnimationType
import com.carto.styles.LineStyle
import com.carto.styles.LineStyleBuilder
import com.carto.styles.MarkerStyleBuilder
import com.carto.utils.BitmapUtils
import com.example.maprouting.R
import com.example.maprouting.viewModel.RouteViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.neshan.common.model.LatLng
import org.neshan.common.utils.PolylineEncoding
import org.neshan.mapsdk.MapView
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline

@AndroidEntryPoint
class TiltCameraActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val viewModel: RouteViewModel by viewModels()
    private var routeOverviewPolylinePoints: ArrayList<LatLng>? = null
    private var decodedStepByStepPath: ArrayList<LatLng>? = null
    private var onMapPolyline: Polyline? = null
    private val markers: ArrayList<Marker> = ArrayList()
    private var overview = false
    private var animSt: AnimationStyle? = null
    private var marker: Marker ? = null
    private var userMarker: Marker? = null
    private var bearing: Double ? = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tilt_camera)
        val firstLat: Double = intent.getDoubleExtra("firstLat", 0.0)
        val firstLng: Double = intent.getDoubleExtra("firstLng", 0.0)
        val secondLat: Double = intent.getDoubleExtra("secondLat", 0.0)
        val secondLng: Double = intent.getDoubleExtra("secondLng", 0.0)
        markers.add(addMarker(LatLng(firstLat, firstLng)))
        markers.add(addMarker(LatLng(secondLat, secondLng)))
    }

    override fun onStart() {
        super.onStart()
        initLayoutReferences()
    }

    private fun initLayoutReferences() {
        initViews()
        initMap()

    }

    private fun initViews() {
        map = findViewById(R.id.mapview)
    }

    private fun initMap() {
        map.setZoom(20f, 0f)
        map.setTilt(10f, 0f)
        map.settings.isMapRotationEnabled = true
        layRoute()
    }
    private fun layRoute() {
        viewModel.getDirections(
            markers[0].latLng,
            markers[1].latLng,
        )
        viewModel.route.observe(
            this,
            Observer { route ->
                if (route == null) {
                    Toast.makeText(this@TiltCameraActivity, "مسیری یافت نشد", Toast.LENGTH_LONG)
                        .show()
                } else {
                    routeOverviewPolylinePoints = ArrayList(
                        PolylineEncoding.decode(
                            route.routes[0].overviewPolyline.encodedPolyline,
                        ),
                    )
                    decodedStepByStepPath = ArrayList()

                    for (step in route.routes[0].legs[0].directionSteps) {
                        decodedStepByStepPath!!.addAll(PolylineEncoding.decode(step.encodedPolyline))
                    }
                    onMapPolyline = Polyline(routeOverviewPolylinePoints, getLineStyle())
                    map.addPolyline(onMapPolyline)
                    bearing = bearing(
                        route.routes[0].legs[0].directionSteps.first().startLocation.longitude,
                        route.routes[0].legs[0].directionSteps.first().startLocation.latitude,
                        route.routes[0].legs[0].directionSteps.last().startLocation.longitude,
                        route.routes[0].legs[0].directionSteps.last().startLocation.latitude,
                    )
                    mapSetPosition(route.routes[0].legs[0].directionSteps[0].startLocation, bearing!!)
                    findRoute()
                }
            },
        )
    }
    private fun mapSetPosition(startLoc: LatLng, bearing: Double) {
        val centerFirstMarkerX = markers[0].latLng.latitude
        val centerFirstMarkerY = markers[0].latLng.longitude

        map.moveCamera(LatLng(centerFirstMarkerX, centerFirstMarkerY), 0.0f)
        map.setBearing(bearing.toFloat(), 0f)
//        map.setZoom(20f, 0f)
        addNavigationMarker(markers[0].latLng)
//        }
    }
    private fun getLineStyle(): LineStyle {
        val lineStCr = LineStyleBuilder()
        lineStCr.color = Color(
            2.toShort(),
            119.toShort(),
            189.toShort(),
            190.toShort(),
        )
        lineStCr.width = 10f
        lineStCr.stretchFactor = 0f
        return lineStCr.buildStyle()
    }
    private fun findRoute() {
        if (markers.size < 2) {
            Toast.makeText(this, "برای مسیریابی باید دو نقطه انتخاب شود", Toast.LENGTH_SHORT).show()
        } else {
            try {
                map.removePolyline(onMapPolyline)
                onMapPolyline = Polyline(routeOverviewPolylinePoints, getLineStyle())
                map.addPolyline(onMapPolyline)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                map.removePolyline(onMapPolyline)
                onMapPolyline = Polyline(decodedStepByStepPath, getLineStyle())
                map.addPolyline(onMapPolyline)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
    private fun addMarker(loc: LatLng): Marker {
        val animStBl = AnimationStyleBuilder()
        animStBl.fadeAnimationType = AnimationType.ANIMATION_TYPE_SMOOTHSTEP
        animStBl.sizeAnimationType = AnimationType.ANIMATION_TYPE_SPRING
        animStBl.phaseInDuration = 0.5f
        animStBl.phaseOutDuration = 0.5f
        val markStCr = MarkerStyleBuilder()
        markStCr.size = 30f
        markStCr.bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources,
                R.drawable.ic_marker,
            ),
        )
        markStCr.animationStyle = animSt
        val markSt = markStCr.buildStyle()
        marker = Marker(loc, markSt)

        return marker as Marker
    }
    private fun addNavigationMarker(startLoc: LatLng) {
        if (userMarker != null) {
            map.removeMarker(userMarker)
        }

        val markStCr = MarkerStyleBuilder()
        markStCr.size = 50f
        markStCr.bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources,
                R.drawable.plane,
            ),
        )
        val markSt = markStCr.buildStyle()

        userMarker = Marker(startLoc, markSt)

        map.addMarker(userMarker)
    }
    private fun bearing(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
    ): Double {
        val dLng = endLng - startLng

        val y = kotlin.math.sin(dLng) * kotlin.math.cos(endLat)
        val x = kotlin.math.cos(startLat) * kotlin.math.sin(endLat)
        -kotlin.math.sin(startLat) * kotlin.math.cos(endLat) * kotlin.math.cos(dLng)

        var bearing = Math.toDegrees(kotlin.math.atan2(y, x))
        if (bearing < 0) {
            bearing += 360
        }
        return bearing
    }
}
