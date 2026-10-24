package com.aggreyshield.app.data.local

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.aggreyshield.app.domain.model.EvidenceFileInfo
import com.aggreyshield.app.domain.repository.EvidenceSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads metadata and bytes for a file the user picked with the system document/photo picker
 * (a content:// Uri). No storage permission is required for this — the system grants
 * temporary read access to exactly the file the user selected.
 */
@Singleton
class EvidenceSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : EvidenceSource {

    override fun describe(uri: String): EvidenceFileInfo? {
        val parsed = uri.toUri()
        val resolver = context.contentResolver
        val mimeType = resolver.getType(parsed) ?: "application/octet-stream"

        var name = "evidence"
        var size = 0L
        resolver.query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) cursor.getString(nameIndex)?.let { name = it }
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                }
            } ?: return null

        return EvidenceFileInfo(uri = uri, name = name, sizeBytes = size, mimeType = mimeType)
    }

    override fun read(uri: String): ByteArray =
        context.contentResolver.openInputStream(uri.toUri())?.use { it.readBytes() }
            ?: throw java.io.IOException("Could not open the selected file.")
}
