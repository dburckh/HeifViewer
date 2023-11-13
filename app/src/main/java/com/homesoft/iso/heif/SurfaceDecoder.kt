package com.homesoft.iso.heif

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo

/**
 * Called after a [MediaCodec] output buffer has been released.
 * Expected to eventually call [BitmapListener.onBitmap]
 */
interface SurfaceDecoder {
    fun onOutputBufferReleased(bufferInfo: BufferInfo)
}