package android.emu6502.nes

import java.io.DataInputStream
import java.io.File
import java.io.InputStream

internal class INESFileParser {
  companion object {
    internal val INES_FILE_MAGIC = byteArrayOf(0x4e, 0x45, 0x53, 0x1a)/*0x4e45531a*/
    internal val PADDING = byteArrayOf(0, 0, 0, 0, 0, 0, 0)

    internal fun parseFileHeader(stream: InputStream): INESFileHeader {
      val dataStream = DataInputStream(stream)
      return INESFileHeader(
          (0..3).map { dataStream.readByte() }.toByteArray(),
          dataStream.readByte(),
          dataStream.readByte(),
          dataStream.readByte(),
          dataStream.readByte(),
          dataStream.readByte(),
          (0..6).map { dataStream.readByte() }.toByteArray())
    }

    fun parseCartridge(file: File): Cartridge =
        parseCartridge(file.inputStream())

    fun parseCartridge(inputStream: InputStream): Cartridge =
        inputStream.use { stream ->
          val inesFileHeader = parseFileHeader(stream)
          if (!inesFileHeader.isValid()) {
            throw IllegalArgumentException("Invalid INES file header")
          }
          // mapper type
          val control1 = inesFileHeader.control1.toInt()
          val mapper1 = control1 shr 4
          val mapper2 = inesFileHeader.control2.toInt() shr 4
          val mapper = mapper1 or (mapper2 shl 4)
          if (mapper != 4) {
            throw IllegalArgumentException("Unsupported mapper type $mapper")
          }
          // mirroring type
          val mirror1 = control1 and 1
          val mirror2 = (control1 shr 3) and 1
          val mirror = mirror1 or (mirror2 shl 1)
          // battery-backed RAM
          val battery = (control1 shr 1).and(1)
          // read prg-rom bank(s)
          val prg = ByteArray(inesFileHeader.numPRG.toInt() * 16384)
          if (stream.read(prg) != prg.size) {
            throw IllegalStateException("Could not load ${prg.size} bytes from the input")
          }
          // read chr-rom bank(s)
          val chr = ByteArray(inesFileHeader.numCHR.toInt() * 8192)
          stream.read(chr)
          return Cartridge(
              prg.map { it.toInt() and 0xff }.toTypedArray(),
              chr.map { it.toInt() and 0xff }.toTypedArray(),
              mapper,
              mirror,
              battery
          )
        }
  }
}