package io.swcode.samples.northarrow.renderer.renderable

import io.swcode.samples.northarrow.renderer.PositioningPose

data class Dancer(override val positioningPose: PositioningPose) : Renderable() {
    override val scaling: Float
        get() = 1.0f
    override val assetFileName: String
        get() = "eren-hiphop-dance.glb"
}