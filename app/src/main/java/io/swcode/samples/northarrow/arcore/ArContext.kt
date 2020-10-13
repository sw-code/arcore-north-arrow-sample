package io.swcode.samples.northarrow.arcore

import io.swcode.samples.northarrow.filament.FilamentContext
import io.swcode.samples.northarrow.renderer.*

data class ArContext(
    val arCore: ArCore,
    val filamentContext: FilamentContext,
    val cameraRenderer: CameraRenderer,
    val lightRenderer: LightRenderer,
    val planeRenderer: PlaneRenderer,
    val modelsRenderer: ModelsRenderer,
    val frameCallback: FrameCallback
)