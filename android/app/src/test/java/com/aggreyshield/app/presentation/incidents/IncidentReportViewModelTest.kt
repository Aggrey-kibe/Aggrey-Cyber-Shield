package com.aggreyshield.app.presentation.incidents

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.domain.model.EvidenceFileInfo
import com.aggreyshield.app.domain.model.IncidentCategory
import com.aggreyshield.app.domain.model.Severity
import com.aggreyshield.app.testutil.FakeEvidenceSource
import com.aggreyshield.app.testutil.FakeIncidentRepository
import com.aggreyshield.app.testutil.MainDispatcherRule
import com.aggreyshield.app.testutil.sampleIncident
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class IncidentReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submitting with a title that is too short shows a validation error`() = runTest {
        val repository = FakeIncidentRepository()
        val viewModel = IncidentReportViewModel(repository, FakeEvidenceSource())

        viewModel.onTitleChanged("Bad")
        viewModel.onDescriptionChanged("A description that is long enough to pass validation.")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.titleError)
        assertEquals(0, repository.createCallCount)
    }

    @Test
    fun `valid submission creates the incident with the selected category and severity`() = runTest {
        val repository = FakeIncidentRepository().apply {
            nextCreateResult = ApiResult.Success(sampleIncident(category = IncidentCategory.MALWARE, severity = Severity.HIGH))
        }
        val viewModel = IncidentReportViewModel(repository, FakeEvidenceSource())

        viewModel.onTitleChanged("Suspicious email attachment")
        viewModel.onDescriptionChanged("Received an email with an .exe file pretending to be an invoice.")
        viewModel.onCategorySelected(IncidentCategory.MALWARE)
        viewModel.onSeveritySelected(Severity.HIGH)
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, repository.createCallCount)
        assertEquals(IncidentCategory.MALWARE, repository.lastCreated?.category)
        assertEquals(Severity.HIGH, repository.lastCreated?.severity)

        val event = viewModel.events.first { it is OneShotEvent.Navigate }
        assertTrue(event is OneShotEvent.Navigate)
    }

    @Test
    fun `an oversized evidence file is rejected before upload`() {
        val viewModel = IncidentReportViewModel(FakeIncidentRepository(), FakeEvidenceSource().apply {
            infoToReturn = EvidenceFileInfo(uri = "content://fake", name = "huge.png", sizeBytes = 50L * 1024 * 1024, mimeType = "image/png")
        })

        viewModel.onEvidenceSelected("content://fake")

        assertNotNull(viewModel.uiState.value.evidenceError)
        assertEquals(null, viewModel.uiState.value.evidence)
    }

    @Test
    fun `a disallowed file type is rejected`() {
        val viewModel = IncidentReportViewModel(FakeIncidentRepository(), FakeEvidenceSource().apply {
            infoToReturn = EvidenceFileInfo(uri = "content://fake", name = "archive.zip", sizeBytes = 1024, mimeType = "application/zip")
        })

        viewModel.onEvidenceSelected("content://fake")

        assertNotNull(viewModel.uiState.value.evidenceError)
    }

    @Test
    fun `a valid evidence file is accepted and uploaded on submit`() = runTest {
        val repository = FakeIncidentRepository().apply {
            nextCreateResult = ApiResult.Success(sampleIncident())
        }
        val evidenceSource = FakeEvidenceSource().apply {
            infoToReturn = EvidenceFileInfo(uri = "content://fake", name = "screenshot.png", sizeBytes = 1024, mimeType = "image/png")
        }
        val viewModel = IncidentReportViewModel(repository, evidenceSource)

        viewModel.onTitleChanged("Phishing attempt received")
        viewModel.onDescriptionChanged("Got a message asking to confirm my bank details urgently.")
        viewModel.onEvidenceSelected("content://fake")
        assertNotNull(viewModel.uiState.value.evidence)

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, repository.uploadEvidenceCallCount)
    }
}
