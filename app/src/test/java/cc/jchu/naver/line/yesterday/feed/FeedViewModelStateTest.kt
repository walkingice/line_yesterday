package cc.jchu.naver.line.yesterday.feed

import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedViewModelStateTest {
    @Test
    fun defaultViewModelExposesEmptyReadyState() {
        val state = FeedViewModel().uiState.value

        assertEquals(emptyList<Any>(), state.items)
        assertEquals(false, state.initialLoading)
        assertEquals(false, state.refreshing)
        assertEquals(FeedFooterState.Ready, state.footerState)
    }
}
