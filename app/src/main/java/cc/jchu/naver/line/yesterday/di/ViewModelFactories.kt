package cc.jchu.naver.line.yesterday.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cc.jchu.naver.line.yesterday.data.repository.DetailReader
import cc.jchu.naver.line.yesterday.data.repository.DummyJsonRepository
import cc.jchu.naver.line.yesterday.data.repository.FavoriteReader
import cc.jchu.naver.line.yesterday.data.repository.FavoritesRepository
import cc.jchu.naver.line.yesterday.data.repository.SpaceFlightRepository
import cc.jchu.naver.line.yesterday.detail.DetailArguments
import cc.jchu.naver.line.yesterday.detail.DetailViewModel
import cc.jchu.naver.line.yesterday.favorites.FavoritesViewModel
import cc.jchu.naver.line.yesterday.feed.FeedViewModel

class FeedViewModelFactory(
    private val dummyJsonRepository: DummyJsonRepository,
    private val spaceFlightRepository: SpaceFlightRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FeedViewModel(dummyJsonRepository, spaceFlightRepository) as T
}

class FavoritesViewModelFactory(
    private val favoritesRepository: FavoritesRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FavoritesViewModel(favoritesRepository) as T
}

class DetailViewModelFactory(
    private val arguments: DetailArguments?,
    private val detailReader: DetailReader,
    private val favoriteReader: FavoriteReader,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DetailViewModel(arguments, detailReader, favoriteReader = favoriteReader) as T
}
