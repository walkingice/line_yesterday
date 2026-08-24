package cc.jchu.naver.line.yesterday.favorites

import cc.jchu.naver.line.yesterday.data.favorite.FavoriteEntry
import cc.jchu.naver.line.yesterday.data.favorite.FavoriteStore
import cc.jchu.naver.line.yesterday.data.provider.FakeDispatcherProvider
import cc.jchu.naver.line.yesterday.data.provider.FakeTimeProvider
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.repository.FavoritesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class FavoritesViewModelTest {
    @Test
    fun initialPageContainsFiveItemsAndLoadMoreRevealsNextFive() = runBlocking {
        val store = FakeStore()
        repeat(11) { index ->
            store.save(FavoriteEntry(
                source = FeedSource.DUMMY_JSON,
                itemId = index.toString(),
                addedAt = index.toLong(),
                title = index.toString(),
                imgUrl = "image",
                description = "",
                extraInformation = "category",
            ))
        }
        val viewModel = FavoritesViewModel(
            FavoritesRepository(store, FakeTimeProvider()),
            FakeDispatcherProvider(),
        )

        assertEquals(5, viewModel.uiState.value.items.size)
        viewModel.loadMoreItems()
        assertEquals(10, viewModel.uiState.value.items.size)
        viewModel.loadMoreItems()
        assertEquals(11, viewModel.uiState.value.items.size)
    }

    @Test
    fun noMoreStateIsUsedWhenEmptyOrAllItemsAreVisible() = runBlocking {
        listOf(0, 1, 5, 6, 10, 11).forEach { count ->
            val store = FakeStore()
            repeat(count) { index -> store.save(entry(index)) }
            val viewModel = FavoritesViewModel(
                FavoritesRepository(store, FakeTimeProvider()),
                FakeDispatcherProvider(),
            )

            repeat(3) { viewModel.loadMoreItems() }

            assertEquals(count, viewModel.uiState.value.items.size)
            assertEquals(FeedFooterState.NoMoreItems, viewModel.uiState.value.footerState)
        }
    }

    private fun entry(index: Int) = FavoriteEntry(
        source = FeedSource.DUMMY_JSON,
        itemId = index.toString(),
        addedAt = index.toLong(),
        title = index.toString(),
        imgUrl = "image",
        description = "",
        extraInformation = "category",
    )

    private class FakeStore : FavoriteStore {
        private val entries = mutableListOf<FavoriteEntry>()

        override suspend fun get(source: FeedSource, itemId: String): FavoriteEntry? =
            entries.find { it.source == source && it.itemId == itemId }

        override suspend fun getAll(): List<FavoriteEntry> = entries.sortedByDescending { it.addedAt }

        override suspend fun save(entry: FavoriteEntry) {
            entries.removeIf { it.source == entry.source && it.itemId == entry.itemId }
            entries += entry
        }

        override suspend fun delete(source: FeedSource, itemId: String) {
            entries.removeIf { it.source == source && it.itemId == itemId }
        }
    }
}
