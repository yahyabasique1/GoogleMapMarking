package com.yahya.mapproject

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMapClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    //boolean to draw circle only one time irrespective of location update
    private var isCircleDrawn = false

    val circleOptions = CircleOptions()
    val radius = 5 * 1000 // setting range in kilometre

    lateinit var geocoder: Geocoder  // geocoder to get the readable info from the given lat lomg

    lateinit var mapFragment: SupportMapFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        geocoder = Geocoder(this, Locale.getDefault())

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation

                if (isCircleDrawn.not()) {
                    val latlng = LatLng(lastLocation.latitude, lastLocation.longitude)
                    circleOptions.center(latlng)
                        .radius(radius.toDouble())
                        .strokeWidth(2f)
                        .strokeColor(Color.BLUE)
                    map.addCircle(circleOptions)
                    onMapClick(latlng)
                    isCircleDrawn = true
                }
                Log.e("TRACKR", "One")
            }
        }
        startLocationRequestUpdate()
    }

    private fun startLocationRequestUpdate() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 0
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdate()


        }

        task.addOnFailureListener {
            Log.e("lkooooooo", "$it")
            if (it is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    it.startResolutionForResult(
                        this@MapsActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    public override fun onResume() {
        super.onResume()
        if (locationUpdateState.not()) {
            startLocationUpdate()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        Log.e("TRACKR", "2")


        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)
        map.setOnMapClickListener(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            Log.e("TRACKR", "3")

            return
        }
        map.isMyLocationEnabled = true

        map.uiSettings.isCompassEnabled = true



        fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) {

            it?.let {
                lastLocation = it
            }

        }

    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        Toast.makeText(this, "Clicked", Toast.LENGTH_LONG).show()
        return false
    }


    override fun onMapClick(p0: LatLng?) {
        val distance = lastLocation.distanceTo(Location(LocationManager.GPS_PROVIDER).apply {
            latitude = p0?.latitude!!
            longitude = p0?.longitude!!
        })

        //If the distance is within the radius limit we show the marker
        if (distance > radius) {
            return
        }


        val addresses = geocoder.getFromLocation(p0?.latitude!!, p0?.longitude!!, 1)

        val city = addresses[0].locality
        val country = addresses[0].countryName
        val postalCode = addresses[0].postalCode

        val markerOptions = MarkerOptions()
        markerOptions.position(p0!!)
        markerOptions.title("$city $country $postalCode")
        // Animating to the touched position
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(p0, 12f))

        // Placing a marker on the touched position
        map.addMarker(markerOptions);


    }

    private fun startLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )

            return
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    startLocationRequestUpdate()
                    mapFragment.getMapAsync(this)
                } else {
                    Toast.makeText(
                        this,
                        "Cannot proceed without accessing location.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdate()
            }
        }

    }


}