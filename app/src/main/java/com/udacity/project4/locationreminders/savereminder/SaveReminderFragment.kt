package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.RemindersActivity.Companion.BACKGROUND_LOCATION_PERMISSION_INDEX
import com.udacity.project4.locationreminders.RemindersActivity.Companion.LOCATION_PERMISSION_INDEX
import com.udacity.project4.locationreminders.RemindersActivity.Companion.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
import com.udacity.project4.locationreminders.RemindersActivity.Companion.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.RemindersActivity.Companion.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

lateinit var currentLocation : LatLng

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment()
{
    override val _viewModel : SaveReminderViewModel by inject()
    private lateinit var binding : FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient

    private val REQUEST_LOCATION_PERMISSION = 1
    private val defaultLocation : LatLng = LatLng(48.8474, 2.3515)

    private var locationPermissionGranted = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
         const val GEOFENCE_RADIUS_IN_METERS = 500f
         private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    //@SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) : View
    {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_save_reminder,
            container, false
        )
        currentLocation = defaultLocation
        geofencingClient = LocationServices.getGeofencingClient(requireContext())
        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        enableMyLocation()
        return binding.root
    }

    //Ori
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
        // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value = NavigationCommand.To(
                SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
            )
        }

        binding.saveReminder.setOnClickListener {

            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value //eddig eredeti

            val reminderData = ReminderDataItem(
                title,
                description,
                location,
                latitude,
                longitude
            )

            if (_viewModel.validateEnteredData(reminderData))
            {
                   //_viewModel.saveReminder(reminderData)
                   _viewModel.validateAndSaveReminder(reminderData)
                //setGeoFence(reminderData)
               addGeofenceToReminder(reminderData)
            }
           checkPermissionsAndStartGeofencing()
        }
    }//onViewCreated end

    private fun saveReminderAndAddGeofenceToIt()
    {
        Log.i("asd", "saveReminderAndAddGeofenceToIt")
        val reminderDataItem = ReminderDataItem(
            title = _viewModel.reminderTitle.value,
            description = _viewModel.reminderDescription.value,
            location = _viewModel.reminderSelectedLocationStr.value,
            latitude = _viewModel.latitude.value,
            longitude = _viewModel.longitude.value
        )

        if (_viewModel.validateEnteredData(reminderDataItem)) {
            _viewModel.saveReminder(reminderDataItem)
            addGeofenceToReminder(reminderDataItem)
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceToReminder(reminderDataItem: ReminderDataItem) {
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
       geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
    }

//  Lesson 2
    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved())
        {
            checkDeviceLocationSettingsAndStartGeofence()
        }
        else
        {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

//  Lesson 2
    @SuppressLint("MissingPermission")
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve)
            {
                try {
                    startIntentSenderForResult(exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,0,0,0,null)
                    // exception.startResolutionForResult(requireActivity(),
                    // REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.i("Location settings error", "Error getting location settings resolution: " + sendEx.message)
                }
            }
            else
            {
                Snackbar.make(
                    binding.saveReminderRoot ,
                   R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                saveReminderAndAddGeofenceToIt()
            }
        }
    }

    //Lesson 2
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    //Lesson 2
    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        requestPermissions(permissionsArray, resultCode)
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to [Activity.onResume] of the containing
     * Activity's lifecycle.
     */
    override fun onResume() {
        super.onResume()
        if (!isPermissionGranted())
        {
            currentLocation = defaultLocation
        }
        else
        {
           enableMyLocation()
        }
    }

    //Lesson 2
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = RemindersActivity.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun isPermissionGranted() : Boolean
    {
        locationPermissionGranted = (activity?.let {
            ContextCompat.checkSelfPermission(
                it.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
                == PackageManager.PERMISSION_GRANTED)
        return locationPermissionGranted
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation()
    {
        if (isPermissionGranted())
        {
             fusedLocationProviderClient.lastLocation.addOnSuccessListener(requireActivity()){
                location ->
                 if (location != null) {
                     currentLocation = LatLng(location.latitude, location.longitude)
             }
        }}
        else
        {
             ActivityCompat.requestPermissions(
             this.requireActivity(),
             arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
             REQUEST_LOCATION_PERMISSION )
             currentLocation =   defaultLocation
        }
    }

//    Lesson 2
      override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            Snackbar.make(
                binding.saveReminderRoot ,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

//  Lesson 2
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

//  Ori
    override fun onDestroy()
    {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}