package cc.jchu.naver.line.yesterday.favorites

import androidx.appcompat.view.menu.MenuBuilder
import cc.jchu.naver.line.yesterday.databinding.FragmentFavoritesBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoritesActivityTest {
    @Test
    fun createsAndAttachesFavoritesFragment() {
        val activity = Robolectric.buildActivity(FavoritesActivity::class.java).setup().get()

        val fragment = activity.supportFragmentManager.fragments.single() as FavoritesFragment

        assertEquals(
            "Favorites",
            FragmentFavoritesBinding.bind(checkNotNull(fragment.view)).screenName.text,
        )
    }

    @Test
    fun hasNoOptionsMenuAndBackFinishes() {
        val activity = Robolectric.buildActivity(FavoritesActivity::class.java).setup().get()
        val menu = MenuBuilder(activity)

        activity.onCreateOptionsMenu(menu)
        assertEquals(0, menu.size())
        activity.onBackPressed()
        assertTrue(activity.isFinishing)
    }
}
