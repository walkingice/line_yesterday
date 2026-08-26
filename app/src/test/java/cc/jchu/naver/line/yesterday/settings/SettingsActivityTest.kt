package cc.jchu.naver.line.yesterday.settings

import com.google.android.material.switchmaterial.SwitchMaterial
import cc.jchu.naver.line.yesterday.R
import cc.jchu.naver.line.yesterday.data.settings.ClientSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class SettingsActivityTest {
    @Test
    fun togglePersistsSettingAndShowsRestartToast() {
        val context = RuntimeEnvironment.getApplication()
        ClientSettings(context).useRealClient = false
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val toggle = activity.findViewById<SwitchMaterial>(R.id.use_real_client)

        assertFalse(toggle.isChecked)

        toggle.isChecked = true

        assertTrue(ClientSettings(context).useRealClient)
        assertEquals("Restart the app to apply this change", ShadowToast.getTextOfLatestToast())
        ClientSettings(context).useRealClient = false
    }

    @Test
    fun attachesSettingsFragment() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()

        assertTrue(activity.supportFragmentManager.fragments.single() is SettingsFragment)
    }
}
