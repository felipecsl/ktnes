package android.emu6502.nes.mappers

class UNROM : Mapper  {
  override fun step() {
  }

  override fun write(address: Int, value: Byte) {
  }

  override fun read(address: Int): Byte {
    throw NotImplementedError()
  }
}