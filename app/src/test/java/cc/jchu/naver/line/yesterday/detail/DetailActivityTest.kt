package cc.jchu.naver.line.yesterday.detail

import android.content.Intent
import cc.jchu.naver.line.yesterday.databinding.FragmentDetailBinding
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

        val fragment = activity.supportFragmentManager.fragments.single() as DetailFragment
        val binding = FragmentDetailBinding.bind(checkNotNull(fragment.view))

        assertEquals("Detail", binding.screenName.text)
    }

    @Test
    fun missingArgumentsShowErrorWithoutFinishing() {
        val activity = Robolectric.buildActivity(DetailActivity::class.java).setup().get()

        val fragment = activity.supportFragmentManager.fragments.single() as DetailFragment

        assertEquals(
            "Invalid detail",
            FragmentDetailBinding.bind(checkNotNull(fragment.view)).screenName.text,
        )
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

        val fragment = activity.supportFragmentManager.fragments.single() as DetailFragment

        assertEquals(
            "Invalid detail",
            FragmentDetailBinding.bind(checkNotNull(fragment.view)).screenName.text,
        )
        assertTrue(!activity.isFinishing)
        activity.onBackPressed()
        assertTrue(activity.isFinishing)
    }
}
