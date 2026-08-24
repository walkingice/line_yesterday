package cc.jchu.naver.line.yesterday

import cc.jchu.naver.line.yesterday.detail.DetailViewModel
import cc.jchu.naver.line.yesterday.favorites.FavoritesViewModel
import cc.jchu.naver.line.yesterday.feed.FeedViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenViewModelTest {
    @Test
    fun initialScreenNamesAreReadable() {
        assertEquals("Feed", FeedViewModel().screenName)
        assertEquals("Detail", DetailViewModel().screenName)
        assertEquals("Favorites", FavoritesViewModel().screenName)
    }
}
