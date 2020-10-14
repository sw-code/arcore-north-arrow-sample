package io.swcode.samples.northarrow.renderer.renderable

import com.google.ar.core.Frame
import com.google.ar.core.Pose
import io.swcode.samples.northarrow.renderer.PositioningPose

abstract class Renderable {
    abstract val scaling: Float
    abstract val assetFileName: String

    /**
     * PositionPose ob the object
     */
    abstract val positioningPose: PositioningPose

    /**
     * Base pose of the object, e.g. if the initial rotation does not fit you requirement
     */
    open val basePose: Pose = Pose.IDENTITY

    fun pose(frame: Frame): Pose {
        return positioningPose.basePose(frame).compose(basePose)
    }
}