package cc.jchu.naver.line.yesterday.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.jchu.naver.line.yesterday.data.domain.DataError
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.FeedItem
import cc.jchu.naver.line.yesterday.data.domain.FeedPageResult
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.domain.FeedUiState
import cc.jchu.naver.line.yesterday.data.domain.PageCursor
import cc.jchu.naver.line.yesterday.data.provider.DefaultDispatcherProvider
import cc.jchu.naver.line.yesterday.data.provider.DispatcherProvider
import cc.jchu.naver.line.yesterday.data.repository.DummyJsonRepository
import cc.jchu.naver.line.yesterday.data.repository.SpaceFlightRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val dummyJsonRepository: DummyJsonRepository? = null,
    private val spaceFlightRepository: SpaceFlightRepository? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
) : ViewModel() {
    val screenName = "Feed"

    private val mutableUiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = mutableUiState.asStateFlow()

    private data class SourceState(
        val items: List<FeedItem> = emptyList(),
        val cursor: PageCursor = PageCursor("0"),
        val exhausted: Boolean = false,
    )

    private var dummyState = SourceState()
    private var spaceState = SourceState()
    private val operationLock = Any()
    private var operationRunning = false

    init {
        if (hasRepositories()) {
            mutableUiState.value = FeedUiState(
                initialLoading = true,
                footerState = FeedFooterState.Loading,
            )
            startLoadMore()
        }
    }

    fun loadMoreItems() {
        startLoadMore()
    }

    private fun startLoadMore() {
        if (!claimOperation()) return
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                val outcomes = loadSources()
                applyLoadResults(outcomes)
                mutableUiState.value = mutableUiState.value.copy(
                    initialLoading = false,
                    footerState = evaluateLoadFooter(
                        outcomes.map { it.result },
                        outcomes.any { it.added },
                        dummyState.exhausted && spaceState.exhausted,
                    ),
                )
            } finally {
                releaseOperation()
            }
        }
    }

    private data class SourceOutcome(
        val source: FeedSource,
        val result: FeedPageResult,
        var added: Boolean = false,
    )

    private suspend fun loadSources(): List<SourceOutcome> = coroutineScope {
        val dummy = if (!dummyState.exhausted) async {
            SourceOutcome(FeedSource.DUMMY_JSON, dummyJsonRepository!!.getFeedPage(dummyState.cursor))
        } else null
        val space = if (!spaceState.exhausted) async {
            SourceOutcome(FeedSource.SPACE_FLIGHT, spaceFlightRepository!!.getFeedPage(spaceState.cursor))
        } else null
        listOfNotNull(dummy?.await(), space?.await())
    }

    private fun applyLoadResults(outcomes: List<SourceOutcome>) {
        outcomes.forEach { outcome ->
            val current = stateFor(outcome.source)
            val newItems = deduplicateFeedItems(current.items + outcome.result.items)
            outcome.added = newItems.size > current.items.size
            if (outcome.result.loadFailure == null) {
                setState(outcome.source, SourceState(
                    items = newItems,
                    cursor = outcome.result.nextCursor ?: current.cursor,
                    exhausted = outcome.result.isExhausted,
                ))
            } else if (outcome.added) {
                setState(outcome.source, current.copy(items = newItems))
            }
        }
        publishItems()
    }

    private fun stateFor(source: FeedSource): SourceState = when (source) {
        FeedSource.DUMMY_JSON -> dummyState
        FeedSource.SPACE_FLIGHT -> spaceState
    }

    private fun setState(source: FeedSource, state: SourceState) {
        when (source) {
            FeedSource.DUMMY_JSON -> dummyState = state
            FeedSource.SPACE_FLIGHT -> spaceState = state
        }
    }

    private fun publishItems() {
        mutableUiState.value = mutableUiState.value.copy(
            items = deduplicateFeedItems(mergeFeedItems(dummyState.items, spaceState.items)),
        )
    }

    private fun hasRepositories(): Boolean =
        dummyJsonRepository != null && spaceFlightRepository != null

    private fun claimOperation(): Boolean = synchronized(operationLock) {
        if (!hasRepositories() || operationRunning || mutableUiState.value.refreshing ||
            mutableUiState.value.footerState == FeedFooterState.NoMoreItems
        ) return false
        operationRunning = true
        mutableUiState.value = mutableUiState.value.copy(footerState = FeedFooterState.Loading)
        true
    }

    private fun releaseOperation() = synchronized(operationLock) {
        operationRunning = false
    }
}

internal fun evaluateLoadFooter(
    results: List<FeedPageResult>,
    addedItems: Boolean,
    allSourcesExhausted: Boolean = results.isNotEmpty() &&
        results.all(FeedPageResult::isExhausted),
): FeedFooterState {
    if (addedItems) return FeedFooterState.Ready
    if (allSourcesExhausted) {
        return FeedFooterState.NoMoreItems
    }
    val failures = results.mapNotNull(FeedPageResult::loadFailure)
    if (failures.isNotEmpty()) {
        return if (failures.all { it is DataError.Offline }) {
            FeedFooterState.Offline
        } else {
            FeedFooterState.Error
        }
    }
    return FeedFooterState.Ready
}
