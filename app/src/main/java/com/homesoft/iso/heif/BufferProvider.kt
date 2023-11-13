package com.homesoft.iso.heif

import android.media.MediaCodec
import java.nio.ByteBuffer

interface BufferProvider:AutoCloseable {
    /**
     * Populate the next image buffer
     * @return false if the last buffer
     */
    fun populateImageBuffer(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo):Int
}