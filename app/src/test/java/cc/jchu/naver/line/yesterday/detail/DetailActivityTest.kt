package cc.jchu.naver.line.yesterday.detail

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.view.menu.MenuBuilder
import cc.jchu.naver.line.yesterday.data.domain.DataError
import cc.jchu.naver.line.yesterday.data.domain.Detail
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.databinding.FragmentDetailBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DetailActivityTest {
    @Test
    fun rendersLoadingAndDetailContent() {
        val binding = FragmentDetailBinding.inflate(
            LayoutInflater.from(RuntimeEnvironment.getApplication()),
            FrameLayout(RuntimeEnvironment.getApplication()),
            false,
        )

        renderDetailState(binding, DetailUiState(isLoading = true))

        assertEquals(View.VISIBLE, binding.detailLoading.visibility)
        assertEquals(View.GONE, binding.detailContent.visibility)

        renderDetailState(binding, DetailUiState(detail = detail()))

        assertEquals(View.GONE, binding.detailLoading.visibility)
        assertEquals(View.VISIBLE, binding.detailContent.visibility)
        assertEquals("Title", binding.detailTitle.text)
        assertEquals("Description", binding.detailDescription.text)
        assertEquals("Extra", binding.detailExtraInformation.text)
        assertEquals("Time: 2025-04-30T09:41:02.053Z", binding.detailTime.text)
        assertEquals("Source: DummyJson", binding.detailSource.text)
    }

    @Test
    fun rendersErrorWithoutRemovingExistingDetail() {
        val binding = FragmentDetailBinding.inflate(
            LayoutInflater.from(RuntimeEnvironment.getApplication()),
            FrameLayout(RuntimeEnvironment.getApplication()),
            false,
        )

        renderDetailState(
            binding,
            DetailUiState(detail = detail(), isLoading = true, error = DataError.Offline),
        )

        assertEquals(View.VISIBLE, binding.detailContent.visibility)
        assertEquals(View.GONE, binding.detailLoading.visibility)
        assertEquals(View.GONE, binding.detailErrorPanel.visibility)
        assertEquals(View.VISIBLE, binding.detailRefreshError.visibility)
        assertEquals("Title", binding.detailTitle.text)
    }

    @Test
    fun showsRetryForNoCacheError() {
        val binding = FragmentDetailBinding.inflate(
            LayoutInflater.from(RuntimeEnvironment.getApplication()),
            FrameLayout(RuntimeEnvironment.getApplication()),
            false,
        )

        renderDetailState(
            binding,
            DetailUiState(error = DataError.Offline),
        )

        assertEquals(View.VISIBLE, binding.detailErrorPanel.visibility)
        assertEquals(View.VISIBLE, binding.detailRetry.visibility)
        assertEquals(View.GONE, binding.detailRefreshError.visibility)
    }

    @Test
    fun rendersFavoriteActionInAppBar() {
        val activity = Robolectric.buildActivity(DetailActivity::class.java).setup().get()
        val menu = MenuBuilder(activity)
        activity.onCreateOptionsMenu(menu)
        val menuItem = menu.getItem(0)

        activity.renderFavoriteAction(DetailUiState(detail = detail(), isFavorite = true))

        assertTrue(menuItem.isVisible)
        assertTrue(menuItem.isEnabled)
        assertEquals("Remove from favorites", menuItem.contentDescription)

        activity.renderFavoriteAction(DetailUiState(isLoading = true))

        assertFalse(menuItem.isEnabled)
    }

    @Test
    fun rendersUpdatedDetailAfterStaleContent() {
        val binding = FragmentDetailBinding.inflate(
            LayoutInflater.from(RuntimeEnvironment.getApplication()),
            FrameLayout(RuntimeEnvironment.getApplication()),
            false,
        )

        renderDetailState(binding, DetailUiState(detail = detail()))
        renderDetailState(binding, DetailUiState(detail = detail().copy(title = "Updated")))

        assertEquals("Updated", binding.detailTitle.text)
        assertEquals(View.VISIBLE, binding.detailContent.visibility)
    }

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
        assertEquals(
            View.VISIBLE,
            FragmentDetailBinding.bind(checkNotNull(fragment.view)).detailErrorPanel.visibility,
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
        assertEquals(
            View.VISIBLE,
            FragmentDetailBinding.bind(checkNotNull(fragment.view)).detailErrorPanel.visibility,
        )
        assertTrue(!activity.isFinishing)
        activity.onBackPressed()
        assertTrue(activity.isFinishing)
    }

    private fun detail() = Detail(
        id = "1",
        source = FeedSource.DUMMY_JSON,
        title = "Title",
        imgUrl = "https://example.com/image.jpg",
        description = "Description",
        extraInformation = "Extra",
        time = "2025-04-30T09:41:02.053Z",
    )

}
