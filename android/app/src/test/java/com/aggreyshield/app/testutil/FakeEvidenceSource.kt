package com.aggreyshield.app.testutil

import com.aggreyshield.app.domain.model.EvidenceFileInfo
import com.aggreyshield.app.domain.repository.EvidenceSource

class FakeEvidenceSource : EvidenceSource {
    var infoToReturn: EvidenceFileInfo? = null

    override fun describe(uri: String): EvidenceFileInfo? = infoToReturn
    override fun read(uri: String): ByteArray = ByteArray(0)
}
