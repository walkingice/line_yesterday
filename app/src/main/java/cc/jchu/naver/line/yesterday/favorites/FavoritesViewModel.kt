package cc.jchu.naver.line.yesterday.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.jchu.naver.line.yesterday.data.domain.FavoritesUiState
import cc.jchu.naver.line.yesterday.data.provider.DefaultDispatcherProvider
import cc.jchu.naver.line.yesterday.data.provider.DispatcherProvider
import cc.jchu.naver.line.yesterday.data.repository.FavoritesRepository
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: FavoritesRepository? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
) : ViewModel() {
    val screenName = "Favorites"

    private val mutableUiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = mutableUiState.asStateFlow()

    init {
        refreshPage()
    }

    fun loadMoreItems() {
        val current = mutableUiState.value
        if (repository == null || current.footerState == FeedFooterState.NoMoreItems) return
        loadPage(current.items.size)
    }

    private fun refreshPage() {
        if (repository != null) loadPage(0)
    }

    private fun loadPage(offset: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            val page = repository!!.getPage(offset)
            val total = repository.count()
            val items = if (offset == 0) page else mutableUiState.value.items + page
            mutableUiState.value = FavoritesUiState(
                items = items.take(total),
                totalCount = total,
                footerState = if (items.size >= total) {
                    FeedFooterState.NoMoreItems
                } else {
                    FeedFooterState.Ready
                },
            )
        }
    }
}
