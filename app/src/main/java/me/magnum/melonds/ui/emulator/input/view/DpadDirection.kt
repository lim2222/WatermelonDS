package me.magnum.melonds.ui.emulator.input.view

import me.magnum.melonds.domain.model.Input

/** The visual direction of the on-screen keypad. */
enum class DpadDirection {
    NONE,
    UP,
    UP_RIGHT,
    RIGHT,
    DOWN_RIGHT,
    DOWN,
    DOWN_LEFT,
    LEFT,
    UP_LEFT;

    companion object {
        /** Resolves the direction from the currently pressed inputs. Opposite inputs cancel out. */
        fun from(pressedInputs: Collection<Input>): DpadDirection {
            val x = (if (Input.RIGHT in pressedInputs) 1 else 0) - (if (Input.LEFT in pressedInputs) 1 else 0)
            val y = (if (Input.DOWN in pressedInputs) 1 else 0) - (if (Input.UP in pressedInputs) 1 else 0)
            return when {
                x > 0 && y < 0 -> UP_RIGHT
                x > 0 && y > 0 -> DOWN_RIGHT
                x < 0 && y < 0 -> UP_LEFT
                x < 0 && y > 0 -> DOWN_LEFT
                x > 0 -> RIGHT
                x < 0 -> LEFT
                y < 0 -> UP
                y > 0 -> DOWN
                else -> NONE
            }
        }
    }
}
