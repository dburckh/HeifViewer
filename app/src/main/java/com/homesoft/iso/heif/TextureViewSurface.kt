package com.homesoft.iso.heif;

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView

/**
 * Providers a [Surface] for [MediaCodec] and decodes Bitmaps from it.
 * Requires a [TextureView], but is backwards compatible to API 21
 * Generally you should prefer [ImageReaderSurface] unless you need to target pre-Android N
 */
class TextureViewSurface(private val surfaceListener: SurfaceListener,
                         private val bitmapListener: BitmapListener,
                         private val width:Int, private val height:Int,
                         private val textureView: TextureView):
    TextureView.SurfaceTextureListener, AutoCloseable, SurfaceDecoder {

    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        textureView.surfaceTextureListener = this
        val surfaceTexture = textureView.surfaceTexture
        if (surfaceTexture != null) {
            maybeNotify(surfaceTexture, textureView.width, textureView.height)
        }
    }

    private fun maybeNotify(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        if (width == this.width && height == this.height) {
            val mySurface = Surface(surfaceTexture)
            surfaceListener.onSurfaceReady(mySurface, this)
        } else {
            uiHandler.post {
                val layoutParams = textureView.layoutParams
                layoutParams.width = width
                layoutParams.height = height
                textureView.layoutParams = layoutParams
                textureView.translationX = -width.toFloat()
            }
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        //Log.d(TAG, "onSurfaceTextureAvailable() $width,$height")
        maybeNotify(surface, width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        //Log.d(TAG, "onSurfaceTextureSizeChanged() $width,$height")
        maybeNotify(surface, width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    override fun onOutputBufferReleased(bufferInfo: MediaCodec.BufferInfo) {
        val ptsUs = textureView.surfaceTexture?.timestamp ?: 0L
        textureView.getBitmap(bitmap)
        //Log.d(TAG, "onOutputBufferReleased() ${bitmap.width}x${bitmap.height} (0,0)=${bitmap.getPixel(0,0)}")
        bitmapListener.onBitmap(bitmap, ptsUs)
    }

    override fun close() {
        textureView.surfaceTextureListener = null
    }
}
