package com.homesoft.iso.heif

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.Surface
import java.io.IOException
import java.nio.ByteBuffer
import java.util.BitSet

/**
 * Wrapper around [MediaCodec] calls back to a [BufferProvider] to fill its input [ByteBuffer]s
 */
class VideoDecoder(mediaFormat: MediaFormat, surface: Surface, handler:Handler,
                   private val bufferProvider: BufferProvider, private val surfaceDecoder: SurfaceDecoder):
    MediaCodec.Callback(), AutoCloseable {
    private val mediaCodec: MediaCodec
    private val bufferInfo = BufferInfo()
    private val bitSet = BitSet()
    private var queuePending = false

    init {
        val mime =
            mediaFormat.getString(MediaFormat.KEY_MIME) ?: throw IOException("Mime required.")
        mediaCodec = MediaCodec.createDecoderByType(mime)
        //mediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaCodec.setCallback(this, handler)
        } else {
            mediaCodec.setCallback(this)
        }
        mediaCodec.configure(mediaFormat, surface, null, 0)
        mediaCodec.start()
    }

    override fun close() {
        mediaCodec.flush()
        mediaCodec.release()
        bufferProvider.close()
    }

    fun queueInputBuffer() {
        val index = bitSet.nextSetBit(0)
        if (index >= 0) {
            queueInputBuffer(index)
        } else {
            queuePending = true
        }
    }

    private fun queueInputBuffer(index: Int) {
        mediaCodec.getInputBuffer(index)?.let { byteBuffer ->
            when (bufferProvider.populateImageBuffer(byteBuffer, bufferInfo)) {
                0 -> {
                    //Log.d("VideoDecoder", "queueInputBuffer ${bufferInfo.presentationTimeUs}")
                    mediaCodec.queueInputBuffer(
                        index, bufferInfo.offset, bufferInfo.size,
                        bufferInfo.presentationTimeUs, bufferInfo.flags
                    )
                }

                MediaCodec.BUFFER_FLAG_END_OF_STREAM -> {
                    mediaCodec.queueInputBuffer(
                        index,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                }
                // Subtle: return without clearing bit
                MediaCodec.INFO_TRY_AGAIN_LATER -> return
            }
            bitSet.clear(index)
        }
    }

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        bitSet.set(index)
        if (queuePending) {
            queuePending = false
            queueInputBuffer()
        }
    }

    override fun onOutputBufferAvailable(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo
    ) {
        mediaCodec.releaseOutputBuffer(index, true)
        //Log.d("VideoDecoder", "onOutputBufferAvailable ${info.presentationTimeUs}")
        surfaceDecoder.onOutputBufferReleased(info)
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        Log.e(TAG, "Decoder Error", e)
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        //Log.d(TAG, "Output Format: $format")
    }

    private fun byteArrayToHex(byteBuffer: ByteBuffer): String {
        val sb = StringBuilder(byteBuffer.remaining() * 3)
        while (byteBuffer.hasRemaining()) {
            sb.append(String.format("%02x ", byteBuffer.get()))
        }
        return sb.toString()
    }

    companion object {
        const val TAG = "ImageDecoder"
   }
}