package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

//Medium Test to test the repository
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {

//  Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var localDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun initDatabase()
    {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataSource =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun saveReminder_getReminder_Test() = runBlocking {
        val reminder = ReminderDTO(
            "Barrow",
            "Be thankful to God for being here.",
            "Barrow airport",
            71.284809,
            -156.773695,
            "1")

        localDataSource.saveReminder(reminder)

        //WHEN:
        val result = localDataSource.getReminder(reminder.id) as Result.Success

        //THEN:
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.location, `is`(reminder.location))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
        assertThat(result.data.id, `is`(reminder.id))
    }

    @Test
    fun saveReminder_getReminders_Test() = runBlocking {
        val reminder1 = ReminderDTO(
            "Barrow",
            "Be thankful to God for being here.",
            "Barrow airport",
            71.284809,
            -156.773695,
            "1")
        val reminder2 = ReminderDTO(
            "Ein Gedi Oasis",
            "Be thankful to God for being here.",
            "Ein Gedi",
            31.450816,
            35.382375,
            "2")

        localDataSource.saveReminder(reminder1)
        localDataSource.saveReminder(reminder2)
        //WHEN:
        val results = localDataSource.getReminders() as Result.Success
        //THEN:
        assertThat(results.data.count(),`is` (2))
    }

    @Test
    fun getReminder_IdNotExists_Error() = runBlocking {

        val reminder = ReminderDTO(
            "Ein Gedi Oasis",
            "Be thankful to God for being here.",
            "Ein Gedi",
            31.450816,
            35.382375,
            "2"
        )
        //HWEN:
        val result = (localDataSource.getReminder(reminder.id) as Result.Error).message
        //THEN:
        assertThat(result, `is`("Reminder not found!"))
    }

    @Test
    fun saveReminder_deleteAllReminders_Test() = runBlocking {
        val reminder1 = ReminderDTO(
            "Barrow",
            "Be thankful to God for being here.",
            "Barrow airport",
            71.284809,
            -156.773695,
            "1"
        )
        val reminder2 = ReminderDTO(
            "Ein Gedi Oasis",
            "Be thankful to God for being here.",
            "Ein Gedi",
            31.450816,
            35.382375,
            "2"
        )

        localDataSource.saveReminder(reminder1)
        localDataSource.saveReminder(reminder2)
        //WHEN:
        localDataSource.deleteAllReminders()
        val results = localDataSource.getReminders() as Result.Success
        //THEN:
        assertThat(results.data.count(), `is`(0))
        assertThat(results.data.isEmpty(), `is`(true))
    }
}