package io.swcode.samples.northarrow.renderer

import android.content.Context
import android.util.Log
import com.google.android.filament.gltfio.FilamentAsset
import com.google.ar.core.Frame
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.swcode.samples.northarrow.arcore.ArCore
import io.swcode.samples.northarrow.filament.FilamentContext
import io.swcode.samples.northarrow.math.m4Identity
import io.swcode.samples.northarrow.math.scale
import io.swcode.samples.northarrow.math.translate
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class ModelRenderer(
    context: Context,
    private val arCore: ArCore,
    private val filamentContext: FilamentContext,
    private val renderableNode: RenderableNode
) {

    private val doFrameEvent: PublishSubject<Frame> = PublishSubject.create()
    private val compositeDisposable = CompositeDisposable()

    private var scaling: Float = 0.04f

    init {
        // update filament
        Log.i("ModelRenderer", "Init")
        Single
            .create<FilamentAsset> { singleEmitter ->
                Log.i("ModelRenderer", "Load file")
                context.assets
                    .open("st_naviArrow.glb")
                    .use { input ->
                        val bytes = ByteArray(input.available())
                        input.read(bytes)

                        filamentContext.assetLoader
                            .createAssetFromBinary(ByteBuffer.wrap(bytes))!!
                            .let { singleEmitter.onSuccess(it) }
                    }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { filamentContext.resourceLoader.loadResources(it) }
            .flatMapObservable { filamentAsset ->
                Observable.merge(
                    doFrameEvent.map { frame ->
                        // update animator
                        val animator = filamentAsset.animator
                        if (animator.animationCount > 0) {
                            animator.applyAnimation(
                                0,
                                (frame.timestamp /
                                        TimeUnit.SECONDS.toNanos(1).toDouble())
                                    .toFloat() %
                                        animator.getAnimationDuration(0)
                            )

                            animator.updateBoneMatrices()
                        }
                        Unit
                    },
                    Observable.create {
                        val pose = renderableNode.pose
//                        val pose = arCore.frame.camera.pose
//                            .compose(Pose.makeTranslation(0.1f, 0.0f, -2f))
//                            .extractTranslation()

                        val localTransform = m4Identity()
                            .translate(pose.tx(), pose.ty(), pose.tz())
                            .scale(scaling, scaling, scaling)
                            .floatArray
                        filamentContext.applyOnFrame(filamentAsset, localTransform)
                    }
                )
            }
            .subscribe({}, {})
            .also { compositeDisposable.add(it) }
    }

    fun destroy() {
        compositeDisposable.dispose()
    }

    fun doFrame(frame: Frame) {
        doFrameEvent.onNext(frame)
    }
}