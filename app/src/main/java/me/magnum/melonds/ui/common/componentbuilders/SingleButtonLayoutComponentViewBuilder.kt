package me.magnum.melonds.ui.common.componentbuilders

import android.content.Context
import android.view.View
import me.magnum.melonds.R
import me.magnum.melonds.common.buttonpack.OverlayImages
import me.magnum.melonds.domain.model.layout.LayoutComponent
import me.magnum.melonds.ui.common.LayoutComponentViewBuilder
import me.magnum.melonds.ui.emulator.input.view.PressableButtonImageView

class SingleButtonLayoutComponentViewBuilder(private val layoutComponent: LayoutComponent) : LayoutComponentViewBuilder() {
    override fun build(context: Context): View {
        val (normalDrawableId, pressedDrawableId) = getInputDrawables()
        return PressableButtonImageView(context).apply {
            if (normalDrawableId != -1) {
                val images = OverlayImages.buttonImages(context, normalDrawableId, pressedDrawableId)
                setImages(images.normal, images.pressed)
            }
        }
    }

    override fun getAspectRatio() = 1f

    private fun getInputDrawables(): Pair<Int, Int?> {
        return when (layoutComponent) {
            LayoutComponent.BUTTON_A -> R.drawable.button_a to R.drawable.button_a_pressed
            LayoutComponent.BUTTON_B -> R.drawable.button_b to R.drawable.button_b_pressed
            LayoutComponent.BUTTON_X -> R.drawable.button_x to R.drawable.button_x_pressed
            LayoutComponent.BUTTON_Y -> R.drawable.button_y to R.drawable.button_y_pressed
            LayoutComponent.BUTTON_L -> R.drawable.button_l to R.drawable.button_l_pressed
            LayoutComponent.BUTTON_R -> R.drawable.button_r to R.drawable.button_r_pressed
            LayoutComponent.BUTTON_START -> R.drawable.button_start to R.drawable.button_start_pressed
            LayoutComponent.BUTTON_SELECT -> R.drawable.button_select to R.drawable.button_select_pressed
            LayoutComponent.BUTTON_HINGE -> R.drawable.button_toggle_lid to R.drawable.button_toggle_lid_pressed
            LayoutComponent.BUTTON_PAUSE -> R.drawable.button_pause to R.drawable.button_pause_pressed
            LayoutComponent.BUTTON_FAST_FORWARD_TOGGLE -> R.drawable.button_fast_forward to null
            LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT -> R.drawable.ic_touch_enabled to null
            LayoutComponent.BUTTON_RESET -> R.drawable.button_reset to R.drawable.button_reset_pressed
            LayoutComponent.BUTTON_SWAP_SCREENS -> R.drawable.button_swap_screens to R.drawable.button_swap_screens_pressed
            LayoutComponent.BUTTON_QUICK_SAVE -> R.drawable.button_quick_save to R.drawable.button_quick_save_pressed
            LayoutComponent.BUTTON_QUICK_LOAD -> R.drawable.button_quick_load to R.drawable.button_quick_load_pressed
            LayoutComponent.BUTTON_REWIND -> R.drawable.button_rewind to null
            LayoutComponent.BUTTON_MICROPHONE_TOGGLE -> R.drawable.button_microphone to null
            LayoutComponent.TATACON_BUTTON -> R.drawable.taiko to R.drawable.taiko_pressed
            else -> -1 to null
        }
    }
}
