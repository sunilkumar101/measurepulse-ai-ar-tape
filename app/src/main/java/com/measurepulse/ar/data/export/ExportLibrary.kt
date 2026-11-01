package com.measurepulse.ar.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Exported PDFs live in files/exports (not cache) so the Projects gallery can list and re-share them. */
object ExportLibrary {
    fun dir(context: Context): File = File(context.filesDir, "exports").apply { mkdirs() }

    fun list(context: Context): List<File> =
        dir(context).listFiles { f -> f.extension.equals("pdf", true) }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun shareIntent(context: Context, file: File, title: String = "Share PDF"): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension.replace('_', ' '))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun openIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
