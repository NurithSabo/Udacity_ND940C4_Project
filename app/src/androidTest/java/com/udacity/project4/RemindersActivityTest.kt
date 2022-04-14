package com.udacity.project4

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.FragmentScenario.launch
import androidx.fragment.app.testing.launchFragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.util.monitorFragment
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.mockito.Mockito.mock
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

//    private lateinit var repository: ReminderDataSource
    private lateinit var fakeRepository : FakeDataSource
    private lateinit var appContext: Application
    private lateinit var reminder1 : ReminderDTO
    private lateinit var reminder2 : ReminderDTO
    // An Idling Resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun registerIdlingResource(): Unit = IdlingRegistry.getInstance().run {
        register(EspressoIdlingResource.countingIdlingResource)
        register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource(): Unit = IdlingRegistry.getInstance().run {
        unregister(EspressoIdlingResource.countingIdlingResource)
        unregister(dataBindingIdlingResource)
    }
    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        fakeRepository = FakeDataSource()
        appContext = getApplicationContext()

        val myModule = module {
            single {
                RemindersListViewModel(
                    appContext,
                    fakeRepository
                    as ReminderDataSource)
            }

            single {
                SaveReminderViewModel(
                    appContext,
                    fakeRepository
                    as ReminderDataSource
                )
            }
            single <ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        } //end myModule

        fakeRepository = FakeDataSource()
        runBlocking {

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
            fakeRepository.saveReminder(reminder1)
            fakeRepository.saveReminder(reminder2)
            
        }
        //declare a new koin module
        startKoin {
            androidContext(appContext)
            modules(listOf(myModule))
        }
    }

//   _TODO: add End to End testing to the app

    @Test
    fun launchEndToEndTest() {
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)
        onView(withText(reminder1.title)).check(matches(isDisplayed()))
        onView(withText(reminder1.description)).check(matches(isDisplayed()))
        onView(withText(reminder1.location)).check(matches(isDisplayed()))
        onView(withText(reminder2.title)).check(matches(isDisplayed()))
        onView(withText(reminder2.description)).check(matches(isDisplayed()))
        onView(withText(reminder2.location)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        Thread.sleep(6000)
        onView(withId(R.id.map)).perform(ViewActions.longClick())
        onView(withId(R.id.btnConfirm)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(ViewActions.typeText("Title"))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.typeText("Description"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())
        onView(withText("Title")).check(matches(isDisplayed()))
        onView(withText("Description")).check(matches(isDisplayed()))
        onView(withText(appContext.getString(R.string.logout))).check(matches(isDisplayed()));
        scenario.close()
//        onView(withId(R.id.login_button)).check(matches(withText(R.string.login_button_text)))

//        onView(withText(R.string.login_button_text)).check(matches(isDisplayed()))
    }
}