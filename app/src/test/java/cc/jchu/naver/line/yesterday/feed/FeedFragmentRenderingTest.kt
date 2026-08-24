package cc.jchu.naver.line.yesterday.feed

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.FeedUiState
import cc.jchu.naver.line.yesterday.databinding.FragmentFeedBinding
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FeedFragmentRenderingTest {
    @Test
    fun initialLoadingHidesListAndShowsProgress() {
        val binding = createBinding()

        renderFeedState(
            binding,
            FeedAdapter({}, {}),
            FeedUiState(initialLoading = true, footerState = FeedFooterState.Loading),
        )

        assertEquals(View.VISIBLE, binding.initialLoading.visibility)
        assertEquals(View.GONE, binding.recyclerView.visibility)
        assertEquals(View.GONE, binding.emptyContent.visibility)
    }

    @Test
    fun emptyErrorShowsErrorWithoutChangingViewModelState() {
        val binding = createBinding()

        renderFeedState(
            binding,
            FeedAdapter({}, {}),
            FeedUiState(footerState = FeedFooterState.Error),
        )

        assertEquals(View.VISIBLE, binding.errorContent.visibility)
        assertEquals(View.GONE, binding.emptyContent.visibility)
        assertEquals(View.VISIBLE, binding.recyclerView.visibility)
    }

    private fun createBinding(): FragmentFeedBinding = FragmentFeedBinding.inflate(
        LayoutInflater.from(RuntimeEnvironment.getApplication()),
        FrameLayout(RuntimeEnvironment.getApplication()),
        false,
    )
}
