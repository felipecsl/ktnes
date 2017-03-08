package android.emu6502.nes

import java.io.DataInputStream
import java.io.File
import java.io.InputStream

internal class INESFileParser {
  companion object {
    internal val INES_FILE_MAGIC = byteArrayOf(0x4e, 0x45, 0x53, 0x1a)/*0x4e45531a*/
    internal val PADDING = byteArrayOf(0, 0, 0, 0, 0, 0, 0)

    internal fun parseFileHeader(stream: InputStream): INESFileHeader {
      val dataStream = DataInputStream(stream.buffered())
      return INESFileHeader(
          (0..3).map { dataStream.readByte() }.toByteArray(),
          dataStream.readByte(),
          dataStream.readByte(),
          dataStream.readByte(),
          dataStream.readByte(),
          dataStream.readByte(),
          (0..6).map { dataStream.readByte() }.toByteArray())
    }

    internal fun parseCartridge(file: File): Cartridge {
      val stream = file.inputStream()
      stream.use {
        val inesFileHeader = parseFileHeader(stream)
        // mapper type
        val control1 = inesFileHeader.control1.toInt()
        val mapper1 = control1.shr(4)
        val mapper2 = inesFileHeader.control2.toInt().shr(4)
        val mapper = if (mapper1 != 0) mapper1 else mapper2
        // mirroring type
        val mirror1 = control1.and(1)
        val mirror2 = control1.shr(3).and(1)
        val mirror = if (mirror1 != 0) mirror1 else mirror2.shl(1)
        // battery-backed RAM
        val battery = control1.shr(1).and(1).toByte()
        // read prg-rom bank(s)
        val pgr = ByteArray(inesFileHeader.numPRG.toInt() * 16384)
        stream.read(pgr)
        // read chr-rom bank(s)
        val chr = ByteArray(inesFileHeader.numCHR.toInt() * 8192)
        stream.read(chr)
        return Cartridge(pgr, chr, mapper.toByte(), mirror.toByte(), battery)
      }
    }
  }
}