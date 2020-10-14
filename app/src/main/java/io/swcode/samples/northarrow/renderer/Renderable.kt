package io.swcode.samples.northarrow.renderer

import com.google.ar.core.Frame
import com.google.ar.core.Pose

abstract class Renderable {
    abstract val scaling: Float
    abstract val assetFileName: String
    abstract val positioningPose: PositioningPose
    abstract val basePose: Pose

    fun pose(frame: Frame): Pose {
        return positioningPose.basePose(frame).compose(basePose)
    }
}