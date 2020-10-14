package io.swcode.samples.northarrow.renderer.node

import com.google.ar.core.Frame
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.swcode.samples.northarrow.arcore.CompassPoseEvent
import io.swcode.samples.northarrow.eventbus.SimpleEventBus
import io.swcode.samples.northarrow.renderer.position.AirPositioning
import io.swcode.samples.northarrow.renderer.position.NorthPositioningPose
import io.swcode.samples.northarrow.renderer.position.TouchPositioning
import io.swcode.samples.northarrow.renderer.renderable.Renderable

class RenderableNodeFactory {
    private val doFrameEvent: PublishSubject<Frame> = PublishSubject.create()
    private val compositeDisposable = CompositeDisposable()

    init {
        SimpleEventBus.listen(Renderable::class.java)
            .filter{r -> r.positioningPose is AirPositioning || r.positioningPose is TouchPositioning}
            .withLatestFrom(doFrameEvent, { rendereable, frame ->
                SimpleEventBus.publish(
                    RenderableNode(
                        rendereable.pose(frame),
                        rendereable
                    )
                )

            })
            .subscribe()
            .let { compositeDisposable.add(it) }

        SimpleEventBus.listen(Renderable::class.java)
            .filter{r -> r.positioningPose is NorthPositioningPose}
            .withLatestFrom(SimpleEventBus.listen(CompassPoseEvent::class.java), { rendereable, composePoseEvent ->

                val compassPose = rendereable.pose(composePoseEvent.frame)
                    .compose(composePoseEvent.pose)

                SimpleEventBus.publish(
                    RenderableNode(
                        compassPose,
                        rendereable
                    )
                )

            })
            .subscribe()
            .let { compositeDisposable.add(it) }

    }

    fun onFrame(frame: Frame) {
        doFrameEvent.onNext(frame)
    }

    fun destroy() {
        compositeDisposable.clear()
    }
}