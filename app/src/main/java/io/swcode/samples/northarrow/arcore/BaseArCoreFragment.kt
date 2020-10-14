package io.swcode.samples.northarrow.arcore

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.spotar.tour.filament.sample.event.ConfigurationChangedEvents
import io.spotar.tour.filament.sample.event.ResumeBehavior
import io.spotar.tour.filament.sample.event.ResumeEvents
import io.spotar.tour.filament.sample.event.exception.UserCanceledException
import io.spotar.tour.filament.sample.orientation.Orientation
import io.swcode.samples.northarrow.R
import io.swcode.samples.northarrow.event.TrackingStateEvent
import io.swcode.samples.northarrow.eventbus.SimpleEventBus
import io.swcode.samples.northarrow.filament.FilamentContext
import io.swcode.samples.northarrow.location.LiveLocationService
import io.swcode.samples.northarrow.orientation.OrientationService
import io.swcode.samples.northarrow.renderer.*
import io.swcode.samples.northarrow.renderer.node.RenderableNodeFactory
import kotlinx.android.synthetic.main.fragment_arcore.*
import java.util.concurrent.TimeUnit

abstract class BaseArCoreFragment : Fragment(),
    ConfigurationChangedEvents, ResumeEvents, ResumeBehavior, AutoHideSystemUi {

    override val onSystemUiFlagHideNavigationDisposable: CompositeDisposable =
        CompositeDisposable()

    override val resumeBehavior: BehaviorSubject<Boolean> =
        BehaviorSubject.create()

    override val resumeEvents: PublishSubject<Unit> =
        PublishSubject.create()

    private val onCreateDisposable: CompositeDisposable =
        CompositeDisposable()

    private val arTrackingEvents: PublishSubject<Unit> =
        PublishSubject.create()

    protected val trackingStateEvents: PublishSubject<TrackingStateEvent> = PublishSubject.create()

    private val arContextSignal: BehaviorSubject<ArContext> =
        BehaviorSubject.create()

    override val configurationChangedEvents: PublishSubject<Configuration> =
        PublishSubject.create()

    private val onStartDisposable: CompositeDisposable = CompositeDisposable()

    protected val onResumeDisposable: CompositeDisposable = CompositeDisposable()

    private lateinit var orientationService: OrientationService

    private lateinit var locationService: LiveLocationService

    private lateinit var arSensorService: ArSensorService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCreateAutoHideSystemUi(requireActivity())

        resumeBehavior
            .filter { it }
            .firstOrError()
            .flatMap { checkArCore() }
            .flatMapObservable { ArCore.arCoreSignal(requireActivity(), textureView) }
            .flatMap { arCore ->
                Observable.create<ArContext> { observableEmitter ->
                    val filamentContext = FilamentContext(requireContext(), arCore, textureView)

                    val cameraRenderer = CameraRenderer(filamentContext, arCore)
                    val lightRenderer = LightRenderer(filamentContext)
                    val planeRenderer = PlaneRenderer(requireContext(), filamentContext)
                    val modelsRenderer = ModelsRenderer(requireContext(), filamentContext)
                    val renderableNodeFactory = RenderableNodeFactory()

                    val frameCallback = FrameCallback(
                        arCore,
                        filamentContext,
                        doFrame = { frame ->
                            arSensorService.onUpdate(frame)
                            if (frame.getUpdatedTrackables(Plane::class.java)
                                    .any { it.trackingState == TrackingState.TRACKING }
                            ) {
                                arTrackingEvents.onNext(Unit)
                            }

                            renderableNodeFactory.onFrame(frame)
                            cameraRenderer.doFrame(frame)
                            lightRenderer.doFrame(frame)
                            planeRenderer.doFrame(frame)
                            modelsRenderer.doFrame(frame)
                        }
                    )

                    ArContext(
                        arCore,
                        filamentContext,
                        cameraRenderer,
                        lightRenderer,
                        planeRenderer,
                        modelsRenderer,
                        frameCallback
                    )
                        .let { observableEmitter.onNext(it) }

                    observableEmitter.setCancellable {
                        modelsRenderer.destroy()
                        filamentContext.destroy()
                        renderableNodeFactory.destroy()
                    }
                }
                    .subscribeOn(AndroidSchedulers.mainThread())
            }
            .subscribe({ arContextSignal.onNext(it) }, { errorHandler(it) })
            .let { onCreateDisposable.add(it) }

        arContextSignal
            .firstOrError()
            .flatMapObservable { arContext ->
                configurationChangedEvents.map { Pair(arContext.arCore, arContext.filamentContext) }
            }
            .subscribe(
                { (arCore, filament) -> arCore.configurationChange(filament) },
                { errorHandler(it) }
            )
            .let { onCreateDisposable.add(it) }

        arSensorService = ArSensorService(requireContext())
        locationService = LiveLocationService(requireContext())
        orientationService = OrientationService(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onCreateDisposable.clear()
    }

    override fun onStart() {
        super.onStart()
        arContextSignal
            .flatMap { arContext ->
                Observable
                    .create<Nothing> { observableEmitter ->
                        arContext.arCore.session.resume()
                        arContext.frameCallback.start()

                        observableEmitter.setCancellable {
                            arContext.frameCallback.stop()
                            arContext.arCore.session.pause()
                        }
                    }
            }
            .subscribe({}, { errorHandler(it) })
            .let { onStartDisposable.add(it) }

        setupTrackingState()
    }

    override fun onStop() {
        super.onStop()
        onSystemUiFlagHideNavigationDisposable.clear()
        onStartDisposable.clear()
    }

    override fun onResume() {
        super.onResume()
        orientationService.onResume()
        arSensorService.onResume()

        onResumeAutoHideSystemUi(requireActivity())
        resumeEvents.onNext(Unit)
        resumeBehavior.onNext(true)

        SimpleEventBus.listen(Orientation::class.java)
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe { updateDebugView(it) }
            .let {
                onResumeDisposable.add(it)
            }
    }

    private fun updateDebugView(orientation: Orientation) {
        azimuthTextView.text = "azimuth: ${orientation.azimuth}"
    }

    override fun onPause() {
        super.onPause()
        arSensorService.onPause()
        orientationService.onPause()
        resumeBehavior.onNext(false)
        onResumeDisposable.clear()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChangedEvents.onNext(newConfig)
    }


    private fun setupTrackingState() {
        // TrackingState event is fired as soon as the first arTracking event arrives (TrackingState.tracking=true)
        // or if there is no arTracking event for 2 seconds (TrackingState.tracking=false)
        arContextSignal
            .flatMap {
                Observable
                    .amb(listOf(
                        Observable
                            .concat(
                                Observable
                                    .just(false)
                                    .delay(
                                        resources
                                            .getInteger(R.integer.show_hand_motion_timeout_seconds)
                                            .toLong(),
                                        TimeUnit.SECONDS
                                    ),
                                arTrackingEvents
                                    .take(1)
                                    .map { true }
                            ),
                        arTrackingEvents
                            .take(1)
                            .map { true }
                    ))
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnDispose {
                trackingStateEvents.onNext(TrackingStateEvent(false))
            }
            .subscribe { trackingStateEvents.onNext(TrackingStateEvent(it)) }
            .let { onStartDisposable.add(it) }

        trackingStateEvents.subscribe {
            handMotionContainer.isVisible = !it.tracking
        }.let { onStartDisposable.add(it) }
    }

    private fun errorHandler(error: Throwable) {
        if (isAdded) {
            requireActivity().finish()
        }
        if (error is UserCanceledException) {
            return
        }
        error.printStackTrace()
    }
}
