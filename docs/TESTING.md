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
- 單元測試 `EditorLogicTest`:場景 JSON round-trip、姿勢角度 wrap、四元數/縮放/平移運算、**分享碼 round-trip + 惡意/損毀輸入回 null 不丟例外**。
- 打包 jar 內 Adventure 已 relocate 到 `com/tinyyana/awesomeArmorStandEditor/libs/kyori`,原 `net/kyori` 無殘留。

## L3 已驗證(本輪,2026-07-08)

- Paper 26.2 `runServer` enable 成功:`AwesomeArmorStandEditor enabled` + `Done (25s)`,**零 Exception**;編輯過的 `plugin.yml`(新權限 `aase.preset.save`、`aase.export.command`/`preset.save` 改 `default: op`)正常解析。測試服已關、port 25565 已釋放。
- 玩家端行為(裝備選單、分享/匯入、資訊、匯出/範本存檔的權限擋人)為 L4,需真人;步驟見下。

## L2–L4 手動測試

前置:`./gradlew runServer` 起服,或把 jar 丟進既有 Paper/Spigot 服。

### 管理員視角

1. `/aase reload` — 應回「設定已重載」,無報錯。
2. 權限:預設 `aase.use`/`aase.create.*` 給所有人;`aase.admin`/`aase.bypass.*` 給 OP。用非 OP 帳號確認 bypass 無效、限制生效。
3. 數量上限:把 `config.yml` `limits.per-player` 調小(如 3)→ reload → 連續 `/aase addstand` 超過即被擋(「數量已達上限」)。
4. 領地(需要 GriefPrevention/WorldGuard,且測試帳號**不是 OP**——OP 有 `aase.bypass.region`)。
   準備:玩家 A 用金鏟圈一塊領地;玩家 B 站進 A 的領地內。以 B 的身分逐條測,每條都該回「這裡受保護」且**地上不留任何東西**:
   1. `/aase addstand`
   2. `/aase load <B 自己存的作品>` — 存檔是 B 的,但落點在 A 的地
   3. `/aase import <B 自己產的分享碼>`
   4. `/aase fx <任一特效>`(先在領地外開 session、選一個元件,再走進來)
   5. `/aase particle add FLAME`

   跨界搬運:B 在領地**外**放一個盔甲座 → 拿工具切 MOVE 模式 → 朝 A 的領地方向推。
   元件應停在領地邊界前一格,越界的那一下被擋(「這裡受保護」),元件不會進去。

   動畫夾帶:在領地外做一個帶動畫的作品,把某個 keyframe 的位置拉進 A 的領地 → 存檔 → `/aase load`。
   應在放置階段就被擋掉,而不是等播放時才把元件甩進去。

   最後把 `region.event-probe: false` → `/aase reload` → 上面每一條都應該放行(確認開關真的有效)。

5. 效能:反覆放置/編輯時開 Spark 或 `/tps`,確認無掉 TPS(不應有 chunk 掃描)。
   MOVE 模式連續微調時,只有跨越方塊邊界的那一下會發探針事件;若裝了 CoreProtect,確認不會被洗版。

### 玩家視角(遊戲內手冊)

0. `/aase guide`(或 `/aase help`、或面板右上角📖)→ 打開一本可翻頁的書,10 頁涵蓋:最快上手、開始、範本庫、工具微調、控制面板、裝備外觀、粒子、動畫、存檔匯出、小提醒。翻頁確認文字在紙底色上看得清楚(深色)。完整版看 `docs/MANUAL.md`。

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
7. 裝備(選單):選取盔甲座 → `/aase equip`(或面板「裝備」鍵)→ 開 27 格裝備選單。**把物品拿到游標上(點一下背包物品)→ 點頭盔/胸甲/…格子**,盔甲座立刻穿上;**空手(游標空)點格子=卸下**。全程你的物品不會被消耗或複製(游標物品保留)。也可 `/aase setequip head`(副手物品)舊路徑。
8. `/aase info` → 聊天顯示場景資訊(元件數/盔甲座/Display/發射器/動畫/目前選取/存檔狀態)。
9. `/aase save` → 「已儲存(共 N 個元件)」;檢查 `plugins/AwesomeArmorStandEditor/scenes/<uuid>/<id>.json`。
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

### 玩家視角(分享碼 P4)

19. 有作品的玩家 `/aase share` → 聊天出現可點擊「複製分享碼」,複製到剪貼簿(一串 `AASE1:...`);沒有元件時回「還沒有任何元件」。
20. 另一個玩家(或自己換地方)`/aase import <貼上的碼> [新名稱]` → 在腳下放置同一份作品,回「已匯入並放置 …」;匯入的作品 owner 變成匯入者、是新的 id(不影響原作者存檔)。
21. 亂碼防護:`/aase import 隨便亂打` → 回「分享碼無效或已損毀」,不應有紅字例外。
22. 上限防護:把 `limits.per-player` 調小 → 匯入元件數超過上限的碼 → 被擋(「元件太多」)。

### 權限分界(本輪新增,管理員視角)

23. 用**非 OP** 帳號:`/aase export command` / `/aase export function` / `/aase pose save x` 應回「你沒有權限」(這三個會寫伺服器檔案 / 改全服 `presets.yml`,預設 `op`)。
24. 非 OP 開 `/aase` 控制面板:**匯出鍵應不顯示**;就算點到該格也不會匯出(GUI 有二次權限檢查,不能繞過指令權限)。
25. 給該帳號 `aase.export.command` / `aase.preset.save`(LuckPerms)後,以上恢復可用。

### 管理員強制移除工具(0.2.0 新增,管理員視角)

> 需要 `aase.admin`。這組指令**只作用於用 `/aase` 放置的元件**、**只碰已載入區塊**、**不刪玩家的存檔**。

26. 請另一個玩家(或用第二個帳號)`/aase new x` → `/aase addstand` 放一個盔甲座。用管理員站到旁邊 `/aase admin whois` → 應顯示**擁有者名稱 / 作品名 / 元件編號 / 世界+座標**。
27. **邊界(必測)**:用手放一個**原版盔甲座**(拿盔甲座物品右鍵地面,不經 `/aase`),站旁邊 `/aase admin whois` → 應回「附近沒有本插件放置的元件」。**絕不能把它當成可移除目標。**
28. `/aase admin remove` → 最近的那一個元件消失;再 `/aase admin whois` 應找不到。
29. 粒子發射器也算元件:`/aase particle add FLAME` 後對著它 `/aase admin whois` → 位置行尾應標「(粒子發射器)」;`remove` 可清掉,粒子隨之停止。
30. **兩段式清除**:`/aase admin purge 16` → 只印「半徑 16 格內有 N 個元件即將被移除」+ 確認提示,**世界上的東西一個都不能少**。接著 `/aase admin confirm` → 才真的清掉並回報數量。
31. 只清某人的:`/aase admin purge 16 <玩家名>` → 預覽行應標「(只算 <玩家名> 的)」;confirm 後**別人的元件要留著**。
32. 找不到玩家:`/aase admin purge 16 不存在的名字` → 回「找不到玩家」,不進入待確認狀態。
33. **參數防呆**:`/aase admin purge`(不給半徑)、`purge 0`、`purge -5`、`purge abc` → 一律印用法,**什麼都不清、也不建立待確認**。
34. **確認防呆**:沒先 purge 就 `/aase admin confirm` → 回「沒有待確認的清除」。
35. **逾時**:`/aase admin purge 16` 後等 **超過 60 秒** 再 `confirm` → 回「確認已逾時」,不執行。
36. **半徑夾限**:`/aase admin purge 9999` → 實際只會用 `config.yml` 的 `admin.max-purge-radius`(預設 64)。
37. **session 清理**:讓某玩家正在 `/aase edit` 他的作品時,管理員 `remove` 掉其中一個元件 → 該玩家繼續操作不應噴例外(元件已從他的 session 移除)。
38. **權限與 tab**:一般玩家 `/aase admin whois` → 「你沒有權限」;且輸入 `/aase ` 按 tab **看不到 `admin` / `reload`**。
39. **稽核**:若伺服器有 LycoLib,`remove` / `purge` 應各寫一筆稽核紀錄(誰、清了誰的、幾個、座標)。沒有 LycoLib 時應安靜略過、不報錯。
40. **未載入區塊**:把元件放在很遠的地方讓區塊卸載,回到出生點 `purge 64` → 那些元件**不會**被清掉(刻意:不掃描世界)。訊息會提醒「只會影響已載入區塊內的元件」。

### 驗收層級標記

回報測試時標明做到哪層:L1 build / L2 deployed(enable 無錯)/ L3 runtime(指令有回應)/ L4 玩家端(姿勢/存讀/匯出真的可見可用)。

## 已實作範圍

- **P1** 靜態編輯器、**P2** 粒子、**P3** 關鍵影格動畫 + mcfunction 匯出、**P4** 分享碼/匯入 + 對外事件 API 都已實作(見上方測試步驟)。
- 另有:裝備選單 GUI、`/aase info`、寫檔/共用資料的權限分界。
- **0.2.0**:管理員強制移除工具 `/aase admin whois|remove|purge|confirm`(兩段式確認 + 稽核紀錄)。
- 仍待做:粒子/動畫的**視覺化編輯面板 / 時間軸 GUI**(目前走指令 + 範本庫)。

## 已知限制

- summon / mcfunction 匯出:**NBT 格式已對 Paper 26.2 用 RCON `/summon` 實測**(Pose/旗標/裝備 ArmorItems+HandItems/transformation/item/block/brightness/glow 皆正確;自訂名與文字用 **SNBT** `CustomName:"..."` / `text:"..."`,不是舊的 JSON 字串)。仍為最佳努力:裝備只帶物品 id(不含附魔/自訂資料);方塊只帶方塊名(不含 blockstate 屬性);名稱/文字轉純文字(顏色不保留)。若未來版本再變,`SummonExporter` / `McFunctionExporter` 是集中修改點。
- 動畫即時播放僅在編輯 session 內(播放中盔甲座逐 tick 有成本,故不常駐);要常駐播放請用匯出的 datapack。
- 粒子發射器位置目前是玩家加入時的站位(offset),尚無視覺化搬移(可刪除重加);編輯只有 add/clear。**效能**:每 tick 迴圈不再解析 PDC(生成/索引時解一次並快取解好的 `Particle`),marker 空時 ticker 早退。
- 裝備 GUI 用「手持物品點格子」而非真拖放(刻意:全程 cancel 事件、只複製游標物品,確保玩家物品零消耗/零複製);`/aase setequip`(副手)舊路徑仍在。
- 數量上限以記憶體計數,未載入區塊的既有元件不計入(不做世界掃描的取捨)。
