package cc.jchu.naver.line.yesterday.feed

import cc.jchu.naver.line.yesterday.data.domain.DataError
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.FeedPageResult
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedFooterEvaluationTest {
    @Test
    fun newItemsWinOverExhaustionAndFailures() {
        val result = FeedPageResult(emptyList(), null, isExhausted = true,
            loadFailure = DataError.Client(IllegalStateException()))

        assertEquals(FeedFooterState.Ready, evaluateLoadFooter(listOf(result), true, true))
    }

    @Test
    fun onlyBothExhaustedSourcesShowNoMoreItems() {
        val result = FeedPageResult(emptyList(), null, isExhausted = true)

        assertEquals(FeedFooterState.NoMoreItems, evaluateLoadFooter(listOf(result), false, true))
        assertEquals(FeedFooterState.Ready, evaluateLoadFooter(listOf(result), false, false))
    }

    @Test
    fun offlineAndMixedFailuresUseDifferentStates() {
        val offline = FeedPageResult(emptyList(), null, false, loadFailure = DataError.Offline)
        val client = FeedPageResult(emptyList(), null, false,
            loadFailure = DataError.Client(IllegalStateException()))

        assertEquals(FeedFooterState.Offline, evaluateLoadFooter(listOf(offline), false, false))
        assertEquals(FeedFooterState.Error, evaluateLoadFooter(listOf(offline, client), false, false))
    }
}
