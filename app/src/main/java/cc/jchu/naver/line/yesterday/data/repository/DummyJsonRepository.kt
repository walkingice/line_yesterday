package cc.jchu.naver.line.yesterday.data.repository

import cc.jchu.naver.line.yesterday.data.cache.CacheType
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheEntry
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheStore
import cc.jchu.naver.line.yesterday.data.client.DummyJsonClient
import cc.jchu.naver.line.yesterday.data.domain.ClientResult
import cc.jchu.naver.line.yesterday.data.domain.DataError
import cc.jchu.naver.line.yesterday.data.domain.Detail
import cc.jchu.naver.line.yesterday.data.domain.FeedPageResult
import cc.jchu.naver.line.yesterday.data.domain.PageCursor
import cc.jchu.naver.line.yesterday.data.dto.ApiDtoMappers
import cc.jchu.naver.line.yesterday.data.dto.ApiDtoParser
import cc.jchu.naver.line.yesterday.data.dto.FeedPageParser
import cc.jchu.naver.line.yesterday.data.provider.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DummyJsonRepository(
    private val client: DummyJsonClient,
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

    suspend fun getFeedPage(
        cursor: PageCursor,
        forceRefresh: Boolean = false,
    ): FeedPageResult {
        val mode = when {
            forceRefresh -> FeedCacheMode.FORCE_REFRESH_FIRST_PAGE
            requiresNetworkRecovery -> FeedCacheMode.NETWORK_ONLY_RECOVERY
            else -> FeedCacheMode.NORMAL
        }
        return loadFeedPage(cursor, mode)
    }

    suspend fun loadFeedPage(
        cursor: PageCursor,
        forceRefresh: Boolean = false,
    ): FeedPageResult = getFeedPage(cursor, forceRefresh)

    fun getDetail(id: String): Flow<cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent> = flow {
        val cache = readCache(CacheType.DUMMY_JSON_DETAIL, id).getOrNull()
        val cached = cache?.let(::parseDetail)
        if (cached != null) {
            val stale = !freshnessValidator.isFresh(cache.timestamp)
            emit(cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent.Cached(cached, stale))
            if (!stale) return@flow
        }

        when (val result = client.getProduct(id)) {
            is ClientResult.Success -> {
                val detail = parseDetail(result.rawJson)
                if (detail == null) {
                    emit(detailFailure(cached, DataError.Parse(IllegalArgumentException("Invalid DummyJson detail"))))
                } else {
                    writeDetail(id, result.rawJson)
                    emit(cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent.Updated(detail))
                }
            }
            ClientResult.Offline -> emit(detailFailure(cached, DataError.Offline))
            is ClientResult.Failure -> emit(
                detailFailure(cached, DataError.Client(result.cause)),
            )
        }
    }

    private suspend fun loadFeedPage(cursor: PageCursor, mode: FeedCacheMode): FeedPageResult {
        val cacheRead = if (mode == FeedCacheMode.NETWORK_ONLY_RECOVERY ||
            mode == FeedCacheMode.FORCE_REFRESH_FIRST_PAGE
        ) {
            Result.success(null)
        } else {
            readCache(CacheType.DUMMY_JSON_FEED, cursor.value)
        }
        val cached = cacheRead.getOrNull()
        val parsedCache = cached?.let(::parseFeed)
        val cacheIsFresh = cached != null && parsedCache != null &&
            freshnessValidator.isFresh(cached.timestamp)
        if (cacheIsFresh) return parsedCache!!

        val clientResult = client.getProducts(cursor)
        if (clientResult is ClientResult.Success) {
            val parsed = parseFeed(clientResult.rawJson)
            if (parsed != null) return storeFeedResult(cursor, mode, clientResult.rawJson, parsed)
            return staleOrFailure(
                cursor,
                cached,
                parsedCache,
                DataError.Parse(IllegalArgumentException("Invalid DummyJson feed")),
            )
        }

        val error = clientResult.toDataError(cacheRead.exceptionOrNull())
        return staleOrFailure(cursor, cached, parsedCache, error)
    }

    private suspend fun storeFeedResult(
        cursor: PageCursor,
        mode: FeedCacheMode,
        rawJson: String,
        result: FeedPageResult,
    ): FeedPageResult {
        if (mode == FeedCacheMode.NETWORK_ONLY_RECOVERY) return result
        return try {
            if (mode == FeedCacheMode.FORCE_REFRESH_FIRST_PAGE) {
                cacheStore.replaceFeedPages(
                    CacheType.DUMMY_JSON_FEED,
                    cursor.value,
                    rawJson,
                    timeProvider.getCurrentTimeMillis(),
                )
                requiresNetworkRecovery = false
            } else {
                cacheStore.put(
                    CacheType.DUMMY_JSON_FEED,
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

    private fun staleOrFailure(
        cursor: PageCursor,
        cached: JsonCacheEntry?,
        parsedCache: FeedPageResult?,
        failure: DataError,
    ): FeedPageResult {
        if (cached != null && parsedCache != null && !requiresNetworkRecovery) {
            return parsedCache.copy(
                isStale = true,
                loadFailure = failure,
                nextCursor = cursor,
            )
        }
        return FeedPageResult(
            items = emptyList(),
            nextCursor = null,
            isExhausted = false,
            loadFailure = failure,
        )
    }

    private fun parseFeed(entry: JsonCacheEntry): FeedPageResult? = parseFeed(entry.rawJson)

    private fun parseFeed(rawJson: String): FeedPageResult? = runCatching {
        pageParser.parseDummyJson(rawJson)
    }.getOrNull()

    private fun parseDetail(entry: JsonCacheEntry): Detail? = parseDetail(entry.rawJson)

    private fun parseDetail(rawJson: String): Detail? = runCatching {
        dtoParser.parseDummyJsonProduct(rawJson).let {
            ApiDtoMappers.run { it.toDetail() }
        }
    }.getOrNull()

    private suspend fun readCache(type: CacheType, key: String): Result<JsonCacheEntry?> =
        runCatching { cacheStore.get(type, key) }

    private suspend fun writeDetail(id: String, rawJson: String) {
        runCatching {
            cacheStore.put(
                CacheType.DUMMY_JSON_DETAIL,
                id,
                rawJson,
                timeProvider.getCurrentTimeMillis(),
            )
        }
    }

    private fun detailFailure(cached: Detail?, error: DataError): cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent =
        if (cached == null) {
            cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent.LoadFailed(error)
        } else {
            cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent.RefreshFailed(error)
        }

    private fun ClientResult.toDataError(cacheFailure: Throwable?): DataError = when (this) {
        ClientResult.Offline -> if (cacheFailure == null) {
            DataError.Offline
        } else {
            DataError.Storage(cacheFailure)
        }
        is ClientResult.Failure -> DataError.Client(cause.withSuppressed(cacheFailure))
        is ClientResult.Success -> error("Success is not a failure")
    }

    private fun Throwable.withSuppressed(other: Throwable?): Throwable {
        if (other != null) addSuppressed(other)
        return this
    }
}
