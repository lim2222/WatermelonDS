package me.magnum.melonds.ui.emulator.input.view

import android.content.Context
import android.graphics.Canvas
import android.view.View
import me.magnum.melonds.R
import me.magnum.melonds.common.buttonpack.OverlayImages
import me.magnum.melonds.domain.model.Input
import kotlin.math.roundToInt

/**
 * Draws the composite ABXY artwork and overlays the pressed image of every currently pressed
 * button on top of it.
 *
 * The positions below use the 512x512 coordinate space of `buttons.png`. Each individual pressed
 * image is 190x190 with the 160px button circle centered in it, so it is drawn centered on the
 * matching circle of the composite artwork.
 */
class ButtonsView(context: Context) : View(context) {
    private val artwork = OverlayImages.drawable(context, R.drawable.buttons)
    private val pressedImages = mapOf(
        Input.A to OverlayImages.drawable(context, R.drawable.button_a_pressed),
        Input.B to OverlayImages.drawable(context, R.drawable.button_b_pressed),
        Input.X to OverlayImages.drawable(context, R.drawable.button_x_pressed),
        Input.Y to OverlayImages.drawable(context, R.drawable.button_y_pressed),
    )
    private val pressedCenters = mapOf(
        Input.A to (416.5f to 255.5f),
        Input.B to (255.5f to 414.5f),
        Input.X to (255.5f to 94.5f),
        Input.Y to (97.5f to 255.5f),
    )
    private var pressedInputs: Set<Input> = emptySet()

    init {
        contentDescription = context.getString(R.string.input_abxy_buttons)
    }

    fun setPressedInputs(inputs: Collection<Input>) {
        val newPressedInputs = inputs.filterTo(mutableSetOf()) { it in pressedImages }
        if (pressedInputs == newPressedInputs) return
        pressedInputs = newPressedInputs
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        artwork?.let {
            it.setBounds(0, 0, width, height)
            it.draw(canvas)
        }
        if (pressedInputs.isEmpty()) return

        val scaleX = width / ARTWORK_SIZE
        val scaleY = height / ARTWORK_SIZE
        pressedInputs.forEach { input ->
            val image = pressedImages[input] ?: return@forEach
            val (centerX, centerY) = pressedCenters[input] ?: return@forEach
            val halfWidth = PRESSED_IMAGE_SIZE * scaleX / 2f
            val halfHeight = PRESSED_IMAGE_SIZE * scaleY / 2f
            image.setBounds(
                (centerX * scaleX - halfWidth).roundToInt(),
                (centerY * scaleY - halfHeight).roundToInt(),
                (centerX * scaleX + halfWidth).roundToInt(),
                (centerY * scaleY + halfHeight).roundToInt(),
            )
            image.draw(canvas)
        }
    }

    companion object {
        private const val ARTWORK_SIZE = 512f
        private const val PRESSED_IMAGE_SIZE = 190f
    }
}
