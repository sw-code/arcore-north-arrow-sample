package io.swcode.samples.northarrow.renderer

import com.google.android.filament.EntityInstance
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.ar.core.Frame
import com.google.ar.core.LightEstimate
import io.swcode.samples.northarrow.filament.FilamentContext
import io.swcode.samples.northarrow.math.V3
import io.swcode.samples.northarrow.math.div
import io.swcode.samples.northarrow.math.getEnvironmentalHdrSphericalHarmonics
import kotlin.math.max

class LightRenderer(private val filamentContext: FilamentContext) {
    @EntityInstance
    private var directionalLightInstance: Int = EntityManager
        .get()
        .create()
        .let { directionalLight ->
            filamentContext.scene.addEntity(directionalLight)

            LightManager
                .Builder(LightManager.Type.DIRECTIONAL)
                .castShadows(true)
                .build(filamentContext.engine, directionalLight)

            filamentContext.engine.lightManager.getInstance(directionalLight)
        }

    fun doFrame(frame: Frame) {
        // update lighting estimate
        if (frame.lightEstimate.state != LightEstimate.State.VALID) {
            return
        }

        filamentContext.scene.indirectLight = IndirectLight
            .Builder()
            .irradiance(
                3,
                frame.lightEstimate.environmentalHdrAmbientSphericalHarmonics
                    .let(::getEnvironmentalHdrSphericalHarmonics)
            )
            .build(filamentContext.engine)

        with(frame.lightEstimate.environmentalHdrMainLightDirection) {
            filamentContext.engine.lightManager.setDirection(
                directionalLightInstance,
                -get(0),
                -get(1),
                -get(2)
            )
        }

        with(frame.lightEstimate.environmentalHdrMainLightIntensity) {
            // Scale hdr rgb values to fit in range [0, 1).
            // There may be a better way to do this conversion.
            val rgbMax = max(max(get(0), get(1)), get(2))
            // prevent div by zero
            val color = V3(this).div(max(0.00001f, rgbMax))

            filamentContext.engine.lightManager.setColor(
                directionalLightInstance,
                color.floatArray[0],
                color.floatArray[1],
                color.floatArray[2]
            )
        }
    }
}
