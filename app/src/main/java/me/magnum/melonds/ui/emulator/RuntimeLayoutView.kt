package me.magnum.melonds.ui.emulator

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.input.SoftInputBehaviour
import me.magnum.melonds.domain.model.layout.LayoutComponent
import me.magnum.melonds.ui.common.LayoutView
import me.magnum.melonds.ui.emulator.input.ButtonsInputHandler
import me.magnum.melonds.ui.emulator.input.DpadInputHandler
import me.magnum.melonds.ui.emulator.input.FrontendInputHandler
import me.magnum.melonds.ui.emulator.input.HybridScreenTouchscreenInputHandler
import me.magnum.melonds.ui.emulator.input.IInputListener
import me.magnum.melonds.ui.emulator.input.MultiButtonInputHandler
import me.magnum.melonds.ui.emulator.input.Slot2AnalogInput
import me.magnum.melonds.ui.emulator.input.Slot2AnalogInputHandler
import me.magnum.melonds.ui.emulator.input.SingleButtonInputHandler
import me.magnum.melonds.ui.emulator.input.TataconInputHandler
import me.magnum.melonds.ui.emulator.input.TouchscreenInputHandler
import me.magnum.melonds.ui.emulator.input.view.ButtonsView
import me.magnum.melonds.ui.emulator.input.view.DpadDirection
import me.magnum.melonds.ui.emulator.input.view.DpadView
import me.magnum.melonds.ui.emulator.input.view.PressableButtonImageView
import me.magnum.melonds.ui.emulator.input.view.TataconView
import me.magnum.melonds.ui.emulator.input.view.TaikoButtonView
import me.magnum.melonds.ui.emulator.input.view.ToggleableImageView
import me.magnum.melonds.ui.emulator.model.ConnectedControllersState
import me.magnum.melonds.ui.emulator.model.RuntimeInputLayoutConfiguration
import me.magnum.melonds.ui.layouteditor.model.LayoutTarget
import javax.inject.Inject

@AndroidEntryPoint
class RuntimeLayoutView(context: Context, attrs: AttributeSet? = null) : LayoutView(context, attrs) {

    @Inject
    lateinit var touchVibrator: TouchVibrator

    private var currentRuntimeLayout: RuntimeInputLayoutConfiguration? = null
    private var frontendInputHandler: IInputListener? = null
    private var systemInputHandler: IInputListener? = null
    private var isSoftInputVisible = true
    private var areScreensSwapped = false
    private var connectedControllersState: ConnectedControllersState = ConnectedControllersState.NoControllers
    private var slot2AnalogInput: Slot2AnalogInput? = null
    private var slot2AnalogHandler: Slot2AnalogInputHandler? = null
    private var tataconHandler: TataconInputHandler? = null
    private var dpadInputHandler: MultiButtonInputHandler? = null
    private var buttonsInputHandler: MultiButtonInputHandler? = null
    private val virtualButtonInputs = mutableMapOf<LayoutComponent, SingleButtonInputHandler>()
    private val blockingComponentRect = Rect()

    fun setSlot2AnalogInput(input: Slot2AnalogInput) {
        releaseVirtualButtonInputs()
        slot2AnalogInput = input
        updateInputs()
    }

    fun setFrontendInputHandler(frontendInputHandler: FrontendInputHandler) {
        this.frontendInputHandler = frontendInputHandler
        updateInputs()
    }

    fun setSystemInputHandler(systemInputHandler: IInputListener) {
        this.systemInputHandler = systemInputHandler
        updateInputs()
    }

    fun setConnectedControllersState(state: ConnectedControllersState) {
        connectedControllersState = state
        updateVisibility()
    }

    fun toggleSoftInputVisibility() {
        isSoftInputVisible = !isSoftInputVisible
        setLayoutComponentToggleState(LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT, isSoftInputVisible)
        updateVisibility()
    }

    fun swapScreens() {
        areScreensSwapped = !areScreensSwapped
        updateScreenInputs()
    }

    fun areScreensSwapped(): Boolean {
        return areScreensSwapped
    }

    fun setLayoutComponentToggleState(layoutComponent: LayoutComponent, isEnabled: Boolean) {
        val toggleableImageView = getLayoutComponentView(layoutComponent)?.view as? ToggleableImageView ?: return
        toggleableImageView.setToggleState(isEnabled)
    }

    fun instantiateLayout(runtimeLayout: RuntimeInputLayoutConfiguration, layoutTarget: LayoutTarget) {
        currentRuntimeLayout = runtimeLayout
        instantiateLayout(runtimeLayout.layout, layoutTarget)
        updateInputs()
        updateVisibility()
        setLayoutComponentToggleState(LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT, isSoftInputVisible)
    }

    private fun updateInputs() {
        releaseVirtualButtonInputs()
        virtualButtonInputs.clear()
        dpadInputHandler = null
        buttonsInputHandler = null
        slot2AnalogHandler = null
        tataconHandler = null
        val currentRuntimeLayout = currentRuntimeLayout
        if (currentRuntimeLayout == null) {
            isGone = true
            return
        }

        isVisible = true
        val inputAlpha = currentRuntimeLayout.softInputOpacity / 100f

        slot2AnalogInput?.let { input ->
            getLayoutComponentView(LayoutComponent.SLOT2_ANALOG)?.view?.let { view ->
                Slot2AnalogInputHandler(input).also {
                    slot2AnalogHandler = it
                    view.setOnTouchListener(it)
                }
            }
        }
        val enableHapticFeedback = currentRuntimeLayout.isHapticFeedbackEnabled
        systemInputHandler?.let { inputListener ->
            // The drum does not get a touch listener: it owns the whole layout surface, so its touches
            // are picked up in dispatchTouchEvent() instead
            getLayoutComponentView(LayoutComponent.TATACON)?.view?.let { view ->
                if (view is TataconView) {
                    tataconHandler = TataconInputHandler(inputListener, enableHapticFeedback, touchVibrator)
                }
            }
        }
        systemInputHandler?.let {
            getLayoutComponentView(LayoutComponent.DPAD)?.view?.let { view ->
                dpadInputHandler = DpadInputHandler(it, enableHapticFeedback, touchVibrator).also { handler ->
                    view.setOnTouchListener(handler)
                }
            }
            getLayoutComponentView(LayoutComponent.BUTTONS)?.view?.let { view ->
                buttonsInputHandler = ButtonsInputHandler(it, enableHapticFeedback, touchVibrator).also { handler ->
                    view.setOnTouchListener(handler)
                }
            }
            for (component in listOf(LayoutComponent.BUTTON_A, LayoutComponent.BUTTON_B, LayoutComponent.BUTTON_X, LayoutComponent.BUTTON_Y)) {
                val componentView = getLayoutComponentView(component) ?: continue
                val handler = SingleButtonInputHandler(it, component.matchingInputs.single(), enableHapticFeedback, touchVibrator, componentView.buttonMode)
                virtualButtonInputs[component] = handler
                componentView.view.setOnTouchListener(handler)
            }
            getLayoutComponentView(LayoutComponent.BUTTON_L)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.L, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_R)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.R, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.TATACON_BUTTON)?.view?.let { view ->
                val handler = SingleButtonInputHandler(it, Input.TAIKO, enableHapticFeedback, touchVibrator)
                virtualButtonInputs[LayoutComponent.TATACON_BUTTON] = handler
                view.setOnTouchListener { v, event ->
                    val result = handler.onTouch(v, event)
                    if (v is TaikoButtonView) {
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> v.setPressedState(true)
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.setPressedState(false)
                                toggleTataconVisibility()
                            }
                        }
                    }
                    return@setOnTouchListener result
                }
            }
            getLayoutComponentView(LayoutComponent.BUTTON_SELECT)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.SELECT, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_START)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.START, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_HINGE)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.HINGE, enableHapticFeedback, touchVibrator))
        }
        frontendInputHandler?.let {
            getLayoutComponentView(LayoutComponent.BUTTON_RESET)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.RESET, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_PAUSE)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.PAUSE, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_FAST_FORWARD_TOGGLE)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.FAST_FORWARD, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_MICROPHONE_TOGGLE)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.MICROPHONE, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.TOGGLE_SOFT_INPUT, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_SWAP_SCREENS)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.SWAP_SCREENS, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_QUICK_SAVE)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.QUICK_SAVE, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_QUICK_LOAD)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.QUICK_LOAD, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_REWIND)?.view?.setOnTouchListener(SingleButtonInputHandler(it, Input.REWIND, enableHapticFeedback, touchVibrator))
        }

        getLayoutComponentViews().forEach {
            if (!it.component.isScreen()) {
                // A component whose alpha was customized in the layout editor keeps its own
                // opacity; the rest keep following the global soft input opacity.
                it.view.alpha = if (it.hasCustomAlpha) it.baseAlpha else inputAlpha
            }
        }

        updateScreenInputs()
    }

    private fun updateScreenInputs() {
        val (touchScreenComponent, nonTouchScreenComponent) = if (areScreensSwapped) {
            LayoutComponent.TOP_SCREEN to LayoutComponent.BOTTOM_SCREEN
        } else {
            LayoutComponent.BOTTOM_SCREEN to LayoutComponent.TOP_SCREEN
        }
        systemInputHandler?.let {
            getLayoutComponentView(touchScreenComponent)?.view?.setOnTouchListener(TouchscreenInputHandler(it))
            getLayoutComponentView(LayoutComponent.HYBRID_SCREEN)?.view?.setOnTouchListener(HybridScreenTouchscreenInputHandler(it))
        }
        getLayoutComponentView(nonTouchScreenComponent)?.view?.setOnTouchListener(null)
    }

    fun releaseVirtualButtonInputs() {
        slot2AnalogHandler?.release()
        virtualButtonInputs.values.forEach { it.release() }
        dpadInputHandler?.release()
        buttonsInputHandler?.release()
        tataconHandler?.release()
    }

    override fun destroyLayout() {
        releaseVirtualButtonInputs()
        virtualButtonInputs.clear()
        dpadInputHandler = null
        buttonsInputHandler = null
        slot2AnalogHandler = null
        tataconHandler = null
        super.destroyLayout()
    }

    override fun onDetachedFromWindow() {
        releaseVirtualButtonInputs()
        super.onDetachedFromWindow()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val handler = tataconHandler
        val drum = getLayoutComponentView(LayoutComponent.TATACON)?.view as? TataconView
        if (handler == null || drum == null || !isVisible || !drum.isVisible) {
            handler?.release()
            return super.dispatchTouchEvent(ev)
        }

        val drumPointers = handler.onLayoutTouch(ev, drum, ::isBlockedByOtherComponent)
        if (drumPointers.isEmpty()) {
            return super.dispatchTouchEvent(ev)
        }

        // Some pointers belong to the drum and must not reach the other components
        val filteredEvent = obtainFilteredEvent(ev, drumPointers) ?: return true
        return try {
            super.dispatchTouchEvent(filteredEvent)
        } finally {
            if (filteredEvent !== ev) {
                filteredEvent.recycle()
            }
        }
    }

    private fun isBlockedByOtherComponent(x: Int, y: Int): Boolean {
        for (componentView in getLayoutComponentViews()) {
            if (componentView.component.isScreen() || componentView.component == LayoutComponent.TATACON) continue
            val view = componentView.view
            if (!view.isVisible) continue
            view.getHitRect(blockingComponentRect)
            if (blockingComponentRect.contains(x, y)) return true
        }
        return false
    }

    /**
     * Builds an event that only contains the pointers that do not belong to the drum. The action is
     * rewritten accordingly, since the remaining components never saw the removed pointers going
     * down. Returns null if no pointer is left, and the original event if nothing was removed.
     */
    private fun obtainFilteredEvent(ev: MotionEvent, drumPointers: Set<Int>): MotionEvent? {
        val keptPointers = (0 until ev.pointerCount).filter { ev.getPointerId(it) !in drumPointers }
        if (keptPointers.size == ev.pointerCount) return ev
        if (keptPointers.isEmpty()) return null

        val actionIndex = ev.actionIndex
        val isActionPointerKept = ev.getPointerId(actionIndex) !in drumPointers
        val action = when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> when {
                !isActionPointerKept -> MotionEvent.ACTION_MOVE
                keptPointers.size == 1 -> MotionEvent.ACTION_DOWN
                else -> MotionEvent.ACTION_POINTER_DOWN or
                        (keptPointers.indexOf(actionIndex) shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> when {
                !isActionPointerKept -> MotionEvent.ACTION_MOVE
                keptPointers.size == 1 -> MotionEvent.ACTION_UP
                else -> MotionEvent.ACTION_POINTER_UP or
                        (keptPointers.indexOf(actionIndex) shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            }
            else -> ev.actionMasked
        }

        val pointerProperties = Array(keptPointers.size) { i ->
            MotionEvent.PointerProperties().also { ev.getPointerProperties(keptPointers[i], it) }
        }
        val pointerCoords = Array(keptPointers.size) { i ->
            MotionEvent.PointerCoords().also { ev.getPointerCoords(keptPointers[i], it) }
        }
        return MotionEvent.obtain(
            ev.downTime, ev.eventTime, action, keptPointers.size, pointerProperties, pointerCoords,
            ev.metaState, ev.buttonState, ev.xPrecision, ev.yPrecision,
            ev.deviceId, ev.edgeFlags, ev.source, ev.flags
        )
    }

    private fun resetPressState(view: View) {
        when (view) {
            is DpadView -> view.setDirection(DpadDirection.NONE)
            is ButtonsView -> view.setPressedInputs(emptyList())
            is PressableButtonImageView -> view.isPressed = false
        }
    }

    private fun updateVisibility() {
        // A component that is about to be hidden must not leave any of its inputs pressed
        tataconHandler?.release()
        val currentConnectedControllersState = connectedControllersState
        var hiddenComponents = when(currentRuntimeLayout?.softInputBehaviour) {
            SoftInputBehaviour.ALWAYS_VISIBLE -> emptyList()
            SoftInputBehaviour.HIDE_SYSTEM_BUTTONS_WHEN_CONTROLLERS_CONNECTED, null -> {
                when(currentConnectedControllersState) {
                    ConnectedControllersState.NoControllers -> emptyList()
                    is ConnectedControllersState.ControllersConnected -> listOf(
                        LayoutComponent.BUTTONS,
                        LayoutComponent.BUTTON_A,
                        LayoutComponent.BUTTON_B,
                        LayoutComponent.BUTTON_X,
                        LayoutComponent.BUTTON_Y,
                        LayoutComponent.SLOT2_ANALOG,
                        LayoutComponent.DPAD,
                        LayoutComponent.BUTTON_L,
                        LayoutComponent.BUTTON_R,
                        LayoutComponent.BUTTON_START,
                        LayoutComponent.BUTTON_SELECT
                    )
                }
            }
            SoftInputBehaviour.HIDE_ALL_BUTTONS_ASSIGNED_TO_CONNECTED_CONTROLLERS -> when(currentConnectedControllersState) {
                ConnectedControllersState.NoControllers -> emptyList()
                is ConnectedControllersState.ControllersConnected -> {
                    LayoutComponent.entries.filter {
                        // The component can be hidden if all matching inputs are assigned to connected controllers
                        it != LayoutComponent.SLOT2_ANALOG && it.matchingInputs.all {
                            currentConnectedControllersState.assignedInputs.contains(it)
                        }
                    }
                }
            }
            SoftInputBehaviour.ALWAYS_INVISIBLE -> LayoutComponent.entries.toList()
        }

        if (!isSoftInputVisible) {
            // Hide everything except the soft input toggle button if it was not already hidden by the soft input behaviour logic
            hiddenComponents = if (hiddenComponents.contains(LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT)) {
                LayoutComponent.entries.toList()
            } else {
                LayoutComponent.entries.toList().filter { it != LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT}
            }
        }

        val visibleComponents = LayoutComponent.entries.filter { !hiddenComponents.contains(it) }

        hiddenComponents.forEach { component ->
            if (!component.isScreen()) {
                if (component == LayoutComponent.SLOT2_ANALOG) slot2AnalogHandler?.release()
                if (component == LayoutComponent.DPAD) dpadInputHandler?.release()
                if (component == LayoutComponent.BUTTONS) buttonsInputHandler?.release()
                virtualButtonInputs[component]?.release()
                getLayoutComponentView(component)?.view?.let { view ->
                    resetPressState(view)
                    view.isVisible = false
                }
            }
        }
        visibleComponents.forEach { component ->
            if (!component.isScreen()) {
                getLayoutComponentView(component)?.view?.isVisible = true
            }
        }
    }

    /** Toggles the visibility of the TaTaCon drum overlay when the TaTaCon hotkey is pressed. */
    private fun toggleTataconVisibility() {
        val drumView = getLayoutComponentView(LayoutComponent.TATACON)?.view
        val isCurrentlyVisible = drumView?.isVisible ?: false
        drumView?.isVisible = !isCurrentlyVisible

        if (isCurrentlyVisible) {
            // Hiding the drum: stop tracking its input
            tataconHandler?.release()
            tataconHandler = null
        } else {
            // Showing the drum again: re-create its input handler
            systemInputHandler?.let { inputListener ->
                if (tataconHandler == null && drumView is TataconView) {
                    tataconHandler = TataconInputHandler(
                        inputListener,
                        currentRuntimeLayout?.isHapticFeedbackEnabled ?: false,
                        touchVibrator
                    )
                }
            }
        }
    }
}
