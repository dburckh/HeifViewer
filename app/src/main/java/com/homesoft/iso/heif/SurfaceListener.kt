package com.homesoft.iso.heif

import android.view.Surface

/**
 * Called when a [Surface] is ready for the [VideoDecoder]
 */
interface SurfaceListener {
    fun onSurfaceReady(surface: Surface, surfaceDecoder: SurfaceDecoder)
}