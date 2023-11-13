package com.homesoft.iso.heif

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.homesoft.iso.Heif.Grid
import com.homesoft.iso.Heif.Image

/**
 * Decodes a [Grid].  A grid is a matrix of [Image]s
 */
class GridDecoder(grid: Grid,
                  private val listener:BitmapListener):
    BitmapListener {
    private val gridBitmap:Bitmap = run {
        val spatialExtents = grid.imageSpatialExtents
        Bitmap.createBitmap(spatialExtents.width,
            spatialExtents.height, Bitmap.Config.ARGB_8888)
    }
    private val canvas = Canvas(gridBitmap)
    private val rect = Rect()

    override fun onBitmap(bitmap: Bitmap?, ptsUs: Long) {
        if (bitmap == null) {
            listener.onBitmap(null, ptsUs)
            return
        }
        rect.right = rect.right + bitmap.width
        rect.bottom = rect.top + bitmap.height
        canvas.drawBitmap(bitmap, null, rect, null)
        rect.left = rect.right
        if (rect.right >= gridBitmap.width) {
            rect.set(0, rect.bottom, 0, rect.bottom)
        }
        if (rect.top >= gridBitmap.height) {
            listener.onBitmap(gridBitmap, ptsUs)
        }
    }
}