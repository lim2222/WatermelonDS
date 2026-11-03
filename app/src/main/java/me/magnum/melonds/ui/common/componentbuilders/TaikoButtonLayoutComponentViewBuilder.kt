package me.magnum.melonds.ui.common.componentbuilders

import android.content.Context
import android.view.View
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.layout.LayoutComponent
import me.magnum.melonds.ui.common.LayoutComponentViewBuilder
import me.magnum.melonds.ui.emulator.input.view.TaikoButtonView

class TaikoButtonLayoutComponentViewBuilder : LayoutComponentViewBuilder() {
    override fun build(context: Context): View {
        return TaikoButtonView(context).apply {
            normalDrawableId = R.drawable.taiko
            pressedDrawableId = R.drawable.taiko_pressed
            setImageResource(R.drawable.taiko)
        }
    }

    override fun getAspectRatio() = 1f
}
