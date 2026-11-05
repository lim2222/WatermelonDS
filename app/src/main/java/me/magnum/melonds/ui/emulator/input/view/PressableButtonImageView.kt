package me.magnum.melonds.ui.emulator.input.view

import android.content.Context
import android.graphics.ColorFilter
import android.graphics.LightingColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * An [AppCompatImageView] that reacts to [setPressed] like a real button.
 *
 * When both a normal and a pressed image are supplied, a [StateListDrawable] swaps them automatically.
 * When there is no dedicated pressed image, the image is dimmed and scaled down instead, so every
 * button has some press feedback.
 */
open class PressableButtonImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {

    private var hasPressedDrawable = false
    private var pressedState = false

    fun setImages(normal: Drawable?, pressed: Drawable?) {
        hasPressedDrawable = normal != null && pressed != null
        val drawable = if (hasPressedDrawable) {
            StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), pressed)
                addState(intArrayOf(), normal)
            }
        } else {
            normal ?: pressed
        }

        setImageDrawable(drawable)
        pressedState = isPressed
        applyPressVisuals(pressedState)
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        if (pressedState == pressed) return
        pressedState = pressed
        applyPressVisuals(pressed)
    }

    private fun applyPressVisuals(pressed: Boolean) {
        animate().cancel()
        animate()
            .scaleX(if (pressed) PRESSED_SCALE else 1f)
            .scaleY(if (pressed) PRESSED_SCALE else 1f)
            .setDuration(PRESS_ANIMATION_DURATION)
            .start()

        colorFilter = if (!hasPressedDrawable && pressed) PRESSED_COLOR_FILTER else null
    }

    companion object {
        private const val PRESSED_SCALE = 0.92f
        private const val PRESS_ANIMATION_DURATION = 40L
        private val PRESSED_COLOR_FILTER: ColorFilter =
            LightingColorFilter(0xFFBBBBBB.toInt(), 0)
    }
}
