package org.http4s.blaze.http.spdy

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.ISO_8859_1
import scala.annotation.tailrec
import org.http4s.blaze.util.{BufferTools, ScratchBuffer}
import java.util.zip.Deflater

class SpdyHeaderEncoder {
  import SpdyHeaderEncoder._

  private val deflater = new java.util.zip.Deflater
  deflater.setDictionary(spdyCompresionDict)

  def close() {
    deflater.end()
  }

  private def putHeaders(buff: ByteBuffer, headers: Map[String, Seq[String]]) {
    buff.putInt(headers.size)
    // Put all the data in there
    headers.foreach { case (k, v) =>
      buff.putInt(k.length)
        .put(k.toLowerCase.getBytes(ISO_8859_1))

      if (!v.isEmpty) {  // put the values in the buffer
      val pos = buff.position()
        buff.position(pos + 4)  // skip ahead of the value length

        v.foreach( v => buff.put(v.getBytes(ISO_8859_1)).put(0x0.toByte) )
        val keylen = buff.position() - pos - 1 - 4  // drop last byte
        buff.position(pos)

        buff.putInt(keylen)
        buff.position(pos + keylen + 4)  // skip to the end
      }
      else buff.putInt(0)   // No values
    }
  }

  @tailrec
  private def compressToBuffer(start: Int, buff: ByteBuffer): ByteBuffer = {
    val arr = buff.array()
    val pos = buff.position()

    val sz = deflater.deflate(arr, pos, arr.length - pos, Deflater.SYNC_FLUSH)

    if (sz + pos == arr.length) { // Not enough room

      // Don't go past the max header size
      if (arr.length <= 0xffffff)
        throw new ProtocolException(s"Compressed header length larger than 24 bit: ${sz + pos}")

      val n = BufferTools.allocate(math.min(0xffffff, 2*(arr.length - pos)))
      buff.limit(pos + sz)
      n.put(buff)
      compressToBuffer(0, n)
    }
    else {
      buff.limit(pos + sz).position(start)
      buff
    }
  }

  /* Takes headers and returns a frash ByteBuffer with the compressed data */
  def encodeHeaders(headers: Map[String, Seq[String]]): ByteBuffer = {
    // compute the size of the header field
    val headerlen = headers.foldLeft(0){(i, pair) =>
      val pairlen = pair._2.foldLeft(0)(_ + _.length + 1)
      i + pair._1.length + pairlen + 8 - 1
    }

    // Compress the headers into a scratch buffer. Give
    // one length for orig, two for compressed just in case
    val scratch = getScratchBuffer(headerlen * 3)

    putHeaders(scratch, headers)

    try {
      val arr = scratch.array()
      deflater.setInput(arr, 0, scratch.position())

      val buff = compressToBuffer(scratch.position(), scratch)
      if (buff eq scratch) {  // Need to copy it out of the scratch buffer
        val b = BufferTools.allocate(buff.remaining())
        b.put(buff).flip()
        b
      } else buff
    } catch { case t: Throwable => close(); throw t }
  }

}

private object SpdyHeaderEncoder extends ScratchBuffer
