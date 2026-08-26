# Decisions

- Mock Client vs Real Client
    - 我不希望因為網路問題，使得 App 無法使用，所以一開始就預計要利用 LICH，注入 mock 或 real 的 client。
    - 由於已經準備好 fixture 的 json 檔，開發期也可以要求 Agent 閱讀 json 檔來撰寫測試

- Repository + Client + Db
    - 雖然可以用 paging 3，但是現有的設計我更加熟悉也更容易解釋，因此暫時沒用 paging 3

- JsonRespDb
    - 由於有三種 JSON 來源：DummyJson, SpaceFlight, Details. 最簡單粗暴的方式就是直接把 JSON String 塞進去。這樣只要做一份就可以一直用
    - 如果是 production，我會認命地切好幾個 table

- FreshnessValidator
    - 這個類別是用來輔助判斷資料是否仍足夠新鮮
    - 但是「誰來用它」是個問題。
        - 讓 ViewModel 來用感覺比較合理，因為這樣 business logic 就集中在 ViewModel。
        - 但是 Repository 有可能會讀取到已經過期的 json string cache，如果把 cache 轉成 GSON 又轉成 domain data class 之後，ViewModel 才抱怨資料已經腐敗，這會造成浪費。
        - 目前的做法是讓 Repository 來使用 FreshnessValidator，從 DB 剛拿出 JSON string 就馬上檢查當初存入的時間。
    - 更好的設計會是向 Paging 3 的 mediator，由於時間不足，忍痛放棄設計得更複雜。

