package me.magnum.melonds.ui.emulator.input.view

import android.content.Context
import android.util.AttributeSet
import me.magnum.melonds.common.buttonpack.OverlayImages

/**
 * A [PressableButtonImageView] whose normal image depends on an enabled/disabled toggle state.
 *
 * The pressed image is only used while the toggle is enabled; the disabled image has no dedicated
 * pressed variant, so pressing it falls back to the default dim/scale feedback.
 */
class ToggleableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PressableButtonImageView(context, attrs) {

    private var enabledDrawableId: Int = 0
    private var enabledPressedDrawableId: Int = 0
    private var disabledDrawableId: Int = 0
    private var toggleEnabled = true

    fun setDrawables(enabledDrawableId: Int, enabledPressedDrawableId: Int?, disabledDrawableId: Int) {
        this.enabledDrawableId = enabledDrawableId
        this.enabledPressedDrawableId = enabledPressedDrawableId ?: 0
        this.disabledDrawableId = disabledDrawableId
        setToggleState(toggleEnabled)
    }

    fun setToggleState(enabled: Boolean) {
        toggleEnabled = enabled
        val normalId = if (enabled) enabledDrawableId else disabledDrawableId
        val pressedId = if (enabled) enabledPressedDrawableId else 0
        val images = OverlayImages.buttonImages(context, normalId, pressedId.takeIf { it != 0 })
        setImages(images.normal, images.pressed)
    }
}
