# 實作計畫

本文件依據根目錄的 `SPEC.md`，將功能拆成可以逐步實作、測試與
review 的工作。每個 Phase 都應在前一個 Phase 通過測試後再開始，避免 UI、
狀態管理與資料來源同時變動。

## 實作原則

- `SPEC.md` 是產品行為的唯一依據；若本文件與 SPEC 衝突，先更新並確認
  SPEC，再修改實作計畫。
- 使用 `FeedActivity`、`FeedFragment`、`FeedViewModel`、
  `FavoritesActivity`、`FavoritesFragment`、`FavoritesViewModel`、
  `DetailActivity`、`DetailFragment` 與 `DetailViewModel` 等名稱。
- 使用 `FavoriteStore`、`FavoritesRepository` 與
  `FreshnessValidator`，不要使用草案中的 `FavoritesStore`、
  `FavoritestRepository`、`FreshnessValidatoer` 等錯誤名稱。
- 程式碼、註解與 commit message 使用英文；文件與開發過程中的說明使用
  台灣繁體中文，技術名詞保留英文。
- 一個 commit 只完成一個短目標。若修改超過 100 行且橫跨多個檔案，應依
  model、storage、behavior、test 等步驟拆成多個 commit。
- 每項 production code 變更都要有對應的 local JVM test。SPEC 不要求完整的
  UI test suite，但 Activity、Fragment 與 navigation 仍要有 focused
  Robolectric local JVM tests；不加入 emulator／instrumentation tests。
- 每個變更在 commit 前都要由 sub-agent 執行 code review 與適用的測試；
  發現問題後修正並重新 review。
- 不直接呼叫 `System.currentTimeMillis()`、寫死 network 狀態、使用真實 delay，
  或在測試中依賴真實時間。
- function 應保持單一職責；超過 50 行時優先拆出 pure helper 或較小的
  collaborator。
- 禁止執行 `git push` 與 `git branch -D`。
- commit subject 以 `feat:`、`fix:`、`test:`、`docs:`、`refactor:` 等 type
  開頭，subject 不超過 50 字元，body 每行不超過 72 字元。
- commit body 說明意圖、修改內容、原因與 behavior change，最後加入
  `Coding-Assistant: <Agent-Tool-Name> <version> (<Model name>)` trailer。

## 已確定的設計決策

### Feed 合併順序

兩個來源的完整序列分別保存，再依下列固定順序合併：

```text
DummyJson[0], SpaceFlight[0], DummyJson[1], SpaceFlight[1], ...
```

如果其中一個來源先用完，另一個來源剩餘的 item 依原本順序接在後面。
去除重複時使用 `(FeedSource, id)` 作為 identity，不可只比較 `id`。

### 首次載入

`FeedFragment` 第一次開啟時自動要求第一頁。尚未有任何內容時顯示全畫面的
Loading；已有內容後，load more 的狀態改由列表底部的 state button 顯示。

### Cursor

Repository 與 ViewModel 不假設所有來源都使用整數頁碼。兩個來源都使用
opaque `PageCursor`，底層可以先以 `String` 實作。DummyJson Client 可以把
cursor 解讀為 `skip`，SpaceFlight Client 可以把它解讀為 `offset` 或 fixture
識別值。

兩個來源的 initial cursor 都定義為 `PageCursor("0")`；cache key 使用 cursor
內部的原始字串。每次只採用 response 產生的 `nextCursor`，不由 ViewModel
自行加一。

`FeedPageResult` 必須同時回傳 items、`nextCursor` 與 `isExhausted`。只有 response
明確表示沒有下一頁時，才將該來源標記為 exhausted。

### Cache write failure

Client 回傳的新 JSON 只有在成功 parse 與轉換後才可寫入 cache。cache 更新
必須是 atomic upsert，不可先刪除舊資料。

如果 response 可成功轉成 domain data，但 cache 寫入失敗，本次仍可顯示新
資料並允許 cursor 前進；舊 cache 必須保留。Repository 將 cache write failure
記錄為 `cacheWarning`，不把本次內容讀取改判為 Error，也不把 warning 納入
Feed footer 的 failure matrix。

如果 refresh 的第一頁 cache transaction 寫入失敗，對應的 source Repository
在內部設定 `requiresNetworkRecovery`。flag 存在期間，後續頁使用
`NETWORK_ONLY_RECOVERY` mode：完全不讀取、不 fallback，也不寫入該來源的
Feed page cache，只使用 Client response 更新目前的 in-memory sequence。只有
後續 refresh 的第一頁 transaction 成功才清除 flag。如此失敗的 transaction
不會讓 persistent cache 或畫面混合 refresh 前後的 page generation。

### Refresh 後的 Feed cache

成功 refresh 某個來源時，JsonCacheStore 在同一 transaction 中先寫入新的
第一頁，再刪除該來源其他 cursor 的 Feed cache。只有整個 transaction 成功
才 commit；失敗時保留該來源全部舊 cache，並套用前述 non-fatal warning 與
`requiresNetworkRecovery` 規則。refresh 開始前絕不清除 cache。

### 無法解析的 cache

無論 cache 是否仍在 freshness 期間，只要 JSON 無法解析，就不能顯示或當成
成功結果。Repository 應保留該 cache、嘗試 Client，並只在新 response 成功
parse 且 cache 寫入成功後取代舊資料。如果 Client 也失敗，回傳 Client 對應的
Offline 或 Error。

### Cache read failure

Store read failure 代表沒有可用 cache，但不阻止 Repository 嘗試 Client。
Client 成功且 response 可解析時，顯示新資料；後續 cache write 若仍失敗，只
記錄 `cacheWarning`。如果 Client 也失敗，load failure 同時包含 Storage 與
Client error，因此整體使用 `Error`；只有所有實際 load failures 都是 Offline
時才使用 `Offline`。

### Stale Feed cache

Repository 讀到可解析但已過期的 Feed cache 時，先嘗試 Client：

- Client 成功：回傳並快取新資料。
- Client 失敗，但 stale page 尚未存在於 ViewModel 的來源序列：回傳 stale
  items 與 failure metadata，讓 ViewModel 可以顯示內容，但 cursor 不前進。
- Client 失敗，而且 stale page 已在目前序列：保留既有序列與 cursor。

Load more 只要真的加入至少一個新 identity，整體 footer state 回到 `Ready`。
若沒有加入新 identity，且至少一個未 exhausted 的來源失敗，才依 failure matrix
顯示 `Offline` 或 `Error`。

### Detail stale-while-revalidate

Detail Repository 使用 `Flow<DetailLoadEvent>` 表達兩階段結果：

1. 可解析的 cache 存在時，先 emit `Cached(detail, isStale)`。
2. fresh cache 到此結束，不呼叫 Client。
3. stale cache 存在時，繼續呼叫 Client。成功後 emit `Updated(detail)`；失敗時
   emit `RefreshFailed(error)`，畫面繼續保留 stale detail。
4. cache miss 或 cache 無法解析時呼叫 Client。成功後 emit `Updated(detail)`；
   失敗時 emit `LoadFailed(error)`，畫面顯示可 retry 的 Offline 或 Error。

## Data Layer 責任分層

資料從 Client 或 JsonCacheStore 取得 raw JSON，只有 Repository 可以解析 API
schema 並轉換成 domain model：

```text
Client ─────────────┐
                    ├─ raw JSON → Repository → API DTO → Domain Model
JsonCacheStore ─────┘
```

### Client

- Client interface 與 mock implementation 都只回傳 raw JSON，不回傳 API DTO
  或 domain model。
- Client 封裝 endpoint／asset 選擇、network 狀態檢查與模擬 latency。
- 每次呼叫 mock client 先經過 coroutine 的 `delay(10000)` 來模擬 latency，再檢查
  `NetworkStatusProvider`。因此 offline call 也有模擬 latency，但不會讀 asset。
  production 使用 coroutine delay，unit test 使用立即完成或可控制的 fake。
- Client 不使用 Room、不讀 cache、不執行 Gson parsing，也不決定 freshness。
- Client 使用 sealed result，至少區分 `Success(rawJson)`、`Offline` 與
  `Failure(cause)`。

建議介面：

```kotlin
interface DummyJsonClient {
    suspend fun getProducts(cursor: PageCursor): ClientResult
    suspend fun getProduct(id: String): ClientResult
}

interface SpaceFlightClient {
    suspend fun getArticles(cursor: PageCursor): ClientResult
    suspend fun getArticle(id: String): ClientResult
}
```

### JsonCacheStore

- Store 只保存與回傳 raw JSON、timestamp、cache type 與 cache key。
- Store 將 JSON 視為 opaque `String`，不使用 Gson，也不知道 API schema。
- `(type, key)` 具有 unique index，同一位置只有一筆有效 cache。
- `put` 使用 atomic upsert；寫入失敗時原有 cache 仍可讀取。
- successful refresh 使用 transaction 寫入第一頁並刪除同來源其他 Feed page；
  transaction 失敗時保留全部舊 page。
- Feed page 使用來源的 cursor 當 key；Detail 使用 item id 當 key。
- API cache 與 Favorite table 完全獨立；清除 API cache 不得影響 Favorite。

建議介面：

```kotlin
data class JsonCacheEntry(
    val rawJson: String,
    val timestamp: Long,
)

interface JsonCacheStore {
    suspend fun get(type: CacheType, key: String): JsonCacheEntry?
    suspend fun put(
        type: CacheType,
        key: String,
        rawJson: String,
        timestamp: Long,
    )
    suspend fun delete(type: CacheType, key: String)
    suspend fun replaceFeedPages(
        type: CacheType,
        firstPageKey: String,
        rawJson: String,
        timestamp: Long,
    )
}
```

### Repository

- Repository 是唯一使用 Gson API DTO 與 mapper 的 layer。
- Repository 先選擇 fresh cache、stale cache、forced refresh 或 Client，再把
  raw JSON parse 成 DTO，驗證必要欄位，最後轉成 domain model。
- Repository 對 ViewModel 只公開 domain model、cursor、exhausted 狀態與
  定義過的錯誤，不公開 raw JSON、Gson DTO、Room Entity 或 DAO。
- Repository 負責 cache freshness、parse failure、cache write failure 與
  stale fallback；ViewModel 不直接操作 cache。
- DummyJsonRepository 與 SpaceFlightRepository 分別處理自己的 Feed 與
  Detail schema。DetailRepository 依 `FeedSource` 將要求送到正確來源。
- FavoritesRepository 只依賴 `FavoriteStore`，不依賴 API cache。它負責
  toggle、查詢、分頁與 Favorite snapshot 的 domain mapping。

### DTO、Entity 與 Domain Model

- API DTO 完整反映 fixture schema，只存在於 data layer。
- Room Entity 只反映 database schema，不直接給 UI 使用。
- Domain model 是 ViewModel 與 UI 唯一使用的資料型別。
- DTO mapper 與 Entity mapper 使用純函式，分別測試正常資料、缺少必要欄位、
  空列表與不支援的值。

### ViewModel

- ViewModel 只呼叫 Repository，不呼叫 Client、Store、DAO 或 Gson。
- ViewModel 保存每個 Feed source 各自的 items、cursor 與 exhausted 狀態，再
  產生合併後的 UI list。
- ViewModel 使用 immutable `StateFlow` 公開畫面狀態，並阻止 refresh 與
  load more 同時執行。
- coroutine dispatcher 必須可注入，讓併發與重複操作測試保持 deterministic。

### UI

- Fragment 只把使用者操作送給 ViewModel，並依 `StateFlow` render UI。
- Activity 之間只傳 `FeedSource` 與 item id，不傳整個 item、DTO 或 JSON。
- Fragment 使用 lifecycle-aware collection；重建畫面不可產生重複資料請求。

## 共用狀態與錯誤 contract

在開始 Repository 前先定義以下型別，避免兩個來源各自發明不同語意：

- `FeedSource`：`DUMMY_JSON`、`SPACE_FLIGHT`。
- `PageCursor`：包裝 opaque `String` 的 value class 或 data class。
- `DataError`：至少包含 `Offline`、`Client`、`Parse` 與 `Storage`。
- `FeedPageResult`：包含 domain items、`nextCursor`、`isExhausted`、
  `isStale`、optional `loadFailure` 與 optional `cacheWarning`。只有
  `loadFailure` 參與 footer failure matrix。
- `FeedFooterState`：`Ready`、`Loading`、`NoMoreItems`、`Error`、
  `Offline`。
- `FeedUiState`：至少包含 items、initial loading、refreshing 與 footer state。
- `DetailUiState`：包含 optional detail、loading／refreshing、favorite 狀態與
  optional retryable error。
- `FavoritesUiState`：包含目前可見 items、總數與 footer state。

Load more 依表格由上到下判斷，第一個符合的結果即為 footer state：

| 結果 | Footer state |
| --- | --- |
| 任一來源加入新 identity | `Ready` |
| 未加入資料，而且兩來源皆 exhausted | `NoMoreItems` |
| 未加入資料，至少一個 load failure，且全部都是 offline | `Offline` |
| 未加入資料，而且至少一個 load failure 不是 offline | `Error` |
| 其他情況 | `Ready` |

最後一種涵蓋成功 response 只包含重複 item，但仍提供下一個 cursor 的情況；
cursor 可以前進，讓使用者繼續 load more，不應誤顯示 Error。

Refresh 也依表格由上到下判斷：

| 結果 | Footer state |
| --- | --- |
| 任一來源成功取得非空第一頁 | `Ready` |
| 沒有成功的非空頁，而且兩來源皆 exhausted | `NoMoreItems` |
| 沒有成功的非空頁，至少一個 load failure，且全部都是 offline | `Offline` |
| 沒有成功的非空頁，而且至少一個 load failure 不是 offline | `Error` |
| 其他情況 | `Ready` |

Refresh 的第一頁即使只包含畫面原本就有的 identity，只要 response 成功且
非空，仍然是 `Ready`。因此最後一頁帶回新資料時先顯示 `Ready`；下一次操作
沒有新資料且兩來源已 exhausted 時才顯示 `NoMoreItems`。失敗來源繼續保留
refresh 前的 sequence 與 cursor。

## Phase 0：建立專案與 fixture

### 實作

- [x] 建立單一 Android application module 與最小 Hello World app。
- [x] 設定 Kotlin、Android Jetpack、coroutines、Flow、Room、RecyclerView、
  SwipeRefreshLayout、Gson、Coil、Lich Component、Mockito、kotlin-test 與
  Robolectric。若 Robolectric test 需要替換 Lich component，再加入對應的
  Lich component test helper，不加入其他未使用的 Lich modules。
- [x] 設定 local JVM test source set 與 Robolectric resources。
- [x] 決定 minSdk、targetSdk、application id、Java/Kotlin toolchain 與 dependency
  versions，集中放在 version catalog 或一致的 Gradle 設定。
- [x] 在 `assets` 加入四個 endpoint 的 fixture：DummyJson Feed、DummyJson
  Detail、SpaceFlight Feed、SpaceFlight Detail。

### 驗證

- 執行 Gradle local unit test task，確認測試框架可啟動。
- 執行 debug assemble，確認空專案可編譯。
- 人工確認 assets 路徑與命名能表達 source、endpoint、cursor 與 scenario。

### 完成條件

- 空 app 可以 build。
- 測試 task 能在沒有 emulator 的環境執行。
- 後續 Client test 不需要臨時新增正常流程 fixture。

## Phase 1：建立三個畫面骨架與 navigation

### 實作

- [x] 建立三組 Activity、Fragment、layout 與 ViewModel。
- [x] 每個 Fragment 暫時只顯示畫面名稱。
- [x] `FeedActivity` 是 launcher Activity。
- [x] Feed 的暫時 item action 可以使用固定 source/id 開啟 Detail。
- [x] Feed option menu 可以開啟 Favorites。
- [x] Favorites 沒有 option menu，Back 會結束 FavoritesActivity 並回到 Feed。
- [x] Activity intent 只放 source 與 id；Detail 對缺少或不合法 argument 顯示錯誤
  並允許使用者 Back，不 crash。

### 測試與驗證

- 為三個 ViewModel 建立 local JVM smoke tests，確認初始 state 可讀取。
- 使用 Robolectric local JVM tests 確認三個 Activity 建立、Fragment 掛載、
  Feed 到 Detail／Favorites navigation、intent extras 與 Back 行為。
- 另以手動方式確認三個 Activity 的 navigation 與 Back。
- 執行 unit tests 與 debug assemble。

### 完成條件

- 三個畫面都能開啟。
- navigation 沒有傳遞 DTO、domain object 或 raw JSON。

## Phase 2：建立 domain contract、DTO、parser 與 Client

### Step 2.1：共用 domain 與 result contract

- [x] 建立 `FeedSource`、`FeedItem`、`DummyJsonItem`、`SpaceFlightItem`、
  `Detail`、`PageCursor`、`DataError` 與共用 result type。
- [x] 建立 `NetworkStatusProvider`、`TimeProvider` 與 production／
  fake implementations。
- [x] 建立 injectable coroutine dispatcher provider。
- [x] 為 identity、cursor 與 fake provider 加入 unit tests。

### Step 2.2：DTO、parser 與 mapper

- [ ] 依四種 fixture 建立 API DTO。
- [ ] 建立 Gson parser 與 DTO-to-domain mapper；不要把 Gson annotation 放進
  domain model。
- [ ] Feed parser 同時產生 items 與下一個 cursor／exhausted 資訊。
- [ ] 測試正常 JSON、空頁、最後一頁、malformed JSON、缺少必要欄位、不同
  source 相同 id，以及重複 id。

### Step 2.3：Mock Clients

- [ ] 建立 `DummyJsonClient`、`DummyJsonClientMock`、`SpaceFlightClient` 與
  `SpaceFlightClientMock`。
- [ ] Client 依 cursor/id 選擇 asset，並只回傳 raw JSON。
- [ ] 每個 Client call 先執行可控制的 delay，再執行 network check；offline 時不讀
  asset。
- [ ] 無對應 fixture、asset read failure 與其他 I/O error 回傳 `Failure`。
- [ ] 測試 raw JSON 完整回傳、所有 cursor/id mapping、offline 不讀 asset、
  controllable delay 與 missing asset。

### 完成條件

- Client test 完全不檢查 domain model。
- Parser／mapper test 不需要 Android UI。
- ViewModel 與 Fragment 不 import DTO package。

## Phase 3：實作 Room database 與 Stores

### Step 3.1：Json cache persistence

- [ ] 建立 `JsonCacheEntity`，包含 auto-generated `_id`、timestamp、type、key 與
  `jsonString`。Store interface 將 `jsonString` 暴露為語意較清楚的 `rawJson`。
- [ ] 固定使用以下 `CacheType` mapping，不可依 enum ordinal 自動產生 database
  value：`1 = DummyJsonFeed`、`2 = SpaceFlightFeed`、
  `3 = DummyJsonDetail`、`4 = SpaceFlightDetail`。
- [ ] 對 `(type, key)` 建立 unique index。
- [ ] 建立 `JsonCacheDao`，提供 query、atomic upsert、指定刪除與清除全部 API
  cache。
- [ ] 建立 `JsonCacheStore` interface 與 Room-backed implementation。
- [ ] `replaceFeedPages` 使用 Room transaction，先 upsert 新第一頁，再刪除同一
  Feed type 的其他 key；任何一步失敗都 rollback。
- [ ] Store 不 import Gson DTO 或 domain model。

### Step 3.2：Favorite persistence

- [ ] 建立 `FavoriteEntity`，以 `(sourceType, itemId)` 為 composite primary key。
- [ ] 保存 `addedAt`、title、imgUrl、description 與 extraInformation snapshot。
- [ ] `FavoriteDao` 依 `addedAt DESC` 查詢；相同 timestamp 時再依 sourceType、
  itemId 排序，確保測試 deterministic。
- [ ] 建立 `FavoriteStore` interface 與 Room-backed implementation。
- [ ] 更新既有 Favorite snapshot 時保留原本的 `addedAt`。

### Step 3.3：AppDatabase

- [ ] 建立 app 唯一的 `AppDatabase`，同時提供 JsonCacheDao 與 FavoriteDao。
- [ ] Repository 只依賴 Store interface，不直接依賴 AppDatabase 或 DAO。
- [ ] 本 demo 的初始 schema 使用 database version 1；不預先建立無用途 migration。

### 測試

使用 Robolectric local JVM tests 驗證：

- cache get／put／delete／clear 與 `(type, key)` unique 行為；
- atomic upsert 後只有一筆有效 cache；
- 模擬 atomic upsert 與 `replaceFeedPages` 失敗，確認舊 cache 仍可讀；
- Store 對 raw JSON byte-for-byte 回傳，不執行 parsing；
- Favorite composite identity、toggle 所需操作與 newest-first ordering；
- snapshot update 不修改 `addedAt`；
- 清除 API cache 不會刪除 Favorite；
- 排除 Room internal metadata tables 後，AppDatabase 只有兩張 application
  tables。

### 完成條件

- 所有 DAO 與 Store tests 在 local JVM 通過。
- database 關閉與 test isolation 正確，測試彼此不共用狀態。

## Phase 4：實作 Repository

### Step 4.1：Freshness 與 cache policy

- [ ] 實作 `FreshnessValidator`，注入 freshness duration 與 `TimeProvider`。
- [ ] 決定並以常數保存實際 freshness duration。
- [ ] `age = maxOf(0, now - timestamp)`；`age < duration` 才 fresh，剛好等於
  duration 已過期，未來 timestamp 視為 age 0。
- [ ] 明確測試 timestamp 未過期、剛好到 boundary、已過期與未來 timestamp。
- [ ] 將 fresh hit、miss、stale hit、forced refresh、parse failure 與 write failure
  流程封裝成小型 helper，避免兩個來源複製整段流程。
- [ ] Feed Repository 公開 API 只接收 cursor 與 `forceRefresh`。內部使用
  `NORMAL`、`FORCE_REFRESH_FIRST_PAGE`、`NETWORK_ONLY_RECOVERY` 三種 cache
  mode；ViewModel 不知道 mode 或 recovery flag。

### Step 4.2：DummyJsonRepository

- [ ] 實作 Feed page 與 Detail loading。
- [ ] 使用 DummyJson parser／mapper，不對外公開 DTO。
- [ ] 支援 `forceRefresh` 取得第一頁，但不預先刪除 cache。
- [ ] successful refresh 使用 `replaceFeedPages`；transaction failure 回傳新資料、
  `cacheWarning`，並在 Repository 內進入 `NETWORK_ONLY_RECOVERY`。
- [ ] recovery mode 的 Client failure 不 fallback stale cache，Client success 也不
  寫 page cache；只有後續 first-page refresh transaction 成功才恢復 NORMAL。
- [ ] cache read failure 後仍嘗試 Client；Client 成功可顯示資料，Client 也失敗時
  以包含 Storage 的 `loadFailure` 回傳 Error。
- [ ] 依本文件的 stale 與 cache write failure 決策回傳結果。
- [ ] 加入 fresh、miss、stale success、stale offline、stale failure、forced refresh、
  malformed cache、malformed Client JSON、cache read failure 與 cache write failure
  tests。
- [ ] 測試 recovery mode 完全不讀／不寫 page cache、Client failure 不 stale
  fallback、Repository 重建只讀到完整舊 generation，以及下一次 refresh
  transaction 成功後恢復 NORMAL。

### Step 4.3：SpaceFlightRepository

- [ ] 實作與 DummyJson 相同的 policy，但使用 SpaceFlight schema 與 cursor。
- [ ] 不複製 DummyJson 專屬 DTO 或 mapper。
- [ ] 執行與 Step 4.2 對稱的測試矩陣，另測 `next` 為 null 的 exhausted 判斷。

### Step 4.4：DetailRepository router

- [ ] 只依 `FeedSource` 與 id 導向對應 source repository。
- [ ] 使用 `Flow<DetailLoadEvent>` 表達 cached、updated、refresh failed 與 load
  failed。
- [ ] 測試每種 source routing、fresh cache 單次 emission、stale cache 兩階段
  emission、無 cache offline 與 retry。

### Step 4.5：FavoritesRepository

- [ ] 使用 `FavoriteStore` 實作 `isFavorite`、add、remove、toggle、query 與
  snapshot update。
- [ ] add 時透過 `TimeProvider` 產生 `addedAt`。
- [ ] toggle 與 API network 完全無關。
- [ ] 使用 limit／offset 或等價介面支援每次五筆的列表分頁。
- [ ] 測試新增、移除、重複 add、排序、五筆分頁、最後一頁、snapshot update 保留
  `addedAt`，以及任何 network state 都不影響本機操作。

### 完成條件

- Repository tests 使用 fake Client 與 fake Store，不啟動 Room。
- 每個來源都通過相同的 cache policy test matrix。
- 所有公開結果只包含 domain model 與定義過的 error/result types。

## Phase 5：實作 FeedViewModel

### 實作

- [ ] 建立 immutable `FeedUiState` 與 private mutable state。
- [ ] 初始化時自動同時取得兩個來源的第一頁。
- [ ] 分別保存兩個 source 的完整序列、cursor 與 exhausted 狀態。
- [ ] 使用兩個 child coroutine 同時讀取未 exhausted 的來源，再一起 evaluate。
- [ ] 成功來源可獨立更新並前進；失敗來源保留 cursor，下次重試同一頁。
- [ ] exhausted source 不建立 request。
- [ ] 用純函式完成 deduplication 與 deterministic alternating merge。
- [ ] `loadMoreItems()` 在 Loading、refreshing 或 NoMoreItems 時立即 return。
- [ ] `refresh()` 在 load more 或 refresh 執行中立即 return。
- [ ] refresh 對兩個來源強制讀第一頁。成功來源替換自己的完整序列與 cursor；
  失敗來源保留原 sequence、cursor 與 exhausted 狀態。
- [ ] 分別使用 load-more 與 refresh failure matrix 計算 footer state。

### 測試

- 初次載入成功、部分成功、雙 offline 與混合 failure；
- 兩個來源確實併發開始；
- 固定交錯、來源長度不同、空來源與 `(source, id)` deduplication；
- 各來源 cursor 獨立前進，失敗來源重試相同 cursor；
- exhausted source 不再 request，兩個來源 exhausted 才 NoMoreItems；
- stale page 新增資料與 stale page 已存在的差異；
- duplicate-only page 可以前進且維持 Ready；
- load more partial success 的所有重要組合；
- refresh partial success 只替換成功來源；
- refresh 回傳非空但 identity 全相同時仍是 Ready；
- repeated load、repeated refresh 與交叉操作不產生重複 request；
- dispatcher-controlled test 不使用 real delay。

### 完成條件

- ViewModel test 覆蓋 SPEC 中所有 Feed state transition。
- merge 與 failure evaluation 是小型 pure functions，有獨立測試。

## Phase 6：實作 DetailViewModel 與 FavoritesViewModel

### Step 6.1：DetailViewModel

- [ ] 從 saved state／建立參數取得 `FeedSource` 與 id，並驗證輸入。
- [ ] 畫面第一次開啟時才開始 Detail load。
- [ ] 收集 `DetailLoadEvent` 並保留已顯示的 stale detail。
- [ ] 沒有 detail 時，Offline／Error 顯示 retry；已有 stale detail 時，refresh
  failure 不清空內容。
- [ ] 同時查詢 Favorite 狀態；toggle 使用目前 Detail snapshot 寫入本機。
- [ ] 成功取得新版 Detail 時，如果該項目已是 Favorite，可以更新 snapshot，但
  必須保留 `addedAt`。
- [ ] 阻止重複 load 與重複 toggle 操作。

### Step 6.2：FavoritesViewModel

- [ ] 第一次顯示五筆，`loadMoreItems()` 每次增加五筆可見資料。
- [ ] 全部顯示後進入 NoMoreItems；空列表也視為沒有更多項目。
- [ ] 每次 Fragment 回到 resumed 狀態時呼叫 refresh，透過
  FavoritesRepository 重讀資料，以反映 Detail 畫面的 toggle。
- [ ] refresh 後保留合理的 visible limit，但不可超過目前總數。
- [ ] Favorite 是本機操作，不因 offline 顯示 Offline。

### 測試

- Detail fresh cache、stale-first then updated、stale refresh failure、無 cache
  Offline／Error、retry、invalid arguments；
- Favorite 初始狀態、add、remove、toggle failure 與 snapshot update；
- Favorites 0、1、5、6、10、11 筆的分頁狀態；
- return refresh 的新增／移除反映與 ordering；
- repeated action 不建立重複工作。

### 完成條件

- ViewModel tests 不依賴 Android Activity／Fragment。
- state 中已有內容時，背景更新失敗不會清除可顯示資料。

## Phase 7：使用 Lich 組裝 application dependencies

### 實作

- [ ] 建立 application-level dependency component，使用 Lich `ComponentFactory`
  管理 singleton dependency graph；Lich 只負責取得 component，內部物件仍使用
  constructor injection。
- [ ] 由該 component 建立並持有單一 AppDatabase。
- [ ] 建立 production TimeProvider、NetworkStatusProvider、Clients、
  Stores 與 Repositories。
- [ ] 建立三個 ViewModel factories；Fragment 只透過 Lich component 取得 factory，
  不手動 new Repository 或 Database。
- [ ] database、Store 與 Repository 的生命週期不得短於使用它們的 ViewModel。
- [ ] mock network 狀態應有 demo app 可控制的來源，並保留未來替換成真實實作的
  interface boundary。

### 測試與驗證

- 使用 Lich test helper 或獨立 test component 替換 dependency，為
  factory／composition 的重要 mapping 建立 Robolectric local JVM test。
- 執行全部 local tests 與 debug assemble。
- 檢查沒有第二個 RoomDatabase subclass。

### 完成條件

- 三個 ViewModel 可以透過 factory 取得完整 dependency graph。
- 每個 application context 只取得同一組 singleton components，測試之間不
  共用 component state。
- UI layer 不知道 Room、Gson DTO 或 Client implementation。

## Phase 8：實作 FeedFragment

### 實作

- [ ] 建立 SwipeRefreshLayout、RecyclerView、空內容／initial loading 與錯誤畫面。
- [ ] 使用單一 adapter 支援 DummyJson item、SpaceFlight item 與 footer state
  button 三種 view type。
- [ ] item layout 顯示 title、source-specific information 與 Coil image。
- [ ] 使用 stable `(source, id)` identity 與 DiffUtil。
- [ ] lifecycle-aware 收集 `FeedUiState`，render 時不直接修改 ViewModel state。
- [ ] pull-to-refresh 呼叫 `refresh()`；footer button 依狀態呼叫 load more 或 retry。
- [ ] `Ready`、`Loading`、`NoMoreItems`、`Error`、`Offline` 都有明確文案與 enabled
  狀態。
- [ ] 點擊 item 只傳 source 與 id 到 DetailActivity。
- [ ] option menu 開啟 FavoritesActivity。

### 驗證

- 使用 Robolectric local JVM tests 驗證主要 state 的 view visibility／enabled
  狀態、pull-to-refresh 與 footer action forwarding、item click extras，以及
  Fragment 重建不重複觸發初次載入。
- 執行全部 local tests、lint 與 debug assemble。
- 手動確認初次載入、load more、refresh、rapid repeated taps、partial failure、
  Offline、Error、NoMoreItems、不同長度來源與圖片失敗 placeholder。

### 完成條件

- UI 顯示順序與 FeedViewModel state 完全一致。
- 旋轉或重建 Fragment 不會建立重複資料 request。

## Phase 9：實作 DetailFragment

### 實作

- [ ] layout 顯示 image、title、description、extra information、loading 與 error。
- [ ] stale detail 出現後保持可見，背景 refresh 成功時更新；失敗時提供
  非破壞性的 error indication。
- [ ] 無 cache 的 Offline／Error state 提供 retry action。
- [ ] favorite icon render 當前狀態；操作期間避免重複點擊，成功後更新 icon。
- [ ] invalid source/id 顯示錯誤並允許 Back，不 crash。

### 驗證

- 使用 Robolectric local JVM tests 驗證 loading、content、stale content、
  Offline／Error、retry、favorite action forwarding 與 invalid arguments。
- 執行全部 local tests、lint 與 debug assemble。
- 手動確認兩種 source、fresh cache、stale update、stale failure、無 cache
  offline、retry，以及離線 Favorite toggle。

### 完成條件

- Activity 只靠 source/id 可以重新建立完整 Detail 畫面。
- 任何 network failure 都不會刪除既有 detail 或 favorite。

## Phase 10：實作 FavoritesFragment

### 實作

- [ ] 重用 Feed item adapter/model 能力，必要時只增加 Favorite 專用 binding，不另建
  重複的 Favorites domain model。
- [ ] 初始顯示五筆，footer button 每次顯示後五筆，全部顯示後為 NoMoreItems。
- [ ] pull-to-refresh 只重讀本機 Favorite，不清除資料。
- [ ] `onResume` 通知 ViewModel refresh，以反映 Detail 中的變更。
- [ ] 按 newest-first 順序 render；點擊 item 只傳 source/id 到 Detail。
- [ ] 不提供 option menu，系統 Back 回到 Feed。

### 驗證

- 使用 Robolectric local JVM tests 驗證空列表、content、footer states、refresh、
  item click extras、沒有 option menu，以及 resumed refresh forwarding。
- 執行全部 local tests、lint 與 debug assemble。
- 手動確認空列表、少於五筆、剛好五筆、多頁、Detail add/remove 後返回、app
  restart、offline，以及清除 API cache 後 Favorite 仍可顯示。

### 完成條件

- Favorite 的可見性與 API cache、network 狀態無關。
- 分頁與返回刷新不產生重複或錯誤排序。

## Phase 11：整體驗收

### 自動檢查

- 執行全部 local JVM unit tests。
- 執行全部 Robolectric local JVM UI tests。
- 執行 lint。
- 執行 debug assemble。
- 確認測試不使用 real network、real delay 或 wall clock。
- 由 sub-agent review architecture boundary、error matrix、cache policy、state
  transition、測試缺口與不必要 dependency。

### 手動 smoke test

- 依序走過 Feed → Detail → Favorite toggle → Back → Favorites。
- 驗證兩來源 load more partial success 與各自 cursor。
- 驗證 refresh partial success 保留失敗來源內容。
- 驗證 fresh、stale、offline、generic failure 與 retry。
- 驗證 app restart 與 API cache clear 不移除 Favorite。
- 驗證 repeated taps 不建立重複 request。

### 最終完成條件

- `SPEC.md` 每項可觀察行為都有 production implementation 與對應 local JVM
  test；layout 視覺品質另以 manual verification 補充。
- 所有自動檢查通過，沒有未處理的 review finding。
- Git history 由小型、單一目的且含 Coding-Assistant trailer 的 commits 組成。
