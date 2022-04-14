package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource (var reminders: MutableList<ReminderDTO>? = mutableListOf()):
    ReminderDataSource {

//    _TODO: Create a fake data source to act as a double to the real data source
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        //_TODO("Return the reminders")
        reminders?.let {
             return Result.Success(ArrayList(it))
        } ?: return Result.Error("No reminder found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        //_TODO("save the reminder")
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String):
            Result<ReminderDTO> =
        //_TODO("return the reminder with the id")
        reminders?.firstOrNull{ it.id == id}?.let {
            Result.Success(it)
        } ?: Result.Error("Reminder $id not found")

    override suspend fun deleteAllReminders() {
        //_TODO("delete all the reminders")
        reminders?.clear()
    }


}