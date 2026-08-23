## 簡介

* An app to show list of contents that renders from API response.
* This is a demo app, so it does not need to be perfect in all details.
* 三個頁面，一個 Activity 包含一個 Fragment

## 架構

* 一個 Activity 包含一個 Fragment
* 每個 Fragment 都會有對應的 ViewModel，例如 FeedFragment 會有 FeedViewModel
* Fragment 的 UI 操作會呼叫 ViewModel 的介面
* Fragment 訂閱 ViewModel 內部的狀態。當 ViewModel 內部的狀態改變時，Fragment 隨之更新介面。
* ViewModel 內部有 Repository class，用來提供資料給 ViewModel 做判斷並更新內部狀態。

## UI

* FeedFragment
    * 有一個 RecyclerView List 能顯示兩種以上不同來源的內容
    * List 可以 pull to refresh，清除現有 cache 並且重新取得內容
    * List 最下方會有 Loading state button，按下後就觸發事件，讀取更多內容
        * 該 State 有幾種狀態：Ready, Loading, No more items, Error, Offline
    * ListItem clicked 會打開 DetailFragment
    * 右上角有 Menu options 裡面可以選擇打開 FavoritesFragment

* FavoritesFragment
    * 介面類似 FeedFragment，但是只顯示已經被標註為 Favorites 的 items
        * 預設只顯示五個，每按一次 Loading state button 會觸發讀取後面五個，直到全部顯示
    * 每次回到此畫面，都會要求 ViewModel 刷新 List 的資料，對應 Favorites 可能的變化
    * 沒有 Menu options，只能透過 Back 回到 FeedFragment
    * clicked item 後可以打開 DetailFragment

* DetailFragment
    * 顯示某個 Item 的詳細頁面
    * 右上角有個 favorite icon，可以 toggle 該 item 的 favorites 狀態


## Data Layer

這部談論資料處理的部分。目前內容來源有 DummyJson 與SpaceFlight 兩種，兩者處理的邏輯相同，後續以 FeedFragment, DummyJson 為範例。

```txt
FeedFragment <-> FeedViewModel <-> DummyJsonRepository <-> DummyJsonClient, JsonRespDb
```

* FeedViewModel 與 FeedFragment 不使用任何 API 相關的 GSON 格式檔案，反之，使用轉化過的 Domain specific data class
    * 譬如說 DummyJsonItem 用來顯示列表
    * DummyJsonDetail 用來顯示某個 item 的細節
* DummyJsonRepository 提供高階的 API，FeedViewModel 能藉此取得可用的 data class
    * DummyJsonRepository 內部有 DummyJsonClient 與 JsonRespDb，從此兩處皆可取得與 API 相依的 GSON 類別
    * DummyJsonRepository 負責呼叫其他 helper 將 GSON 轉換成 data class，或回報潛在的各種錯誤
* DummyJsonClient
    * 僅是 Interface，實作該介面的子類別會封裝 API Endpoint，並負責執行 http call
    * 但是目前只會有一個 DummyJsonClientMock 的實作，該實作僅會讀取並回傳，預先準備好的 JSON 檔案。
    * 讀檔之前會先檢查當下的網路狀態，若無網路會 throws Exception
    * DummyJsonClientMock 的任何呼叫，皆會模擬網路延遲
* JsonRespDb
    * 提供 cache 功能，可以儲存或刪除從 Client 取得的 API response
    * 透過 Android Room，將 API response 的 JSON 以 String 的格式完整存入資料庫

其他還有關於時間的實作

* TimeProvider

    ```kotlin
    # 使用此 interface 避免直接使用 System.currentTimeMillis() 造成測試困難
    interface TimeProvider {
        fun getCurrentTimeMillis(): Long
    }
    ```

* FreshnessValidator
    * 用來判斷一筆資料是否已經過期，使用當下的時間與給定時間的差異
    * 產生實體時需要給定時間長度，代表新鮮期。並且注入 TimeProvider

取得資料的邏輯如下

1. UI Trigger loading(按下 Loaind state button)，呼叫 ViewModel.loadMoreItems()
1. ViewModel 判斷當下狀態
    1. 如果是 Loading or NoMoreItems, stop and return
1. ViewModel 用 DummyJsonRepository 取得資料，並且傳入 FreshnessValidator 用做判斷
    1. 若順利取得：append and display items
    1. 若無法取得，根據錯誤狀態更新 UI
1. DummyJsonRepository 處理的邏輯
    1. 首先跟 RespDb 取得資料
    1. 若順利取得，利用傳入的 FreshnessValidator 比對新鮮度
        1. 若足夠新鮮，轉換成 model class 回傳
        1. 若不夠新鮮，要求 JsonRespDb 清除 cache。呼叫 DummyJsonRepository.fetchNewItems
    1. 若無法取得，呼叫 DummyJsonRepository.fetchNewItems
    1. DummyJsonRepository.fetchNewItems
        1. 使用 DummyJsonClient 取得新的 GSON 檔案，並且存入 JsonRespDb 的 cache
        1. 轉換成 model class 回傳

## Data structure

四個 API endpoint 取得的 response 如下，僅列出關鍵欄位，部分有所刪減

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
    },
    ...(略)
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
    },
    ...(略)
  ]
}
```

在 FeedFragment 會用到的 domain model class 為

```kotlin
sealed interface FeedItem {
    val title: String,
    val imgUrl: String,
}

data class DummyJsonItem(
    override val title: String,
    override val imgUrl: String,
    val category: String,
) : ListItem

data class SpaceFlightItem(
    override val title: String,
    override val imgUrl: String,
    val description: String,
) : ListItem
```

DetailFragment 使用的 JSON 檔目前先暫時略過，但是會使用的 domain model class 為

```kotlin
data class Detail(
    val title: String,
    val imgUrl: String,
    val description: String,
    val extraInformation: String,
)
```

在 Database 儲存 cache 的 schema 為

* _id
* timestamp: long
* type: int
* key: string
* jsonString: string

type 有以下幾種

1. 1, 代表 DummyJsonFeed，此時 key 代表第幾頁
1. 2, 代表 SpaceFlightFeed，此時 key 代表第幾頁
1. 3, 代表 DummyJsonDetail，此時 key 代表產品 id
1. 4, 代表 SpaceFlightDetail，此時 key 代表文章 id

## Dependencies

As less dependencies as possible.

* kotlin coroutine, flow
* Android jetpack
    * room, for database
    * recyclerview
* google gson
* coil, for drawing image
* lich (https://central.sonatype.com/artifact/com.linecorp.lich/component)
* mockito
* kotlin-test

## Test

* 目前只需為 ViewModel, Repository, Client, Db 寫 unit test

