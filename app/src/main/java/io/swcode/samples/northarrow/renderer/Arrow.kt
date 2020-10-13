package io.swcode.samples.northarrow.renderer

class Arrow(override val renderablePostionProvider: RenderablePostionProvider) : Renderable() {
    override val assetFileName: String
        get() = "st_naviArrow.glb"
}