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

    internal fun parseCartridge(file: File): Cartridge =
        file.inputStream().use {
          val inesFileHeader = parseFileHeader(it)
          // mapper type
          val control1 = inesFileHeader.control1.toInt()
          val mapper1 = control1 shr 4
          val mapper2 = inesFileHeader.control2.toInt() shr 4
          val mapper = mapper1 or (mapper2 shl 4)
          // mirroring type
          val mirror1 = control1 and 1
          val mirror2 = (control1 shr 3) and 1
          val mirror = mirror1 or (mirror2 shl 1)
          // battery-backed RAM
          val battery = (control1 shr 1).and(1).toByte()
          // read prg-rom bank(s)
          val prg = ByteArray(inesFileHeader.numPRG.toInt() * 16384)
          it.read(prg)
          // read chr-rom bank(s)
          val chr = ByteArray(inesFileHeader.numCHR.toInt() * 8192)
          it.read(chr)
          return Cartridge(
              prg.map(Byte::toInt).toIntArray(),
              chr.map(Byte::toInt).toIntArray(),
              mapper.toByte(), mirror, battery
          )
        }
  }
}