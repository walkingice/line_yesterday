package cc.jchu.naver.line.yesterday.feed

import androidx.fragment.app.FragmentContainerView
import androidx.appcompat.view.menu.MenuBuilder
import cc.jchu.naver.line.yesterday.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Robolectric
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FeedActivityTest {
    @Test
    fun createsAndAttachesFeedFragment() {
        val activity = Robolectric.buildActivity(FeedActivity::class.java).setup().get()

        assertTrue(activity.findViewById<FragmentContainerView>(R.id.fragment_container) != null)
        assertTrue(activity.supportFragmentManager.fragments.single() is FeedFragment)
        assertEquals("Feed", activity.findViewById<android.widget.TextView>(R.id.screen_name).text)
    }

    @Test
    fun opensDetailWithOnlySourceAndId() {
        val activity = Robolectric.buildActivity(FeedActivity::class.java).setup().get()

        activity.findViewById<android.widget.TextView>(R.id.screen_name).performClick()

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals("dummy_json", intent.getStringExtra("source"))
        assertEquals("1", intent.getStringExtra("id"))
        assertEquals(2, intent.extras?.size())
    }

    @Test
    fun menuOpensFavorites() {
        val activity = Robolectric.buildActivity(FeedActivity::class.java).setup().get()
        val menu = MenuBuilder(activity)

        activity.onCreateOptionsMenu(menu)
        activity.onOptionsItemSelected(menu.findItem(FeedActivity.MENU_FAVORITES))

        assertEquals(
            "cc.jchu.naver.line.yesterday.favorites.FavoritesActivity",
            shadowOf(activity).nextStartedActivity.component?.className,
        )
    }

    @Test
    fun backFinishesFeed() {
        val activity = Robolectric.buildActivity(FeedActivity::class.java).setup().get()

        activity.onBackPressed()

        assertTrue(activity.isFinishing)
    }
}
