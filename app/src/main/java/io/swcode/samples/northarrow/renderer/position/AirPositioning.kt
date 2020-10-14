package io.swcode.samples.northarrow.renderer.position

import com.google.ar.core.Frame
import com.google.ar.core.Pose
import io.swcode.samples.northarrow.renderer.PositioningPose

class AirPositioning(private val x: Float, private val y: Float, private val z: Float) : PositioningPose {

    constructor() : this(0.0f, 0.0f, -2f)

    override fun basePose(frame: Frame): Pose {
        return frame.camera.pose
            .compose(Pose.makeTranslation(x, y, z))
            .extractTranslation()
    }
}