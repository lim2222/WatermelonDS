package me.magnum.melonds.ui.common.componentbuilders

import android.content.Context
import me.magnum.melonds.ui.common.LayoutComponentViewBuilder
import me.magnum.melonds.ui.emulator.input.view.TataconView

class TataconLayoutComponentViewBuilder : LayoutComponentViewBuilder() {
    override fun build(context: Context) = TataconView(context)

    // The drum artwork is wider than it is tall
    override fun getAspectRatio() = 221f / 223f
}
