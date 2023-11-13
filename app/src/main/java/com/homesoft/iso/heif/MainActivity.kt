package com.homesoft.iso.heif

import android.graphics.Bitmap
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.RadioButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.homesoft.iso.Heif
import com.homesoft.iso.Heif.Grid
import com.homesoft.iso.Heif.Image
import com.homesoft.iso.reader.CodecSpecificData
import com.homesoft.iso.reader.HevcDecoderConfig
import com.homesoft.iso.reader.ItemInfoEntry
import java.nio.ByteBuffer
import java.util.Collections


class MainActivity : AppCompatActivity(), BitmapListener {
    private val handlerThread = HandlerThread("HeifWorker")
    private lateinit var imageView: ImageView
    private lateinit var textureView: TextureView
    private lateinit var radioTextureView: RadioButton
    private lateinit var radioImageReader: RadioButton

    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var videoDecoder: VideoDecoder? = null
    private var surfaceProvider: AutoCloseable? = null

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val handler = Handler(handlerThread.looper)
            handler.post {
                cleanUp()
                contentResolver.openFileDescriptor(it, "r")?.let {pfd ->
                    val reader = FDStreamReader(pfd.fileDescriptor, 512)
                    val heif = Heif.parse(reader)
                    val primaryItem = heif.primaryItem
                    val imageList: List<Image>
                    val bitmapListener: BitmapListener
                    val image:Image
                    if (primaryItem is Grid) {
                        bitmapListener = GridDecoder(primaryItem, this)
                        imageList = primaryItem.imageList
                        image = imageList[0]
                    } else if (primaryItem is Image) {
                        bitmapListener = this
                        image = primaryItem
                        imageList = Collections.singletonList(image)
                    } else {
                        return@let
                    }
                    val imageSpatialExtents = image.imageSpatialExtents
                    val bufferProvider = ImageListBufferProvider(reader, imageList)
                    val surfaceListener = object:SurfaceListener {
                        override fun onSurfaceReady(surface: Surface, surfaceDecoder: SurfaceDecoder) {
                            val mediaFormat = getMediaFormat(image)
                            videoDecoder = VideoDecoder(mediaFormat, surface, handler, bufferProvider, surfaceDecoder)
                            videoDecoder?.queueInputBuffer()
                        }
                    }
                    // Keeps the VideoDecoder from overrunning the Surface
                    val bufferThrottle = object:BitmapListener {
                        override fun onBitmap(bitmap: Bitmap?, ptsUs: Long) {
                            bitmapListener.onBitmap(bitmap, ptsUs)
                            videoDecoder?.queueInputBuffer()
                        }
                    }
                    runOnUiThread { imageView.setImageBitmap(null) }
                    if (radioTextureView.isChecked) {
                        surfaceProvider = TextureViewSurface(surfaceListener, bufferThrottle, imageSpatialExtents.width, imageSpatialExtents.height, textureView)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && radioImageReader.isChecked) {
                        surfaceProvider = ImageReaderSurface(surfaceListener, bufferThrottle, imageSpatialExtents.width, imageSpatialExtents.height, handler)
                    } else {
                        return@let
                    }
                    parcelFileDescriptor = pfd
                }
            }
        }
    }

    init {
        handlerThread.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        radioTextureView = findViewById(R.id.radioTextureView)
        radioImageReader = findViewById(R.id.radioImageReader)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            radioImageReader.isEnabled = false
        }
        setSupportActionBar(toolbar)
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread.quit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.open) {
            openDocument.launch(arrayOf("*/*"))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun cleanUp() {
        parcelFileDescriptor = null
        videoDecoder?.let{
            it.close()
            videoDecoder = null
        }
        surfaceProvider?.let {
            it.close()
            surfaceProvider = null
        }
    }

    @WorkerThread
    override fun onBitmap(bitmap: Bitmap?, ptsUs: Long) {
        runOnUiThread {
            bitmap?.let {
                Log.d("MainActivity", "0x0={${it.getPixel(0,0)}")
            }
            imageView.setImageBitmap(bitmap)
        }
        cleanUp()
    }
    companion object {
        fun getMediaFormat(image: Image): MediaFormat {
            val codecSpecificData = image.codecSpecificData
            val csdList = codecSpecificData.typedConfigList
            if (csdList.isEmpty()) {
                throw IllegalArgumentException("CodeSpecificData empty")
            }
            val mime: String

            val csd0: ByteBuffer
            when (image.type) {
                ItemInfoEntry.ITEM_TYPE_hvc1 -> {
                    mime = MediaFormat.MIMETYPE_VIDEO_HEVC
                    val vps = CodecSpecificData.TypedConfig.findType(HevcDecoderConfig.TYPE_VPS, csdList)
                    val vpsSize = vps?.capacity() ?: throw IllegalArgumentException("VPS Required")
                    val sps = CodecSpecificData.TypedConfig.findType(HevcDecoderConfig.TYPE_SPS, csdList)
                    val spsSize = sps?.capacity() ?: throw IllegalArgumentException("SPS Required")
                    val pps = CodecSpecificData.TypedConfig.findType(HevcDecoderConfig.TYPE_PPS, csdList)
                    val ppsSize = pps?.capacity() ?: throw IllegalArgumentException("PPS Required")
                    csd0 = ByteBuffer.allocateDirect(vpsSize + spsSize + ppsSize + 12)
                    csd0.putInt(1)
                    csd0.put(vps)
                    csd0.putInt(1)
                    csd0.put(sps)
                    csd0.putInt(1)
                    csd0.put(pps)
                    csd0.clear()
                }
                ItemInfoEntry.ITEM_TYPE_av01 -> {
                    mime = MediaFormat.MIMETYPE_VIDEO_AV1
                    // Codec tries to access bytes directly, which blows up on RO ByteBuffer
                    val csd0ro = csdList[0].byteBuffer
                    csd0 = ByteBuffer.allocateDirect(csd0ro.capacity())
                    csd0.put(csd0ro)
                }
                else -> throw IllegalArgumentException("Unknown type")
            }
            val spatialExtents = image.imageSpatialExtents
            val itemLocation = image.itemLocation

            if (spatialExtents == null || itemLocation == null || itemLocation.extentCount == 0) {
                throw IllegalArgumentException("Missing Required Data")
            }
            val mediaFormat = MediaFormat.createVideoFormat(mime, spatialExtents.width, spatialExtents.height)
            mediaFormat.setByteBuffer("csd-0", csd0)
            return mediaFormat
        }
    }
}

