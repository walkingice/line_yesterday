package cc.jchu.naver.line.yesterday

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RobolectricResourceTest {
    @Test
    fun applicationResourcesAreAvailable() {
        val appName = RuntimeEnvironment.getApplication()
            .getString(R.string.app_name)

        assertEquals("LINE Yesterday", appName)
    }
}
