package me.magnum.melonds.ui.common.componentbuilders

import android.content.Context
import android.view.View
import me.magnum.melonds.R
import me.magnum.melonds.common.buttonpack.OverlayImages
import me.magnum.melonds.ui.common.LayoutComponentViewBuilder
import me.magnum.melonds.ui.emulator.input.view.TaikoButtonView

class TaikoButtonLayoutComponentViewBuilder : LayoutComponentViewBuilder() {
    override fun build(context: Context): View {
        return TaikoButtonView(context).apply {
            val images = OverlayImages.buttonImages(context, R.drawable.taiko, R.drawable.taiko_pressed)
            setImages(images.normal, images.pressed)
        }
    }

    override fun getAspectRatio() = 1f
}
