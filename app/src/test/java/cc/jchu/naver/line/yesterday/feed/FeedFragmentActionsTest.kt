package cc.jchu.naver.line.yesterday.feed

import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.SpaceFlightItem
import org.junit.Assert.assertEquals
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class FeedFragmentActionsTest {
    @Test
    fun itemNavigationContainsOnlySourceAndId() {
        val dummyIntent = detailIntentFor(
            RuntimeEnvironment.getApplication(),
            DummyJsonItem("7", "Product", "", "beauty"),
        )
        val spaceIntent = detailIntentFor(
            RuntimeEnvironment.getApplication(),
            SpaceFlightItem("42", "Article", "", "summary"),
        )

        assertEquals("dummy_json", dummyIntent.getStringExtra("source"))
        assertEquals("7", dummyIntent.getStringExtra("id"))
        assertEquals(2, dummyIntent.extras?.size())
        assertEquals("space_flight", spaceIntent.getStringExtra("source"))
        assertEquals("42", spaceIntent.getStringExtra("id"))
        assertEquals(2, spaceIntent.extras?.size())
    }

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
