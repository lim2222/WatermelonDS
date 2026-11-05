package me.magnum.melonds.common.buttonpack

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

/**
 * Central entry point for every overlay image used by the on-screen controls.
 *
 * Images are first looked up in the active [ButtonPack], then in the built-in drawables. All
 * overlay builders should go through this object so that adding a new image automatically makes it
 * overridable by a button pack.
 */
object OverlayImages {
    fun drawable(context: Context, @DrawableRes id: Int): Drawable? {
        if (id == 0) return null
        ButtonPack.load(context, id)?.let { return BitmapDrawable(context.resources, it) }
        return AppCompatResources.getDrawable(context, id)
    }

    fun packDrawable(context: Context, name: String): Drawable? {
        return ButtonPack.load(context, name)?.let { BitmapDrawable(context.resources, it) }
    }

    fun entryName(context: Context, @DrawableRes id: Int): String? {
        if (id == 0) return null
        return runCatching { context.resources.getResourceEntryName(id) }.getOrNull()
    }

    data class ButtonImages(val normal: Drawable?, val pressed: Drawable?)

    /**
     * Resolves the normal and pressed images for a button. The pressed image may come from the
     * pack only (that is, a drawable without a dedicated built-in pressed variant).
     */
    fun buttonImages(
        context: Context,
        @DrawableRes normalId: Int,
        @DrawableRes pressedId: Int?,
    ): ButtonImages {
        val normal = drawable(context, normalId)
        val pressed = pressedId
            ?.takeIf { it != 0 }
            ?.let { drawable(context, it) }
            ?: entryName(context, normalId)?.let { packDrawable(context, "${it}_pressed") }
        return ButtonImages(normal, pressed)
    }
}
