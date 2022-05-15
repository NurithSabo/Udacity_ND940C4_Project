package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.CompObjs.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.utils.CompObjs.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.fragment_select_location.*
import org.koin.android.ext.android.inject
import java.util.*

const val TAG = "SelectLocationFragment"
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback,
    OnCompleteListener<LocationSettingsResponse> {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var map : GoogleMap
    private lateinit var binding: FragmentSelectLocationBinding
    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val defaultLocation : LatLng = LatLng(70.0, 25.0)
    private lateinit var selectedLatLng : LatLng
    private var currentLocation : LatLng = defaultLocation
    private var locationPermissionGranted = false
    private var selectedMarker: Marker?=null

    private val zoomLevel = 18f
    private var selectedPOI: PointOfInterest? = null

    var reminderSelectedLocationStr = ""

//    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

       //setmapfragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        _viewModel.mapSelectedEvent.observe(viewLifecycleOwner, androidx.lifecycle.Observer { isSelected ->
             binding.btnConfirm.isEnabled = isSelected
        })

        binding.btnConfirm.setOnClickListener {
            if (reminderSelectedLocationStr.isNotEmpty()) {
                onLocationSelected()
            } else
                _viewModel.showToast.value = "Please select a location"
        }
        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap)
    {
        map = googleMap
        val zoomLevel = 15f

        enableMyLocation()
        setMapStyle(map)
        setCurrentLocationClick(map)
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
        map.moveCamera( CameraUpdateFactory.newLatLngZoom(currentLocation, zoomLevel))
        //poi listener:
        setPoiClick(map)
        //Allows the custom pin:
        setMapLongClick(map)
        btnConfirm.isEnabled = false
    }

    private fun setMapStyle(map: GoogleMap){
        // Customize the styling of the base map using a JSON object defined
        // in a raw resource file.
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style2)
            )
            if (!success){
                Log.e(TAG,"Error: Style parsing failed")
            }
        }
        //In the catch block if the file can't be loaded, the method throws a Resources.NotFoundException.
        catch(e: Resources.NotFoundException) {
            Log.e(TAG,"Error: Can't find style. Error: $e")
        }
    }

    private fun setCurrentLocationClick(map: GoogleMap) {
        map.setOnMyLocationClickListener { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            updateCurrentLocation(latLng)
        }
    }

    private fun updateCurrentLocation(latLng: LatLng) {
        _viewModel.showToast.value = "Current Location Selected"
        selectedMarker?.remove()
        reminderSelectedLocationStr = "Lat: ${currentLocation.latitude}, Long: ${currentLocation.longitude}"
        selectedPOI = PointOfInterest(latLng, reminderSelectedLocationStr, "Current Location")
        currentLocation = LatLng(currentLocation.longitude, currentLocation.latitude)
        _viewModel.mapSelected()
        selectedLatLng = currentLocation
        _viewModel.showToast.value =getString(R.string.marker_added)
    }

    //Lesson 1 : click on poi
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            selectedMarker?.remove()
            selectedPOI = poi
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
            poiMarker.showInfoWindow()
            reminderSelectedLocationStr =poi.name
//            selectedPOI =poi
            currentLocation = LatLng(poi.latLng.latitude, poi.latLng.longitude)
            _viewModel.mapSelected()
            selectedLatLng = currentLocation
            _viewModel.showToast.value ="Marker added"
        }
    }

    //Lesson 1:
    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            selectedMarker?.remove()
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            reminderSelectedLocationStr = snippet
            selectedPOI = PointOfInterest(latLng, snippet, "Custom location")
            currentLocation = LatLng(latLng.latitude, latLng.longitude)
            _viewModel.mapSelected()
            selectedLatLng = latLng
            _viewModel.showToast.value ="Marker added"
        }
    }

    private fun onLocationSelected() {
//      send back the selected location details to the view model
//      and navigate back to the previous fragment to save the reminder and add the geofence

        _viewModel.reminderSelectedLocationStr.value = reminderSelectedLocationStr
        _viewModel.selectedPOI.value = selectedPOI
        _viewModel.latitude.value = currentLocation.latitude
        _viewModel.longitude.value = currentLocation.longitude
        _viewModel.navigationCommand.value = NavigationCommand.Back
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    private fun isPermissionGranted() : Boolean {
        locationPermissionGranted = false
        if (activity?.let {
                ContextCompat.checkSelfPermission(
                    it.applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION

                )
            } == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        }
        return locationPermissionGranted
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation()
    {
        if (isPermissionGranted())
        {
            map.isMyLocationEnabled = true
            fusedLocationProviderClient.lastLocation.addOnSuccessListener(requireActivity()){
                    location ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,zoomLevel))
                    map.isMyLocationEnabled = true
                }
            }}
        else{
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE)
            Log.i(TAG, "enableMyLocation else ág")
        }
    }

    //    Lesson 2
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE)
            {
                if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    enableMyLocation()
                    Log.i(TAG, "enableMyLocation+")
                }
                else {
                    // build alert dialog
                    val dialogBuilder = AlertDialog.Builder(requireContext())

                    // set message of alert dialog
                    dialogBuilder.setMessage(R.string.permission_denied_explanation)
                        // if the dialog is cancelable
                        .setCancelable(false)
                        // positive button text and action
                        .setPositiveButton("Proceed", DialogInterface.OnClickListener {
                                dialog, id ->
                            startActivity(Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        })
                        // negative button text and action
                        .setNegativeButton("Cancel", DialogInterface.OnClickListener {
                                dialog, id -> dialog.cancel()
                        })

                    // create dialog box
                    val alert = dialogBuilder.create()
                    // set title for alert dialog box
                    alert.setTitle("Location permission needed")
                    // show alert dialog
                    alert.show()
                }
            }
    }

    //Lesson 1
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            if (!this::map.isInitialized) {
                false
            } else {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            }
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onComplete(task: Task<LocationSettingsResponse>) {
        task.result?.let {
            zoomToPosition(it)
        }
    }

    private fun zoomToPosition(result: LocationSettingsResponse) {
    }
}