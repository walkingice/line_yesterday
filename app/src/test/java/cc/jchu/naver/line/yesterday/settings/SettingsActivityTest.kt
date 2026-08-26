package cc.jchu.naver.line.yesterday.settings

import android.content.Context
import com.google.android.material.switchmaterial.SwitchMaterial
import cc.jchu.naver.line.yesterday.R
import cc.jchu.naver.line.yesterday.data.settings.ClientSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class SettingsActivityTest {
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
    fun togglePersistsSettingAndShowsRestartToast() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val toggle = activity.findViewById<SwitchMaterial>(R.id.use_real_client)

        assertFalse(toggle.isChecked)

        toggle.isChecked = true

        assertTrue(ClientSettings(context).useRealClient)
        assertEquals("Restart the app to apply this change", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun assigningExistingValueDoesNotShowRestartToast() {
        ClientSettings(context).useRealClient = true
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val toggle = activity.findViewById<SwitchMaterial>(R.id.use_real_client)
        ShadowToast.reset()

        toggle.isChecked = true

        assertNull(ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun attachesSettingsFragment() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()

        assertTrue(activity.supportFragmentManager.fragments.single() is SettingsFragment)
    }
}
