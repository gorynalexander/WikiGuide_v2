package com.santo.wikiguide.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.coroutines.sendSuspend
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.santo.wikiguide.R
import com.santo.wikiguide.presentation.map.MapFragment
import com.santo.wikiguide.presentation.places.PlacesFragment
import com.santo.wikiguide.util.permissions.PermissionChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var permissionChecker: PermissionChecker


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initLocation()
//        supportFragmentManager.beginTransaction().replace(R.id.container, PlacesFragment()).commit()
        supportFragmentManager.beginTransaction().replace(R.id.container, MapFragment()).commit()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun initLocation() {

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                Timber.d("locations ${locationResult.locations}")
                val location = locationResult.locations.firstOrNull()
                if (location != null) {
                    viewModel.sendLocation(location)
                }
            }
        }

        lifecycleScope.launch {
            val request =
                permissionsBuilder(Manifest.permission.ACCESS_FINE_LOCATION).build().sendSuspend()
            when {
                request.allGranted() -> subscribeOnLocationUpdates()
                else -> Timber.d("No permissions")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeOnLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (permissionChecker.isLocationPermissionGranted()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                Timber.d("location $location")
                if (location != null) {
                    viewModel.sendLocation(location)
                }
            }
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.create(),
            locationCallback,
            Looper.getMainLooper()
        )
    }


}