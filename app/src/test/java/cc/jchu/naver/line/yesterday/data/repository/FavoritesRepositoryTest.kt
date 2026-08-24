package cc.jchu.naver.line.yesterday.data.repository

import cc.jchu.naver.line.yesterday.data.domain.Detail
import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.favorite.FavoriteEntry
import cc.jchu.naver.line.yesterday.data.favorite.FavoriteStore
import cc.jchu.naver.line.yesterday.data.provider.FakeTimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritesRepositoryTest {
    private val item = DummyJsonItem("1", "One", "image", "cat")

    @Test
    fun addRemoveAndToggleAreLocalOperations() = runBlocking {
        val store = FakeStore()
        val repository = FavoritesRepository(store, FakeTimeProvider(20L))

        repository.add(item)
        assertTrue(repository.isFavorite(FeedSource.DUMMY_JSON, "1"))
        assertFalse(repository.toggle(item))
        assertTrue(repository.toggle(item))
        assertTrue(repository.isFavorite(FeedSource.DUMMY_JSON, "1"))
    }

    @Test
    fun pageUsesNewestFirstAndFiveItemLimit() = runBlocking {
        val store = FakeStore()
        val repository = FavoritesRepository(store, FakeTimeProvider())
        repeat(6) { repository.add(item.copy(id = it.toString()), it.toLong()) }

        assertEquals(5, repository.getPage(0).size)
        assertEquals("5", repository.getPage(0).first().id)
        assertEquals(1, repository.getPage(5).size)
    }

    @Test
    fun detailSnapshotUpdatePreservesAddedAt() = runBlocking {
        val store = FakeStore()
        val repository = FavoritesRepository(store, FakeTimeProvider())
        repository.add(item, 10L)
        repository.updateSnapshot(Detail("1", FeedSource.DUMMY_JSON, "New", "new", "desc", "extra"))

        assertEquals(10L, store.entries.single().addedAt)
        assertEquals("New", store.entries.single().title)
    }

    private class FakeStore : FavoriteStore {
        val entries = mutableListOf<FavoriteEntry>()

        override suspend fun get(source: FeedSource, itemId: String): FavoriteEntry? =
            entries.find { it.source == source && it.itemId == itemId }

        override suspend fun getAll(): List<FavoriteEntry> = entries.sortedWith(
            compareByDescending<FavoriteEntry> { it.addedAt }
                .thenBy { it.source }
                .thenBy { it.itemId },
        )

        override suspend fun save(entry: FavoriteEntry) {
            entries.removeIf { it.source == entry.source && it.itemId == entry.itemId }
            entries += entry
        }

        override suspend fun delete(source: FeedSource, itemId: String) {
            entries.removeIf { it.source == source && it.itemId == itemId }
        }
    }
}
