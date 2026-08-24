package cc.jchu.naver.line.yesterday.favorites

import android.widget.TextView
import androidx.fragment.app.FragmentContainerView
import androidx.appcompat.view.menu.MenuBuilder
import cc.jchu.naver.line.yesterday.R
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

        assertTrue(activity.findViewById<FragmentContainerView>(R.id.fragment_container) != null)
        assertTrue(activity.supportFragmentManager.fragments.single() is FavoritesFragment)
        assertEquals("Favorites", activity.findViewById<TextView>(R.id.screen_name).text)
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
