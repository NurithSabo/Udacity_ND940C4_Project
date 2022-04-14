package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

import org.hamcrest.Matchers.hasSize

//_TODO: provide testing to the RemindersListViewModel and its live data objects

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var fakeDataSource : FakeDataSource

    //reminder
    private lateinit var reminder : ReminderDTO

    @Before
    fun setupViewModel() {

        stopKoin()
        fakeDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(),
        fakeDataSource)
        reminder = ReminderDTO(
            "Barrow",
            "Be thankful to God for being here.",
            "Barrow airport",
            71.284809,
            -156.773695,
            "1"
        )
    }

    @Test
    fun loadRemindersTest_TrueFalse() = runBlocking{
        fakeDataSource.deleteAllReminders()
        fakeDataSource.saveReminder(reminder)
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        var value =  remindersListViewModel.showLoading.getOrAwaitValue()
        MatcherAssert.assertThat(value, CoreMatchers.`is`(true))
        mainCoroutineRule.resumeDispatcher()
        value =  remindersListViewModel.showLoading.getOrAwaitValue()
        MatcherAssert.assertThat(value, CoreMatchers.`is`(false))
    }

    @Test
    fun loadNoDataTest_TrueFalse() = runBlocking{
        remindersListViewModel.loadReminders()
        var value = remindersListViewModel.showNoData.getOrAwaitValue()
        MatcherAssert.assertThat(value,CoreMatchers.`is`(true))
        fakeDataSource.saveReminder(reminder)
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        mainCoroutineRule.resumeDispatcher()
        value = remindersListViewModel.showNoData.getOrAwaitValue()
        MatcherAssert.assertThat(value,CoreMatchers.`is`(false))
    }

    @Test
    fun reminderListLengthTest_NoDataIsData() = runBlocking {
        fakeDataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.remindersList.getOrAwaitValue() , hasSize(0) )
        fakeDataSource.saveReminder(reminder)
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.remindersList.getOrAwaitValue() , hasSize(1) )
    }
}