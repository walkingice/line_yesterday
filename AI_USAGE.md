# AI Usages

紀錄使用 AI Agent 的過程

使用 codex cli + 20 鎂方案

## Principle

秉持客家精神，盡可能減少 token 的使用。前期思考與討論使用 `gpt-5.6-sol + High Reasoning`，實作時使用 `gpt-5.6-terra + Medium/Low Reasoning`。

## Steps

1. 首先思考整個專案需要的東西與大概的架構，然後寫出 [SPEC_DRAFT](docs/SPEC_DRAFT.md)
1. 請 Agent 思考並與我討論，然後根據我的要求寫出 [SPEC_REVISED](docs/SPEC_REVISED.md)，以及 [SPEC](SPEC.md)
    - 前者是中文版，讓我可以閱讀。後者是英文版，實作階段給 Agent 閱讀。
    - 留下所有檔案，供面試官參考
1. 根據我希望的實作順序， 寫出 [PLANS_DRAFT](docs/PLANS_DRAFT.md)
1. 請 Agent 思考並與我討論，寫出[PLANS_REVISED](docs/PLANS_REVISED.md)，以及 [PLANS](PLANS.md)
    - 兩個檔案的理由同上
1. 要求 Agent 根據 SPEC.md 與 PLANS.md，漸進式地開發
    - 每完成一個小目標，就要求 Agent 更新 PLANS 的 checkbox
    - clear session，再要求 Agent 進行下一個 step
1. 每完成一個 Phase，就會手動編譯並做 smoke test
1. 全部完成後，再繼續修改更新，直到 App 符合我的需求

## Correction

- 在 Phase 1 完成後，要求改用 ViewBinding
    - 拿掉 `_binding`，改用 `lifecycle-aware viewBinding delegate`

- 在 Phase 2.1
    - 要求補上 permission for ConnectivityNetworkStatusProvider
    - 移除太細節的 DelayProvider

- 修改 FavoritesFragment 的顏色
    - git commit: ee7f0a84ad61edf1687635b9084fa6fbf12d71a9
    - 要求 Agent 修改之後，發現 Agent 開始用程式邏輯的方式去修改顏色，眉頭一皺覺得不太對勁，中斷 Agent 的動作

        > 等等，現在這樣修改顏色的方法正確嗎？一般來說不是為這個 Fragment 改 theme 之類的方法就好

    - 送出上面的 prompt 之後，Agent 就回神了

        > 你的判斷是對的。剛才的方式能生效，但屬於在 Fragment 生命週期中直接修改 Activity 的 ActionBar；這個 bar 實際上由 FavoritesActivity 擁有，較合適的做法是為該 Activity 指定專屬 theme，以 colorPrimary 設桃紅色。接下來我會移除程式化設定，改用 FavoritesActivity 的 theme 與測試驗證 theme 色彩。

