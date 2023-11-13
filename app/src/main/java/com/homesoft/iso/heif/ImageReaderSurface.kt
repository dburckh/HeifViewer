package com.homesoft.iso.heif

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.ImageReader
import android.media.MediaCodec
import android.os.Build
import android.os.Handler
import android.view.PixelCopy
import android.view.Surface
import androidx.annotation.RequiresApi

/**
 * Completely non-visual [Surface] provider
 */
@RequiresApi(Build.VERSION_CODES.N)
class ImageReaderSurface(surfaceListener: SurfaceListener,
                         private val bitmapListener: BitmapListener,
                         width:Int, height:Int,
                         private val handler: Handler):
    SurfaceDecoder, AutoCloseable {

    private val imageReader = ImageReader.newInstance(width, height, ImageFormat.PRIVATE, 1)
    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    init {
        surfaceListener.onSurfaceReady(imageReader.surface, this)
    }
    override fun onOutputBufferReleased(bufferInfo: MediaCodec.BufferInfo) {
        PixelCopy.request(imageReader.surface, bitmap, { rc->
            val image = imageReader.acquireLatestImage()
            val ptsUs = image.timestamp
            image.close()
            bitmapListener.onBitmap(if (rc == PixelCopy.SUCCESS) bitmap else null, ptsUs)
        }, handler)
    }

    override fun close() {
        imageReader.close()
    }
}
