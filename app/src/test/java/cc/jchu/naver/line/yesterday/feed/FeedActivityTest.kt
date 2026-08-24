package cc.jchu.naver.line.yesterday.feed

import androidx.fragment.app.FragmentContainerView
import cc.jchu.naver.line.yesterday.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Robolectric
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedActivityTest {
    @Test
    fun createsAndAttachesFeedFragment() {
        val activity = Robolectric.buildActivity(FeedActivity::class.java).setup().get()

        assertTrue(activity.findViewById<FragmentContainerView>(R.id.fragment_container) != null)
        assertTrue(activity.supportFragmentManager.fragments.single() is FeedFragment)
        assertEquals("Feed", activity.findViewById<android.widget.TextView>(R.id.screen_name).text)
    }
}
