package io.swcode.samples.northarrow.renderer

import android.content.Context
import com.google.ar.core.Frame
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.swcode.samples.northarrow.eventbus.SimpleEventBus
import io.swcode.samples.northarrow.filament.FilamentContext
import io.swcode.samples.northarrow.renderer.node.RenderableNode

class ModelsRenderer(private val context: Context,
                     private val filamentContext: FilamentContext) {

    private val renderer: MutableList<ModelRenderer> = arrayListOf()
    private val compositeDisposable = CompositeDisposable()

    init {
        SimpleEventBus.listen(RenderableNode::class.java)
            .subscribe {
                renderer.add(ModelRenderer(context, filamentContext, it))
            }
            .let { compositeDisposable.add(it) }
    }

    fun doFrame(frame: Frame) {
        renderer.forEach {
            it.doFrame(frame)
        }
    }

    fun destroy() {
        compositeDisposable.clear()
        renderer.forEach {
            it.destroy()
        }
        renderer.clear()
    }
}