package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.domain.model.EvidenceFileInfo

/** Reads a file the user picked with the system file picker. No storage permission is needed. */
interface EvidenceSource {
    fun describe(uri: String): EvidenceFileInfo?
    fun read(uri: String): ByteArray
}
