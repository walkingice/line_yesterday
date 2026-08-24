package cc.jchu.naver.line.yesterday.data.repository

import cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import kotlinx.coroutines.flow.Flow

class DetailRepository(
    private val dummyJsonRepository: DummyJsonRepository,
    private val spaceFlightRepository: SpaceFlightRepository,
) {
    fun getDetail(source: FeedSource, id: String): Flow<DetailLoadEvent> = when (source) {
        FeedSource.DUMMY_JSON -> dummyJsonRepository.getDetail(id)
        FeedSource.SPACE_FLIGHT -> spaceFlightRepository.getDetail(id)
    }
}
