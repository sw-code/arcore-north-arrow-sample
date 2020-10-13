package io.swcode.samples.northarrow.renderer

import com.google.ar.core.Frame
import com.google.ar.core.Pose

interface RenderablePostionProvider {
    fun pose(frame: Frame) : Pose
}