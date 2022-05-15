package com.udacity.project4.locationreminders.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.CompObjs
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext


//Ori
class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    lateinit var geofencingClient: GeofencingClient
    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = CompObjs.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573
        lateinit var fenceid: String
        // Call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }//end enqueueWork
    }

    override fun onHandleWork(intent: Intent) {

        //Lesson 2/6
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent.hasError()) {
                    return
                }
            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                ||
                geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL
                )
            {
                sendNotification(geofencingEvent.triggeringGeofences)
            }
    }

    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        for (item in triggeringGeofences.indices) {
            val fenceId =
                if (triggeringGeofences.isNotEmpty())
                    triggeringGeofences[item].requestId

                else return

//          Get the local repository instance
            val remindersLocalRepository: ReminderDataSource by inject()
//          Interaction to the repository has to be through a coroutine scope
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            fenceid = fenceId
//              get the reminder with the request id
                val result = remindersLocalRepository.getReminder(fenceId)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
//                  send a notification to the user with the reminder details
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )

                }//if
            }//Coroutine
        }//end of for loop
    }//sendnotification

    private fun removeGeofence2(triggeringGeofences: MutableList<Geofence>) {
        Log.i("asd", "removeGeofence2Start")
        for (item in triggeringGeofences.indices) {
            val fenceId =
                if (triggeringGeofences.isNotEmpty())
                    triggeringGeofences[item].requestId
                else return
            var geofencesForDelete: MutableList<String> = mutableListOf()
            geofencesForDelete.add((triggeringGeofences[item].requestId).toString())
            geofencingClient.removeGeofences(geofencesForDelete)?.run {
                addOnSuccessListener {
                    Log.d("asd", "Geofence removed")
                    Toast.makeText(applicationContext, "Geofence removed", Toast.LENGTH_SHORT)
                        .show()
                }
                addOnFailureListener {
                    Log.d("asd", "Geofence not removed")
                }
            }
        }
    }


} //class
