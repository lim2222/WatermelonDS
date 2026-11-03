package me.magnum.melonds.ui.emulator.input.view

import android.content.Context
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView

class TaikoButtonView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    var normalDrawableId: Int = -1
    var pressedDrawableId: Int = -1
    private var isPressed = false

    fun setPressedState(pressed: Boolean) {
        if (this.isPressed == pressed) return
        this.isPressed = pressed
        setImageResource(if (pressed) pressedDrawableId else normalDrawableId)
        animate()
            .scaleX(if (pressed) 0.9f else 1f)
            .scaleY(if (pressed) 0.9f else 1f)
            .setDuration(80)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}
