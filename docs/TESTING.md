# AwesomeArmorStandEditor — 測試指南

## 建置

```powershell
./gradlew build          # 產生 build/libs/AwesomeArmorStandEditor-<版本>.jar(已 shade Adventure)
./gradlew test           # 純邏輯單元測試(model/codec/pose/transform)
./gradlew runServer      # 起一個 Paper 測試伺服器(run/ 目錄)
```

Spigot 相容性:編譯只用 Bukkit/Spigot API 面,Adventure 以 shade+relocate 內嵌,執行期不依賴 Paper。
要在真 Spigot 驗證,把 jar 丟進 Spigot server 的 `plugins/`,確認 enable 無錯、指令與 GUI 正常。

## L1 已驗證(自動)

- `./gradlew build` 通過:編譯 + shadowJar + 單元測試。
- 單元測試 `EditorLogicTest`:場景 JSON round-trip、姿勢角度 wrap、四元數/縮放/平移運算。
- 打包 jar 內 Adventure 已 relocate 到 `com/tinyyana/awesomeArmorStandEditor/libs/kyori`,原 `net/kyori` 無殘留。

## L2–L4 手動測試

前置:`./gradlew runServer` 起服,或把 jar 丟進既有 Paper/Spigot 服。

### 管理員視角

1. `/aase reload` — 應回「設定已重載」,無報錯。
2. 權限:預設 `aase.use`/`aase.create.*` 給所有人;`aase.admin`/`aase.bypass.*` 給 OP。用非 OP 帳號確認 bypass 無效、限制生效。
3. 數量上限:把 `config.yml` `limits.per-player` 調小(如 3)→ reload → 連續 `/aase addstand` 超過即被擋(「數量已達上限」)。
4. 領地:在 GriefPrevention/WorldGuard 別人的領地內 `/aase addstand` 應被擋(「這裡受保護」);`region.event-probe: false` 則不擋。
5. 效能:反覆放置/編輯時開 Spark 或 `/tps`,確認無掉 TPS(不應有 chunk 掃描)。

### 玩家視角(零基礎最快路徑 — 範本庫)

給不會擺姿勢的人:
1. `/aase new 測試` → `/aase addstand`(生一個盔甲座)→ 右鍵點它選取。
2. `/aase presets`(或 `/aase` 面板點「範本庫」)→ 開範本庫 GUI。
3. 點上排任一姿勢(立正/T字/萬歲/揮手/指向/沉思/坐姿/跑步)→ 盔甲座立刻變那個姿勢。
4. 點「鏡像」讓左右對稱;點下排特效(火焰光環/愛心/櫻花/星塵/靈魂之焰)直接加粒子。
5. 擺好一個滿意的姿勢 → `/aase pose save myPose 我的姿勢` → 以後 `/aase pose myPose` 一鍵重用。
6. `/aase save` 存檔。**全程不需要懂任何角度數字。**

### 玩家視角(核心流程 — 進階手動微調)

1. `/aase new 測試` → 「已建立場景」。
2. `/aase tool` → 拿到「盔甲座編輯工具」。
3. `/aase addstand` → 腳下出現盔甲座,actionbar 顯示 `盔甲座#1 | 姿勢頭 | 軸 Y | 步進 …`。
4. 拿工具:**右鍵點盔甲座**選取 → **左/右鍵**微調角度 → **滾輪**換步進 → **潛行+滾輪**換軸 → **潛行+左鍵**換模式(姿勢/移動)→ **潛行+右鍵**換部位。盔甲座姿勢應即時改變。
5. `/aase` 開控制面板:點模式/部位/軸/微調/旗標(迷你、隱形、無底板、手臂…)按鈕,盔甲座應即時反映。
6. Display:`/aase adddisplay block` → 出現方塊 Display;切到 SCALE/ROTATE 模式微調,應即時縮放/旋轉。`/aase adddisplay item`(副手拿物品先)、`/aase adddisplay text` + `/aase settext <內容>`。
7. 裝備:副手拿頭盔 → `/aase setequip head` → 盔甲座戴上。
8. `/aase save` → 「已儲存(共 N 個元件)」;檢查 `plugins/AwesomeArmorStandEditor/scenes/<uuid>/<id>.json`。
9. `/aase close` → 作品保留在世界。走遠再回來 `/aase edit`(站在作品旁)→ 重新綁定既有作品繼續編輯(不應產生分身)。
10. `/aase list` → 列出場景;`/aase load 測試` → 在腳下放一份新的。
11. `/aase export command` → 聊天出現可點擊「複製 summon 指令」,同時存成 `exports/測試.txt`;把指令貼到遊戲執行,應重現作品(NBT 為最佳努力,若版本格式有變請回報)。
12. 反格里芬:換別的玩家 → 嘗試打/互動你的元件應無效(受保護);別人不能用工具選取你的元件。

### 玩家視角(粒子 P2)

13. `/aase particle add FLAME`(tab 補全看清單)→ 你腳下位置持續冒火焰粒子;`/aase particle add HEART`、`CHERRY_LEAVES`、`DUST` 等。
14. 走遠超過 `particles.render-range`(預設 32 格)→ 粒子停;走近再冒(不掃世界、只在附近玩家時發射)。
15. `/aase save` → 粒子發射器一起存進 JSON;`/aase particle clear` 清除。

### 玩家視角(關鍵影格動畫 P3)

16. 選取一個元件 → 擺姿勢 → `/aase anim key 0`(在 tick 0 記錄)→ 換個姿勢 → `/aase anim key 20` → `/aase anim length 20` → `/aase anim play`:元件應在兩個姿勢間平滑來回(Display 走客戶端插值、盔甲座逐 tick)。
17. `/aase anim loop` 切換循環;`/aase anim stop` 停止(元件回存檔姿勢);`/aase anim clear` 清空動畫。

### 玩家視角(mcfunction 匯出 P3)

18. `/aase export function` → 匯出資料包到 `exports/<場景>/datapack/`(含 `pack.mcmeta`、`summon.mcfunction`、有動畫則含 `load/tick/frames/*`)。把資料夾丟進世界 `datapacks/`,`/reload`,`/function aase:summon`(有動畫再 `/function aase:load` 然後 `/function aase:tick`)。**最佳努力**:pack_format/function 資料夾名依版本可能要調。

### 驗收層級標記

回報測試時標明做到哪層:L1 build / L2 deployed(enable 無錯)/ L3 runtime(指令有回應)/ L4 玩家端(姿勢/存讀/匯出真的可見可用)。

## 已實作範圍

- **P1** 靜態編輯器、**P2** 粒子、**P3** 關鍵影格動畫 + mcfunction 匯出 都已實作(見上方測試步驟)。
- **P4**(分享碼/匯入、對外 API 事件)未做;分享目前靠複製 `scenes/<owner>/<id>.json` 檔。

## 已知限制

- summon / mcfunction 匯出為最佳努力:裝備只帶物品 id(不含附魔/自訂資料);方塊只帶方塊名(不含 blockstate 屬性);自訂名轉純文字。26.2 NBT 格式若有變,`SummonExporter` / `McFunctionExporter` 是集中修改點。
- 動畫即時播放僅在編輯 session 內(播放中盔甲座逐 tick 有成本,故不常駐);要常駐播放請用匯出的 datapack。
- 粒子發射器位置目前是玩家加入時的站位(offset),尚無視覺化搬移(可刪除重加);編輯只有 add/clear。
- 裝備編輯用 `/aase setequip <欄位>`(副手物品),尚無拖放式裝備 GUI。
- 數量上限以記憶體計數,未載入區塊的既有元件不計入(不做世界掃描的取捨)。
