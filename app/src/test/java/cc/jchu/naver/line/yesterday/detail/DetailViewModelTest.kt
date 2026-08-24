package cc.jchu.naver.line.yesterday.detail

import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailViewModelTest {
    @Test
    fun parsesSupportedArguments() {
        val arguments = DetailArguments.from(DetailActivity.SOURCE_DUMMY_JSON, "47")

        assertEquals(DetailArguments(FeedSource.DUMMY_JSON, "47"), arguments)
    }

    @Test
    fun rejectsUnsupportedOrBlankArguments() {
        assertNull(DetailArguments.from("unsupported", "47"))
        assertNull(DetailArguments.from(DetailActivity.SOURCE_SPACE_FLIGHT, " "))
        assertNull(DetailArguments.from(null, "47"))
    }

    @Test
    fun viewModelExposesValidatedArguments() {
        val viewModel = DetailViewModel(
            DetailArguments.from(DetailActivity.SOURCE_SPACE_FLIGHT, "39639"),
        )

        assertTrue(viewModel.isArgumentsValid)
        assertEquals(FeedSource.SPACE_FLIGHT, viewModel.detailArguments?.source)
        assertEquals("39639", viewModel.detailArguments?.id)
    }
}
