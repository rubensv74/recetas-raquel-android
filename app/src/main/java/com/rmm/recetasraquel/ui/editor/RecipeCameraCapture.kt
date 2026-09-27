package com.rmm.recetasraquel.ui.editor

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

internal data class PendingCameraCapture(
    val uri: Uri,
    val file: File,
)

internal object RecipeCameraCapture {
    private const val CAPTURE_DIR = "recipe_camera_captures"

    fun create(context: Context): PendingCameraCapture {
        val captureDir = File(context.cacheDir, CAPTURE_DIR).also { it.mkdirs() }
        cleanupStaleFiles(captureDir)

        val file = File(captureDir, "capture_\${UUID.randomUUID()}.jpg")
        if (!file.createNewFile()) {
            error("No se pudo preparar el archivo temporal de cámara")
        }

        val uri = FileProvider.getUriForFile(
            context,
            "\${context.packageName}.fileprovider",
            file,
        )
        return PendingCameraCapture(uri = uri, file = file)
    }

    fun discard(capture: PendingCameraCapture?) {
        capture?.file?.takeIf(File::exists)?.delete()
    }

    private fun cleanupStaleFiles(directory: File) {
        val staleBefore = System.currentTimeMillis() - STALE_CAPTURE_AGE_MS
        directory.listFiles()
            ?.filter { it.isFile && it.lastModified() < staleBefore }
            ?.forEach(File::delete)
    }

    private const val STALE_CAPTURE_AGE_MS = 24L * 60L * 60L * 1000L
}
