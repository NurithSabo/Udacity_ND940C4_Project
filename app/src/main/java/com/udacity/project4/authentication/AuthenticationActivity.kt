package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import kotlinx.android.synthetic.main.activity_authentication.*
import kotlinx.android.synthetic.main.fragment_reminders.*


/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    lateinit var login: TextView
    private val viewModel by viewModels<LoginViewModel>()

    companion object {
        const val TAG = "AuthenticationActivity"
        const val SIGN_IN_RESULT_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        //ori

        observeAuthenticationState()
        login = findViewById(R.id.login_button)
        login.setOnClickListener {
            launchSignInFlow()
        }
//         _TODO: 1. Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google
//         _TODO: 2. If the user was authenticated, send him to RemindersActivity
//         _TODO: 3. a bonus is to customize the sign in flow to look nice using :
//         https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout
    }

    //Lesson 6
    private fun observeAuthenticationState() {

        viewModel.authenticationState.observe(this, Observer { authenticationState ->
            // Use the authenticationState variable you just added
            // in LoginViewModel and change the UI accordingly.
            when (authenticationState) {
                //If the user is logged in,
                // you can customize the welcome message they see by
                // utilizing the getFactWithPersonalization() function provided
                LoginViewModel.AuthenticationState.AUTHENTICATED -> {
                    login.text =
                        getString(R.string.logout_text) + FirebaseAuth.getInstance().currentUser?.displayName
                    login.setOnClickListener {
                        AuthUI.getInstance().signOut(this)
                    }
                    val reminderActivityIntent =
                        Intent(applicationContext, RemindersActivity::class.java)

                    Toast.makeText(this, "You're in!", Toast.LENGTH_LONG).show()

                    startActivity(reminderActivityIntent)
                    finish()
                }
                else -> {
                    // Lastly, if there is no logged-in user,
                    // auth_button should display Login and
                    // launch the sign in screen when clicked.
                    //binding.welcomeText.text = factToDisplay

                    login.text = getString(R.string.login_button_text)
                    login.setOnClickListener {
                        launchSignInFlow()
                    }
                }
            }
        })
    }

//Lesson 6
    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email or Google account.
        // If users choose to register with their email,
        // they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()

            // This is where you can provide more ways for users to register and
            // sign in.
        )

    val customLayout = AuthMethodPickerLayout.Builder(R.layout.firebase_login)
        .setGoogleButtonId(R.id.google_button)
        .setEmailButtonId(R.id.email_button)
        .build()
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setAuthMethodPickerLayout(customLayout)
                .build(),
            SIGN_IN_RESULT_CODE
        )
    }
}
