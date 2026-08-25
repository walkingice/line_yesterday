package cc.jchu.naver.line.yesterday.favorites

import androidx.appcompat.view.menu.MenuBuilder
import androidx.fragment.app.FragmentContainerView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cc.jchu.naver.line.yesterday.R
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
    fun appliesTopInsetBelowAppBar() {
        val activity = Robolectric.buildActivity(FavoritesActivity::class.java).setup().get()
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
    fun createsAndAttachesFavoritesFragment() {
        val activity = Robolectric.buildActivity(FavoritesActivity::class.java).setup().get()

        val fragment = activity.supportFragmentManager.fragments.single() as FavoritesFragment

        assertEquals(
            "Favorites",
            FragmentFavoritesBinding.bind(checkNotNull(fragment.view)).screenName.text,
        )
    }

    @Test
    fun usesPinkPrimaryColorForTopAppBar() {
        val activity = Robolectric.buildActivity(FavoritesActivity::class.java).setup().get()
        val attributes = activity.obtainStyledAttributes(
            intArrayOf(androidx.appcompat.R.attr.colorPrimary),
        )

        assertEquals(activity.getColor(R.color.pink_500), attributes.getColor(0, 0))
        attributes.recycle()
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
