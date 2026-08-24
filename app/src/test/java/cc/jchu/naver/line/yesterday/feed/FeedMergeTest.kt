package cc.jchu.naver.line.yesterday.feed

import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.FeedItem
import cc.jchu.naver.line.yesterday.data.domain.SpaceFlightItem
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedMergeTest {
    @Test
    fun mergeAlternatesAndAppendsRemainingItems() {
        val dummy = listOf(dummy("1"), dummy("2"), dummy("3"))
        val space = listOf(space("1"))

        assertEquals(listOf(dummy[0], space[0], dummy[1], dummy[2]), mergeFeedItems(dummy, space))
    }

    @Test
    fun deduplicationUsesSourceAndId() {
        val items = listOf(dummy("1"), dummy("1"), space("1"), space("1"))

        assertEquals(listOf(dummy("1"), space("1")), deduplicateFeedItems(items))
    }

    private fun dummy(id: String): FeedItem = DummyJsonItem(id, id, "", "category")

    private fun space(id: String): FeedItem = SpaceFlightItem(id, id, "", "description")
}
