package cc.jchu.naver.line.yesterday.data.repository

import cc.jchu.naver.line.yesterday.data.provider.FakeTimeProvider
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CachePolicyTest {
    private val timeProvider = FakeTimeProvider(now = 10_000L)
    private val validator = FreshnessValidator(1_000L, timeProvider)

    @Test
    fun timestampBeforeExpirationIsFresh() {
        assertTrue(validator.isFresh(9_001L))
    }

    @Test
    fun timestampAtBoundaryIsExpired() {
        assertFalse(validator.isFresh(9_000L))
    }

    @Test
    fun timestampAfterExpirationIsExpired() {
        assertFalse(validator.isFresh(8_999L))
    }

    @Test
    fun futureTimestampHasZeroAgeAndIsFresh() {
        assertTrue(validator.isFresh(10_001L))
    }
}
