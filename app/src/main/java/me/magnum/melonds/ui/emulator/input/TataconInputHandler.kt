package me.magnum.melonds.ui.emulator.input

import android.view.MotionEvent
import android.view.View
import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.ui.emulator.input.view.TataconRegion
import me.magnum.melonds.ui.emulator.input.view.TataconView

/**
 * Handles the drum input of the TaTaCon overlay.
 *
 * Unlike the remaining input handlers, this one is not attached to a view: the drum owns the whole
 * layout surface outside of the other components, so its touches are picked up by the layout and
 * fed here (see [me.magnum.melonds.ui.emulator.RuntimeLayoutView.dispatchTouchEvent]).
 *
 * Every pointer that belongs to the drum is tracked until it is lifted, even if it slides over the
 * area of another component, so that a stroke is never handed over to a different component midway.
 */
class TataconInputHandler(
    inputListener: IInputListener,
    enableHapticFeedback: Boolean,
    touchVibrator: TouchVibrator,
) : FeedbackInputHandler(inputListener, enableHapticFeedback, touchVibrator) {

    // Region currently struck by each pointer. Several pointers may strike the same region; the key
    // is only pressed while at least one pointer holds it.
    private val pointerRegions = HashMap<Int, TataconRegion>()
    private val regionPointerCount = IntArray(TataconRegion.entries.size)
    private var drumView: TataconView? = null

    /**
     * Processes a touch event received by the layout. The event coordinates are in the layout's
     * coordinate space, so points outside of the drum's own view can be mapped to the rim.
     *
     * Returns the ids of the pointers that belong to the drum: the layout must not dispatch them to
     * the remaining components, since they were never given a chance to see them going down.
     *
     * [isBlockedByOtherComponent] tells whether a point that is outside of the drum's own view is
     * covered by another interactive component, in which case the touch belongs to that component.
     */
    fun onLayoutTouch(
        event: MotionEvent,
        drum: TataconView,
        isBlockedByOtherComponent: (Int, Int) -> Boolean,
    ): Set<Int> {
        if (drumView !== drum) {
            release()
            drumView = drum
        }

        // Pointers that the drum is releasing in this event must also be kept from the other
        // components, or they would receive an event for a pointer they never saw going down.
        val owned = HashSet(pointerRegions.keys)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val region = regionAt(event.getX(index), event.getY(index), drum, isBlockedByOtherComponent)
                if (region != null) {
                    val pointerId = event.getPointerId(index)
                    strike(pointerId, region)
                    owned.add(pointerId)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    val currentRegion = pointerRegions[pointerId] ?: continue
                    // Pointers that already belong to the drum stay with it until they are lifted
                    val region = regionAt(event.getX(i), event.getY(i), drum, null) ?: continue
                    if (region != currentRegion) {
                        lift(pointerId)
                        strike(pointerId, region)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                lift(event.getPointerId(event.actionIndex))
            }
            MotionEvent.ACTION_CANCEL -> {
                release()
            }
        }

        return owned
    }

    /**
     * The drum is never attached to a view as a touch listener, so this is not used. See
     * [onLayoutTouch] for the events this handler actually processes.
     */
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return false
    }

    /** Releases every region that is currently held. */
    fun release() {
        if (pointerRegions.isEmpty()) return
        pointerRegions.keys.toList().forEach { lift(it) }
    }

    private fun regionAt(
        x: Float,
        y: Float,
        drum: TataconView,
        isBlockedByOtherComponent: ((Int, Int) -> Boolean)?,
    ): TataconRegion? {
        val left = drum.left.toFloat()
        val top = drum.top.toFloat()
        val insideDrumView = x >= left && y >= top && x < left + drum.width && y < top + drum.height

        val region = if (insideDrumView) {
            drum.getRegionAt(x - left, y - top) ?: return null
        } else {
            // Outside of the drum view: rim, split at the horizontal center of the drum (same as Azahar)
            if (x < left + drum.width / 2f) TataconRegion.RIM_LEFT else TataconRegion.RIM_RIGHT
        }

        // Only the rim yields to other components; the drum head is not guarded (same as Azahar)
        if (!region.isHead && isBlockedByOtherComponent != null && isBlockedByOtherComponent(x.toInt(), y.toInt())) {
            return null
        }
        return region
    }

    private fun strike(pointerId: Int, region: TataconRegion) {
        pointerRegions[pointerId] = region
        val isFirstPointer = regionPointerCount[region.ordinal] == 0
        regionPointerCount[region.ordinal]++

        if (!isFirstPointer) return

        inputListener.onKeyPress(region.input)
        drumView?.let {
            it.setRegionPressed(region, true)
            performHapticFeedback(it, HapticFeedbackType.KEY_PRESS)
        }
    }

    private fun lift(pointerId: Int) {
        val region = pointerRegions.remove(pointerId) ?: return
        regionPointerCount[region.ordinal] = (regionPointerCount[region.ordinal] - 1).coerceAtLeast(0)

        if (regionPointerCount[region.ordinal] != 0) return

        inputListener.onKeyReleased(region.input)
        drumView?.let {
            it.setRegionPressed(region, false)
            performHapticFeedback(it, HapticFeedbackType.KEY_RELEASE)
        }
    }
}
