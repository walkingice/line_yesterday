package cc.jchu.naver.line.yesterday.feed

import cc.jchu.naver.line.yesterday.data.domain.FeedItem

internal fun mergeFeedItems(
    dummyItems: List<FeedItem>,
    spaceItems: List<FeedItem>,
): List<FeedItem> {
    val merged = ArrayList<FeedItem>(dummyItems.size + spaceItems.size)
    val maxSize = maxOf(dummyItems.size, spaceItems.size)
    repeat(maxSize) { index ->
        dummyItems.getOrNull(index)?.let(merged::add)
        spaceItems.getOrNull(index)?.let(merged::add)
    }
    return merged
}

internal fun deduplicateFeedItems(items: List<FeedItem>): List<FeedItem> {
    val identities = HashSet<FeedIdentity>()
    return items.filter { item -> identities.add(FeedIdentity(item)) }
}

private data class FeedIdentity(
    val source: cc.jchu.naver.line.yesterday.data.domain.FeedSource,
    val id: String,
) {
    constructor(item: FeedItem) : this(item.source, item.id)
}
