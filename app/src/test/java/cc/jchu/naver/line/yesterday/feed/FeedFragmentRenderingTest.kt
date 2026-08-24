package cc.jchu.naver.line.yesterday.feed

import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import cc.jchu.naver.line.yesterday.R
import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.FeedUiState
import cc.jchu.naver.line.yesterday.databinding.FragmentFeedBinding
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.Executor

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
        val layoutManager = RecordingLayoutManager(binding.root.context, binding.recyclerView)
        binding.recyclerView.layoutManager = layoutManager
        val executor = QueuedExecutor()
        val adapter = FeedAdapter({}, {}, executor)
        binding.recyclerView.adapter = adapter

        renderFeedState(
            binding,
            adapter,
            FeedUiState(footerState = FeedFooterState.Ready),
        )
        shadowOf(Looper.getMainLooper()).idle()
        measureAndLayout(binding.recyclerView)
        binding.recyclerView.findViewById<Button>(R.id.footer_button).requestFocus()

        renderFeedState(
            binding,
            adapter,
            FeedUiState(
                items = List(20) { index ->
                    DummyJsonItem(index.toString(), "Item $index", "", "category")
                },
            ),
        )
        executor.runNext()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(21, adapter.itemCount)
        measureAndLayout(binding.recyclerView)

        assertEquals(
            0,
            layoutManager.findFirstVisibleItemPosition(),
        )
        assertEquals(21, layoutManager.itemCountWhenScrolledToTop)
    }

    private fun measureAndLayout(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        recyclerView.layout(0, 0, 1080, 1920)
    }

    private class RecordingLayoutManager(
        context: android.content.Context,
        private val recyclerView: androidx.recyclerview.widget.RecyclerView,
    ) : LinearLayoutManager(context) {
        var itemCountWhenScrolledToTop = 0

        override fun scrollToPosition(position: Int) {
            super.scrollToPosition(position)
            if (position == 0) {
                itemCountWhenScrolledToTop = recyclerView.adapter?.itemCount ?: 0
            }
        }
    }

    private class QueuedExecutor : Executor {
        private val tasks = mutableListOf<Runnable>()

        override fun execute(command: Runnable) {
            tasks += command
        }

        fun runNext() {
            tasks.removeFirst().run()
        }
    }

    private fun createBinding(): FragmentFeedBinding = FragmentFeedBinding.inflate(
        LayoutInflater.from(RuntimeEnvironment.getApplication()),
        FrameLayout(RuntimeEnvironment.getApplication()),
        false,
    )
}
