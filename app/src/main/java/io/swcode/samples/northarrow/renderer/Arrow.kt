package io.swcode.samples.northarrow.renderer

import com.google.ar.core.Pose
import io.swcode.samples.northarrow.math.Quaternion
import io.swcode.samples.northarrow.math.Vector3

class Arrow(override val positioningPose: PositioningPose) : Renderable() {
    override val scaling: Float
        get() = 0.04f
    override val assetFileName: String
        get() = "st_naviArrow.glb"
    override val basePose: Pose
        get() {
            val baseRotation = Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), -90.0f)
            return Pose.makeRotation(baseRotation.x, baseRotation.y, baseRotation.z, baseRotation.w)
        }
}