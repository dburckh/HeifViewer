package com.homesoft.iso.heif;

import android.graphics.Bitmap

/**
 * Listens for the creation of a [Bitmap]
 */
interface BitmapListener {
    /**
     * @param bitmap null if an error occurred
     */
    fun onBitmap(bitmap: Bitmap?, ptsUs:Long)
}
