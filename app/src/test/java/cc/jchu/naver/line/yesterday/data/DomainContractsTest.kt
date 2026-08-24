package cc.jchu.naver.line.yesterday.data

import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.domain.PageCursor
import cc.jchu.naver.line.yesterday.data.provider.FakeDelayProvider
import cc.jchu.naver.line.yesterday.data.provider.FakeDispatcherProvider
import cc.jchu.naver.line.yesterday.data.provider.FakeNetworkStatusProvider
import cc.jchu.naver.line.yesterday.data.provider.FakeTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainContractsTest {
    @Test
    fun feedIdentityIncludesSourceAndId() {
        val dummy = DummyJsonItem("1", "Title", "image", "category")
        val otherSource = FeedSource.SPACE_FLIGHT to dummy.id

        assertEquals(dummy.id, otherSource.second)
        assertNotEquals(dummy.source to dummy.id, otherSource)
    }

    @Test
    fun cursorPreservesOpaqueValue() {
        assertEquals("offset=next", PageCursor("offset=next").value)
    }

    @Test
    fun fakeProvidersAreControllable() = runBlocking {
        val network = FakeNetworkStatusProvider(false)
        val delay = FakeDelayProvider()
        val time = FakeTimeProvider(42L)
        val dispatchers = FakeDispatcherProvider(Dispatchers.Unconfined)

        assertTrue(!network.isOnline())
        delay.delayMillis(250L)
        assertEquals(listOf(250L), delay.requestedDelays)
        assertEquals(42L, time.getCurrentTimeMillis())
        assertEquals(Dispatchers.Unconfined, dispatchers.io)

        network.online = true
        time.now = 43L
        assertTrue(network.isOnline())
        assertEquals(43L, time.getCurrentTimeMillis())
    }
}
