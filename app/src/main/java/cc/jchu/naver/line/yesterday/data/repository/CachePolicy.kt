package cc.jchu.naver.line.yesterday.data.repository

import cc.jchu.naver.line.yesterday.data.provider.TimeProvider

const val CACHE_FRESHNESS_DURATION_MILLIS: Long = 5 * 60 * 1000L

class FreshnessValidator(
    private val durationMillis: Long,
    private val timeProvider: TimeProvider,
) {
    init {
        require(durationMillis > 0) { "Freshness duration must be positive" }
    }

    fun isFresh(timestamp: Long): Boolean {
        val age = maxOf(0L, timeProvider.getCurrentTimeMillis() - timestamp)
        return age < durationMillis
    }
}

internal enum class FeedCacheMode {
    NORMAL,
    FORCE_REFRESH_FIRST_PAGE,
    NETWORK_ONLY_RECOVERY,
}
