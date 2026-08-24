package cc.jchu.naver.line.yesterday.feed

import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import cc.jchu.naver.line.yesterday.databinding.ItemDummyJsonBinding
import cc.jchu.naver.line.yesterday.databinding.ItemSpaceFlightBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FeedItemLayoutTest {
    @Test
    fun dummyJsonItemPlacesTextBeforeImage() {
        val binding = ItemDummyJsonBinding.inflate(
            LayoutInflater.from(RuntimeEnvironment.getApplication()),
            FrameLayout(RuntimeEnvironment.getApplication()),
            false,
        )

        assertEquals(LinearLayout.HORIZONTAL, binding.root.orientation)
        assertEquals(binding.title.parent, binding.root.getChildAt(0))
        assertEquals(binding.image, binding.root.getChildAt(1))
    }

    @Test
    fun spacingDecorationAddsSpaceAboveEveryItem() {
        val decoration = FeedItemSpacingDecoration(8)
        val offsets = Rect()

        decoration.getItemOffsets(
            offsets,
            FrameLayout(RuntimeEnvironment.getApplication()),
            RecyclerView(RuntimeEnvironment.getApplication()),
            RecyclerView.State(),
        )

        assertEquals(Rect(0, 8, 0, 0), offsets)
    }

    @Test
    fun spaceFlightItemUsesRoundedBorderBackground() {
        val binding = ItemSpaceFlightBinding.inflate(
            LayoutInflater.from(RuntimeEnvironment.getApplication()),
            FrameLayout(RuntimeEnvironment.getApplication()),
            false,
        )
        val background = binding.root.background

        assertTrue(background is GradientDrawable)
        assertEquals(12f, (background as GradientDrawable).cornerRadius)
    }
}
