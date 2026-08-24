package cc.jchu.naver.line.yesterday.feed

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
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

    @Test
    fun firstItemsAreShownFromTheTop() {
        val binding = createBinding()
        binding.recyclerView.layoutManager = LinearLayoutManager(binding.root.context)
        val adapter = FeedAdapter({}, {})
        binding.recyclerView.adapter = adapter

        renderFeedState(
            binding,
            adapter,
            FeedUiState(
                items = List(20) { index ->
                    DummyJsonItem(index.toString(), "Item $index", "", "category")
                },
            ),
        )

        binding.recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        binding.recyclerView.layout(0, 0, 1080, 1920)

        assertEquals(
            0,
            (binding.recyclerView.layoutManager as LinearLayoutManager)
                .findFirstVisibleItemPosition(),
        )
    }

    private fun createBinding(): FragmentFeedBinding = FragmentFeedBinding.inflate(
        LayoutInflater.from(RuntimeEnvironment.getApplication()),
        FrameLayout(RuntimeEnvironment.getApplication()),
        false,
    )
}
