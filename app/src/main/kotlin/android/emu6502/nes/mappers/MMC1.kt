package android.emu6502.nes.mappers

class MMC1 : Mapper  {
  override fun read(address: Int): Byte {
    throw NotImplementedError()
  }

  override fun write(address: Int, value: Byte) {
  }

  override fun step() {
  }
}