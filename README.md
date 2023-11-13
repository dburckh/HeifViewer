# Heif Viewer
This is an Android app that I was using to test the Heif parsing of IsoParserLite.  I was nerding out using MediaCodec to decode HEIC/AVIF manually.  For educational purposes only.  Use ImageDecoder or BitmapFactory for production code.

This also illustrates how to convert MediaCodec output to Bitmaps.  See TextureViewSurface.kt and ImageReaderSurface.kt for examples.
## Usage
- Use RadioButtons to select the Surface->Bitmap processor
- Click the Toolbar button, select a .heic or .avif file and be amazed (or not).
