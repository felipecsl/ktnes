package android.emu6502.instructions

enum class AddressingMode {
  IMMEDIATE, ZERO_PAGE, ZERO_PAGE_X, ABSOLUTE, ABSOLUTE_X, ABSOLUTE_Y, INDIRECT_X, INDIRECT_Y
}
