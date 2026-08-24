## 簡介

* 一個顯示 API response 內容列表的 App。
* 這是一個 Demo App，不需要涵蓋所有實務細節。
* App 有三個頁面，每個頁面各有一個 Activity，以及由該 Activity 包含的一個 Fragment。

## 架構

* 三個頁面分別為 FeedActivity + FeedFragment、FavoritesActivity + FavoritesFragment、DetailActivity + DetailFragment。
* 每個 Fragment 都有對應的 ViewModel，例如 FeedFragment 會有 FeedViewModel。
* Fragment 的 UI 操作會呼叫 ViewModel 的介面。
* Fragment 訂閱 ViewModel 內部的狀態。當 ViewModel 內部的狀態改變時，Fragment 隨之更新介面。
* ViewModel 透過 Repository 取得資料，判斷結果並更新內部狀態。
* Activity 之間只傳遞 item 的 source type 與 id。接收端透過 Repository 取得顯示所需的資料。

## UI

* FeedFragment
    * 有一個 RecyclerView List，能顯示 DummyJson 與 SpaceFlight 兩種來源的內容。
    * 兩個來源的內容使用固定交錯方式合併，同時保留各來源原本的順序。
    * List 可以 pull to refresh。Refresh 會忽略 cache freshness，並且重新取得兩個來源的內容。
        * Refresh 不會預先刪除 cache。某個來源成功取得新資料後，才以新資料取代該來源的舊 cache 與畫面內容。
        * 若某個來源 refresh 失敗，保留該來源原有的 cache 與畫面內容。
    * List 最下方會有 Loading state button，按下後會同時讀取兩個來源的更多內容。
        * State 有以下幾種：Ready, Loading, No more items, Error, Offline。
        * 任一來源取得新資料，整體就視為成功，更新該來源的序列、重新合併列表並回到 Ready。
        * 只有兩個來源都明確沒有下一頁時，才進入 No more items。
        * 若沒有取得新資料，而且至少一個尚未 exhausted 的來源讀取失敗，顯示 Error 或 Offline，讓使用者可以 retry。
        * 沒有新資料時，若所有失敗原因都是 offline，則顯示 Offline；其他整體失敗的組合顯示 Error。
    * Load more 與 pull to refresh 不會同時執行；Loading 中的重複操作不會建立重複請求。
    * 點擊 ListItem 會開啟 DetailActivity，並顯示 DetailFragment。
    * 右上角有 Menu options，可以開啟 FavoritesActivity，並顯示 FavoritesFragment。

* FavoritesFragment
    * 介面類似 FeedFragment，但是只顯示已經被標註為 Favorite 的 items。
    * Favorites 會持久保存，App 重啟、pull to refresh 或清除 API cache 都不會移除 Favorites。
    * Items 依加入 Favorite 的時間由新到舊排序。
    * 預設只顯示五個，每按一次 Loading state button 會顯示後面五個，直到全部顯示。
    * 每次回到此畫面，都會要求 ViewModel 刷新 List 的資料，以反映 Favorites 可能的變化。
    * 沒有 Menu options，只能透過 Back 回到 FeedActivity。
    * 點擊 item 後可以開啟 DetailActivity，並顯示 DetailFragment。

* DetailFragment
    * 顯示某個 Item 的詳細頁面。
    * Activity 透過 source type 與 id 指定要顯示的 item。
    * 開啟頁面時才讀取 Detail。Repository 會先查詢 cache；cache miss 時才從 Client 讀取。
    * 若 Detail cache 已過期，仍可先顯示舊內容。取得新資料成功後才取代舊 cache；offline 或讀取失敗時不刪除舊內容。
    * 若沒有任何 Detail cache 且 offline，顯示可以 retry 的 Offline state。
    * 右上角有 favorite icon，可以 toggle 該 item 的 Favorite 狀態。Favorite 操作只修改本機資料，不需要網路。

## Data Layer

目前內容來源有 DummyJson 與 SpaceFlight 兩種，兩者處理的邏輯相同，後續以 FeedFragment 與 DummyJson 為範例。

```txt
FeedFragment <-> FeedViewModel <-> DummyJsonRepository <-> DummyJsonClient, JsonCacheStore
                              <-> SpaceFlightRepository <-> SpaceFlightClient, JsonCacheStore

DetailFragment <-> DetailViewModel <-> Repository <-> Client, JsonCacheStore
FavoritesFragment <-> FavoritesViewModel <-> FavoritesRepository <-> FavoriteStore

AppDatabase <-> JsonCacheDao, FavoriteDao
```

* FeedViewModel 與 FeedFragment 不使用任何 API 相關的 GSON 類別，而是使用轉換過的 domain-specific data class。
    * 例如 DummyJsonItem 用來顯示列表。
    * Detail 用來顯示某個 item 的細節。
* 每個 FeedItem 使用 source type 與 id 組成唯一 identity。兩個來源即使有相同 id，也會被視為不同 item。
* DummyJsonRepository 提供高階 API，ViewModel 能藉此取得可用的 domain data class。
    * DummyJsonRepository 內部有 DummyJsonClient，並與其他 API Repository 共用 JsonCacheStore。
    * Client 使用與 API response 相依的 GSON 類別或原始 JSON。JsonCacheStore 只儲存原始 JSON，不解析或依賴特定 API schema。
    * DummyJsonRepository 負責呼叫 helper，將 GSON 類別轉換成 domain data class，或回報定義過的錯誤。
* DummyJsonClient
    * 僅是 interface。實作該介面的 class 會封裝 API endpoint，並負責執行 HTTP call。
    * 目前只有一個 DummyJsonClientMock 實作，僅讀取並回傳預先準備好的 JSON 檔案。
    * 讀檔之前會先檢查當下的網路狀態，若無網路則回報 Offline error，以模擬真實 API 行為。
    * DummyJsonClientMock 的任何呼叫皆會模擬網路延遲。延遲機制必須可以在 unit test 中控制。
* JsonCacheStore
    * 是所有 API Repository 共用的 interface，提供讀取、儲存與刪除 API response cache 的功能。
    * 使用 cache type 與 cache key 區分資料來源、Feed page 與 Detail item。
    * Room-backed implementation 透過 JsonCacheDao 將 API response 的 JSON 以 String 格式完整存入資料庫。
    * 寫入新的 cache 必須成功後才能取代舊 cache，避免讀取失敗時失去可顯示的資料。
* FavoriteStore
    * 是 FavoritesRepository 使用的 interface。Room-backed implementation 透過 FavoriteDao 與獨立的 Favorite table 持久保存 Favorite items。
    * Favorite identity 為 source type 與 id。
    * 除了 identity 與 addedAt，也保存 Favorites 列表所需的 domain data snapshot，讓列表在 offline 或 API cache 被清除後仍可顯示。
    * Detail 成功取得較新的資料時，可以更新 Favorite snapshot，但不能改變原本的 addedAt。
* AppDatabase
    * 是 app 中唯一繼承 RoomDatabase 的 class，包含 API cache table 與 Favorite table。
    * AppDatabase 提供 JsonCacheDao 與 FavoriteDao。不同 Repository 不建立各自的 Room Database class。
    * Repository 依賴 JsonCacheStore 或 FavoriteStore interface，不直接依賴 AppDatabase 或 Room DAO，讓 unit test 可以替換成 fake implementation。

其他還有關於時間的實作：

* TimeProvider

    ```kotlin
    // Avoid direct System.currentTimeMillis() calls to keep tests deterministic.
    interface TimeProvider {
        fun getCurrentTimeMillis(): Long
    }
    ```

* FreshnessValidator
    * 用來判斷一筆資料是否已經過期，使用當下的時間與給定時間的差異。
    * 建立 instance 時需要給定代表新鮮期的時間長度，並注入 TimeProvider。
    * 實際 freshness 時間長度屬於實作階段決定的常數。

取得 Feed 資料的邏輯如下：

1. UI trigger loading，例如按下 Loading state button，呼叫 ViewModel.loadMoreItems()。
1. ViewModel 判斷當下狀態。
    1. 如果是 Loading 或 NoMoreItems，停止並 return。
1. ViewModel 同時向 DummyJsonRepository 與 SpaceFlightRepository 要求各自的下一頁。
    1. 每個來源各自維護 page cursor 與 exhausted 狀態。
    1. 已經 exhausted 的來源不再發出請求。
    1. 某個來源失敗時保留原本的 cursor，下次 Load retry 同一頁；另一個成功的來源可以繼續前進。
1. Repository 取得某一頁資料的邏輯如下：
    1. 首先向 JsonCacheStore 取得該頁 cache。
    1. 若 cache 足夠新鮮，轉換成 domain model 後回傳。
    1. 若 cache 不存在，使用 Client 取得新資料，成功寫入 cache 後轉換並回傳。
    1. 若 cache 已過期，保留舊 cache 並嘗試使用 Client 取得新資料。
        1. Client 成功時，以新資料取代 cache 後回傳。
        1. Client 失敗或 offline 時，不刪除舊 cache，並回傳可顯示的 stale data 與對應狀態。
1. ViewModel 分別保存兩個來源的完整序列，並使用 source type 與 id 排除重複項目，再判斷兩個來源的結果。
    1. 任一來源取得新資料時，更新該來源的序列，再依固定交錯規則重新合併並顯示完整列表，整體狀態回到 Ready。
    1. 只有兩個來源都 exhausted 時，整體狀態才是 NoMoreItems。
    1. 沒有新資料且至少一個未 exhausted 的來源失敗時，整體狀態是 Error 或 Offline。

Pull to refresh 的邏輯如下：

1. ViewModel 保留兩個來源目前的 page cursor、exhausted 狀態、cache 與畫面內容，並開始 refresh。
1. ViewModel 同時要求兩個來源忽略 freshness 並取得第一頁。
1. 每個來源獨立處理結果。
    1. 成功的來源以新資料取代該來源的 Feed cache 與完整序列，並依第一頁 response 重設 cursor 與 exhausted 狀態。
    1. 失敗的來源保留原有 cache、完整序列、cursor 與 exhausted 狀態，等待下一次 pull to refresh 再重試第一頁。
1. ViewModel 重新使用固定交錯規則合併兩個來源的內容。
1. ViewModel 使用與 Load more 相同的規則判斷整體狀態。
    1. 任一來源取得非空的第一頁時，整體狀態回到 Ready。
    1. 只有兩個來源都 exhausted 時，整體狀態才是 NoMoreItems。
    1. 沒有取得資料且至少一個未 exhausted 的來源失敗時，依失敗原因顯示 Error 或 Offline。

取得 Detail 資料的邏輯如下：

1. DetailActivity 將 source type 與 id 傳給 DetailFragment 與 DetailViewModel。
1. Repository 使用 source type 與 id 查詢 Detail cache。
1. Cache miss 時，使用對應的 Client 讀取 Detail response，成功後寫入 cache 並回傳 Detail。
1. Cache 過期時先保留舊資料，再嘗試讀取新資料；只有成功時才取代 cache。
1. 沒有 cache 且讀取失敗時，顯示 Error 或 Offline state，並允許 retry。

## Data structure

四個 API endpoint 分別提供 DummyJson Feed、SpaceFlight Feed、DummyJson Detail 與 SpaceFlight Detail。以下僅列出兩個 Feed response 的關鍵欄位，部分內容有所刪減。Detail JSON schema 與 mock fixtures 會在實作前補上。

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

在 FeedFragment 會用到的 domain model class 為：

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

DetailFragment 使用的 JSON schema 目前暫時略過，但是會使用的 domain model class 為：

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

AppDatabase 使用一張共用 table 儲存所有 API cache。其 schema 為：

* _id: long, auto-generated primary key
* timestamp: long
* type: int
* key: string
* jsonString: string

type 與 key 有以下組合：

1. 1 代表 DummyJsonFeed，此時 key 代表 page cursor。
1. 2 代表 SpaceFlightFeed，此時 key 代表 page cursor。
1. 3 代表 DummyJsonDetail，此時 key 代表產品 id。
1. 4 代表 SpaceFlightDetail，此時 key 代表文章 id。

type 與 key 的組合必須建立 unique index，確保相同來源與位置只有一筆有效 cache。

JsonCacheDao 與對應的 Room-backed JsonCacheStore implementation 不解析 jsonString。JSON parsing 與轉換成 domain model 的責任屬於各來源的 Repository。

Favorite data 不屬於 API response cache，因此使用同一個 AppDatabase 中的獨立 Favorite table。Favorite table 至少包含：

* sourceType: int
* itemId: string
* addedAt: long
* title: string
* imgUrl: string
* description: string
* extraInformation: string

sourceType 與 itemId 組成 composite primary key。Favorites 依 addedAt 由新到舊排序。

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

* 目前只需為 ViewModel、Repository、Client 與 data store 寫 local JVM unit test。
* JsonCacheDao、FavoriteDao 與 Room-backed store test 使用 Robolectric 在 local JVM 執行。
* Activity、Fragment、navigation 與其他 UI test 目前不在範圍內。
* TimeProvider、network status 與 mock network delay 必須可以替換或注入，讓 unit test 保持 deterministic。
