package com.felipecsl.knes

import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

class KeyboardController(private val controller: Controller) {
  fun handleKeyUp(e: Event) {
    e as KeyboardEvent
    KEYS[e.keyCode]?.also { (_, button) ->
      controller.onButtonUp(button)
      e.preventDefault()
    }
  }

  fun handleKeyDown(e: Event) {
    e as KeyboardEvent
    KEYS[e.keyCode]?.also { (_, button) ->
      controller.onButtonDown(button)
      e.preventDefault()
    }
  }

  fun handleKeyPress(e: Event) {
    e.preventDefault()
  }

  companion object {
    // @formatter:off
    private val KEYS = mapOf(
        88  to (1 to Buttons.BUTTON_A), // X
        90  to (1 to Buttons.BUTTON_B), // Z
        17  to (1 to Buttons.BUTTON_SELECT), // Right Ctrl
        13  to (1 to Buttons.BUTTON_START), // Enter
        38  to (1 to Buttons.ARROW_UP), // Up
        40  to (1 to Buttons.ARROW_DOWN), // Down
        37  to (1 to Buttons.ARROW_LEFT), // Left
        39  to (1 to Buttons.ARROW_RIGHT), // Right
        103 to (2 to Buttons.BUTTON_A), // Num-7
        105 to (2 to Buttons.BUTTON_B), // Num-9
        99  to (2 to Buttons.BUTTON_SELECT), // Num-3
        97  to (2 to Buttons.BUTTON_START), // Num-1
        104 to (2 to Buttons.ARROW_UP), // Num-8
        98  to (2 to Buttons.ARROW_DOWN), // Num-2
        100 to (2 to Buttons.ARROW_LEFT), // Num-4
        102 to (2 to Buttons.ARROW_RIGHT )// Num-6
    )
    // @formatter:on
  }
}