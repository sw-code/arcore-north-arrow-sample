package io.swcode.samples.northarrow.arcore

import android.annotation.SuppressLint
import io.reactivex.rxjava3.core.Observable
import android.app.Activity
import android.hardware.camera2.*
import android.opengl.EGLContext
import android.opengl.Matrix
import android.os.Build
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import arrow.core.orNull
import com.google.android.filament.*
import com.google.ar.core.*
import io.spotar.tour.filament.sample.renderer.createEglContext
import io.spotar.tour.filament.sample.renderer.createExternalTextureId
import io.spotar.tour.filament.sample.renderer.destroyEglContext
import io.swcode.samples.northarrow.filament.FilamentContext
import io.swcode.samples.northarrow.math.*
import kotlin.math.roundToInt

@SuppressLint("MissingPermission")
class ArCore private constructor(private val activity: Activity, private val view: View) {
    companion object {
        private val cameraScreenSpaceVertices: V4A =
            floatArrayOf(
                -1f, +1f, -1f, 1f,
                -1f, -3f, -1f, 1f,
                +3f, +1f, -1f, 1f
            )
                .let { V4A(it) }

        private val arCameraStreamTriangleIndices: ShortArray =
            shortArrayOf(0, 1, 2)

        private val cameraUvs: V2A =
            floatArrayOf(
                0f, 0f,
                2f, 0f,
                0f, 2f
            )
                .let { V2A(it) }

        private const val arCameraStreamPositionBufferIndex: Int = 0
        private const val arCameraStreamUvBufferIndex: Int = 1

        fun arCoreSignal(activity: Activity, view: View): Observable<ArCore> = Observable.create { observableEmitter ->
                val arCore = ArCore(activity, view)
                observableEmitter.onNext(arCore)
                observableEmitter.setCancellable { arCore.destroy() }
            }
    }

    val eglContext: EGLContext = createEglContext().orNull()!!
    private val arCameraStreamTextureId1: Int = createExternalTextureId()

    val session: Session = Session(activity)
        .also { session ->
            session.config
                .apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    focusMode = Config.FocusMode.AUTO
                    depthMode = Config.DepthMode.DISABLED
                    lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    // getting ar frame doesn't block and gives last frame
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }
                .let(session::configure)

            session.setCameraTextureName(arCameraStreamTextureId1)
        }

    private val cameraId: String = session.cameraConfig.cameraId

    private val cameraManager: CameraManager =
        ContextCompat.getSystemService(activity, CameraManager::class.java)!!

    var timestamp: Long = 0L

    @Entity
    private var arCameraStreamRenderable: Int = 0

    @EntityInstance
    var arCameraStreamTransform: Int = 0

    lateinit var frame: Frame
    private lateinit var arCameraStreamMaterialInstance1: MaterialInstance
    private lateinit var cameraDevice: CameraDevice
    private lateinit var arCameraStreamVertexBuffer: VertexBuffer

    fun destroy() {
        session.close()
        cameraDevice.close()
        destroyEglContext(eglContext)
    }

    fun configurationChange(filamentContext: FilamentContext) {
        if (this::frame.isInitialized.not()) return

        val intrinsics = frame.camera.textureIntrinsics
        val dimensions = intrinsics.imageDimensions

        val displayWidth: Int
        val displayHeight: Int
        val displayRotation: Int

        DisplayMetrics()
            .also { displayMetrics ->
                @Suppress("DEPRECATION")
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display
                else activity.windowManager.defaultDisplay)!!
                    .also { display ->
                        display.getRealMetrics(displayMetrics)
                        displayRotation = display.rotation
                    }

                displayWidth = displayMetrics.widthPixels
                displayHeight = displayMetrics.heightPixels
            }

        // camera width and height relative to display
        val cameraWidth: Int
        val cameraHeight: Int

        when (cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!) {
            0, 180 -> when (displayRotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    cameraWidth = dimensions[0]
                    cameraHeight = dimensions[1]
                }
                else -> {
                    cameraWidth = dimensions[1]
                    cameraHeight = dimensions[0]
                }
            }
            else -> when (displayRotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    cameraWidth = dimensions[1]
                    cameraHeight = dimensions[0]
                }
                else -> {
                    cameraWidth = dimensions[0]
                    cameraHeight = dimensions[1]
                }
            }
        }

        val cameraRatio: Float = cameraWidth.toFloat() / cameraHeight.toFloat()
        val displayRatio: Float = displayWidth.toFloat() / displayHeight.toFloat()

        val viewWidth: Int
        val viewHeight: Int

        if (displayRatio < cameraRatio) {
            // width constrained
            viewWidth = displayWidth
            viewHeight = (displayWidth.toFloat() / cameraRatio).roundToInt()
        } else {
            // height constrained
            viewWidth = (displayHeight.toFloat() * cameraRatio).roundToInt()
            viewHeight = displayHeight
        }

        view.updateLayoutParams<ConstraintLayout .LayoutParams> {
            width = viewWidth
            height = viewHeight
        }

        session.setDisplayGeometry(displayRotation, viewWidth, viewHeight)

        arCameraStreamVertexBuffer.setBufferAt(
            filamentContext.engine,
            arCameraStreamUvBufferIndex,
            cameraUvs.floatArray.toFloatBuffer()
        )
    }

    private fun init(filamentContext: FilamentContext) {
        val camera = frame.camera
        val intrinsics = camera.textureIntrinsics
        val dimensions = intrinsics.imageDimensions
        val width = dimensions[0]
        val height = dimensions[1]

        arCameraStreamMaterialInstance1 = activity
            .readUncompressedAsset("materials/unlit.filamat")
            .let { byteBuffer ->
                Material
                    .Builder()
                    .payload(byteBuffer, byteBuffer.remaining())
            }
            .build(filamentContext.engine)
            .createInstance()
            .apply {
                setParameter(
                    "videoTexture",
                    Texture
                        .Builder()
                        .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
                        .format(Texture.InternalFormat.RGB8)
                        .build(filamentContext.engine)
                        .apply {
                            setExternalStream(
                                filamentContext.engine,
                                Stream
                                    .Builder()
                                    .stream(arCameraStreamTextureId1.toLong())
                                    .width(width)
                                    .height(height)
                                    .build(filamentContext.engine)
                            )
                        },
                    TextureSampler(
                        TextureSampler.MinFilter.LINEAR,
                        TextureSampler.MagFilter.LINEAR,
                        TextureSampler.WrapMode.CLAMP_TO_EDGE
                    )
                )
            }

        val cameraIndexBuffer: IndexBuffer = IndexBuffer
            .Builder()
            .indexCount(arCameraStreamTriangleIndices.size)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(filamentContext.engine)
            .apply { setBuffer(filamentContext.engine, arCameraStreamTriangleIndices.toShortBuffer()) }

        arCameraStreamVertexBuffer = VertexBuffer
            .Builder()
            .vertexCount(arCameraStreamTriangleIndices.size)
            .bufferCount(2)
            .attribute(
                VertexBuffer.VertexAttribute.POSITION,
                arCameraStreamPositionBufferIndex,
                VertexBuffer.AttributeType.FLOAT4,
                0,
                0
            )
            .attribute(
                VertexBuffer.VertexAttribute.UV0,
                arCameraStreamUvBufferIndex,
                VertexBuffer.AttributeType.FLOAT2,
                0,
                0
            )
            .build(filamentContext.engine)

        arCameraStreamVertexBuffer.setBufferAt(
            filamentContext.engine,
            arCameraStreamUvBufferIndex,
            cameraUvs.floatArray.toFloatBuffer()
        )

        arCameraStreamRenderable = EntityManager.get().create()

        RenderableManager
            .Builder(1)
            .castShadows(false)
            .receiveShadows(false)
            .culling(false)
            .priority(7) // Always draw the camera feed last to avoid overdraw
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, arCameraStreamVertexBuffer, cameraIndexBuffer)
            .material(0, arCameraStreamMaterialInstance1)
            .build(filamentContext.engine, arCameraStreamRenderable)

        // add to the scene
        filamentContext.scene.addEntity(arCameraStreamRenderable)

        arCameraStreamTransform = filamentContext.engine.transformManager.create(arCameraStreamRenderable)
        configurationChange(filamentContext)
    }

    fun update(frame: Frame, filamentContext: FilamentContext) {
        val firstFrame = this::frame.isInitialized.not()
        this.frame = frame

        if (firstFrame) {
            init(filamentContext)
        }

        val projectionMatrixInv = m4Rotate(-activity.displayRotationDegrees().toFloat(), 0f, 0f, 1f)
            .multiply(frame.projectionMatrix()).invert()

        val cameraVertices = FloatArray(cameraScreenSpaceVertices.floatArray.size)

        for (i in cameraScreenSpaceVertices.floatArray.indices step 4) {
            Matrix.multiplyMV(
                cameraVertices,
                i,
                projectionMatrixInv.floatArray,
                0,
                cameraScreenSpaceVertices.floatArray,
                i
            )
        }

        for (i in cameraScreenSpaceVertices.floatArray.indices step 4) {
            cameraVertices[i + 0] *= cameraVertices[i + 3]
            cameraVertices[i + 1] *= cameraVertices[i + 3]
            cameraVertices[i + 2] *= cameraVertices[i + 3]
            cameraVertices[i + 3] *= 1f
        }

        val vertexBufferData = cameraVertices.toFloatBuffer()

        vertexBufferData.rewind()

        arCameraStreamVertexBuffer
            .setBufferAt(filamentContext.engine, arCameraStreamPositionBufferIndex, vertexBufferData)

        arCameraStreamVertexBuffer
            .setBufferAt(filamentContext.engine, arCameraStreamPositionBufferIndex, vertexBufferData)
    }
}
