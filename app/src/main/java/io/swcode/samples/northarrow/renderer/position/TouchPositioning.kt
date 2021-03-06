package io.swcode.samples.northarrow.renderer.position

import android.view.MotionEvent
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import io.swcode.samples.northarrow.renderer.PositioningPose

class TouchPositioning(private val motionEvent: MotionEvent) : PositioningPose {
    override fun basePose(frame: Frame): Pose {
        return frame.hitTest(motionEvent)
            .firstOrNull()!!
            .hitPose
    }
}