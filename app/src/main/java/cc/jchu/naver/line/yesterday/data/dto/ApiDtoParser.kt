package cc.jchu.naver.line.yesterday.data.dto

import com.google.gson.Gson

class ApiDtoParser(
    private val gson: Gson = Gson(),
) {
    fun parseDummyJsonFeed(rawJson: String): DummyJsonFeedDto =
        gson.fromJson(rawJson, DummyJsonFeedDto::class.java)

    fun parseDummyJsonProduct(rawJson: String): DummyJsonProductDto =
        gson.fromJson(rawJson, DummyJsonProductDto::class.java)

    fun parseSpaceFlightFeed(rawJson: String): SpaceFlightFeedDto =
        gson.fromJson(rawJson, SpaceFlightFeedDto::class.java)

    fun parseSpaceFlightArticle(rawJson: String): SpaceFlightArticleDto =
        gson.fromJson(rawJson, SpaceFlightArticleDto::class.java)
}
