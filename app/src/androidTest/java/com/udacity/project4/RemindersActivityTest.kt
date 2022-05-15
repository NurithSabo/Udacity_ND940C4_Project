package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import java.time.LocalDateTime


@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var realRepository : ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var title : String
    private lateinit var description : String

    // An Idling Resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun registerIdlingResource(){
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(ToastManager.idlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource(){
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
        IdlingRegistry.getInstance().register(ToastManager.idlingResource)
    }

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        title = "Title"+ LocalDateTime.now()
        description = "Description "+LocalDateTime.now()
        appContext = getApplicationContext()

        val myModule = module {
            single {
                RemindersListViewModel(
                    appContext,
                    realRepository
                    as ReminderDataSource)
            }

            single {
                SaveReminderViewModel(
                    appContext,
                    realRepository
                    as ReminderDataSource
                )
            }
            single <ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        } //end myModule

        //declare a new koin module
        startKoin {
            androidContext(appContext)
            modules(listOf(myModule))
        }
        realRepository = get()
    }

    @After
    fun reset() {
        runBlocking {
           realRepository.deleteAllReminders()
        }
    }

    @Test
    fun showToastTest(){
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        Thread.sleep(3000)
        onView(withId(R.id.map)).perform(ViewActions.longClick())
        val toastMessage = R.string.marker_added

//Works under API30:
        onView(withText(toastMessage))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
        Thread.sleep(2000)
        scenario.close()
    }

    //https://alexzh.com/ui-testing-of-android-runtime-permissions/
    @Test
    fun showSnackbarTest(){
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())
        var snackBarMessage = R.string.err_enter_title
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(snackBarMessage)))
        Thread.sleep(2000)

        onView(withId(R.id.reminderTitle)).perform(ViewActions.typeText("Blabla Title"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())
        snackBarMessage = R.string.err_select_location
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(snackBarMessage)))
        scenario.close()
    }

    @Test
    fun launchEndToEndTest() {

        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(ViewActions.typeText(title))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.selectLocation)).perform(click())
        Thread.sleep(6000)
        onView(withId(R.id.map)).perform(ViewActions.longClick())
        onView(withId(R.id.btnConfirm)).perform(click())
        onView(withId(R.id.reminderDescription)).perform(ViewActions.typeText(description))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withText(title)).check(matches(isDisplayed()))
        onView(withText(description)).check(matches(isDisplayed()))
        onView(withText(appContext.getString(R.string.logout))).check(matches(isDisplayed()))

        scenario.close()
//        onView(withId(R.id.login_button)).check(matches(withText(R.string.login_button_text)))
//        onView(withText(R.string.login_button_text)).check(matches(isDisplayed()))
    }

}