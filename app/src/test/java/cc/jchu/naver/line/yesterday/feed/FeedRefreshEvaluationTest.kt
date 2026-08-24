package cc.jchu.naver.line.yesterday.feed

import cc.jchu.naver.line.yesterday.data.domain.DataError
import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.FeedPageResult
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedRefreshEvaluationTest {
    @Test
    fun nonEmptySuccessfulRefreshRemainsReadyWhenItemsAreDuplicates() {
        val item = DummyJsonItem("1", "title", "", "category")
        val result = FeedPageResult(listOf(item), null, isExhausted = true)

        assertEquals(FeedFooterState.Ready, evaluateRefreshFooter(listOf(result), true))
    }

    @Test
    fun refreshFailureUsesOfflineOnlyWhenAllFailuresAreOffline() {
        val offline = FeedPageResult(emptyList(), null, false, loadFailure = DataError.Offline)
        val client = FeedPageResult(emptyList(), null, false,
            loadFailure = DataError.Client(IllegalStateException()))

        assertEquals(FeedFooterState.Offline, evaluateRefreshFooter(listOf(offline), false))
        assertEquals(FeedFooterState.Error, evaluateRefreshFooter(listOf(offline, client), false))
    }

    @Test
    fun refreshWithoutNonEmptySuccessCanShowNoMoreItems() {
        val result = FeedPageResult(emptyList(), null, isExhausted = true)

        assertEquals(FeedFooterState.NoMoreItems, evaluateRefreshFooter(listOf(result), true))
    }
}
