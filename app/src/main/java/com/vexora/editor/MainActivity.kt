package com.vexora.editor

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var editor: EditorView
    private var previewVideo: VideoView? = null
    private var previewImage: ImageView? = null
    private var mediaUri: Uri? = null
    private var durationMs = 3080L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.rgb(242, 242, 242)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        root = object : FrameLayout(this) {
            override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                super.onSizeChanged(w, h, oldw, oldh)
                post { positionPreview() }
            }
        }.apply { setBackgroundColor(Color.rgb(20, 20, 24)) }

        editor = EditorView(this) { action -> handleAction(action) }
        root.addView(editor, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    private fun handleAction(action: String) {
        when (action) {
            "add" -> pickMedia()
            "play" -> togglePlay()
            "music" -> Toast.makeText(this, "Music track picker", Toast.LENGTH_SHORT).show()
            "subtitle" -> Toast.makeText(this, "Subtitle layer ready", Toast.LENGTH_SHORT).show()
            "overlay" -> pickMedia()
            "export" -> Toast.makeText(this, "Export", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this, "$action tool", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickMedia() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "image/*"))
        }
        startActivityForResult(intent, 1001)
    }

    @Deprecated("Activity result API kept simple for the standalone editor build")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 1001 || resultCode != RESULT_OK || data?.data == null) return
        mediaUri = data.data
        try {
            contentResolver.takePersistableUriPermission(mediaUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) { }
        loadPreview(mediaUri!!)
    }

    private fun clearPreview() {
        previewVideo?.stopPlayback()
        previewVideo?.let { root.removeView(it) }
        previewImage?.let { root.removeView(it) }
        previewVideo = null
        previewImage = null
    }

    private fun loadPreview(uri: Uri) {
        clearPreview()
        val mime = contentResolver.getType(uri) ?: ""
        if (mime.startsWith("image/")) {
            previewImage = ImageView(this).apply {
                setImageURI(uri)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.BLACK)
            }
            root.addView(previewImage, 0, previewParams())
            durationMs = 3080L
        } else {
            durationMs = readDuration(uri)
            previewVideo = VideoView(this).apply {
                setVideoURI(uri)
                setBackgroundColor(Color.BLACK)
                setOnPreparedListener { it.isLooping = true }
            }
            root.addView(previewVideo, 0, previewParams())
            previewVideo?.start()
        }
        editor.setMedia(durationMs)
        positionPreview()
    }

    private fun previewParams(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(601, 360)

    private fun positionPreview() {
        val v = previewVideo ?: previewImage ?: return
        val w = root.width
        val h = root.height
        if (w <= 0 || h <= 0) return
        val sx = w / 1536f
        val sy = h / 824f
        val lp = v.layoutParams as FrameLayout.LayoutParams
        lp.width = (601f * sx).toInt()
        lp.height = (360f * sy).toInt()
        lp.leftMargin = (467f * sx).toInt()
        lp.topMargin = (27f * sy).toInt()
        v.layoutParams = lp
        v.bringToFront()
        editor.bringToFront()
        // Keep the media below the editor's controls but above its empty preview placeholder.
        root.removeView(v)
        root.addView(v, 0, lp)
    }

    private fun readDuration(uri: Uri): Long = try {
        val r = MediaMetadataRetriever()
        r.setDataSource(this, uri)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 3080L
    } catch (_: Exception) { 3080L }

    private fun displayName(uri: Uri): String = try {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else "Media"
        } ?: "Media"
    } catch (_: Exception) { "Media" }

    private fun togglePlay() {
        previewVideo?.let { if (it.isPlaying) it.pause() else it.start() }
        editor.togglePlaying()
    }
}
