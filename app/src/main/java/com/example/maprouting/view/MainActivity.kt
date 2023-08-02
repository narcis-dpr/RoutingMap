package com.example.maprouting.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
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
import com.example.maprouting.databinding.ActivityMainBinding
import com.example.maprouting.utiles.Constants.FASTEST_LOCATION_UPDATE_INTERVAL
import com.example.maprouting.utiles.Constants.LOCATION_UPDATE_INTERVAL
import com.example.maprouting.utiles.Constants.REQUEST_CODE
import com.example.maprouting.utiles.location.LocationClient
import com.example.maprouting.viewModel.RouteViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient
import com.karumi.dexter.BuildConfig
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.neshan.common.model.LatLng
import org.neshan.common.utils.PolylineEncoding
import org.neshan.mapsdk.MapView
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity() : AppCompatActivity() {
    private val viewModel: RouteViewModel by viewModels()
    private lateinit var mapView: MapView
    private var binding: ActivityMainBinding? = null

    @Inject
    lateinit var fusedLocationClient: FusedLocationProviderClient

    @Inject
    lateinit var locationClient: LocationClient

    private var userLocation: Location? = null
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationRequest: LocationRequest
    private var locationSettingsRequest: LocationSettingsRequest? = null
    private var locationCallback: LocationCallback? = null
    private var lastUpdateTime: String? = null
    private val TAG: String = MainActivity::class.java.name
    private var mRequestingLocationUpdates: Boolean? = null
    private var userMarker: Marker? = null
    private var marker: Marker ? = null
    val markers: ArrayList<Marker> = ArrayList()
    private var animSt: AnimationStyle? = null
    private var onMapPolyline: Polyline? = null
    private var routeOverviewPolylinePoints: ArrayList<LatLng>? = null
    private var decodedStepByStepPath: ArrayList<LatLng>? = null
    private var overview = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        mapView = findViewById(R.id.mapview)
        binding?.getCurrentLocation?.setOnClickListener {
            focusOnUserLocation()
        }
        mapView.setOnMapLongClickListener {
            if (markers.size < 2) {
                markers.add(addMarker(it))
                binding!!.overviewInfoBanner.setText(R.string.target_press)
                if (markers.size == 2) {
                    runBlocking(Dispatchers.Main) {
                        neshanRoutingApi()
                    }
                }
            } else {
                runBlocking(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "مسیریابی بین دو نقطه انجام میشود!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initMap()
        initLocation()
        startReceivingLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    private fun initMap() {
        mapView.moveCamera(LatLng(35.767234, 51.330743), 0f)
        mapView.setZoom(14f, 0f)
        if (onMapPolyline != null) {
            mapView.removePolyline(onMapPolyline)
        }
    }

    private fun initLocation() {
        settingsClient = LocationServices.getSettingsClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                userLocation = locationResult.lastLocation
                lastUpdateTime = DateFormat.getTimeInstance().format(Date())
                onLocationChange()
            }
        }
        mRequestingLocationUpdates = false
        locationRequest = LocationRequest()
        locationRequest.numUpdates = 10
        locationRequest.interval = LOCATION_UPDATE_INTERVAL
        locationRequest.fastestInterval = FASTEST_LOCATION_UPDATE_INTERVAL
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
    }

    private fun onLocationChange() {
        if (userLocation != null) {
            addUserMarker(LatLng(userLocation!!.latitude, userLocation!!.longitude))
        }
    }

    private fun addUserMarker(loc: LatLng) {
        if (userMarker != null) {
            mapView.removeMarker(userMarker)
        }

        val markStCr = MarkerStyleBuilder()
        markStCr.size = 30f
        markStCr.bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources,
                R.drawable.googlemapbluedot,
            ),
        )
        val markSt = markStCr.buildStyle()

        userMarker = Marker(loc, markSt)

        mapView.addMarker(userMarker)
    }
    private fun addMarker(loc: LatLng): Marker {
        val animStBl = AnimationStyleBuilder()
        animStBl.fadeAnimationType = AnimationType.ANIMATION_TYPE_SMOOTHSTEP
        animStBl.sizeAnimationType = AnimationType.ANIMATION_TYPE_SPRING
        animStBl.phaseInDuration = 0.5f
        animStBl.phaseOutDuration = 0.5f
        animSt = animStBl.buildStyle()
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

        mapView.addMarker(marker)
        return marker as Marker
    }
    private fun startReceivingLocationUpdates() {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    mRequestingLocationUpdates = true
                    startLocationUpdates()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied()) {
                        openSettings()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken,
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        settingsClient.checkLocationSettings(locationSettingsRequest!!).addOnSuccessListener(this) {
            Log.i(TAG, "All location settings are satisfied.")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.myLooper(),
            )
            onLocationChange()
        }
            .addOnFailureListener(this) { e ->
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(
                            TAG,
                            "Location settings are not satisfied. Attempting to upgrade " +
                                "location settings ",
                        )
                        if (mRequestingLocationUpdates == true) {
                            try {
                                val rae = e as ResolvableApiException
                                rae.startResolutionForResult(this, REQUEST_CODE)
                            } catch (sie: IntentSender.SendIntentException) {
                                Log.i(
                                    TAG,
                                    "PendingIntent unable to execute request.",
                                )
                            }
                        }
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage = "Location settings are inadequate, and cannot be " +
                            "fixed here. Fix in Settings."
                        Log.e(
                            TAG,
                            errorMessage,
                        )
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
                onLocationChange()
            }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback!!)
            .addOnCompleteListener(
                this,
            ) {
                Toast.makeText(applicationContext, "Location updates stopped!", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts(
            "package",
            BuildConfig.APPLICATION_ID,
            null,
        )
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    fun focusOnUserLocation() {
        if (userLocation != null) {
            mapView.moveCamera(
                LatLng(userLocation!!.latitude, userLocation!!.longitude),
                0.25f,
            )
            mapView.setZoom(15f, 0.25f)
        }
    }

    private fun findRoute() {
        if (markers.size < 2) {
            Toast.makeText(this, "برای مسیریابی باید دو نقطه انتخاب شود", Toast.LENGTH_SHORT).show()
        } else {
            try {
                mapView.removePolyline(onMapPolyline)
                onMapPolyline = Polyline(routeOverviewPolylinePoints, getLineStyle())
                mapView.addPolyline(onMapPolyline)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                mapView.removePolyline(onMapPolyline)
                onMapPolyline = Polyline(decodedStepByStepPath, getLineStyle())
                mapView.addPolyline(onMapPolyline)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
    private fun neshanRoutingApi() {
        viewModel.getDirections(
            markers[0].latLng,
            markers[1].latLng,
        )
        viewModel.route.observe(
            this,
            Observer { route ->
                if (route == null) {
                    Toast.makeText(this@MainActivity, "مسیری یافت نشد", Toast.LENGTH_LONG)
                        .show()
                } else {
                    routeOverviewPolylinePoints = java.util.ArrayList(
                        PolylineEncoding.decode(
                            route.overviewPolyline.encodedPolyline,
                        ),
                    )
                    decodedStepByStepPath = java.util.ArrayList()

                    for (step in route.legs[0].directionSteps) {
                        decodedStepByStepPath!!.addAll(PolylineEncoding.decode(step.encodedPolyline))
                    }
                    onMapPolyline = Polyline(routeOverviewPolylinePoints, getLineStyle())
                    mapView.addPolyline(onMapPolyline)
                    mapSetPosition(overview)
                    findRoute()
                    RouteBottomSheet(this@MainActivity).show(supportFragmentManager, "")
                    binding?.overviewInfoBanner?.visibility = View.GONE
                }
            },
        )
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
    private fun mapSetPosition(overview: Boolean) {
        val centerFirstMarkerX = markers[0].latLng.latitude
        val centerFirstMarkerY = markers[0].latLng.longitude
        if (overview) {
            val centerFocalPositionX = (centerFirstMarkerX + markers[1].latLng.latitude) / 2
            val centerFocalPositionY = (centerFirstMarkerY + markers[1].latLng.longitude) / 2
            mapView.moveCamera(LatLng(centerFocalPositionX, centerFocalPositionY), 0.5f)
            mapView.setZoom(14f, 0.5f)
        } else {
            mapView.moveCamera(LatLng(centerFirstMarkerX, centerFirstMarkerY), 0.5f)
            mapView.setZoom(14f, 0.5f)
        }
    }
}
