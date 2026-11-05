package me.magnum.melonds.ui.emulator.input.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import me.magnum.melonds.R
import me.magnum.melonds.common.buttonpack.ButtonPack
import me.magnum.melonds.domain.model.Input
import kotlin.math.max

// Ported from the Azahar TaTaCon overlay (GPL-2.0-or-later)
enum class TataconRegion(val input: Input) {
    HEAD_UP_LEFT(Input.LEFT),
    HEAD_DOWN_LEFT(Input.RIGHT),
    HEAD_UP_RIGHT(Input.A),
    HEAD_DOWN_RIGHT(Input.B),
    RIM_LEFT(Input.L),
    RIM_RIGHT(Input.R);

    val isHead: Boolean get() = this != RIM_LEFT && this != RIM_RIGHT
}

class TataconView(context: Context) : View(context) {
    private val artwork: Bitmap? = loadArtwork(context)?.let { cropArtwork(it) }
    private val srcRect = Rect()
    private val dstRect = RectF()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pressed = BooleanArray(TataconRegion.entries.size)
    private var glowCx = 0f
    private var glowCy = 0f
    private var glowRx = 0f
    private var glowRy = 0f

    init {
        contentDescription = context.getString(R.string.input_tatacon)
    }

    fun setRegionPressed(region: TataconRegion, isPressed: Boolean) {
        if (pressed[region.ordinal] != isPressed) {
            pressed[region.ordinal] = isPressed
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        glowCx = HEAD_CENTER_X * w
        glowCy = HEAD_CENTER_Y * h
        glowRx = HEAD_RADIUS_X * w * GLOW_SCALE
        glowRy = HEAD_RADIUS_Y * h * GLOW_SCALE
        glowPaint.shader = RadialGradient(
            glowCx, glowCy, max(glowRx, glowRy),
            GLOW_COLOR, GLOW_COLOR_TRANSPARENT, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val artwork = artwork
        if (artwork == null) {
            super.onDraw(canvas)
            return
        }
        if (srcRect.isEmpty) {
            srcRect.set(0, 0, artwork.width, artwork.height)
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val save = canvas.save()
        if (pressed.any { it }) {
            // The drum sinks a little while it is being hit
            canvas.translate(0f, h * SINK_RATIO)
        }
        dstRect.set(0f, 0f, w, h)
        canvas.drawBitmap(artwork, srcRect, dstRect, bitmapPaint)
        if (TataconRegion.entries.any { it.isHead && pressed[it.ordinal] }) {
            canvas.drawOval(glowCx - glowRx, glowCy - glowRy, glowCx + glowRx, glowCy + glowRy, glowPaint)
        }
        canvas.restoreToCount(save)
    }

    /**
     * Returns the region at the given point, which must be local to this view, or null if the point
     * is outside of it.
     */
    fun getRegionAt(x: Float, y: Float): TataconRegion? {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || x < 0f || y < 0f || x >= w || y >= h) return null

        val nx = (x / w - HEAD_CENTER_X) / HEAD_RADIUS_X
        val ny = (y / h - HEAD_CENTER_Y) / HEAD_RADIUS_Y
        if (nx * nx + ny * ny <= 1f) {
            val left = x < HEAD_CENTER_X * w
            val up = y < HEAD_CENTER_Y * h
            return when {
                left && up -> TataconRegion.HEAD_UP_LEFT
                left -> TataconRegion.HEAD_DOWN_LEFT
                up -> TataconRegion.HEAD_UP_RIGHT
                else -> TataconRegion.HEAD_DOWN_RIGHT
            }
        }
        return if (x < w / 2f) TataconRegion.RIM_LEFT else TataconRegion.RIM_RIGHT
    }

    private fun loadArtwork(context: Context): Bitmap? {
        ButtonPack.load(context, R.drawable.tatacon)?.let { return it }
        return BitmapFactory.decodeResource(resources, R.drawable.tatacon)
    }

    companion object {
        // Position of the drum artwork inside the original 800x512 tatacon.png (normalized)
        private const val ART_LEFT = 0.3625f
        private const val ART_TOP = 0.3125f
        private const val ART_RIGHT = 0.6388f
        private const val ART_BOTTOM = 0.7480f

        // Geometry of the drum head inside the cropped artwork (normalized)
        private const val HEAD_CENTER_X = 0.5000f
        private const val HEAD_CENTER_Y = 0.3810f
        private const val HEAD_RADIUS_X = 0.4620f
        private const val HEAD_RADIUS_Y = 0.3720f

        private const val GLOW_SCALE = 0.98f
        private const val GLOW_COLOR = 0xFFFBAD96.toInt()
        private const val GLOW_COLOR_TRANSPARENT = 0x00FBAD96
        private const val SINK_RATIO = 0.01f

        private fun cropArtwork(bitmap: Bitmap): Bitmap {
            val left = (ART_LEFT * bitmap.width).toInt()
            val top = (ART_TOP * bitmap.height).toInt()
            val right = (ART_RIGHT * bitmap.width).toInt().coerceAtMost(bitmap.width)
            val bottom = (ART_BOTTOM * bitmap.height).toInt().coerceAtMost(bitmap.height)
            if (right <= left || bottom <= top) return bitmap
            return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        }
    }
}
