package net.aquadc.sphericalVacuumWebService.server

import org.junit.Assert.assertEquals
import org.junit.Test
import org.rapidoid.buffer.Buf
import org.rapidoid.buffer.BufBytes
import org.rapidoid.bytes.Bytes
import org.rapidoid.data.BufRange
import org.rapidoid.data.BufRanges
import org.rapidoid.wrap.IntWrap
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

class QueryParserTest {

    @Test fun parseTest() { // todo: url-decoding
        val query = "a=x&b=&x=fuck".toByteArray()
        val buf: Buf = object : Buf {
            override fun bufferIndexOf(position: Int): Int = TODO("not implemented")
            override fun write(byteValue: Int) = TODO("not implemented")
            override fun write(src: ByteArray?, offset: Int, length: Int) = TODO("not implemented")
            override fun append(value: Byte) = TODO("not implemented")
            override fun append(wrap: ByteBuffer?) = TODO("not implemented")
            override fun append(channel: ReadableByteChannel?): Int = TODO("not implemented")
            override fun append(s: String?): Int = TODO("not implemented")
            override fun append(bytes: ByteArray?) = TODO("not implemented")
            override fun append(bytes: ByteArray?, offset: Int, length: Int) = TODO("not implemented")
            override fun append(src: ByteArrayOutputStream?) = TODO("not implemented")
            override fun getN(range: BufRange?): Long = TODO("not implemented")
            override fun readNbytes(count: Int): ByteArray = TODO("not implemented")
            override fun size(): Int = TODO("not implemented")
            override fun deleteAfter(position: Int) = TODO("not implemented")
            override fun writeByte(byteValue: Byte) = TODO("not implemented")
            override fun getSingle(): ByteBuffer = TODO("not implemented")
            override fun asText(): String = TODO("not implemented")
            override fun readLn(): String = TODO("not implemented")
            override fun asOutputStream(): OutputStream = TODO("not implemented")
            override fun deleteBefore(position: Int) = TODO("not implemented")
            override fun deleteLast(count: Int) = TODO("not implemented")
            override fun position(): Int = TODO("not implemented")
            override fun position(position: Int) = TODO("not implemented")
            override fun data(): String = TODO("not implemented")
            override fun unitCount(): Int = TODO("not implemented")
            override fun scanUntil(value: Byte, range: BufRange?) = TODO("not implemented")
            override fun clear() = TODO("not implemented")
            override fun writeBytes(src: ByteArray?) = TODO("not implemented")
            override fun writeBytes(src: ByteArray?, offset: Int, length: Int) = TODO("not implemented")
            override fun putNumAsText(position: Int, num: Long, forward: Boolean): Int = TODO("not implemented")
            override fun bufferOffsetOf(position: Int): Int = TODO("not implemented")
            override fun readN(count: Int): String = TODO("not implemented")
            override fun unwrap(): Buf = TODO("not implemented")
            override fun scanTo(sep: Byte, range: BufRange?, failOnLimit: Boolean) = TODO("not implemented")
            override fun scanTo(sep1: Byte, sep2: Byte, range: BufRange?, failOnLimit: Boolean): Int = TODO("not implemented")
            override fun upto(value: Byte, range: BufRange?) = TODO("not implemented")
            override fun unitSize(): Int = TODO("not implemented")
            override fun next(): Byte = TODO("not implemented")
            override fun hasRemaining(): Boolean = TODO("not implemented")
            override fun exposed(): ByteBuffer = TODO("not implemented")
            override fun scanWhile(value: Byte, range: BufRange?) = TODO("not implemented")
            override fun get(position: Int): Byte = query[position]
            override fun get(range: BufRange?): String = TODO("not implemented")
            override fun get(range: BufRange?, dest: ByteArray?, offset: Int) = TODO("not implemented")
            override fun first(): ByteBuffer = TODO("not implemented")
            override fun bufCount(): Int = TODO("not implemented")
            override fun peek(): Byte = TODO("not implemented")
            override fun bufAt(index: Int): ByteBuffer = TODO("not implemented")
            override fun limit(): Int = TODO("not implemented")
            override fun limit(limit: Int) = TODO("not implemented")
            override fun setReadOnly(readOnly: Boolean) = TODO("not implemented")
            override fun put(position: Int, value: Byte) = TODO("not implemented")
            override fun put(position: Int, bytes: ByteArray?, offset: Int, length: Int) = TODO("not implemented")
            override fun isSingle(): Boolean = TODO("not implemented")
            override fun skip(count: Int) = TODO("not implemented")
            override fun scanLn(range: BufRange?) = TODO("not implemented")
            override fun back(count: Int) = TODO("not implemented")
            override fun bytes(): Bytes = BufBytes(this)
            override fun scanN(count: Int, range: BufRange?) = TODO("not implemented")
            override fun writeTo(channel: WritableByteChannel?): Int = TODO("not implemented")
            override fun writeTo(channel: WritableByteChannel?, srcOffset: Int, length: Int): Int = TODO("not implemented")
            override fun writeTo(buffer: ByteBuffer?): Int = TODO("not implemented")
            override fun writeTo(buffer: ByteBuffer?, srcOffset: Int, length: Int): Int = TODO("not implemented")
            override fun remaining(): Int = TODO("not implemented")
            override fun scanLnLn(ranges: BufRanges?) = TODO("not implemented")
            override fun scanLnLn(ranges: BufRanges?, result: IntWrap?, end1: Byte, end2: Byte) = TODO("not implemented")
            override fun checkpoint(): Int = TODO("not implemented")
            override fun checkpoint(checkpoint: Int) = TODO("not implemented")
        }
        val queryRange = BufRange(0, query.size)
        val countTarget = IntWrap()
        val queryTarget = BufRanges(10).apply { add(); add(); add(); add(); add(); add(); add(); add(); add(); add() }
        parseQueryString(buf, queryRange, countTarget, queryTarget)
        assertEquals(3, countTarget.value)
        assertEquals("a", queryTarget[0].str(buf))
        assertEquals("x", queryTarget[1].str(buf))
        assertEquals("b", queryTarget[2].str(buf))
        assertEquals("", queryTarget[3].str(buf))
        assertEquals("x", queryTarget[4].str(buf))
        assertEquals("fuck", queryTarget[5].str(buf))
    }

}