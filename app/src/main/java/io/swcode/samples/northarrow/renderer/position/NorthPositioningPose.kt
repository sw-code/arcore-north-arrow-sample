package io.swcode.samples.northarrow.renderer.position

import com.google.ar.core.Frame
import com.google.ar.core.Pose
import io.swcode.samples.northarrow.renderer.PositioningPose

class NorthPositioningPose : PositioningPose {

    private val airPositioning = AirPositioning()

    override fun basePose(frame: Frame): Pose {
        return airPositioning.basePose(frame)
    }
}