# Changelog

本專案的詳細變更紀錄。格式大致遵循 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)。

## [Unreleased]

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

- **只碰本插件放置的元件**(PDC 標記)。玩家手放的原版盔甲座、其他外掛生成的實體一律不動——否則這個外掛就變成 entity killer。`whois` 找不到時會明講。
- **只碰已載入區塊**。本外掛不做世界/區塊掃描(效能紅線),`purge` 使用 bounded 的 `getNearbyEntities` 再以球形距離過濾,因此到不了沒人載入的區塊;訊息會提醒管理員這一點。
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
- **對外事件 API(P4)**:`api/AaseSceneSaveEvent`(通知,玩家存檔後觸發)、`api/AaseScenePlaceEvent`(可取消,load/import 放置前觸發)——讓其他外掛掛我們的事件,零反向依賴。(`2e729ca`)
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
