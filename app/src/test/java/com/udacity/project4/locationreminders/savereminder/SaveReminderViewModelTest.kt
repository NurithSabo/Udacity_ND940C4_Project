package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@ExperimentalCoroutinesApi
class SaveReminderViewModelTest : TestCase() {

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var fakeDataSource : FakeDataSource

    //reminder
    private lateinit var reminder1 : ReminderDataItem
    private lateinit var reminder2 : ReminderDataItem

    //mapselected event
    private var mapSelectedEvent : Boolean = false

    @Before
    public override fun setUp() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource)
        reminder1 = ReminderDataItem(
            "Barrow",
            "Be thankful to God for being here.",
            "Barrow airport",
            71.284809,
            -156.773695,
            "1"
        )
        mapSelectedEvent = false
    }

    @Test
    fun onClearTest()
    {
        saveReminderViewModel.saveReminder(reminder1)
        val notNullLocation = saveReminderViewModel.validateEnteredData(reminder1)
        assertThat(notNullLocation,`is`(true))

        saveReminderViewModel.onClear()

        val reminderTitle = saveReminderViewModel.reminderTitle.getOrAwaitValue()
        val reminderDescription = saveReminderViewModel.reminderDescription.getOrAwaitValue()
        val reminderLocationStr = saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue()
        val selectedPOI = saveReminderViewModel.selectedPOI.getOrAwaitValue()
        val latitude = saveReminderViewModel.latitude.getOrAwaitValue()
        val longitude = saveReminderViewModel.longitude.getOrAwaitValue()

        assertThat(reminderTitle,nullValue())
        assertThat(reminderDescription,nullValue())
        assertThat(reminderLocationStr,nullValue())
        assertThat(selectedPOI,nullValue())
        assertThat(latitude,nullValue())
        assertThat(longitude,nullValue())
    }

    @Test
    fun validateEnteredDataTest() {
        //Title
        reminder2 = ReminderDataItem(
            null,
            "Be thankful to God for being here.",
            "Ein Gedi",
            31.450816,
            35.382375,
            "2"
        )
        val nullTitle = saveReminderViewModel.validateEnteredData(reminder2)
        var value = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(nullTitle,`is`(false))
        assertThat(value).isEqualTo(R.string.err_enter_title)

        val notNullTitle = saveReminderViewModel.validateEnteredData(reminder1)
        assertThat(notNullTitle,`is`(true))

        //Location
        reminder2 = ReminderDataItem(
            "Ein Gedi Oasis",
            "Be thankful to God for being here.",
            null,
            31.450816,
            35.382375,
            "2"
        )
        val nullLocation = saveReminderViewModel.validateEnteredData(reminder2)
        value = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(nullLocation,`is`(false))
        assertThat(value).isEqualTo(R.string.err_select_location)
        val notNullLocation = saveReminderViewModel.validateEnteredData(reminder1)
        assertThat(notNullLocation,`is`(true))
    }

    @Test
    fun saveReminderAndShowToastTest() = runBlocking{
        //
       saveReminderViewModel.saveReminder(reminder1)
       val result = fakeDataSource.getReminders() as Result.Success

       assertThat(result.data,hasSize(1))
       assertThat(result.data.last().title).isEqualTo("Barrow")
       assertThat(result.data.last().id).isEqualTo("1")
       assertThat(result.data.last().description).isEqualTo("Be thankful to God for being here.")
       assertThat(result.data.last().location).isEqualTo("Barrow airport")
       assertThat(result.data.last().latitude).isEqualTo(71.284809)
       assertThat(result.data.last().longitude).isEqualTo( -156.773695)

       val value = saveReminderViewModel.showToast.getOrAwaitValue()
       val cont = InstrumentationRegistry.getInstrumentation().context
       val toast = cont.resources.getString(R.string.reminder_saved)
       assertThat(value).isEqualTo(toast)
    }

    @Test
    fun mapSelectedEventTest() {
        var result = saveReminderViewModel.mapSelectedEvent
        assertThat(result.value).isFalse()
        saveReminderViewModel.mapSelected()
        result = saveReminderViewModel.mapSelectedEvent
        assertThat(result.value).isTrue()
    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest {

        mainCoroutineRule.pauseDispatcher()
        saveReminderViewModel.validateAndSaveReminder(reminder1)
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }
}