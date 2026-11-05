package me.magnum.melonds.ui.emulator.input

import android.view.View
import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.ui.emulator.input.view.ButtonsView

class ButtonsInputHandler(inputListener: IInputListener, enableHapticFeedback: Boolean, touchVibrator: TouchVibrator) : MultiButtonInputHandler(inputListener, enableHapticFeedback, touchVibrator) {
    override fun getTopInput() = Input.X
    override fun getLeftInput() = Input.Y
    override fun getBottomInput() = Input.B
    override fun getRightInput() = Input.A

    override fun onPressedInputsChanged(v: View, pressed: List<Input>) {
        (v as? ButtonsView)?.setPressedInputs(pressed)
    }
}
