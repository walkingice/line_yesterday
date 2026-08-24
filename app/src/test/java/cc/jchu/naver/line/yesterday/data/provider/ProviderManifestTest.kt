package cc.jchu.naver.line.yesterday.data.provider

import android.Manifest
import android.content.pm.PackageManager
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProviderManifestTest {
    @Test
    fun declaresNetworkStatePermission() {
        val context = RuntimeEnvironment.getApplication()
        val packageInfo = context.packageManager
            .getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS,
            )

        assertTrue(
            packageInfo.requestedPermissions?.contains(Manifest.permission.ACCESS_NETWORK_STATE) == true,
        )
    }
}
