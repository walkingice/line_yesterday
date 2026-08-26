package cc.jchu.naver.line.yesterday.feed

import android.view.View
import android.os.Looper
import android.widget.ImageView
import android.widget.Button
import android.widget.FrameLayout
import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.SpaceFlightItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
class FeedAdapterTest {
    @Test
    fun createsSourceSpecificRowsAndFooter() {
        val adapter = FeedAdapter({}, {})
        adapter.submitFeed(
            listOf(
                DummyJsonItem("1", "Product", "", "beauty"),
                SpaceFlightItem("1", "Article", "", "Summary"),
            ),
            FeedFooterState.Ready,
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(3, adapter.itemCount)
        assertEquals(1, adapter.getItemViewType(0))
        assertEquals(2, adapter.getItemViewType(1))
        assertEquals(3, adapter.getItemViewType(2))
        assertTrue(adapter.getItemId(0) != adapter.getItemId(1))
    }

    @Test
    fun invokesCommitCallbackAfterUpdatingItems() {
        val executor = QueuedExecutor()
        val adapter = FeedAdapter({}, {}, executor)
        var itemCountWhenCommitted = 0

        adapter.submitFeed(emptyList(), FeedFooterState.Loading)
        shadowOf(Looper.getMainLooper()).idle()
        adapter.submitFeed(
            listOf(DummyJsonItem("1", "Product", "", "beauty")),
            FeedFooterState.Ready,
        ) {
            itemCountWhenCommitted = adapter.itemCount
        }
        executor.runNext()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(2, itemCountWhenCommitted)
    }

    @Test
    fun footerForNoMoreItemsIsDisabled() {
        val adapter = FeedAdapter({}, {})
        adapter.submitFeed(emptyList(), FeedFooterState.NoMoreItems)
        shadowOf(Looper.getMainLooper()).idle()
        val parent = FrameLayout(RuntimeEnvironment.getApplication())
        val holder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0))

        adapter.onBindViewHolder(holder, 0)

        assertEquals(false, holder.itemView.findViewById<View>(
            cc.jchu.naver.line.yesterday.R.id.footer_button,
        ).isEnabled)
    }

    @Test
    fun sourceRowsBindInformationAndImageViews() {
        val adapter = FeedAdapter({}, {})
        adapter.submitFeed(
            listOf(
                DummyJsonItem("1", "Product", "invalid-url", "beauty"),
                SpaceFlightItem("2", "Article", "invalid-url", "Summary", "2026-08-25T14:57:26Z"),
            ),
            FeedFooterState.Ready,
        )
        shadowOf(Looper.getMainLooper()).idle()
        val parent = FrameLayout(RuntimeEnvironment.getApplication())

        val dummyHolder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0))
        adapter.onBindViewHolder(dummyHolder, 0)
        val spaceHolder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(1))
        adapter.onBindViewHolder(spaceHolder, 1)

        assertEquals("Product", dummyHolder.itemView.findViewById<android.widget.TextView>(
            cc.jchu.naver.line.yesterday.R.id.title,
        ).text)
        assertEquals("Summary", spaceHolder.itemView.findViewById<android.widget.TextView>(
            cc.jchu.naver.line.yesterday.R.id.description,
        ).text)
        assertEquals("Time: Not available", dummyHolder.itemView.findViewById<android.widget.TextView>(
            cc.jchu.naver.line.yesterday.R.id.time,
        ).text)
        assertEquals("Source: DummyJson", dummyHolder.itemView.findViewById<android.widget.TextView>(
            cc.jchu.naver.line.yesterday.R.id.source,
        ).text)
        assertEquals("Time: 2026-08-25T14:57:26Z", spaceHolder.itemView.findViewById<android.widget.TextView>(
            cc.jchu.naver.line.yesterday.R.id.time,
        ).text)
        assertEquals("Source: SpaceFlight", spaceHolder.itemView.findViewById<android.widget.TextView>(
            cc.jchu.naver.line.yesterday.R.id.source,
        ).text)
        assertTrue(dummyHolder.itemView.findViewById<ImageView>(
            cc.jchu.naver.line.yesterday.R.id.image,
        ) != null)
        assertTrue(spaceHolder.itemView.findViewById<ImageView>(
            cc.jchu.naver.line.yesterday.R.id.image,
        ) != null)
    }

    @Test
    fun footerStatesHaveClearTextAndEnabledBehavior() {
        val expected = mapOf(
            FeedFooterState.Ready to ("Load more" to true),
            FeedFooterState.Loading to ("Loading..." to false),
            FeedFooterState.NoMoreItems to ("No more items" to false),
            FeedFooterState.Error to ("Retry" to true),
            FeedFooterState.Offline to ("Retry while online" to true),
        )
        val parent = FrameLayout(RuntimeEnvironment.getApplication())

        expected.forEach { (state, expectedValues) ->
            val adapter = FeedAdapter({}, {})
            adapter.submitFeed(emptyList(), state)
            shadowOf(Looper.getMainLooper()).idle()
            val holder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0))
            adapter.onBindViewHolder(holder, 0)
            val button = holder.itemView.findViewById<Button>(
                cc.jchu.naver.line.yesterday.R.id.footer_button,
            )

            assertEquals(expectedValues.first, button.text)
            assertEquals(expectedValues.second, button.isEnabled)
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
}
