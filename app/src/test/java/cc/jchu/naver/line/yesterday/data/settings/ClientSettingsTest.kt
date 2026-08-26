package cc.jchu.naver.line.yesterday.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ClientSettingsTest {
    @Test
    fun useRealClientDefaultsToFalseAndPersistsChanges() {
        val context = RuntimeEnvironment.getApplication()
        val settings = ClientSettings(context)

        settings.useRealClient = false
        assertFalse(settings.useRealClient)

        settings.useRealClient = true

        assertTrue(ClientSettings(context).useRealClient)
    }
}
