package io.swcode.samples.northarrow.arcore

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import io.swcode.samples.northarrow.eventbus.SimpleEventBus
import io.swcode.samples.northarrow.filament.FilamentContext
import io.swcode.samples.northarrow.location.LiveLocationService
import io.swcode.samples.northarrow.orientation.OrientationService
import io.swcode.samples.northarrow.renderer.*
import io.swcode.samples.northarrow.renderer.position.CameraPositioning
import io.swcode.samples.northarrow.renderer.position.TouchPositioning
import kotlinx.android.synthetic.main.fragment_arcore.*
import java.util.concurrent.TimeUnit

class ArCoreFragment : Fragment(),
    ConfigurationChangedEvents, ResumeEvents, ResumeBehavior {

    override val resumeBehavior: BehaviorSubject<Boolean> =
        BehaviorSubject.create()

    override val resumeEvents: PublishSubject<Unit> =
        PublishSubject.create()

    private val onCreateDisposable: CompositeDisposable =
        CompositeDisposable()

    private val arTrackingEvents: PublishSubject<Unit> =
        PublishSubject.create()

    private val arContextSignal: BehaviorSubject<ArContext> =
        BehaviorSubject.create()

    override val configurationChangedEvents: PublishSubject<Configuration> =
        PublishSubject.create()

    private val onStartDisposable: CompositeDisposable = CompositeDisposable()

    private val onResumeDisposable: CompositeDisposable = CompositeDisposable()

    private lateinit var orientationService: OrientationService

    private lateinit var locationService: LiveLocationService

    private lateinit var arSensorService: ArSensorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_arcore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resumeBehavior
            .filter { it }
            .firstOrError()
            .flatMap { checkArCore() }
            .flatMapObservable { ArCore.arCoreSignal(requireActivity(), textureView) }
            .flatMap { arCore ->
                Observable.create<ArContext> { observableEmitter ->
                    val filamentService = FilamentContext(requireContext(), arCore, textureView)

                    val cameraRenderer = CameraRenderer(filamentService, arCore)
                    val lightRenderer = LightRenderer(filamentService)
                    val planeRenderer = PlaneRenderer(requireContext(), filamentService)
                    val modelsRenderer = ModelsRenderer(requireContext(), arCore, filamentService)
                    val renderableNodeFactory = RenderableNodeFactory()

                    val frameCallback = FrameCallback(
                        arCore,
                        filamentService,
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
                        filamentService,
                        cameraRenderer,
                        lightRenderer,
                        planeRenderer,
                        modelsRenderer,
                        frameCallback
                    )
                        .let { observableEmitter.onNext(it) }

                    observableEmitter.setCancellable {
                        modelsRenderer.destroy()
                        filamentService.destroy()
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

        addBehindCameraButton.setOnClickListener {
            SimpleEventBus.publish(Arrow(CameraPositioning()))
        }


        textureView.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP &&
                (motionEvent.eventTime - motionEvent.downTime) <
                resources.getInteger(R.integer.tap_event_milliseconds)
            ) {
                SimpleEventBus.publish(Arrow(TouchPositioning(motionEvent)))
            }
            true
        }
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

        arContextSignal
            .flatMap {
                Observable
                    .amb(listOf(
                        Observable
                            .concat(
                                Observable
                                    .just(true)
                                    .delay(
                                        resources
                                            .getInteger(R.integer.show_hand_motion_timeout_seconds)
                                            .toLong(),
                                        TimeUnit.SECONDS
                                    ),
                                arTrackingEvents
                                    .take(1)
                                    .map { false }
                            ),
                        arTrackingEvents
                            .take(1)
                            .map { false }
                    ))
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnDispose { handMotionContainer.isVisible = false }
            .subscribe({ handMotionContainer.isVisible = it }, { errorHandler(it) })
            .let { onStartDisposable.add(it) }
    }

    override fun onStop() {
        super.onStop()
//        onSystemUiFlagHideNavigationDisposable.clear()
        onStartDisposable.clear()
    }

    override fun onResume() {
        super.onResume()
        orientationService.onResume()
        arSensorService.onResume()
//        onResumeAutoHideSystemUi()
        resumeEvents.onNext(Unit)
        resumeBehavior.onNext(true)

        SimpleEventBus.listen(Orientation::class.java)
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe { updateDebugView(it) }
            .let {
                onResumeDisposable.add(it)
            }

        Observable.combineLatest(arTrackingEvents, arContextSignal, {
                _, contextSignal ->
            Log.i("ArCoreFragment", "onResume")
            addBehindCameraButton.visibility = View.VISIBLE

        }).take(1)
            .subscribe()
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

    private fun errorHandler(error: Throwable) {
        if(isAdded) {
            requireActivity().finish()
        }
        if (error is UserCanceledException) {
            return
        }
        error.printStackTrace()
    }
}
