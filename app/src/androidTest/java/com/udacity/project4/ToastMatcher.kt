package com.udacity.project4

import androidx.test.espresso.Root
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher


//Author: http://www.qaautomated.com/2016/01/how-to-test-toast-message-using-espresso.html

class ToastMatcher : TypeSafeMatcher<Root>() {
    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }

    public override fun matchesSafely(root: Root): Boolean {
        val windowToken = root.decorView.windowToken
        val appToken = root.decorView.applicationWindowToken
        if (windowToken === appToken) {
            //means this window isn't contained by any other windows.
            return true
        }
        return false
    }
}

////https://github.com/lightningnik/AndroidCheat
