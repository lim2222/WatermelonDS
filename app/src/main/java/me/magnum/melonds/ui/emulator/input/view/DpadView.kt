package me.magnum.melonds.ui.emulator.input.view

import android.content.Context
import android.graphics.Canvas
import android.view.View
import me.magnum.melonds.R
import me.magnum.melonds.common.buttonpack.OverlayImages

/**
 * Draws the keypad artwork for the current direction. The idle image is shown when no direction is
 * pressed, and each of the eight directions has its own image. A missing image falls back to the
 * idle artwork.
 */
class DpadView(context: Context) : View(context) {
    private val idleDrawable = OverlayImages.drawable(context, R.drawable.keypad)
    private val directionDrawables = mapOf(
        DpadDirection.UP to OverlayImages.drawable(context, R.drawable.keypad_up),
        DpadDirection.UP_RIGHT to OverlayImages.drawable(context, R.drawable.keypad_up_right),
        DpadDirection.RIGHT to OverlayImages.drawable(context, R.drawable.keypad_right),
        DpadDirection.DOWN_RIGHT to OverlayImages.drawable(context, R.drawable.keypad_down_right),
        DpadDirection.DOWN to OverlayImages.drawable(context, R.drawable.keypad_down),
        DpadDirection.DOWN_LEFT to OverlayImages.drawable(context, R.drawable.keypad_down_left),
        DpadDirection.LEFT to OverlayImages.drawable(context, R.drawable.keypad_left),
        DpadDirection.UP_LEFT to OverlayImages.drawable(context, R.drawable.keypad_up_left),
    )
    private var direction = DpadDirection.NONE

    init {
        contentDescription = context.getString(R.string.input_dpad)
    }

    fun setDirection(direction: DpadDirection) {
        if (this.direction == direction) return
        this.direction = direction
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val drawable = if (direction == DpadDirection.NONE) {
            idleDrawable
        } else {
            directionDrawables[direction] ?: idleDrawable
        }
        drawable?.let {
            it.setBounds(0, 0, width, height)
            it.draw(canvas)
        }
    }
}
