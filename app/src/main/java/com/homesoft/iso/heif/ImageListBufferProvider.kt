package com.homesoft.iso.heif

import android.media.MediaCodec
import com.homesoft.iso.Heif.Image
import com.homesoft.iso.RandomStreamReader
import com.homesoft.iso.reader.ItemInfoEntry
import java.nio.ByteBuffer

/**
 * Fills the [MediaCodec] input [ByteBuffer]s
 */
class ImageListBufferProvider(private val reader: RandomStreamReader,
                              private val imageList: List<Image>):
    AutoCloseable, BufferProvider {

    private var index = 0

    override fun populateImageBuffer(
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ): Int {
        //Log.d("BufferProvider", "populateImageBuffer() $index")
        if (index >= imageList.size) {
            return MediaCodec.BUFFER_FLAG_END_OF_STREAM // 4
        }
        val image = imageList[index++]
        if (image.itemLocation.extentCount > 0) {
            if (image.type == ItemInfoEntry.ITEM_TYPE_hvc1) {
                bufferInfo.size = image.readExtentAsByteStream(0, reader, byteBuffer)
            } else {
                bufferInfo.size = image.readExtent(0, reader, byteBuffer)
            }
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
            bufferInfo.presentationTimeUs = index.toLong()
        }
        return 0
    }

    override fun close() {
        reader.close()
    }
}