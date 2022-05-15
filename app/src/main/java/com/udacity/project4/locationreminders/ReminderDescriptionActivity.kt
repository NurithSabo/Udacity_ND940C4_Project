package com.udacity.project4.locationreminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.utils.CompObjs.ACTION_GEOFENCE_EVENT

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
@SuppressLint("UnspecifiedImmutableFlag")
class ReminderDescriptionActivity : AppCompatActivity()  {
    val TAG = "ReminderDescription"
    private lateinit var binding: ActivityReminderDescriptionBinding
    lateinit var geofencingClient: GeofencingClient
    lateinit var okButton : Button
    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )

        geofencingClient = LocationServices.getGeofencingClient(this)

        if( intent != null) {
            val reminderDataItem: ReminderDataItem? =
                intent.extras?.getSerializable(EXTRA_ReminderDataItem) as ReminderDataItem?

            if (reminderDataItem != null) {
                binding.reminderDataItem = reminderDataItem
            }
        }
        okButton = findViewById(R.id.ok_button)
        okButton.setOnClickListener{
            removeGeofence()
            val pendingIntent = NavDeepLinkBuilder(this.applicationContext)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.reminderListFragment )
                .createPendingIntent()
            pendingIntent.send()
        }
    }//End onCreate

    private fun removeGeofence() {

            val geofenceForDelete : MutableList<String> = mutableListOf()
            geofenceForDelete.add(GeofenceTransitionsJobIntentService.fenceid)
            geofencingClient.removeGeofences(geofenceForDelete)?.run{
                addOnSuccessListener {
                    Toast.makeText(applicationContext, "Geofence removed", Toast.LENGTH_SHORT)
                        .show()
                }
                addOnFailureListener {
                    Toast.makeText(applicationContext, "Geofence not removed", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

//      receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }
}