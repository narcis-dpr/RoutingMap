package com.example.maprouting.view

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.carto.graphics.Color
import com.carto.styles.LineStyle
import com.carto.styles.LineStyleBuilder
import com.example.maprouting.R
import com.example.maprouting.viewModel.RouteViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.neshan.common.model.LatLng
import org.neshan.common.utils.PolylineEncoding
import org.neshan.mapsdk.MapView
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import java.util.ArrayList

@AndroidEntryPoint
class TiltCameraActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val viewModel: RouteViewModel by viewModels()
    private var routeOverviewPolylinePoints: ArrayList<LatLng>? = null
    private var decodedStepByStepPath: ArrayList<LatLng>? = null
    private var onMapPolyline: Polyline? = null
    private val markers: ArrayList<Marker> = ArrayList()
    private var overview = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tilt_camera)
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
        map.setZoom(14f, 0f)
        map.setTilt(10f, 0f)
        map.settings.isMapRotationEnabled = true
        layRoute()
    }
    private fun layRoute() {
        viewModel.route.observe(
            this,
            Observer { route ->
                println(" the route is $route ")
                if (route == null) {
                    Toast.makeText(this@TiltCameraActivity, "مسیری یافت نشد", Toast.LENGTH_LONG)
                        .show()
                } else {
                    routeOverviewPolylinePoints = ArrayList(
                        PolylineEncoding.decode(
                            route.overviewPolyline.encodedPolyline,
                        ),
                    )
                    decodedStepByStepPath = ArrayList()

                    for (step in route.legs[0].directionSteps) {
                        decodedStepByStepPath!!.addAll(PolylineEncoding.decode(step.encodedPolyline))
                    }
                    onMapPolyline = Polyline(routeOverviewPolylinePoints, getLineStyle())
                    map.addPolyline(onMapPolyline)
                    mapSetPosition(overview)
                    findRoute()
                    RouteBottomSheet(this@TiltCameraActivity).show(supportFragmentManager, "")
                }
            },
        )
    }
    private fun mapSetPosition(overview: Boolean) {
        val centerFirstMarkerX = markers[0].latLng.latitude
        val centerFirstMarkerY = markers[0].latLng.longitude
        if (overview) {
            val centerFocalPositionX = (centerFirstMarkerX + markers[1].latLng.latitude) / 2
            val centerFocalPositionY = (centerFirstMarkerY + markers[1].latLng.longitude) / 2
            map.moveCamera(LatLng(centerFocalPositionX, centerFocalPositionY), 0.5f)
            map.setZoom(14f, 0.5f)
        } else {
            map.moveCamera(LatLng(centerFirstMarkerX, centerFirstMarkerY), 0.5f)
            map.setZoom(14f, 0.5f)
        }
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
}
