package io.swcode.samples.northarrow.arcore

import androidx.fragment.app.Fragment
import com.google.ar.core.ArCoreApk
import io.reactivex.rxjava3.core.Single
import io.spotar.tour.filament.sample.event.ResumeBehavior
import io.spotar.tour.filament.sample.event.ResumeEvents
import io.spotar.tour.filament.sample.event.exception.OpenGLVersionNotSupportedException
import io.spotar.tour.filament.sample.renderer.checkIfOpenGlVersionSupported
import io.spotar.tour.filament.sample.renderer.minOpenGlVersion
import io.spotar.tour.filament.sample.renderer.showOpenGlNotSupportedDialog

fun <T> T.checkArCore(): Single<Unit> where T : Fragment, T : ResumeBehavior, T : ResumeEvents =
    Single
        .just(Unit)
        .flatMap {
            if (activity!!.checkIfOpenGlVersionSupported(minOpenGlVersion)) {
                Single.just(Unit)
            } else {
                showOpenGlNotSupportedDialog(this.activity!!)
                    .doOnSuccess { activity!!.finish() }
                    .flatMap { throw OpenGLVersionNotSupportedException() }
            }
        }
        .flatMap { resumeBehavior.filter { it }.firstOrError() }
        .flatMap {
            // check if ARCore is installed
            when (ArCoreApk
                .getInstance()
                .requestInstall(
                    activity!!,
                    true,
                    ArCoreApk.InstallBehavior.REQUIRED,
                    ArCoreApk.UserMessageType.USER_ALREADY_INFORMED
                )) {
                ArCoreApk.InstallStatus.INSTALLED -> Single
                    .just(Unit)
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> resumeEvents
                    .firstOrError()
                    .doOnSuccess {
                        // check if installation was successful
                        when (ArCoreApk
                            .getInstance()
                            .requestInstall(
                                activity!!,
                                false,
                                ArCoreApk.InstallBehavior.REQUIRED,
                                ArCoreApk.UserMessageType.USER_ALREADY_INFORMED
                            )) {
                            ArCoreApk.InstallStatus.INSTALLED -> Single
                                .just(this)
                            else ->
                                throw Exception()
                        }
                    }
                else ->
                    throw Exception()
            }
        }
