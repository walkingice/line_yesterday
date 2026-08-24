## Phase 0

增加空的專案架構，並未開始任何真正的實作

- [ ] Add hello world empty project files
- [ ] Add fake json data to asssets
- [ ] Update gradle file by adding dependencies

## Phase 1

僅有最簡陋的 Hello World 等級的畫面，僅為確保每個畫面基礎元件的存在，並準備好測試的框架，留待後續的實作

- [ ] Add empty Activity and Fragments for Feeds, Favorite and Detail. 每個頁面的中間只有一個簡單的 Text View 寫下該畫面的名字，不需要其他複雜的實作。 Feeds 的主畫面 click 之後可以打開 Detail，也可以在 Option 裡面打開 Favorite 畫面
- [ ] 為每個畫面都產生對應的 ViewModel class，以及對應的 ViewModelTest。ViewModel 目前只會有一個 `getTitle` 的 function，還不需要有其他複雜的實作
- [ ] Add interface DummyJsonClient and 實作 DummyJsonClientMock，以及對應的測試，確認能從 local assets 讀取 JSON 檔並且轉換成 GSON。目前只有兩個 functions： `getProducts(pageNumber: Int)` 與 `getProduct(id: Int)`
- [ ] Add interface SpaceFlightClient and 實作 SpaceFlightClientMock，以及對應的測試，確認能從 local assets 讀取 JSON 檔並且轉換成 GSON。目前只有兩個 functions： `getArticles(pageNumber: Int)` 與 `getArticle(id: Int)`

## Phase 2

開始實作 Data Layer，此階段聚焦在 database 相關的部分

- [ ] 增加必須的 DTO 開始做準備
- [ ] 增加 JsonCacheStore，並且以 prepared JSON assets 做測試
- [ ] 增加 FavoritesStore
- [ ] 增加 AppDatabase

## Phase 3

繼續實作 Data Layer

- [ ] 增加 TimeProvider 以及 FreshnessValidatoer
- [ ] 實作 DummyJsonRepository
- [ ] 實作 SpaceFlightRepository
- [ ] 實作 FavoritestRepository

## Phase 4

實作各個 ViewModel，接上已經做好的 Repository，並且確認能夠正確初始化

- [ ] 實作 FeedsViewModel，在內部增加必須的狀態，並且使用 DummyJsonRepository 與 SpaceFlightRepository
- [ ] 實作 DetailViewModel
- [ ] 實作 FavoritesViewModel

## Phase 5

實作 FeedsFragment

- [ ] 新增 RecyclerView 會用到的 xml for DummyJsonFeedsItem 與 SpaceFlightFeedItem，以及對應的 domain Model class
- [ ] 使用 ViewModel 提供的資料與狀態 render RecyclerView

## Phase 6

實作 DetailFragment

- [ ] 修改 DetailFragment 的 layout xml
- [ ] 使用 ViewModel 提供的資料與狀態 render layout
- [ ] 在畫面上按下 favorite 按鈕時，要能夠收藏或移除該項目

## Phase 7

實作 FavoritesFragment

- [ ] 新增 RecyclerView 會用到的 xml for DummyJsonFavoritesItem 與 SpaceFlightFavoritesItem，以及對應的 domain Model class
- [ ] 使用 ViewModel 提供的資料與狀態 render RecyclerView

