package cc.jchu.naver.line.yesterday.data.domain

sealed interface DataError {
    data object Offline : DataError
    data class Client(val cause: Throwable) : DataError
    data class Parse(val cause: Throwable) : DataError
    data class Storage(val cause: Throwable) : DataError
}

sealed interface ClientResult {
    data class Success(val rawJson: String) : ClientResult
    data object Offline : ClientResult
    data class Failure(val cause: Throwable) : ClientResult
}

data class FeedPageResult(
    val items: List<FeedItem>,
    val nextCursor: PageCursor?,
    val isExhausted: Boolean,
    val isStale: Boolean = false,
    val loadFailure: DataError? = null,
    val cacheWarning: DataError.Storage? = null,
)

sealed interface DetailLoadEvent {
    data class Cached(val detail: Detail, val isStale: Boolean) : DetailLoadEvent
    data class Updated(val detail: Detail) : DetailLoadEvent
    data class RefreshFailed(val error: DataError) : DetailLoadEvent
    data class LoadFailed(val error: DataError) : DetailLoadEvent
}

sealed interface FeedFooterState {
    data object Ready : FeedFooterState
    data object Loading : FeedFooterState
    data object NoMoreItems : FeedFooterState
    data object Error : FeedFooterState
    data object Offline : FeedFooterState
}

data class FeedUiState(
    val items: List<FeedItem> = emptyList(),
    val initialLoading: Boolean = false,
    val refreshing: Boolean = false,
    val footerState: FeedFooterState = FeedFooterState.Ready,
)

data class FavoritesUiState(
    val items: List<FeedItem> = emptyList(),
    val totalCount: Int = 0,
    val footerState: FeedFooterState = FeedFooterState.Ready,
)
