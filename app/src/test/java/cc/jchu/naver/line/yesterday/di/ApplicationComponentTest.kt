package cc.jchu.naver.line.yesterday.di

import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ApplicationComponentTest {
    @Test
    fun applicationComponentIsSingletonForApplicationContext() {
        val context = RuntimeEnvironment.getApplication()

        val first = context.applicationComponent()
        val second = context.applicationComponent()

        assertSame(first, second)
        assertSame(context.applicationContext, first.applicationContext)
        assertSame(context, first.applicationContext)
        assertSame(first.database, second.database)
        assertSame(first.database.jsonCacheDao(), second.database.jsonCacheDao())
        assertTrue(first.timeProvider is cc.jchu.naver.line.yesterday.data.provider.SystemTimeProvider)
        assertTrue(
            first.networkStatusProvider is
                cc.jchu.naver.line.yesterday.data.provider.ConnectivityNetworkStatusProvider,
        )
    }
}
