package me.magnum.melonds.domain.model.layout

import me.magnum.melonds.domain.model.Rect

data class PositionedLayoutComponent(
    val rect: Rect,
    val component: LayoutComponent,
    val alpha: Float = 1f,
    val onTop: Boolean = false,
    val buttonMode: VirtualButtonMode = VirtualButtonMode.NORMAL,
    /**
     * Whether [alpha] was explicitly set by the user for this component. When false, non-screen
     * components keep following the global soft input opacity.
     */
    val hasCustomAlpha: Boolean = false,
) {

    fun isScreen(): Boolean {
        return component.isScreen()
    }
}
