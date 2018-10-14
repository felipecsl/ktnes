package com.felipecsl.knes

/** Translated/Adapted from java.io.ByteArrayInputStream */
open class ByteArrayInputStream
/**
 * Creates a `ByteArrayInputStream`
 * so that it  uses `buf` as its
 * buffer array.
 * The buffer array is not copied.
 * The initial value of `pos`
 * is `0` and the initial value
 * of  `count` is the length of
 * `buf`.
 *
 * @param   buf   the input buffer.
 */(
    /**
     * An array of bytes that was provided
     * by the creator of the stream. Elements `buf[0]`
     * through `buf[count-1]` are the
     * only bytes that can ever be read from the
     * stream;  element `buf[pos]` is
     * the next byte to be read.
     */
    private var buf: ByteArray) {

  /**
   * The index of the next character to read from the input stream buffer.
   * This value should always be nonnegative
   * and not larger than the value of `count`.
   * The next byte to be read from the input stream buffer
   * will be `buf[pos]`.
   */
  private var pos: Int = 0

  /**
   * The currently marked position in the stream.
   * ByteArrayInputStream objects are marked at position zero by
   * default when constructed.  They may be marked at another
   * position within the buffer by the `mark()` method.
   * The current buffer position is set to this point by the
   * `reset()` method.
   *
   *
   * If no mark has been set, then the value of mark is the offset
   * passed to the constructor (or 0 if the offset was not supplied).
   */
  private var mark = 0

  /**
   * The index one greater than the last valid character in the input
   * stream buffer.
   * This value should always be nonnegative
   * and not larger than the length of `buf`.
   * It  is one greater than the position of
   * the last byte within `buf` that
   * can ever be read  from the input stream buffer.
   */
  private var count: Int = 0

  init {
    this.pos = 0
    this.count = buf.size
  }

  /**
   * Reads the next byte of data from this input stream. The value
   * byte is returned as an `int` in the range
   * `0` to `255`. If no byte is available
   * because the end of the stream has been reached, the value
   * `-1` is returned.
   *
   *
   * This `read` method
   * cannot block.
   *
   * @return  the next byte of data, or `-1` if the end of the
   * stream has been reached.
   */
  fun read(): Int {
    return if (pos < count) buf[pos++].toInt() and 0xff else -1
  }

  /**
   * Reads up to `len` bytes of data into an array of bytes
   * from this input stream.
   * If `pos` equals `count`,
   * then `-1` is returned to indicate
   * end of file. Otherwise, the  number `k`
   * of bytes read is equal to the smaller of
   * `len` and `count-pos`.
   * If `k` is positive, then bytes
   * `buf[pos]` through `buf[pos+k-1]`
   * are copied into `b[off]`  through
   * `b[off+k-1]` in the manner performed
   * by `System.arraycopy`. The
   * value `k` is added into `pos`
   * and `k` is returned.
   *
   *
   * This `read` method cannot block.
   *
   * @param   b     the buffer into which the data is read.
   * @param   off   the start offset in the destination array `b`
   * @param   len   the maximum number of bytes read.
   * @return  the total number of bytes read into the buffer, or
   * `-1` if there is no more data because the end of
   * the stream has been reached.
   * @exception  NullPointerException If `b` is `null`.
   * @exception  IndexOutOfBoundsException If `off` is negative,
   * `len` is negative, or `len` is greater than
   * `b.length - off`
   */
  fun read(b: ByteArray, off: Int, len: Int): Int {
    var mutLen = len
    if (off < 0 || mutLen < 0 || mutLen > b.size - off) {
      throw IndexOutOfBoundsException()
    }

    if (pos >= count) {
      return -1
    }

    val avail = count - pos
    if (mutLen > avail) {
      mutLen = avail
    }
    if (mutLen <= 0) {
      return 0
    }
    arrayCopy(buf, pos, b, off, mutLen)
    pos += mutLen
    return mutLen
  }

  private fun arrayCopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, length: Int) {
    if (srcPos < 0 || dstPos < 0 || length < 0 || srcPos > src.size - length || dstPos > dst.size - length) {
      throw IllegalArgumentException("src.length=" + src.size + " srcPos=" + srcPos +
          " dst.length=" + dst.size + " dstPos=" + dstPos + " length=" + length)
    }
    // Copy byte by byte for shorter arrays.
    if (src === dst && srcPos < dstPos && dstPos < srcPos + length) {
      // Copy backward (to avoid overwriting elements before
      // they are copied in case of an overlap on the same array.)
      for (i in length - 1 downTo 0) {
        dst[dstPos + i] = src[srcPos + i]
      }
    } else {
      // Copy forward.
      for (i in 0 until length) {
        dst[dstPos + i] = src[srcPos + i]
      }
    }
  }

  /**
   * Returns the number of remaining bytes that can be read (or skipped over)
   * from this input stream.
   *
   *
   * The value returned is `count&nbsp;- pos`,
   * which is the number of bytes remaining to be read from the input buffer.
   *
   * @return  the number of remaining bytes that can be read (or skipped
   * over) from this input stream without blocking.
   */
  fun available(): Int {
    return count - pos
  }

  /**
   * Resets the buffer to the marked position.  The marked position
   * is 0 unless another position was marked or an offset was specified
   * in the constructor.
   */
  fun reset() {
    pos = mark
  }

  fun read(b: ByteArray): Int {
    return read(b, 0, b.size)
  }
}
