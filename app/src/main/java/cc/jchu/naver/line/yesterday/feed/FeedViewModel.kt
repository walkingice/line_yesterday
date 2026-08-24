package cc.jchu.naver.line.yesterday.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.jchu.naver.line.yesterday.data.domain.FeedUiState
import cc.jchu.naver.line.yesterday.data.repository.DummyJsonRepository
import cc.jchu.naver.line.yesterday.data.repository.SpaceFlightRepository
import cc.jchu.naver.line.yesterday.data.provider.DispatcherProvider
import cc.jchu.naver.line.yesterday.data.provider.DefaultDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeedViewModel(
    private val dummyJsonRepository: DummyJsonRepository? = null,
    private val spaceFlightRepository: SpaceFlightRepository? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
) : ViewModel() {
    val screenName = "Feed"

    private val mutableUiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = mutableUiState.asStateFlow()

    private data class SourceState(
        val items: List<cc.jchu.naver.line.yesterday.data.domain.FeedItem> = emptyList(),
        val cursor: cc.jchu.naver.line.yesterday.data.domain.PageCursor =
            cc.jchu.naver.line.yesterday.data.domain.PageCursor("0"),
        val exhausted: Boolean = false,
    )

    private var dummyState = SourceState()
    private var spaceState = SourceState()

    init {
        if (dummyJsonRepository != null && spaceFlightRepository != null) {
            mutableUiState.value = FeedUiState(initialLoading = true, footerState =
                cc.jchu.naver.line.yesterday.data.domain.FeedFooterState.Loading)
        }
    }
}
