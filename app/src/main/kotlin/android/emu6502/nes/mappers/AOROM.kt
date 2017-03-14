package android.emu6502.nes.mappers

class AOROM : Mapper {
  override fun write(address: Int, value: Byte) {
  }

  override fun step() {
  }

  override fun read(address: Int): Byte {
    throw NotImplementedError()
  }
}