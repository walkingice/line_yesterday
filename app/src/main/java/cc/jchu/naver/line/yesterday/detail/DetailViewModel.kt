package cc.jchu.naver.line.yesterday.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cc.jchu.naver.line.yesterday.data.domain.DataError
import cc.jchu.naver.line.yesterday.data.domain.Detail
import cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.repository.DetailReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class DetailArguments(
    val source: FeedSource,
    val id: String,
) {
    companion object {
        fun from(source: String?, id: String?): DetailArguments? {
            val feedSource = when (source) {
                DetailActivity.SOURCE_DUMMY_JSON -> FeedSource.DUMMY_JSON
                DetailActivity.SOURCE_SPACE_FLIGHT -> FeedSource.SPACE_FLIGHT
                else -> null
            }
            return if (feedSource != null && !id.isNullOrBlank()) {
                DetailArguments(feedSource, id)
            } else {
                null
            }
        }
    }
}

class DetailViewModel(
    val detailArguments: DetailArguments? = null,
    private val detailReader: DetailReader? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    val screenName = "Detail"

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        if (isArgumentsValid && detailReader != null) startInitialLoad()
    }

    val isArgumentsValid: Boolean
        get() = detailArguments != null

    fun retry() {
        if (!isArgumentsValid || detailReader == null || _uiState.value.isLoading) return
        startLoad()
    }

    class Factory(
        private val detailArguments: DetailArguments?,
        private val detailReader: DetailReader? = null,
        private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(detailArguments, detailReader, dispatcher) as T
    }

    private fun startInitialLoad() {
        startLoad()
    }

    private fun startLoad() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val arguments = checkNotNull(detailArguments)
        val reader = checkNotNull(detailReader)
        viewModelScope.launch(dispatcher) {
            reader.getDetail(arguments.source, arguments.id).collect(::handleEvent)
        }
    }

    private fun handleEvent(event: DetailLoadEvent) {
        _uiState.value = when (event) {
            is DetailLoadEvent.Cached -> _uiState.value.copy(
                detail = event.detail,
                isLoading = event.isStale,
                error = null,
            )
            is DetailLoadEvent.Updated -> _uiState.value.copy(
                detail = event.detail,
                isLoading = false,
                error = null,
            )
            is DetailLoadEvent.RefreshFailed -> _uiState.value.copy(
                isLoading = false,
                error = event.error,
            )
            is DetailLoadEvent.LoadFailed -> _uiState.value.copy(
                isLoading = false,
                error = event.error,
            )
        }
    }
}

data class DetailUiState(
    val isLoading: Boolean = false,
    val detail: Detail? = null,
    val error: DataError? = null,
) {
    val canRetry: Boolean
        get() = detail == null && error != null && !isLoading
}
