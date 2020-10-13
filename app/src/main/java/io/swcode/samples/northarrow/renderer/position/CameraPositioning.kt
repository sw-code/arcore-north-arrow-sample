package io.swcode.samples.northarrow.renderer.position

import com.google.ar.core.Frame
import com.google.ar.core.Pose
import io.swcode.samples.northarrow.renderer.RenderablePostionProvider

class CameraPositioning : RenderablePostionProvider {
    override fun pose(frame: Frame): Pose {
        return frame.camera.pose
            .compose(Pose.makeTranslation(0.0f, 0.0f, -2f))
            .extractTranslation()
    }
}