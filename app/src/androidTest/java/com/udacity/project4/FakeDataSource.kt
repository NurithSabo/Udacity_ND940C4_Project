package com.udacity.project4

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource (var reminders: MutableList<ReminderDTO>? = mutableListOf()):
    ReminderDataSource {

    var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        reminders?.let {
            return Result.Success(ArrayList(it))
       } ?: return Result.Error("No reminder found.")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError)
        {
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
        reminders?.clear()
    }

//    override suspend fun deleteReminder(title: String) {
//        TODO("Not yet implemented")
//    }
}