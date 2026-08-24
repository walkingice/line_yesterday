package cc.jchu.naver.line.yesterday.feed

import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedFragmentActionsTest {
    @Test
    fun readyFooterLoadsMore() {
        val actions = mutableListOf<String>()

        handleFooterAction(
            FeedFooterState.Ready,
            loadMore = { actions += "load" },
            retry = { actions += "retry" },
        )

        assertEquals(listOf("load"), actions)
    }

    @Test
    fun failedFooterRetries() {
        listOf(FeedFooterState.Error, FeedFooterState.Offline).forEach { state ->
            val actions = mutableListOf<String>()

            handleFooterAction(
                state,
                loadMore = { actions += "load" },
                retry = { actions += "retry" },
            )

            assertEquals(listOf("retry"), actions)
        }
    }

    @Test
    fun inactiveFooterDoesNotTriggerAnAction() {
        listOf(FeedFooterState.Loading, FeedFooterState.NoMoreItems).forEach { state ->
            val actions = mutableListOf<String>()

            handleFooterAction(state, { actions += "load" }, { actions += "retry" })

            assertEquals(emptyList<String>(), actions)
        }
    }
}
