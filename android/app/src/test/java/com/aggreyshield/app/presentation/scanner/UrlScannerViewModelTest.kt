package com.aggreyshield.app.presentation.scanner

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.AppError
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.domain.model.ScanResult
import com.aggreyshield.app.domain.model.Severity
import com.aggreyshield.app.testutil.FakeScanRepository
import com.aggreyshield.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class UrlScannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleResult = ScanResult(
        id = "scan-1",
        target = "http://example.com",
        riskLevel = Severity.HIGH,
        riskScore = 82.0,
        classification = "phishing",
        confidence = 0.9,
        indicators = emptyList(),
        reasons = listOf("Suspicious domain age", "Brand impersonation detected"),
        recommendation = "Do not enter credentials on this site.",
        scannedAt = null,
        domainInfo = null,
        redirects = emptyList(),
        reputation = emptyList(),
        modelVersion = "v1",
    )

    @Test
    fun `blank url shows a validation error and never calls the repository`() = runTest {
        val repository = FakeScanRepository()
        val viewModel = UrlScannerViewModel(repository)

        viewModel.onScan()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.urlError)
        assertEquals(0, repository.scanCallCount)
    }

    @Test
    fun `bare domain is normalized to http before being sent to the backend`() = runTest {
        val repository = FakeScanRepository().apply { nextUrlResult = ApiResult.Success(sampleResult) }
        val viewModel = UrlScannerViewModel(repository)

        viewModel.onUrlChanged("example.com")
        viewModel.onScan()
        advanceUntilIdle()

        assertEquals("http://example.com", repository.lastScannedUrl)
        assertEquals(sampleResult, viewModel.uiState.value.result)
    }

    @Test
    fun `a failed scan never fabricates a result and reports the real error`() = runTest {
        val repository = FakeScanRepository().apply {
            nextUrlResult = ApiResult.Failure(AppError.ServerUnavailable)
        }
        val viewModel = UrlScannerViewModel(repository)

        viewModel.onUrlChanged("example.com")
        viewModel.onScan()
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertEquals(OneShotEvent.ShowMessage(AppError.ServerUnavailable.userMessage), event)
        assertNull(viewModel.uiState.value.result)
    }
}
