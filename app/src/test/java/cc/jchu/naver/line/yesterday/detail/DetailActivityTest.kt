package cc.jchu.naver.line.yesterday.detail

import android.widget.TextView
import androidx.fragment.app.FragmentContainerView
import cc.jchu.naver.line.yesterday.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DetailActivityTest {
    @Test
    fun createsAndAttachesDetailFragment() {
        val activity = Robolectric.buildActivity(DetailActivity::class.java).setup().get()

        assertTrue(activity.findViewById<FragmentContainerView>(R.id.fragment_container) != null)
        assertTrue(activity.supportFragmentManager.fragments.single() is DetailFragment)
        assertEquals("Detail", activity.findViewById<TextView>(R.id.screen_name).text)
    }
}
