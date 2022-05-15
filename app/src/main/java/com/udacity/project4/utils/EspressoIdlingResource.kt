package com.udacity.project4

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource

object EspressoIdlingResource {
    //TodoApp
    private const val RESOURCE = "GLOBAL"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
}

inline fun <T> wrapEspressoIdlingResource(function: () -> T): T {
    // Espresso does not work well with coroutines yet. See
    // https://github.com/Kotlin/kotlinx.coroutines/issues/982
    EspressoIdlingResource.increment() // Set app as busy.
    return try {
        function()
    } finally {
        EspressoIdlingResource.decrement() // Set app as idle.
    }
}


    //https://stackoverflow.com/a/32023568/
    object ToastManager {
        val idlingResource = CountingIdlingResource("toast")
        private val listener: View.OnAttachStateChangeListener =
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View?) {
                    idlingResource.increment()
                }

                override fun onViewDetachedFromWindow(v: View?) {
                    idlingResource.decrement()
                }
            }

        fun makeText(context: Context?, text: CharSequence?, duration: Int): Toast {
            val t = Toast.makeText(context, text, duration)
            t.view?.addOnAttachStateChangeListener(listener)
            return t
        }

        // For testing
        fun getIdlingResource(): IdlingResource {
            return idlingResource
        }


}