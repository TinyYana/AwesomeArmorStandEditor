# AwesomeArmorStandEditor

一個為**生存與創造伺服器**打造的盔甲座 / Display 場景編輯器,主打好上手的 UI/UX、生存服安全、尊重領地,並可把作品匯出成指令。

> 狀態:P1 靜態編輯器 + P2 粒子 + P3 關鍵影格動畫/mcfunction 匯出 已實作;分享碼/對外 API(P4)後續。見 `docs/DESIGN.md`。

## 特色

- **範本庫(零基礎友善)**:不會擺姿勢?`/aase presets` 開圖形範本庫,點「立正/揮手/萬歲/坐姿…」一鍵套用;「鏡像」一鍵左右對稱;特效範本點一下就加粒子;擺好的姿勢可 `/aase pose save` 存成自己的範本重用。
- **盔甲座 + Display 實體**(item / block / text display)統一編輯 —— 姿勢、變換、縮放、旋轉、裝備、旗標。
- **混合式操作**:控制面板 GUI + 手持工具在世界中直接微調,actionbar 即時讀數。
- **粒子特效**:元件場景可掛粒子發射器,只在附近有玩家 + 已載入區塊時發射,有每 tick 全域預算。
- **關鍵影格動畫**:時間軸 + 關鍵影格,即時預覽播放(Display 走客戶端插值,盔甲座逐 tick)。
- **存檔 / 分享 / 匯出**:每個場景是可攜的 JSON 藍圖,可重複放置;匯出 `/summon` 指令(一鍵複製)或 **mcfunction 資料包**(含動畫驅動)。
- **生存服安全**:元件擁有權標記(反格里芬)、每人/每區塊/全域數量上限、尊重領地保護。
- **零硬依賴、跨平台**:在 Spigot 與 Paper 都能跑;不需要安裝任何其他外掛。

## 相容性

- Minecraft / Paper / Spigot **26.2**,Java 25。
- 只使用 Bukkit/Spigot API 面;文字用內嵌(shade+relocate)的 Adventure + MiniMessage,在 Spigot 上也一致運作。
- 領地整合(GriefPrevention / WorldGuard / Towny / Lands…)透過事件探針自動生效,**不需硬依賴**。

## 安裝

把 `AwesomeArmorStandEditor-<版本>.jar` 放進伺服器的 `plugins/`,重啟即可。無其他相依。

## 指令

| 指令 | 說明 |
|---|---|
| `/aase` | 開啟控制面板 |
| `/aase tool` | 取得編輯工具 |
| `/aase presets` | 開範本庫(姿勢/特效一鍵套用) |
| `/aase pose <id>` · `/aase pose save <id> [名稱]` · `/aase mirror` · `/aase fx <id>` | 套用/儲存姿勢、鏡像對稱、加特效 |
| `/aase new <名稱>` | 開始新場景 |
| `/aase addstand` · `/aase adddisplay <item\|block\|text>` | 新增元件 |
| `/aase setblock/settext/setitem/setname/setequip/flag …` | 編輯內容、名稱、裝備、旗標 |
| `/aase particle add <類型>` · `/aase particle clear` | 掛 / 清 粒子發射器 |
| `/aase anim key <tick>` · `length` · `loop` · `play` · `stop` · `clear` | 關鍵影格動畫 |
| `/aase save` · `/aase load <名稱>` · `/aase list` | 存 / 讀 / 清單 |
| `/aase edit` | 綁定並編輯附近既有作品(不產生分身) |
| `/aase export command` · `/aase export function` | 匯出 summon 指令 / mcfunction 資料包 |
| `/aase reload` | 重載設定(管理) |

**工具操作**:右鍵點元件選取 · 左/右鍵微調 · 滾輪換步進 · 潛行+滾輪換軸 · 潛行+左鍵換模式 · 潛行+右鍵換部位。

## 權限

`aase.use`、`aase.create.armorstand`、`aase.create.display`、`aase.scene.save`、`aase.scene.share`、`aase.export.command`、`aase.animate`、`aase.admin`、`aase.bypass.region`、`aase.bypass.limit`。詳見 `plugin.yml`。

## 建置

```bash
./gradlew build      # 產生 shaded jar 於 build/libs/
./gradlew test       # 純邏輯單元測試
./gradlew runServer  # 本機 Paper 測試伺服器
```

## 設計文件

- `docs/DESIGN.md` — 架構、資料模型、路線圖、效能與安全紅線。
- `docs/TESTING.md` — 測試步驟(管理員 / 玩家視角)。
