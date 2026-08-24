package cc.jchu.naver.line.yesterday.data.repository

import cc.jchu.naver.line.yesterday.data.cache.CacheType
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheEntry
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheStore
import cc.jchu.naver.line.yesterday.data.client.SpaceFlightClient
import cc.jchu.naver.line.yesterday.data.domain.ClientResult
import cc.jchu.naver.line.yesterday.data.domain.DataError
import cc.jchu.naver.line.yesterday.data.domain.Detail
import cc.jchu.naver.line.yesterday.data.domain.FeedPageResult
import cc.jchu.naver.line.yesterday.data.domain.PageCursor
import cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent
import cc.jchu.naver.line.yesterday.data.dto.ApiDtoMappers
import cc.jchu.naver.line.yesterday.data.dto.ApiDtoParser
import cc.jchu.naver.line.yesterday.data.dto.FeedPageParser
import cc.jchu.naver.line.yesterday.data.provider.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SpaceFlightRepository(
    private val client: SpaceFlightClient,
    private val cacheStore: JsonCacheStore,
    private val timeProvider: TimeProvider,
    private val freshnessValidator: FreshnessValidator = FreshnessValidator(
        CACHE_FRESHNESS_DURATION_MILLIS,
        timeProvider,
    ),
    private val pageParser: FeedPageParser = FeedPageParser(),
    private val dtoParser: ApiDtoParser = ApiDtoParser(),
) {
    private var requiresNetworkRecovery = false

    suspend fun getFeedPage(cursor: PageCursor, forceRefresh: Boolean = false): FeedPageResult {
        val mode = when {
            forceRefresh -> FeedCacheMode.FORCE_REFRESH_FIRST_PAGE
            requiresNetworkRecovery -> FeedCacheMode.NETWORK_ONLY_RECOVERY
            else -> FeedCacheMode.NORMAL
        }
        val cacheRead = if (mode == FeedCacheMode.NORMAL) {
            runCatching { cacheStore.get(CacheType.SPACE_FLIGHT_FEED, cursor.value) }
        } else {
            Result.success(null)
        }
        val cached = cacheRead.getOrNull()
        val parsedCache = cached?.let(::parseFeed)
        if (cached != null && parsedCache != null && freshnessValidator.isFresh(cached.timestamp)) {
            return parsedCache
        }

        return when (val clientResult = client.getArticles(cursor)) {
            is ClientResult.Success -> {
                val parsed = parseFeed(clientResult.rawJson)
                if (parsed == null) {
                    failure(cursor, cached, parsedCache, DataError.Parse(IllegalArgumentException("Invalid SpaceFlight feed")))
                } else {
                    storeFeed(cursor, mode, clientResult.rawJson, parsed)
                }
            }
            ClientResult.Offline -> failure(cursor, cached, parsedCache, cacheFailure(cacheRead, DataError.Offline))
            is ClientResult.Failure -> failure(cursor, cached, parsedCache, DataError.Client(clientResult.cause))
        }
    }

    fun getDetail(id: String): Flow<DetailLoadEvent> = flow {
        val cache = runCatching { cacheStore.get(CacheType.SPACE_FLIGHT_DETAIL, id) }.getOrNull()
        val cached = cache?.let(::parseDetail)
        if (cached != null) {
            val stale = !freshnessValidator.isFresh(cache.timestamp)
            emit(DetailLoadEvent.Cached(cached, stale))
            if (!stale) return@flow
        }
        when (val result = client.getArticle(id)) {
            is ClientResult.Success -> {
                val detail = parseDetail(result.rawJson)
                if (detail == null) {
                    emit(detailEvent(cached, DataError.Parse(IllegalArgumentException("Invalid SpaceFlight detail"))))
                } else {
                    runCatching {
                        cacheStore.put(
                            CacheType.SPACE_FLIGHT_DETAIL,
                            id,
                            result.rawJson,
                            timeProvider.getCurrentTimeMillis(),
                        )
                    }
                    emit(DetailLoadEvent.Updated(detail))
                }
            }
            ClientResult.Offline -> emit(detailEvent(cached, DataError.Offline))
            is ClientResult.Failure -> emit(detailEvent(cached, DataError.Client(result.cause)))
        }
    }

    private suspend fun storeFeed(
        cursor: PageCursor,
        mode: FeedCacheMode,
        rawJson: String,
        result: FeedPageResult,
    ): FeedPageResult {
        if (mode == FeedCacheMode.NETWORK_ONLY_RECOVERY) return result
        return try {
            if (mode == FeedCacheMode.FORCE_REFRESH_FIRST_PAGE) {
                cacheStore.replaceFeedPages(
                    CacheType.SPACE_FLIGHT_FEED,
                    cursor.value,
                    rawJson,
                    timeProvider.getCurrentTimeMillis(),
                )
                requiresNetworkRecovery = false
            } else {
                cacheStore.put(
                    CacheType.SPACE_FLIGHT_FEED,
                    cursor.value,
                    rawJson,
                    timeProvider.getCurrentTimeMillis(),
                )
            }
            result
        } catch (cause: Throwable) {
            if (mode == FeedCacheMode.FORCE_REFRESH_FIRST_PAGE) requiresNetworkRecovery = true
            result.copy(cacheWarning = DataError.Storage(cause))
        }
    }

    private fun failure(
        cursor: PageCursor,
        cached: JsonCacheEntry?,
        parsedCache: FeedPageResult?,
        error: DataError,
    ): FeedPageResult = if (cached != null && parsedCache != null && !requiresNetworkRecovery) {
        parsedCache.copy(items = parsedCache.items, nextCursor = cursor, isStale = true, loadFailure = error)
    } else {
        FeedPageResult(emptyList(), null, false, loadFailure = error)
    }

    private fun parseFeed(entry: JsonCacheEntry): FeedPageResult? = runCatching {
        pageParser.parseSpaceFlight(entry.rawJson)
    }.getOrNull()

    private fun parseFeed(rawJson: String): FeedPageResult? = runCatching {
        pageParser.parseSpaceFlight(rawJson)
    }.getOrNull()

    private fun parseDetail(entry: JsonCacheEntry): Detail? = runCatching {
        ApiDtoMappers.run { dtoParser.parseSpaceFlightArticle(entry.rawJson).toDetail() }
    }.getOrNull()

    private fun parseDetail(rawJson: String): Detail? = runCatching {
        ApiDtoMappers.run { dtoParser.parseSpaceFlightArticle(rawJson).toDetail() }
    }.getOrNull()

    private fun detailEvent(cached: Detail?, error: DataError): DetailLoadEvent = if (cached == null) {
        DetailLoadEvent.LoadFailed(error)
    } else {
        DetailLoadEvent.RefreshFailed(error)
    }

    private fun cacheFailure(cacheRead: Result<JsonCacheEntry?>, fallback: DataError): DataError =
        cacheRead.exceptionOrNull()?.let(DataError::Storage) ?: fallback
}
