package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.locationreminders.savereminder.currentLocation
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.fragment_select_location.*
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback,
    OnCompleteListener<LocationSettingsResponse> {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var map : GoogleMap
    private lateinit var binding: FragmentSelectLocationBinding
    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val defaultLocation : LatLng = LatLng(70.0, 25.0)
    private var locationPermissionGranted = false
    private lateinit var locationCallback: LocationCallback
    private var selectedMarker: Marker?=null
    private lateinit var thisLocation : LatLng
    private lateinit var selectedLatLng : LatLng
    private var selectedTitle: String? = null

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity());
        thisLocation = currentLocation

//      _TODO: add the map setup implementation
//      _TODO: zoom to the user location after taking his permission
//      _TODO: add style to the map
//      _TODO: put a marker to location that the user selected

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
/*        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                thisLocation = LatLng(location.latitude, location.longitude)
            }*/

        _viewModel.mapSelectedEvent.observe(viewLifecycleOwner, androidx.lifecycle.Observer { isSelected ->
             binding.btnConfirm.isEnabled = isSelected
        })

//      _TODO: call this function after the user confirms on the selected location
        binding.btnConfirm.setOnClickListener {
                    selectedLatLng.let {
                    onLocationSelected(it)
                }
        }

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap)
    {
        map = googleMap
        val zoomLevel = 18f
        val overlaySize = 10f

        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isZoomControlsEnabled = true

        val androidOverlay = GroundOverlayOptions()
            .image(BitmapDescriptorFactory.fromResource(R.drawable.android))
            .position(thisLocation, overlaySize)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoomLevel))
        //map!!.animateCamera(CameraUpdateFactory.newLatLngZoom(thisLocation , zoomLevel))
        map.addMarker(MarkerOptions().position(thisLocation))
        //Allows the custom pin:
        setMapLongClick(map)
        //poi listener:
        setPoiClick(map)

        map.addGroundOverlay(androidOverlay)
        btnConfirm.isEnabled = false
    }

    //Lesson 1
    private fun setPoiClick(map: GoogleMap) {
        selectedMarker?.remove()
        map.setOnPoiClickListener { poi ->
            map.clear()
            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name))
            selectedMarker!!.showInfoWindow()
            selectedLatLng = poi.latLng
            selectedTitle = poi.name
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
            _viewModel.mapSelected()
            selectedLatLng = latLng
        }
    }

    private fun onLocationSelected(latLng: LatLng) {
//      _TODO: When the user confirms on the selected location,
//      send back the selected location details to the view model
//      and navigate back to the previous fragment to save the reminder and add the geofence
        _viewModel.updateLocation(latLng,selectedTitle)
        _viewModel.navigationCommand.value = NavigationCommand.Back
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

/*    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
            }
        } catch (e: SecurityException) {
            Log.i("asd","Exception: %s"+ e.message, e)
        }
    }*/

    //Lesson 1
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            if (!this::map.isInitialized) {
                Log.i("Map init:", "map has not been initialized yet")
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