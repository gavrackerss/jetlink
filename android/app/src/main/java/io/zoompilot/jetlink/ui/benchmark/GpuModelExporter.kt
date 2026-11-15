package io.zoompilot.jetlink.ui.benchmark

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Exports the exact GPU-prepared LiteRT model associated with the ready engine.
 *
 * Cache layout, as built by Jetlink's ServerCache:
 * <filesDir>/jetlink/cache/engines/<sha16>.<device tag>.litertcache/model.tflite
 * <filesDir>/jetlink/cache/engines/<sha16>.<device tag>.json
 *
 * Only the user-selected Storage Access Framework destination is written.
 * The app does NOT duplicate the ~700 MB model into its cache.
 */
internal object GpuModelExporter {
    private val shaPattern = Regex("^[0-9a-fA-F]{64}$")
    private val expectedIdentifier = byteArrayOf('T'.code.toByte(), 'F'.code.toByte(), 'L'.code.toByte(), '3'.code.toByte())

    data class ExportResult(val bytes: Long, val sha256: String) {
        val mib: Double get() = bytes.toDouble() / (1024.0 * 1024.0)
    }

    fun locate(cacheDirectory: File, modelSha256: String): File {
        require(shaPattern.matches(modelSha256)) { "Loaded model has no valid full SHA256." }
        val root = File(cacheDirectory, "engines")
        val artifacts = root.listFiles()?.filter { artifact ->
            artifact.isDirectory &&
                artifact.name.endsWith(".litertcache") &&
                artifact.name.startsWith(modelSha256.take(16) + ".") &&
                artifact.name.contains(".gpu-", ignoreCase = true)
        }.orEmpty()

        // Don't return an artifact just because the first 16 SHA digits match.
        // The sidecar supplies the full SHA and is checked before exporting.
        val matching = artifacts.filter { artifact ->
            val stem = artifact.name.removeSuffix(".litertcache")
            val sidecar = File(root, "$stem.json")
            if (!sidecar.isFile) return@filter false
            try {
                val value = JSONObject(sidecar.readText())
                    .optJSONObject("spec")
                    ?.optString("sha256")
                    ?: return@filter false
                value.equals(modelSha256, ignoreCase = true)
            } catch (_: Exception) {
                false
            }
        }

        if (matching.size != 1) {
            throw IOException(
                if (matching.isEmpty()) {
                    "No GPU LiteRT artifact for the loaded SHA. Prepare this model on GPU in Jetlink first."
                } else {
                    "More than one GPU artifact matches. Refusing to export an ambiguous model."
                },
            )
        }

        val source = File(matching.single(), "model.tflite")
        if (!source.isFile || !source.canRead() || source.length() < 16) {
            throw IOException("The GPU artifact has no readable model.tflite.")
        }
        // TFLite FlatBuffers carry the TFL3 identifier at bytes 4–7.
        val identifier = ByteArray(4)
        FileInputStream(source).use { input ->
            if (input.skip(4) != 4L || input.read(identifier) != 4 ||
                !identifier.contentEquals(expectedIdentifier)
            ) {
                throw IOException("Artifact does not have the TFL3 FlatBuffer identifier.")
            }
        }
        return source
    }

    /**
     * Streams directly to the Uri granted by ACTION_CREATE_DOCUMENT.
     * Uses ~1 MiB of RAM rather than copying the model to another app directory.
     * Returns the SHA256 of the bytes written, enabling export verification.
     */
    fun export(context: Context, source: File, destination: Uri): ExportResult {
        if (!source.isFile) throw IOException("Prepared LiteRT model disappeared before export.")
        val expectedBytes = source.length()
        val digest = MessageDigest.getInstance("SHA-256")
        var copied = 0L
        val buffer = ByteArray(1024 * 1024)

        val output = context.contentResolver.openOutputStream(destination, "w")
            ?: throw IOException("Android's selected document provider refused the output stream.")
        output.use { sink ->
            FileInputStream(source).use { input ->
                while (true) {
                    val got = input.read(buffer)
                    if (got < 0) break
                    sink.write(buffer, 0, got)
                    digest.update(buffer, 0, got)
                    copied += got
                }
                sink.flush()
            }
        }
        if (copied != expectedBytes) {
            throw IOException(
                "Source changed during export ($copied of $expectedBytes bytes copied); discard the partial destination.",
            )
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        return ExportResult(copied, hash)
    }
}
