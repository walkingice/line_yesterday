package cc.jchu.naver.line.yesterday.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
        _uiState.value = _uiState.value.copy(isLoading = true)
        val arguments = checkNotNull(detailArguments)
        val reader = checkNotNull(detailReader)
        viewModelScope.launch(dispatcher) {
            reader.getDetail(arguments.source, arguments.id).collect { }
        }
    }
}

data class DetailUiState(
    val isLoading: Boolean = false,
)
