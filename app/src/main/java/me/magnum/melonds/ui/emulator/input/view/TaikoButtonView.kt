package me.magnum.melonds.ui.emulator.input.view

import android.content.Context
import android.util.AttributeSet

/**
 * The taiko drum button. Press feedback (image swap, scale and dim fallback) is provided by
 * [PressableButtonImageView]; this class only keeps the API used by the input handler.
 */
class TaikoButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PressableButtonImageView(context, attrs) {

    fun setPressedState(pressed: Boolean) {
        isPressed = pressed
    }
}
