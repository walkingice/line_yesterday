package cc.jchu.naver.line.yesterday.feed

import androidx.appcompat.view.menu.MenuBuilder
import android.view.View
import androidx.fragment.app.FragmentContainerView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cc.jchu.naver.line.yesterday.R
import cc.jchu.naver.line.yesterday.databinding.FragmentFeedBinding
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
    fun appliesTopInsetBelowAppBar() {
        val activity = Robolectric.buildActivity(FeedActivity::class.java).setup().get()
        val container = activity.findViewById<FragmentContainerView>(R.id.fragment_container)

        ViewCompat.dispatchApplyWindowInsets(
            container,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 24, 0, 0))
                .build(),
        )

        assertEquals(24, container.paddingTop)
    }

    @Test
    fun feedLayoutContainsContentAndStateViews() {
        val activity = Robolectric.buildActivity(FeedActivity::class.java).setup().get()
        val binding = FragmentFeedBinding.bind(
            checkNotNull(activity.supportFragmentManager.fragments.single().view),
        )

        assertEquals(View.VISIBLE, binding.swipeRefresh.visibility)
        assertTrue(
            binding.recyclerView.visibility == View.VISIBLE ||
                binding.initialLoading.visibility == View.VISIBLE,
        )
        assertEquals(View.GONE, binding.emptyContent.visibility)
        assertEquals(View.GONE, binding.errorContent.visibility)
    }

    @Test
    fun createsAndAttachesFeedFragment() {
        val activity = Robolectric.buildActivity(FeedActivity::class.java).setup().get()

        val fragment = activity.supportFragmentManager.fragments.single() as FeedFragment

        assertEquals(
            "Feed",
            FragmentFeedBinding.bind(checkNotNull(fragment.view)).screenName.text,
        )
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
