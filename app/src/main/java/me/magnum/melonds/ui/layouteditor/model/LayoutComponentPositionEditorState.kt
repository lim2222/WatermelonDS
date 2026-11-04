package me.magnum.melonds.ui.layouteditor.model

import me.magnum.melonds.domain.model.layout.LayoutComponent

data class LayoutComponentPositionEditorState(
    val component: LayoutComponent,
    val x: Int,
    val y: Int,
    val minX: Int = 0,
    val maxX: Int,
    val minY: Int = 0,
    val maxY: Int,
)
