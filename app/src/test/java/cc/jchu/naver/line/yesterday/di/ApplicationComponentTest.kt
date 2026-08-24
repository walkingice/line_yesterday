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
        assertSame(first.database, second.database)
        assertSame(first.database.jsonCacheDao(), second.database.jsonCacheDao())
        assertSame(first.dummyJsonRepository, second.dummyJsonRepository)
        assertSame(first.spaceFlightRepository, second.spaceFlightRepository)
        assertSame(first.detailRepository, second.detailRepository)
        assertSame(first.favoritesRepository, second.favoritesRepository)
        assertSame(first.feedViewModelFactory, second.feedViewModelFactory)
        assertSame(first.favoritesViewModelFactory, second.favoritesViewModelFactory)
        assertTrue(first.timeProvider is cc.jchu.naver.line.yesterday.data.provider.SystemTimeProvider)
        first.networkStatusProvider.setOnline(false)
        assertTrue(!first.networkStatusProvider.isOnline())
        first.networkStatusProvider.setOnline(true)
        assertTrue(first.networkStatusProvider.isOnline())
        first.networkStatusProvider.clearOverride()
    }
}
