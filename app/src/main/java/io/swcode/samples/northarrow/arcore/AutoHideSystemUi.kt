package io.swcode.samples.northarrow.arcore

import android.app.Activity
import android.view.View
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.swcode.samples.northarrow.R
import java.util.concurrent.TimeUnit

interface AutoHideSystemUi {
    val onSystemUiFlagHideNavigationDisposable: CompositeDisposable
}

fun <T> T.onCreateAutoHideSystemUi(activity: Activity) where T : AutoHideSystemUi {
    @Suppress("DEPRECATION")
    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE

    @Suppress("DEPRECATION")
    activity.window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
        // immersive mode will not automatically hide the navigation bar after being revealed
        if (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0) {
            onResumeAutoHideSystemUi(activity)
        }
    }
}

fun <T> T.onResumeAutoHideSystemUi(activity: Activity) where T : AutoHideSystemUi {
    @Suppress("DEPRECATION")
    if (activity.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0) {
        onSystemUiFlagHideNavigationDisposable.clear()

        Single.just(Unit)
            .delay(
                activity.resources
                    .getInteger(R.integer.hide_system_navigation_timeout_seconds)
                    .toLong(),
                TimeUnit.SECONDS
            )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    activity.window.decorView.systemUiVisibility =
                        activity.window.decorView.systemUiVisibility or
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                },
                {}
            )
            // Do not remove: Kotlin has a bug where it casts Boolean result of previous statement to
            // Unit
            .let { onSystemUiFlagHideNavigationDisposable.add(it); Unit }
    }
}
