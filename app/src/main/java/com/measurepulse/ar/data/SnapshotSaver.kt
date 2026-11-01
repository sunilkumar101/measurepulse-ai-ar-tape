package com.measurepulse.ar.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saves annotated AR snapshots.
 * - Android 10+: Pictures/MeasurePulse in the shared gallery (no storage permission needed).
 * - Android 8–9: the app's own Pictures folder (also shown in the in-app Projects gallery).
 * A private copy is always kept for project thumbnails.
 */
@Singleton
class SnapshotSaver @Inject constructor(@ApplicationContext private val context: Context) {

    data class Saved(val galleryUri: Uri?, val privateFile: File)

    suspend fun save(bitmap: Bitmap): Saved = withContext(Dispatchers.IO) {
        val name = "MeasurePulse_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"

        val dir = File(context.filesDir, "snapshots").apply { mkdirs() }
        val private = File(dir, name)
        private.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }

        val galleryUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MeasurePulse")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also { uri ->
                resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } else {
            val ext = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ext?.let { File(it, name) }?.also { f -> private.copyTo(f, overwrite = true) }?.let { Uri.fromFile(it) }
        }
        Saved(galleryUri, private)
    }
}
