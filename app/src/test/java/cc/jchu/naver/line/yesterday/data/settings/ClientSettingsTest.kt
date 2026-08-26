package cc.jchu.naver.line.yesterday.data.settings

import android.content.Context
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ClientSettingsTest {
    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun clearSettings() {
        context.getSharedPreferences(ClientSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun clearSettingsAfterTest() = clearSettings()

    @Test
    fun useRealClientDefaultsToFalseAndPersistsChanges() {
        val settings = ClientSettings(context)

        assertFalse(settings.useRealClient)

        settings.useRealClient = true

        assertTrue(ClientSettings(context).useRealClient)
    }
}
