# Introduction

LINE Yesterday, a demo app for LINE assignment.


## Build

請使用 Android Studio 編譯並安裝，或是使用命令列

```bash
# Prepare your local.properties
$ ./gradlew installDebug
```

## Mock / Real Client

使用 **spaceflight** 與 **dummyjson** 作為 API 來源，為了開發方便，在 `app/src/main/assets` 有備妥事先下載的 json 檔。

可以在 Settings 頁面切換為 Real client，重開後便會抓取真正的 API responses.

## Freshness

關於新鮮度的設定，放在 `CachePolicy.kt` 的 `CACHE_FRESHNESS_DURATION_MILLIS`，目前設定為 5 分鐘。可調整

## Plans and Sequencing

1. 閱讀文件之後，首先釐清整個 App 的功能，以及有多少畫面要做，每個畫面的功能大概是如何
1. 接著嘗試各個 API，確認如何拿到足以滿足需求的 API response，以及呼叫的方法
    - 選擇了 Space Flight 與 Dummy Json 的理由：比起天氣，這兩個 feed 的邏輯很相近。幾乎可以視為，只要改 API Endpoint 與 resonse parser，兩邊幾乎相同。
1. UI 的部分，確認只要區分成 Fragment + ViewModel + Repository，就能夠在思考 Repository 實作的時候，專心解決 Data Layer 的問題。Fragment 跟 ViewModel 可以不管「資料怎麼來」
1. 先思考 Data Layer 的部分，細節都寫在 SPEC_DRAFT.md
1. 接著撰寫 PLANS，開始實作
