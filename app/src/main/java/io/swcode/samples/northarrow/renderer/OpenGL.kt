package io.spotar.tour.filament.sample.renderer

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.opengl.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import arrow.core.Either
import io.reactivex.rxjava3.core.Single
import io.swcode.samples.northarrow.R
import io.swcode.samples.northarrow.util.Version
import io.swcode.samples.northarrow.util.parse
import io.swcode.samples.northarrow.util.parserVersion
import io.swcode.samples.northarrow.util.versionComparator

val minOpenGlVersion = Version(3, 0, 0, null, null)

fun createEglContext(): Either<Exception, EGLContext> {
    val eglOpenGlEs3Bit = 0x40
    val display: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    EGL14.eglInitialize(display, null, 0, null, 0)
    val configs: Array<EGLConfig?> = arrayOfNulls(1)

    EGL14.eglChooseConfig(
        display,
        intArrayOf(EGL14.EGL_RENDERABLE_TYPE, eglOpenGlEs3Bit, EGL14.EGL_NONE),
        0,
        configs,
        0,
        1,
        intArrayOf(0),
        0
    )

    val context: EGLContext =
        EGL14.eglCreateContext(
            display,
            configs[0],
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE),
            0
        )

    val surface: EGLSurface =
        EGL14.eglCreatePbufferSurface(
            display,
            configs[0],
            intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
            0
        )

    return if (EGL14.eglMakeCurrent(display, surface, surface, context)) {
        Either.right(context)
    } else {
        Either.left(Exception("Error creating EGL Context"))
    }
}

fun createExternalTextureId(): Int = IntArray(1)
    .apply { GLES30.glGenTextures(1, this, 0) }
    .first()
    .apply { GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, this) }

fun destroyEglContext(context: EGLContext): Either<Exception, Unit> =
    if (EGL14.eglDestroyContext(EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY), context)
    ) {
        Either.right(Unit)
    } else {
        Either.left(Exception("Error destroying EGL context"))
    }

fun Context.checkIfOpenGlVersionSupported(minOpenGlVersion: Version): Boolean =
    versionComparator.compare(
        minOpenGlVersion,
        ContextCompat
            .getSystemService(this, ActivityManager::class.java)!!
            .deviceConfigurationInfo
            .glEsVersion
            .let { parserVersion.parse(it) }
    ) <= 0

fun showOpenGlNotSupportedDialog(activity: Activity): Single<Unit> = Single
    .create { singleEmitter ->
        val alertDialog = AlertDialog
            .Builder(activity)
            .setTitle(R.string.opengl_required_title)
            .setMessage(
                activity.getString(
                    R.string.opengl_required_message,
                    minOpenGlVersion.print()
                )
            )
            .setPositiveButton(android.R.string.ok) { _, _ -> singleEmitter.onSuccess(Unit) }
            .setCancelable(false)
            .show()

        singleEmitter.setCancellable { alertDialog.dismiss() }
    }
