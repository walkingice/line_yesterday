package cc.jchu.naver.line.yesterday.detail

import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent
import cc.jchu.naver.line.yesterday.data.domain.Detail
import cc.jchu.naver.line.yesterday.data.domain.DataError
import cc.jchu.naver.line.yesterday.data.repository.DetailReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailViewModelTest {
    @Test
    fun parsesSupportedArguments() {
        val arguments = DetailArguments.from(DetailActivity.SOURCE_DUMMY_JSON, "47")

        assertEquals(DetailArguments(FeedSource.DUMMY_JSON, "47"), arguments)
    }

    @Test
    fun rejectsUnsupportedOrBlankArguments() {
        assertNull(DetailArguments.from("unsupported", "47"))
        assertNull(DetailArguments.from(DetailActivity.SOURCE_SPACE_FLIGHT, " "))
        assertNull(DetailArguments.from(null, "47"))
    }

    @Test
    fun viewModelExposesValidatedArguments() {
        val viewModel = DetailViewModel(
            DetailArguments.from(DetailActivity.SOURCE_SPACE_FLIGHT, "39639"),
        )

        assertTrue(viewModel.isArgumentsValid)
        assertEquals(FeedSource.SPACE_FLIGHT, viewModel.detailArguments?.source)
        assertEquals("39639", viewModel.detailArguments?.id)
    }

    @Test
    fun startsOneLoadForValidArguments() {
        val reader = RecordingDetailReader()

        val viewModel = DetailViewModel(
            DetailArguments(FeedSource.DUMMY_JSON, "47"),
            reader,
            Dispatchers.Unconfined,
        )

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(1, reader.requests)
        assertEquals(FeedSource.DUMMY_JSON, reader.source)
        assertEquals("47", reader.id)
    }

    @Test
    fun doesNotLoadWithoutValidArgumentsOrReader() {
        val reader = RecordingDetailReader()

        DetailViewModel(null, reader, Dispatchers.Unconfined)
        DetailViewModel(DetailArguments(FeedSource.DUMMY_JSON, "47"), null)

        assertEquals(0, reader.requests)
    }

    @Test
    fun preservesStaleDetailWhenRefreshFails() {
        val detail = detail()
        val reader = EventDetailReader(
            flowOf(
                DetailLoadEvent.Cached(detail, isStale = true),
                DetailLoadEvent.RefreshFailed(DataError.Offline),
            ),
        )

        val viewModel = DetailViewModel(
            DetailArguments(FeedSource.DUMMY_JSON, "47"),
            reader,
            Dispatchers.Unconfined,
        )

        assertEquals(detail, viewModel.uiState.value.detail)
        assertEquals(DataError.Offline, viewModel.uiState.value.error)
        assertTrue(!viewModel.uiState.value.isLoading)
    }

    @Test
    fun updatedDetailEndsLoadingAndClearsError() {
        val detail = detail()
        val reader = EventDetailReader(flowOf(DetailLoadEvent.Updated(detail)))

        val viewModel = DetailViewModel(
            DetailArguments(FeedSource.DUMMY_JSON, "47"),
            reader,
            Dispatchers.Unconfined,
        )

        assertEquals(detail, viewModel.uiState.value.detail)
        assertNull(viewModel.uiState.value.error)
        assertTrue(!viewModel.uiState.value.isLoading)
    }

    @Test
    fun failedInitialLoadExposesRetry() {
        val reader = QueuedDetailReader(
            listOf(
                flowOf(DetailLoadEvent.LoadFailed(DataError.Offline)),
                flowOf(DetailLoadEvent.Updated(detail())),
            ),
        )
        val viewModel = DetailViewModel(
            DetailArguments(FeedSource.DUMMY_JSON, "47"),
            reader,
            Dispatchers.Unconfined,
        )

        assertTrue(viewModel.uiState.value.canRetry)
        viewModel.retry()

        assertEquals(2, reader.requests)
        assertTrue(!viewModel.uiState.value.canRetry)
        assertEquals("47", viewModel.uiState.value.detail?.id)
    }

    @Test
    fun retryDoesNotStartWhileLoading() {
        val reader = QueuedDetailReader(listOf(kotlinx.coroutines.flow.flow { }))
        val viewModel = DetailViewModel(
            DetailArguments(FeedSource.DUMMY_JSON, "47"),
            reader,
            Dispatchers.Unconfined,
        )

        viewModel.retry()

        assertEquals(1, reader.requests)
    }

    private class RecordingDetailReader : DetailReader {
        var requests = 0
        var source: FeedSource? = null
        var id: String? = null

        override fun getDetail(source: FeedSource, id: String) =
            emptyFlow<DetailLoadEvent>().also {
                requests++
                this.source = source
                this.id = id
            }
    }

    private class EventDetailReader(
        private val events: Flow<DetailLoadEvent>,
    ) : DetailReader {
        override fun getDetail(source: FeedSource, id: String) = events
    }

    private class QueuedDetailReader(
        private val eventFlows: List<Flow<DetailLoadEvent>>,
    ) : DetailReader {
        var requests = 0

        override fun getDetail(source: FeedSource, id: String): Flow<DetailLoadEvent> {
            return eventFlows[requests++].also { }
        }
    }

    private fun detail() = Detail(
        id = "47",
        source = FeedSource.DUMMY_JSON,
        title = "Product",
        imgUrl = "image",
        description = "Description",
        extraInformation = "category",
    )
}
