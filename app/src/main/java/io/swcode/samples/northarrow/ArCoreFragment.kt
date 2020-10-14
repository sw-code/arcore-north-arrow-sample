package io.swcode.samples.northarrow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.swcode.samples.northarrow.arcore.BaseArCoreFragment
import io.swcode.samples.northarrow.event.TrackingStateEvent
import io.swcode.samples.northarrow.eventbus.SimpleEventBus
import io.swcode.samples.northarrow.renderer.renderable.Arrow
import io.swcode.samples.northarrow.renderer.position.AirPositioning
import io.swcode.samples.northarrow.renderer.position.NorthPositioningPose
import io.swcode.samples.northarrow.renderer.position.TouchPositioning
import io.swcode.samples.northarrow.renderer.renderable.Dancer
import kotlinx.android.synthetic.main.fragment_arcore.*

class ArCoreFragment : BaseArCoreFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_arcore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addBehindCameraButton.setOnClickListener {
            SimpleEventBus.publish(Arrow(AirPositioning()))
        }

        addCompassButton.setOnClickListener {
            SimpleEventBus.publish(Arrow(NorthPositioningPose()))
        }
    }

    override fun onResume() {
        super.onResume()
        trackingStateEvents.subscribe {
            setupUiComponents(it)
        }
            .let { onResumeDisposable.add(it) }
    }

    private fun setupUiComponents(trackingStateEvent: TrackingStateEvent) {
        addCompassButton.isVisible = trackingStateEvent.tracking
        addBehindCameraButton.isVisible = trackingStateEvent.tracking

        if (trackingStateEvent.tracking) {
            textureView.setOnTouchListener { _, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP &&
                    (motionEvent.eventTime - motionEvent.downTime) <
                    resources.getInteger(R.integer.tap_event_milliseconds)
                ) {
                    SimpleEventBus.publish(Dancer(TouchPositioning(motionEvent)))
                }
                true
            }
        } else {
            textureView.setOnTouchListener(null)
        }
    }
}