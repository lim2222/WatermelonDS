package me.magnum.melonds.ui.emulator.input.componentbuilder

import android.content.Context
import android.view.View
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.layout.LayoutComponent
import me.magnum.melonds.ui.common.LayoutComponentViewBuilder
import me.magnum.melonds.ui.emulator.input.view.ToggleableImageView

class ToggleableSingleButtonLayoutComponentViewBuilder(private val layoutComponent: LayoutComponent) : LayoutComponentViewBuilder() {

    override fun build(context: Context): View {
        return ToggleableImageView(context).apply {
            getToggleableImageViewDrawables()?.let { drawables ->
                setDrawables(drawables.enabled, drawables.enabledPressed, drawables.disabled)
            }
        }
    }

    override fun getAspectRatio() = 1f

    private fun getToggleableImageViewDrawables(): ToggleDrawables? {
        return when (layoutComponent) {
            LayoutComponent.BUTTON_FAST_FORWARD_TOGGLE -> ToggleDrawables(
                R.drawable.button_fast_forward,
                null,
                R.drawable.button_fast_forward_disabled,
            )
            LayoutComponent.BUTTON_MICROPHONE_TOGGLE -> ToggleDrawables(
                R.drawable.button_microphone,
                null,
                R.drawable.button_microphone_disabled,
            )
            LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT -> ToggleDrawables(
                R.drawable.ic_touch_enabled,
                null,
                R.drawable.ic_touch_disabled,
            )
            else -> null
        }
    }

    private data class ToggleDrawables(val enabled: Int, val enabledPressed: Int?, val disabled: Int)
}
