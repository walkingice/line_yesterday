package cc.jchu.naver.line.yesterday.favorites

import android.view.LayoutInflater
import android.widget.FrameLayout
import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.FavoritesUiState
import cc.jchu.naver.line.yesterday.databinding.FragmentFavoritesBinding
import cc.jchu.naver.line.yesterday.feed.FeedAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FavoritesFragmentTest {
    @Test
    fun emptyStateShowsEmptyContentAndFooter() {
        val binding = createBinding()
        val adapter = FeedAdapter({}, {})

        renderFavoritesState(binding, adapter, FavoritesUiState(
            footerState = FeedFooterState.NoMoreItems,
        ))

        assertEquals(android.view.View.VISIBLE, binding.emptyContent.visibility)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun contentUsesFeedRowsAndFooter() {
        val binding = createBinding()
        val adapter = FeedAdapter({}, {})
        val item = DummyJsonItem("7", "Product", "", "beauty")

        renderFavoritesState(binding, adapter, FavoritesUiState(
            items = listOf(item),
            totalCount = 1,
            footerState = FeedFooterState.NoMoreItems,
        ))

        assertEquals(android.view.View.GONE, binding.emptyContent.visibility)
        assertEquals(2, adapter.itemCount)
        assertNotNull(favoriteDetailIntent(RuntimeEnvironment.getApplication(), item)
            .getStringExtra("source"))
    }

    @Test
    fun itemIntentContainsOnlySourceAndId() {
        val item = DummyJsonItem("7", "Product", "", "beauty")
        val intent = favoriteDetailIntent(RuntimeEnvironment.getApplication(), item)

        assertEquals("dummy_json", intent.getStringExtra("source"))
        assertEquals("7", intent.getStringExtra("id"))
        assertEquals(2, intent.extras?.size())
    }

    private fun createBinding(): FragmentFavoritesBinding = FragmentFavoritesBinding.inflate(
        LayoutInflater.from(RuntimeEnvironment.getApplication()),
        FrameLayout(RuntimeEnvironment.getApplication()),
        false,
    )
}
