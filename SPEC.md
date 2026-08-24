## Introduction

* An app that displays a list of content rendered from API responses.
* This is a demo app and does not need to cover every production detail.
* The app has three screens. Each screen has one Activity containing one Fragment.

## Architecture

* The three screens are FeedActivity + FeedFragment, FavoritesActivity + FavoritesFragment, and DetailActivity + DetailFragment.
* Each Fragment has a corresponding ViewModel. For example, FeedFragment has FeedViewModel.
* UI operations in a Fragment call interfaces exposed by its ViewModel.
* A Fragment observes state from its ViewModel. When the internal ViewModel state changes, the Fragment updates its UI accordingly.
* A ViewModel obtains data through Repositories, evaluates the results, and updates its internal state.
* Activities pass only an item's source type and id. The receiving screen obtains the data required for display through a Repository.

## UI

* FeedFragment
    * Contains a RecyclerView list that displays content from DummyJson and SpaceFlight.
    * Content from the two sources is merged using a deterministic alternating order while preserving the original order within each source.
    * The list supports pull to refresh. A refresh ignores cache freshness and fetches content from both sources again.
        * A refresh does not delete cache in advance. After a source successfully returns new data, that data replaces the source's old cache and displayed content.
        * If a source fails to refresh, its existing cache and displayed content are retained.
    * A Loading state button appears at the end of the list. Pressing it loads more content from both sources at the same time.
        * The available states are Ready, Loading, No more items, Error, and Offline.
        * If either source returns new data, the overall operation succeeds. The source sequence is updated, the list is merged again, and the state returns to Ready.
        * The state becomes No more items only when both sources explicitly have no next page.
        * If no new data is obtained and at least one non-exhausted source fails, the state becomes Error or Offline so that the user can retry.
        * When there is no new data, the state is Offline if all failures were caused by being offline. Other overall failure combinations use Error.
    * Load more and pull to refresh cannot run at the same time. Repeated operations while Loading do not create duplicate requests.
    * Clicking a ListItem opens DetailActivity, which displays DetailFragment.
    * Menu options in the top-right corner can open FavoritesActivity, which displays FavoritesFragment.

* FavoritesFragment
    * Has an interface similar to FeedFragment but displays only items marked as Favorite.
    * Favorites are persistent. Restarting the app, using pull to refresh, or clearing the API cache does not remove Favorites.
    * Items are ordered by the time they were added as Favorite, from newest to oldest.
    * Five items are displayed by default. Each press of the Loading state button displays the next five until every item is visible.
    * Whenever the user returns to this screen, the ViewModel refreshes the list to reflect possible Favorite changes.
    * There are no Menu options. The user can only navigate Back to FeedActivity.
    * Clicking an item opens DetailActivity, which displays DetailFragment.

* DetailFragment
    * Displays the detail screen for one Item.
    * The Activity identifies the item using its source type and id.
    * Detail data is loaded only when the screen opens. The Repository checks cache first and reads from the Client only on a cache miss.
    * An expired Detail cache can still be displayed first. New data replaces the old cache only after a successful fetch; offline or failed reads do not delete old content.
    * If no Detail cache exists and the device is offline, a retryable Offline state is displayed.
    * A favorite icon in the top-right corner toggles the item's Favorite state. Favorite operations modify only local data and do not require a network connection.

## Data Layer

Content currently comes from DummyJson and SpaceFlight. Both sources follow the same processing rules. FeedFragment and DummyJson are used as examples below.

```txt
FeedFragment <-> FeedViewModel <-> DummyJsonRepository <-> DummyJsonClient, JsonCacheStore
                              <-> SpaceFlightRepository <-> SpaceFlightClient, JsonCacheStore

DetailFragment <-> DetailViewModel <-> Repository <-> Client, JsonCacheStore
FavoritesFragment <-> FavoritesViewModel <-> FavoritesRepository <-> FavoriteStore

AppDatabase <-> JsonCacheDao, FavoriteDao
```

* FeedViewModel and FeedFragment do not use API-related GSON classes. They use converted domain-specific data classes instead.
    * For example, DummyJsonItem is used to display a list item.
    * Detail is used to display an item's details.
* Each FeedItem has a unique identity composed of its source type and id. Items from different sources remain distinct even if they have the same id.
* DummyJsonRepository provides high-level APIs through which a ViewModel can obtain usable domain data classes.
    * DummyJsonRepository contains DummyJsonClient and shares JsonCacheStore with the other API Repositories.
    * The Client uses GSON classes or raw JSON tied to the API response. JsonCacheStore only stores raw JSON and neither parses nor depends on a specific API schema.
    * DummyJsonRepository calls helpers to convert GSON classes into domain data classes or reports defined errors.
* DummyJsonClient
    * This is an interface. Implementing classes encapsulate API endpoints and execute HTTP calls.
    * The only current implementation is DummyJsonClientMock, which reads and returns prepared JSON files.
    * Before reading a file, it checks the current network state and reports an Offline error when no network is available, simulating real API behavior.
    * Every DummyJsonClientMock call simulates network latency. The delay mechanism must be controllable in unit tests.
* JsonCacheStore
    * Is an interface shared by all API Repositories and provides operations for reading, storing, and deleting API response cache entries.
    * Uses a cache type and cache key to distinguish data sources, Feed pages, and Detail items.
    * Its Room-backed implementation uses JsonCacheDao to store the complete API response JSON as a String.
    * A new cache entry must be written successfully before it replaces the old entry, preventing a failed read from removing displayable data.
* FavoriteStore
    * Is the interface used by FavoritesRepository. Its Room-backed implementation persists Favorite items through FavoriteDao and a separate Favorite table.
    * A Favorite identity consists of its source type and id.
    * In addition to the identity and addedAt, it stores a snapshot of the domain data required by the Favorites list so that the list remains available offline or after the API cache is cleared.
    * When Detail successfully obtains newer data, it may update the Favorite snapshot but must not change the original addedAt.
* AppDatabase
    * Is the app's only class that extends RoomDatabase and contains the API cache table and Favorite table.
    * AppDatabase provides JsonCacheDao and FavoriteDao. Individual Repositories do not define separate Room Database classes.
    * Repositories depend on the JsonCacheStore or FavoriteStore interface instead of AppDatabase or a Room DAO directly, allowing unit tests to substitute fake implementations.

The following time-related components are also required:

* TimeProvider

    ```kotlin
    // Avoid direct System.currentTimeMillis() calls to keep tests deterministic.
    interface TimeProvider {
        fun getCurrentTimeMillis(): Long
    }
    ```

* FreshnessValidator
    * Determines whether data has expired by comparing the current time with a provided timestamp.
    * Its instance is created with a duration representing the freshness period and an injected TimeProvider.
    * The actual freshness duration is a constant to be decided during implementation.

The Feed loading flow is as follows:

1. A UI loading trigger, such as pressing the Loading state button, calls ViewModel.loadMoreItems().
1. The ViewModel checks its current state.
    1. If the state is Loading or NoMoreItems, it stops and returns.
1. The ViewModel requests the next page from DummyJsonRepository and SpaceFlightRepository at the same time.
    1. Each source maintains its own page cursor and exhausted state.
    1. No request is made for a source that is already exhausted.
    1. If a source fails, it retains its original cursor and retries the same page on the next Load. The other successful source may continue advancing.
1. A Repository obtains one page using the following flow:
    1. It first requests the page cache from JsonCacheStore.
    1. If the cache is fresh enough, it converts the cache into domain models and returns them.
    1. If the cache does not exist, it obtains new data from the Client, writes the cache successfully, converts the result, and returns it.
    1. If the cache has expired, it retains the old cache and tries to obtain new data from the Client.
        1. If the Client succeeds, the new data replaces the cache and is returned.
        1. If the Client fails or is offline, the old cache is not deleted. Displayable stale data and the corresponding state are returned.
1. The ViewModel stores the complete sequence for each source separately, removes duplicates using source type and id, and then evaluates both results.
    1. When either source obtains new data, the ViewModel updates that source's sequence, merges and displays the complete list again using the deterministic alternating rule, and returns the overall state to Ready.
    1. The overall state becomes NoMoreItems only when both sources are exhausted.
    1. When there is no new data and at least one non-exhausted source fails, the overall state becomes Error or Offline.

The pull-to-refresh flow is as follows:

1. The ViewModel retains the current page cursor, exhausted state, cache, and displayed content for both sources and starts a refresh.
1. The ViewModel asks both sources to ignore freshness and obtain their first page at the same time.
1. Each source handles its result independently.
    1. A successful source replaces its Feed cache and complete sequence with the new data, then resets its cursor and exhausted state according to the first-page response.
    1. A failed source retains its existing cache, complete sequence, cursor, and exhausted state, and retries the first page on the next pull to refresh.
1. The ViewModel merges the two source sequences again using the deterministic alternating rule.
1. The ViewModel determines the overall state using the same rules as Load more.
    1. If either source obtains a non-empty first page, the overall state returns to Ready.
    1. The overall state becomes NoMoreItems only when both sources are exhausted.
    1. If no data is obtained and at least one non-exhausted source fails, the state becomes Error or Offline according to the failure causes.

The Detail loading flow is as follows:

1. DetailActivity passes the source type and id to DetailFragment and DetailViewModel.
1. The Repository uses the source type and id to query the Detail cache.
1. On a cache miss, the corresponding Client reads the Detail response. After a successful response, the Repository writes the cache and returns Detail.
1. When the cache has expired, the old data is retained while new data is fetched. The cache is replaced only after a successful response.
1. When no cache exists and the read fails, an Error or Offline state is displayed with a retry action.

## Data structure

The four API endpoints provide DummyJson Feed, SpaceFlight Feed, DummyJson Detail, and SpaceFlight Detail. Only the key fields from the two Feed responses are shown below, with some content omitted. The Detail JSON schemas and mock fixtures will be added before implementation.

DummyJsonProducts

```json
{
  "products": [
    {
      "id": 6,
      "title": "Calvin Klein CK One",
      "category": "fragrances",
      "thumbnail": "https://cdn.dummyjson.com/product-images/fragrances/calvin-klein-ck-one/thumbnail.webp"
    },
    {
      "id": 7,
      "title": "Chanel Coco Noir Eau De",
      "category": "fragrances",
      "thumbnail": "https://cdn.dummyjson.com/product-images/fragrances/chanel-coco-noir-eau-de/thumbnail.webp"
    }
  ],
  "total": 194,
  "skip": 5,
  "limit": 5
}
```

SpaceFlight

```json
{
  "count": 35790,
  "next": "https://api.spaceflightnewsapi.net/v4/articles/?format=json&limit=5&offset=10",
  "previous": "https://api.spaceflightnewsapi.net/v4/articles/?format=json&limit=5",
  "results": [
    {
      "id": 39639,
      "title": "Curiosity Blog, Sols 4982–4987: Back to Our Regularly Scheduled Programming",
      "authors": [
        {
          "name": "NASA",
          "socials": null
        }
      ],
      "url": "https://science.nasa.gov/blog/curiosity-blog-sols-4982-4987-back-to-our-regularly-scheduled-programming/",
      "image_url": "https://a..XX.jpg",
      "summary": "some summary",
      "published_at": "2026-08-23T07:39:28Z",
      "updated_at": "2026-08-23T07:40:09.877561Z",
      "featured": false,
      "launches": [],
      "events": []
    }
  ]
}
```

The domain model classes used by FeedFragment are:

```kotlin
enum class FeedSource {
    DUMMY_JSON,
    SPACE_FLIGHT,
}

sealed interface FeedItem {
    val id: String
    val source: FeedSource
    val title: String
    val imgUrl: String
}

data class DummyJsonItem(
    override val id: String,
    override val title: String,
    override val imgUrl: String,
    val category: String,
) : FeedItem {
    override val source: FeedSource = FeedSource.DUMMY_JSON
}

data class SpaceFlightItem(
    override val id: String,
    override val title: String,
    override val imgUrl: String,
    val description: String,
) : FeedItem {
    override val source: FeedSource = FeedSource.SPACE_FLIGHT
}
```

The DetailFragment JSON schemas are temporarily omitted. The domain model class is:

```kotlin
data class Detail(
    val id: String,
    val source: FeedSource,
    val title: String,
    val imgUrl: String,
    val description: String,
    val extraInformation: String,
)
```

AppDatabase uses one shared table for all API cache entries. Its schema is:

* _id: long, auto-generated primary key
* timestamp: long
* type: int
* key: string
* jsonString: string

The type and key combinations are:

1. Type 1 represents DummyJsonFeed, and key is the page cursor.
1. Type 2 represents SpaceFlightFeed, and key is the page cursor.
1. Type 3 represents DummyJsonDetail, and key is the product id.
1. Type 4 represents SpaceFlightDetail, and key is the article id.

The combination of type and key must have a unique index so that only one valid cache entry exists for the same source and position.

JsonCacheDao and its Room-backed JsonCacheStore implementation do not parse jsonString. Each source Repository is responsible for parsing JSON and converting it into domain models.

Favorite data is not API response cache, so it uses a separate Favorite table in the same AppDatabase. The Favorite table contains at least:

* sourceType: int
* itemId: string
* addedAt: long
* title: string
* imgUrl: string
* description: string
* extraInformation: string

sourceType and itemId form a composite primary key. Favorites are ordered by addedAt from newest to oldest.

## Dependencies

As few dependencies as possible.

* kotlin coroutine, flow
* Android jetpack
    * room, for database
    * recyclerview
* google gson
* coil, for drawing image
* lich (https://central.sonatype.com/artifact/com.linecorp.lich/component)
* mockito
* kotlin-test
* Robolectric, for Room tests on the local JVM

## Test

* Only local JVM unit tests for ViewModel, Repository, Client, and data stores are currently required.
* JsonCacheDao, FavoriteDao, and Room-backed store tests run on the local JVM using Robolectric.
* Activity, Fragment, navigation, and other UI tests are currently out of scope.
* TimeProvider, network status, and mock network delay must be replaceable or injectable so that unit tests remain deterministic.
