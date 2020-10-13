package io.swcode.samples.northarrow.renderer

import com.google.ar.core.Frame
import com.google.ar.core.Pose

abstract class Renderable {
    abstract val assetFileName: String
    protected abstract val renderablePostionProvider: RenderablePostionProvider

    fun pose(frame: Frame): Pose {
        return renderablePostionProvider.pose(frame);
    }
}