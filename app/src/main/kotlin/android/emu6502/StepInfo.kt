package android.emu6502

data class StepInfo(
    val address: Int,
    val PC: Int,
    val mode: Int
)