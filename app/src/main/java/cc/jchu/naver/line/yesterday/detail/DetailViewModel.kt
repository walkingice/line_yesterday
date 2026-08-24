package cc.jchu.naver.line.yesterday.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cc.jchu.naver.line.yesterday.data.domain.FeedSource

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
) : ViewModel() {
    val screenName = "Detail"

    val isArgumentsValid: Boolean
        get() = detailArguments != null

    class Factory(
        private val detailArguments: DetailArguments?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(detailArguments) as T
    }
}
