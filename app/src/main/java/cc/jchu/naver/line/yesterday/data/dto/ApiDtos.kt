package cc.jchu.naver.line.yesterday.data.dto

import com.google.gson.annotations.SerializedName

data class DummyJsonFeedDto(
    val products: List<DummyJsonProductDto>?,
    val total: Int?,
    val skip: Int?,
    val limit: Int?,
)

data class DummyJsonProductDto(
    val id: Int?,
    val title: String?,
    val description: String?,
    val category: String?,
    val price: Double?,
    val discountPercentage: Double?,
    val rating: Double?,
    val stock: Int?,
    val tags: List<String>?,
    val brand: String?,
    val sku: String?,
    val weight: Int?,
    val dimensions: DummyJsonDimensionsDto?,
    val warrantyInformation: String?,
    val shippingInformation: String?,
    val availabilityStatus: String?,
    val reviews: List<DummyJsonReviewDto>?,
    val returnPolicy: String?,
    val minimumOrderQuantity: Int?,
    val meta: DummyJsonMetaDto?,
    val images: List<String>?,
    val thumbnail: String?,
    val message: String?,
)

data class DummyJsonDimensionsDto(
    val width: Double?,
    val height: Double?,
    val depth: Double?,
)

data class DummyJsonReviewDto(
    val rating: Int?,
    val comment: String?,
    val date: String?,
    val reviewerName: String?,
    val reviewerEmail: String?,
)

data class DummyJsonMetaDto(
    val createdAt: String?,
    val updatedAt: String?,
    val barcode: String?,
    val qrCode: String?,
)

data class SpaceFlightFeedDto(
    val count: Int?,
    val next: String?,
    val previous: String?,
    val results: List<SpaceFlightArticleDto>?,
)

data class SpaceFlightArticleDto(
    val id: Int?,
    val title: String?,
    val authors: List<SpaceFlightAuthorDto>?,
    val url: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("news_site") val newsSite: String?,
    val summary: String?,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    val featured: Boolean?,
    val launches: List<SpaceFlightLaunchDto>?,
    val events: List<SpaceFlightEventDto>?,
)

data class SpaceFlightAuthorDto(
    val name: String?,
    val socials: SpaceFlightSocialsDto?,
)

data class SpaceFlightSocialsDto(
    val x: String?,
    val youtube: String?,
    val instagram: String?,
    val linkedin: String?,
    val facebook: String?,
)

data class SpaceFlightLaunchDto(
    val id: String?,
    val provider: String?,
)

data class SpaceFlightEventDto(
    val id: String?,
    val provider: String?,
)
