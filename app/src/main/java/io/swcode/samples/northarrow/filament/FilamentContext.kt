package io.swcode.samples.northarrow.filament

import android.content.Context
import android.view.Surface
import android.view.TextureView
import com.google.android.filament.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.ResourceLoader
import io.swcode.samples.northarrow.arcore.ArCore
import io.swcode.samples.northarrow.math.M4
import io.swcode.samples.northarrow.math.toDoubleArray

class FilamentContext(context: Context, arCore: ArCore, val textureView: TextureView) {
    companion object {
        const val near = 0.1f
        const val far = 30f
    }

    var timestamp: Long = 0L
    val engine: Engine = Engine.create(arCore.eglContext)
    val renderer: Renderer = engine.createRenderer().apply { clearOptions }
    val scene: Scene = engine.createScene()

    private val camera: Camera = engine
        .createCamera()
        .also { camera ->
            // Set the exposure on the camera, this exposure follows the sunny f/16 rule
            // Since we've defined a light that has the same intensity as the sun, it
            // guarantees a proper exposure
            camera.setExposure(16f, 1f / 125f, 100f)
        }

    val view: View = engine
        .createView()
        .also { view ->
            view.camera = camera
            view.scene = scene
        }

    val assetLoader =
        AssetLoader(engine, MaterialProvider(engine), EntityManager.get())

    val resourceLoader =
        ResourceLoader(engine)

    var swapChain: SwapChain? = null
    val displayHelper = DisplayHelper(context)

    val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
        renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
                displayHelper.attach(renderer, textureView.display)
            }

            override fun onDetachedFromSurface() {
                displayHelper.detach()
                swapChain?.let {
                    engine.destroySwapChain(it)
                    // Required to ensure we don't return before Filament is done executing the
                    // destroySwapChain command, otherwise Android might destroy the Surface
                    // too early
                    engine.flushAndWait()
                    swapChain = null
                }
            }

            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
            }
        }

        attachTo(textureView)
    }

    fun destroy() {
        // Always detach the surface before destroying the engine
        uiHelper.detach()
        engine.destroy()
    }

    fun setProjectionMatrix(projectionMatrix: M4) {
        camera.setCustomProjection(
            projectionMatrix.floatArray.toDoubleArray(),
            near.toDouble(),
            far.toDouble()
        )
    }

    fun setCameraMatrix(@EntityInstance transform: Int, cameraMatrix: M4) {
        camera.setModelMatrix(cameraMatrix.floatArray)
        engine.transformManager.setTransform(transform, cameraMatrix.floatArray)
    }

    fun applyOnFrame(filamentAsset: FilamentAsset, floatArray: FloatArray) {
        scene.addEntities(filamentAsset.entities)
        engine.transformManager.setTransform(engine.transformManager.getInstance(filamentAsset.root), floatArray)
    }
}
