package io.swcode.samples.northarrow.renderer

import com.google.ar.core.Frame
import io.swcode.samples.northarrow.arcore.ArCore
import io.swcode.samples.northarrow.filament.FilamentContext
import io.swcode.samples.northarrow.math.matrix
import io.swcode.samples.northarrow.math.projectionMatrix

class CameraRenderer(private val filamentContext: FilamentContext, private val arCore: ArCore) {
    fun doFrame(frame: Frame) {
        // update camera
        filamentContext.setProjectionMatrix(frame.projectionMatrix())

        filamentContext.setCameraMatrix(
            arCore.arCameraStreamTransform,
            frame.camera.displayOrientedPose.matrix()
        )
    }
}
