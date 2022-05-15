package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.FakeDataSource
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

//UI Testing
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    private lateinit var repository: FakeDataSource
    private lateinit var viewModel: RemindersListViewModel
    private lateinit var reminder1 : ReminderDTO
    private lateinit var reminder2 : ReminderDTO

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init()
    {
        stopKoin()
        repository = FakeDataSource()
        viewModel = RemindersListViewModel(getApplicationContext(),
            repository as ReminderDataSource)

        val myModule = module{
            single { viewModel }}

        //Start a Koin Application as StandAlone
        startKoin {
            modules(listOf(myModule))
        }
        //clear data for a fresh start via calling repository's deleteAllReminders method
       runBlocking {
            repository.deleteAllReminders()
            reminder1 = ReminderDTO(
            "Barrow",
            "Be thankful to God for being here.",
            "Barrow airport",
            71.284809,
            -156.773695,
            "1")
            reminder2 = ReminderDTO(
            "Ein Gedi Oasis",
            "Be happy to be here.",
            "Ein Gedi",
            31.450816,
            35.382375,
            "2")
            repository.saveReminder(reminder1)
            repository.saveReminder(reminder2)
       }
    }

    @Test
    fun navigationTest() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java) // a mock navController

        // make scenario use the mock navController
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())

        // verify
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

@Test
fun displayDataTest()  {
    runBlockingTest {

    launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
    onView(withId(R.id.remindersRecyclerView)).check( ViewAssertions.matches(isDisplayed()))
    launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)

    onView(withText(reminder1.title)).check(ViewAssertions.matches(isDisplayed()))
    onView(withText(reminder1.description)).check(ViewAssertions.matches(isDisplayed()))
    onView(withText(reminder1.location)).check(ViewAssertions.matches(isDisplayed()))
    }}

    @Test
    fun noDataTest() = runBlockingTest {
        repository.deleteAllReminders()
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withText(R.string.no_data))
            .check(ViewAssertions.matches(isDisplayed()))
    }
}