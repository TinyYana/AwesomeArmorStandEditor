# Changelog

本專案的詳細變更紀錄。格式大致遵循 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)。

## [Unreleased]

## [1.0.1] - 2026-07-11

### Fixed(修正)

- **`/aase load` 與 `/aase import` 放置後自動選取第一個元件。** 之前放完是「什麼都沒選」狀態,直接打 `/aase setequip` 會回「只有盔甲座能穿裝備」——場景裡明明就是盔甲座,訊息完全誤導(實測:2026-07-11 Yana 卡在這裡)。現在放完即可直接 setequip/flag/套範本;用工具點選其他元件的流程不變。
- **`/aase setequip` 副手是空的時,回覆從「已更新」改為「已清空」並附提示。** 副手空=清空該欄位是文件明載的行為(保留),但之前不管裝上還是清掉都回同一句「已更新」,拿主手拿著物品去執行的人會以為裝上了。現在:副手有物品→「已更新」;副手空且欄位原本有東西→「已清空+怎麼裝的提示」;兩邊都空→直接給用法提示。新增 lang key `equip.cleared`(兩份語言檔)。

## [1.0.0] - 2026-07-10

> **升級必讀:玩家可見文字換家了。** 舊的 `messages.yml` 與 `guide.yml` **不再被讀取**,內容全部搬進 `lang/<語言代碼>.yml`。啟動時如果偵測到舊檔,log 會提醒你——**它們不會被刪除也不會被覆寫**,把你改過的字搬進新檔之後再自己刪。沒改過任何文字的伺服器直接升上來就好,什麼都不用做。

插件現在會依伺服器所在地區選語言,附繁體中文與英文兩份。此前所有玩家可見文字只有繁體中文,英文圈的伺服器等於拿到一個看不懂的插件;而且有一部分文字(工具讀數列、`/aase info` 的狀態字、範本名、匯出資料包的 README)根本寫死在程式碼裡,連手動翻譯都做不到。這版把那些字全部外部化,補上英文,並把版本推到 1.0.0——功能面自 0.2.2 起沒有變化,是文字層與相容性的定版。

### Added(新增)

- **語言切換**。`config.yml` 新增 `language`,全服一種語言:
  - `auto`(預設)——看伺服器 JVM 的地區設定(`Locale.getDefault()`)。中文語系給 `zh_TW`,其餘給 `en`。**這是伺服器地區,不是玩家 client 的語言**;同一台伺服器上所有人看到同一種語言。
  - `zh_TW` / `en` ——直接指定。填了不認得的值會在 log 警告並退回 `auto`。
- **`lang/zh_TW.yml`、`lang/en.yml`**。兩份都會寫進 `plugins/AwesomeArmorStandEditor/lang/`(切語言前可以先把要用的那份改好),但只讀 `language` 選中的那份;已存在的檔案不會被覆寫。缺的 key 自動退回 jar 內的版本,所以翻譯到一半也不會出現 `<red>some.key`。要加語言就複製一份改名,再把 `language` 指過去。
- 遊戲內手冊(`/aase guide`)的頁面移進語言檔的 `guide.pages`,跟著語言走。
- 單元測試 `LangFilesTest`:兩份語言檔的 key 集合必須一致、`{placeholder}` 必須一一對應、每一條字串與每一頁手冊都要能被 MiniMessage 解析。少一個 key 或漏一個 `{count}` 就紅。

### Changed(變更)

- 以下原本寫死在程式碼裡的繁中文字改讀語言檔:工具讀數列的模式/部位/軸/步進與「未選取元件」、`/aase info` 的「盔甲座 / Display」「開 / 關」「有未儲存變更 / 已儲存」、`/aase export function` 產出的 `README.txt`。
- **內建範本的顯示名**改由語言檔的 `preset.name.<id>` 決定(`presets.yml` 的 `name` 仍是後備)。你用 `/aase pose save` 存的範本沒有對應 key,照樣顯示你取的名字。
- `plugin.yml` 的指令與權限說明改成英文。Bukkit 在插件載入前就讀這個檔,翻不了——與其只給繁中,不如給通用語。
- `messages.yml` / `guide.yml` → `lang/<代碼>.yml`(見上方升級提示)。

### Fixed(修正)

- 語言檔已存在時不再每次啟動都印一行 Bukkit 的 `Could not save ... because ... already exists` 警告。

## [0.2.2] - 2026-07-09

> **從 0.2.0 升上來的人請注意:本版含 0.2.1 的安全性修正。**
> 0.2.0 的領地檢查只蓋到「加單一元件」的路徑,`/aase load`、`/aase import`、工具 MOVE 模式、`/aase fx` **全部繞過領地插件**——任何玩家可以站進別人的領地把整組作品放下去,而且元件受保護、領主打不掉。根因是本插件用 `world.spawn()` 直接生成實體,不觸發任何原版放置事件,領地插件從頭到尾看不到我們的寫入。**如果你在生存服跑 0.2.0,請直接升上來。** 完整說明見 [CHANGELOG.md](CHANGELOG.md) 的 `[0.2.1]` 段落。

0.2.1 擋住了「別人在你的領地裡放新東西」,但**已經在裡面的**元件領主還是拿它沒轍——`EntityProtectionListener` 無條件擋下所有傷害,只有管理員清得掉。這版讓領主自己清。

### Added(新增)

- **`/aase clear <半徑>`**(權限 `aase.clear`,**預設所有人**)——清掉別人放在你有建築權之處的元件。
  - 判斷「誰能清什麼」用的是 RegionGuard 問領地插件的同一個問題:「這個玩家能不能在這個方塊蓋東西?」所以**領地內只有領主與受信任者清得掉**,無主土地上任何人都清得掉(那裡本來就沒有保護,跟方塊一樣)。
  - **絕不碰你自己的元件**——手滑一個 `/aase clear 32` 不該刪掉你擺了一小時的姿勢(自己的用編輯器刪)。
  - 沒有預覽/確認兩段式(不像 `/aase admin purge`):它只可能清掉**別人的**元件、而且只在**你控制的地**上,對方的存檔檔案原封不動,可以重新放置。
  - 每個方塊只發一次領地探針(同一格上疊很多元件是常態),寫一筆 LycoLib 稽核。
- 單元測試 `clearTakesOnlyOthersElementsOnGroundYouMayBuildOn`:規則的兩半各自變異驗證過(拿掉「不是自己的」或拿掉「有建築權」,測試都會紅)。

### Notes(備註)

- **元件仍然打不壞。** 這是刻意的:流彈、爬行者、隨手一拳都不該毀掉別人擺了一小時的作品。移除一律走指令。
- 為什麼不做成「有建築權就能徒手打壞」:那會讓你在自己領地內揮劍掃到自己的盔甲座就直接毀掉,代價比好處大。
- ⚠ **`/kill` 是例外,而且擋不住。** 原版 `ArmorStand` 對帶 `BYPASSES_INVULNERABILITY` 標籤的傷害(`/kill`、虛空)直接呼叫 `kill()`,不經過 `EntityDamageEvent`,所以 `EntityProtectionListener` 攔不到。管理員一句 `/kill @e[type=armor_stand]` 會清掉全服的作品(連同玩家手放的原版盔甲座)。實測確認(三段對照):同一發 `damage ... 1000 minecraft:explosion` 會炸掉沒有標記的盔甲座、卻打不掉本插件的元件(listener 生效);但 `kill` 對本插件的元件照樣移除。要清元件請用 `/aase clear` 或 `/aase admin remove|purge`,它們只碰本插件標記過的東西。

## [0.2.1] - 2026-07-09

領地檢查修正。0.2.0 有 `RegionGuard`,但只有「加一個元件」的路徑會叫它——放整個作品、匯入分享碼、把元件搬走這些路徑都繞過去了。在裝了 GriefPrevention / WorldGuard 的生存服上,任何玩家都能站進別人的領地 `/aase load` 把整組作品放下去,而且元件受 `EntityProtectionListener` 保護,領主打不掉、只能找管理員清。

根因是本插件用 `world.spawn()` 直接生成實體,不觸發任何原版放置事件,領地插件從頭到尾看不到我們的寫入,也就無從否決。`EventProbeGuard` 是我們**主動去問**「這個人能不能在這格蓋東西」,所以每一個生成或傳送元件的地方都必須自己呼叫它,漏一個就等於那條路徑沒有保護。

### Fixed(修正)

- `/aase load` — 補上領地檢查與數量上限(先前**兩者皆無**)。
- `/aase import <分享碼>` — 補上領地檢查(先前只檢查場景大小)。
- 工具 MOVE 模式 — 改用**搬移後**的座標做檢查,擋掉「在自己領地生成、再把元件推進鄰居家」。只在元件跨越方塊邊界時發探針,次方塊微調不觸發。
- `/aase fx` — 檢查點從玩家腳下改成 emitter 的**實際落點**(選取的元件可能離玩家很遠);數量上限改用實際 emitter 數,不再一律算 1。
- 放置整個場景時,探測範圍涵蓋元件、粒子發射器,以及**動畫關鍵影格的位移**——匯入的分享碼可以夾帶把元件甩進遠處領地的 keyframe。

### Added(新增)

- 單元測試 `scenePointsCoverEveryPlaceAnEntityCanLand`:確保 `ScenePoints.offsets()` 收齊原點、元件、發射器、關鍵影格四種落點。

### Notes(備註)

- `AaseScenePlaceEvent` 是本插件自己的 API 事件,沒有領地插件會監聽它。它是給第三方擴充用的掛勾,**不提供領地保護**;先前 `load` / `import` 看起來「有防護」是誤會。
- 未變更:`EntityProtectionListener` 仍會擋下所有對本插件元件的傷害。已經放進別人領地的舊元件,領主仍需要管理員用 `/aase admin remove|purge` 清除。

## [0.2.0] - 2026-07-09

管理員終於有辦法處理「玩家把盔甲座放在奇怪的地方」了。0.1.0 的 `aase.admin` 權限描述雖然寫著 purge,但實際上**從未實作**——當時唯一手段是 `/aase edit`(管理員可繞過擁有權檢查)再 `/aase delete`,一次只能刪一個元件,也查不出是誰放的。

### Added(新增)

- **管理員強制移除工具** `/aase admin`(權限 `aase.admin`,預設 OP):
  - `whois` — 查最近元件的**擁有者 / 作品 / 元件編號 / 座標**;是粒子發射器會標註。
  - `remove` — 移除最近的那一個元件,不必開編輯 session。
  - `purge <半徑> [玩家]` — **只預覽**半徑內會被移除的元件數(可只算某位玩家的)。
  - `confirm` — 60 秒內確認,才真的執行上一次預覽的清除。
- **設定**:`admin.max-purge-radius`(預設 `64`)夾住 `purge` 的半徑上限。
- **稽核**:每次 `remove` / `purge` 都寫一筆 LycoLib AuditLog(誰、清了誰的、幾個、座標);未安裝 LycoLib 時安靜略過。
- **單元測試** `AdminLogicTest`:purge 參數解析(半徑夾限、**未給半徑 / `0` / 負數 / 非整數一律拒絕執行**、空白玩家名視為不篩選)與確認 token 的 TTL 邊界。

### 刻意的邊界(設計決策,不是限制清單)

- **只碰本插件放置的元件**(PDC 標記)。玩家手放的原版盔甲座、其他插件生成的實體一律不動——否則這個插件就變成 entity killer。`whois` 找不到時會明講。
- **只碰已載入區塊**。本插件不做世界/區塊掃描(效能紅線),`purge` 使用 bounded 的 `getNearbyEntities` 再以球形距離過濾,因此到不了沒人載入的區塊;訊息會提醒管理員這一點。
- **不刪玩家的存檔**。`purge` 只清除世界上的實體,玩家的作品檔案保留,可重新放置到合理位置——這是能解決問題的最小破壞。
- **半徑清除為兩段式**(預覽 → `confirm`),因為批次刪除他人作品不可逆。
- 移除元件時會從所有開啟中的編輯 session 摘除該實體參照,避免別的玩家正在編輯時留下失效參照。

### Changed(變更)

- `plugin.yml` 的 `aase.admin` 描述修正:原本宣稱包含 `purge`,但 0.1.0 並未實作;現在描述與實作一致(編輯他人作品、查擁有者、強制移除、reload)。
- Tab 補全對沒有 `aase.admin` 的玩家**隱藏 `admin` 與 `reload`** 子指令。
- 玩家名 → UUID 的解析只查線上玩家與伺服器已知玩家快取,**不使用會阻塞主執行緒的 `Bukkit.getOfflinePlayer(String)`**。

## [0.1.0] - 2026-07-09

第一個可交付版本:P1 靜態編輯器 + P2 粒子 + P3 關鍵影格動畫/mcfunction 匯出 + P4 分享碼/匯入與對外事件 API,皆已實作並通過 L3(Paper 26.2 `runServer` enable 零 Exception)驗證。

### Added(新增)

- **核心編輯器**:盔甲座 + Display 實體(item / block / text)統一編輯——姿勢、變換、縮放、旋轉、裝備、旗標;混合式操作(GUI 控制面板 + 手持工具在世界中直接微調 + actionbar 即時讀數)。(`9d60717`)
- **資料模型與持久化**:Gson 手寫 `SceneCodec`(顯式處理多型 `type` 判別欄位,不用反射對映)、`Pose6`/`TransformOps` 純邏輯附單元測試;場景存成可讀可攜的 JSON 藍圖。(`9d60717`)
- **反格里芬與安全**:元件擁有權 PDC 標記、每人/每區塊/全域數量上限(記憶體計數,不掃世界)、`EventProbeGuard` 事件探針領地尊重(自動相容 GriefPrevention / WorldGuard / Towny / Lands)。(`9d60717`)
- **範本庫**:資料驅動 `presets.yml` + 圖形 GUI——一鍵套用姿勢(立正/T字/萬歲/揮手/指向/沉思/坐姿/跑步)、一鍵鏡像左右對稱、一鍵加特效範本(火焰光環/愛心/櫻花/星塵/靈魂之焰),並可 `/aase pose save` 存成自己的範本重用。(`9d60717`)
- **粒子特效(P2)**:`ParticleService` 用隱形 marker 實體掛發射器,只在附近有玩家 + 已載入區塊時發射,每 tick 全域預算限流。(`9d60717`)
- **關鍵影格動畫(P3)**:時間軸 + 關鍵影格,`AnimationPlayer` 即時預覽播放(Display 走客戶端插值、盔甲座逐 tick,離線自動停止)。(`9d60717`)
- **匯出(P1/P3)**:`SummonExporter` 匯出可點擊複製的 `/summon` 指令;`McFunctionExporter` 匯出含動畫驅動(`load`/`tick`/`frames/*`)的 mcfunction 資料包。(`9d60717`)
- **分享碼與匯入(P4)**:`store/ShareCode`(`AASE1:` + Base64url(gzip(JSON)),對長度/解壓有上限防護,壞碼回 `null` 不丟例外)+ `/aase share`(可點擊複製)/ `/aase import <碼> [名稱]`(重設 owner + 新 id,受每人數量上限守門)。(`2e729ca`)
- **對外事件 API(P4)**:`api/AaseSceneSaveEvent`(通知,玩家存檔後觸發)、`api/AaseScenePlaceEvent`(可取消,load/import 放置前觸發)——讓其他插件掛我們的事件,零反向依賴。(`2e729ca`)
- **裝備選單 GUI**:`menu/EquipmentMenu`,手持物品點格子=裝上、空手點=卸下,只複製游標物品、玩家物品零消耗零複製。(`2e729ca`)
- **`/aase info`**:場景資訊摘要(元件數/盔甲座/Display/發射器/動畫/選取/存檔狀態)。(`2e729ca`)
- **遊戲內翻頁手冊**:`/aase guide` 開啟可翻頁的書(`guide.yml` 資料驅動,深色配紙底色);控制面板右上角加📖鍵;`/aase help` 對玩家改開書、對 console 留指令清單。(`a8519f0`)
- **完整使用說明手冊**:`docs/MANUAL.md`,20 章涵蓋概念、上手、工具、GUI、範本、盔甲座、Display、粒子、動畫、存讀分享、匯出、指令、權限、設定、FAQ、效能與路線圖。(`a8519f0`)
- **中英雙語文件**:README.md 加上完整英文版(語言切換連結);新增 `docs/MANUAL.en.md`(逐章對照 `MANUAL.md` 的完整英文手冊);四張 SVG 圖解(五分鐘上手流程、編輯工具速查、場景/元件/放置三概念、控制面板配置)嵌入 README 與兩份手冊;手冊同步發布到 GitHub Wiki(Home / Manual-EN / Manual-ZH-TW)。(`dc43b18`)

### Changed(變更 / 效能)

- **權限姿態收斂**:凡是**寫入伺服器檔案或改動全服共用資料**的動作預設收回一般玩家權限——`aase.export.command`(匯出寫 `plugins/`)、新增 `aase.preset.save`(`/aase pose save` 改寫共用 `presets.yml`)皆改為 `default: op`;控制面板的匯出/存範本鍵同步在 GUI 邊界補上權限檢查並隱藏,修補了原本能繞過指令權限直接點按鈕匯出的漏洞。(`2e729ca`)
- **粒子效能**:`ParticleService` 不再每 tick 解析 PDC 字串——生成/索引時解一次並預先快取解好的 `Particle` 列舉;marker 列表為空時 ticker 直接早退。(`2e729ca`)

### Fixed(修正)

- **裝備選單卡住游標**:原本無條件 `setCancelled` 選單內所有點擊(含玩家自己背包),導致「先把物品拿到游標」這一步本身就被攔下、整個裝備流程失效。修正為只攔會把物品塞進選單格子的動作(點擊選單格、shift 移動、雙擊聚集、觸及選單的拖曳),玩家自己背包內的正常拿取/放置不受影響。(`62118f3`)
- **匯出座標誤帶 NBT float 後綴**:命令方塊實測回報「應有 3 個座標」的解析錯誤——座標被誤寫成 `~0f ~0f ~0f`,但 `f` 後綴只該用在姿勢/變換等 NBT float 欄位,座標本身不能帶。`SummonExporter` 拆出 `coord()`(座標,不加 `f`)與 `f()`(NBT float,加 `f`)兩個獨立輸出函式,並新增鎖定格式的單元測試。(`e43e3f3`)
- **文字元件匯出成字面 JSON**:對 Paper 26.2 用 RCON 實測發現 `CustomName:'{"text":"X"}'` 會被當成字面字串(實體名變成 `{"text":"X"}` 本身),因為 26.2 的文字元件走 SNBT,純文字要直接寫 `"X"`。`SummonExporter` 與 `McFunctionExporter` 的 `CustomName` / `text` 皆改輸出 SNBT 字串,並新增單元測試鎖定格式。(`d47b768`)

### CI / Tooling

- 新增 GitHub Actions 建置工作流程(`.github/workflows/build.yml`):每次 push/PR 自動 `./gradlew build`(編譯 + 單元測試 + shaded jar)並上傳 jar 產物;推送 `v*` tag 時額外建立 GitHub Release,附上打包好的 jar 與(從本檔案擷取的)該版本變更說明。
- 修正 `gradlew` 在 git 索引中遺失可執行位元(Windows 端 commit 不保留 POSIX exec bit)導致 CI 第一次跑 `Process completed with exit code 126` 失敗的問題;同時在 workflow 加一道防呆 `chmod +x gradlew`。

### Legal(授權)

- 專案授權定案為 **GNU AGPL-3.0**(`LICENSE`):商用伺服器可自由使用;若修改後的 jar 或原始碼交到任何人手上(含把修改版跑成讓其他人連線互動的網路服務),必須以同一授權公開原始碼。單純私下營運不觸發此義務。README 中英文版皆加上授權說明章節。

[Unreleased]: https://github.com/TinyYana/AwesomeArmorStandEditor/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/TinyYana/AwesomeArmorStandEditor/releases/tag/v0.1.0
