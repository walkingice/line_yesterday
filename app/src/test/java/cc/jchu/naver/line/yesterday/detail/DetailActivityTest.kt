package cc.jchu.naver.line.yesterday.detail

import android.widget.TextView
import android.content.Intent
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
        val intent = Intent().apply {
            putExtra(DetailActivity.EXTRA_SOURCE, DetailActivity.SOURCE_DUMMY_JSON)
            putExtra(DetailActivity.EXTRA_ID, "1")
        }
        val activity = Robolectric.buildActivity(DetailActivity::class.java, intent).setup().get()

        assertTrue(activity.findViewById<FragmentContainerView>(R.id.fragment_container) != null)
        assertTrue(activity.supportFragmentManager.fragments.single() is DetailFragment)
        assertEquals("Detail", activity.findViewById<TextView>(R.id.screen_name).text)
    }

    @Test
    fun missingArgumentsShowErrorWithoutFinishing() {
        val activity = Robolectric.buildActivity(DetailActivity::class.java).setup().get()

        assertEquals("Invalid detail", activity.findViewById<TextView>(R.id.screen_name).text)
        assertTrue(!activity.isFinishing)
        activity.onBackPressed()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun invalidSourceShowsErrorWithoutFinishing() {
        val intent = Intent().apply {
            putExtra(DetailActivity.EXTRA_SOURCE, "unsupported")
            putExtra(DetailActivity.EXTRA_ID, "1")
        }
        val activity = Robolectric.buildActivity(DetailActivity::class.java, intent).setup().get()

        assertEquals("Invalid detail", activity.findViewById<TextView>(R.id.screen_name).text)
        assertTrue(!activity.isFinishing)
        activity.onBackPressed()
        assertTrue(activity.isFinishing)
    }
}
