package android.emu6502.nes.mappers

class CNROM : Mapper  {
  override fun read(address: Int): Byte {
    throw NotImplementedError()
  }

  override fun write(address: Int, value: Byte) {
  }

  override fun step() {
  }
}