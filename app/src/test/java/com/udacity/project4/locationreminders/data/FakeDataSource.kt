package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource (var reminders: MutableList<ReminderDTO>? = mutableListOf()):
    ReminderDataSource {

    var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    //    _TODO: Create a fake data source to act as a double to the real data source
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        //_TODO("Return the reminders")
        if (shouldReturnError) {
            return Result.Error("Test Exception")
        }
        else {
            return Result.Success(ArrayList(reminders!!))
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        //_TODO("save the reminder")
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        //_TODO("return the reminder with the id")
        if (shouldReturnError) {
            return Result.Error("Test Exception")
        }
        val reminder = reminders?.find { it.id == id }
        return if (reminder == null) {
            Result.Error("Test Reminder $id not found")
        } else {
            Result.Success(reminder)
        }
    }
    override suspend fun deleteAllReminders() {
        //_TODO("delete all the reminders")
        reminders?.clear()
    }


}