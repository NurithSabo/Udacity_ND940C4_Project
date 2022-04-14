package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

//Unit test the DAO
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

//    _TODO: Add testing implementation to the RemindersDao.kt

    // Executes each task synchronously using Architecture Components. Lesson5/9
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var database: RemindersDatabase

//  Lesson 5
    @Before
    fun initDatabase()
    {
        database =Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

//  5/9
    @After
    fun closeDatabase()
    {
        database.close()
    }

    @Test
    fun savingCallingByIdReminder() = runBlockingTest {
        val reminder = ReminderDTO(
            "Barrow",
            "Be thankful to God for being here.",
            "Barrow airport",
            71.284809,
            -156.773695,
            "1")

        database.reminderDao().saveReminder(reminder)
        val savedReminderDTO = database.reminderDao().getReminderById(reminder.id)
        assertThat<ReminderDTO>(savedReminderDTO, notNullValue())
        assertThat(savedReminderDTO?.id, `is`(reminder.id))
    }

    @Test
    fun deleteReminders() {
        runBlockingTest {
            val reminder = ReminderDTO(
                "Barrow",
                "Be thankful to God for being here.",
                "Barrow airport",
                71.284809,
                -156.773695,
                "1"
            )
            database.reminderDao().saveReminder(reminder)
            database.reminderDao().deleteAllReminders()
            val savedReminderDtoList = database.reminderDao().getReminders()
            assertThat(savedReminderDtoList.isEmpty(), `is`(true))
        }
    }
}