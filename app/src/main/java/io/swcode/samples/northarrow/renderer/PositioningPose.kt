package io.swcode.samples.northarrow.renderer

import com.google.ar.core.Frame
import com.google.ar.core.Pose

interface PositioningPose {
    fun basePose(frame: Frame) : Pose
}