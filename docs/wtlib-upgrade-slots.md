# AE2WTLib 升级卡槽（终端右侧卡槽）提取说明

来源工程：`AE2WirelessTerminalLibrary-main`（AE2WTLib，NeoForge / 1.21）。
WCWT 的右侧卡槽就是它的 `de.mari_023.ae2wtlib.api.gui.ScrollingUpgradesPanel`（WCWT JSON 里
`widgets.scrollingUpgrades` = `right 2, top 0`，滑块 `upgradeScrollbar` = `right -17, top 6`）；
AE2 自带的那块 `upgrades` 面板被 WCWT 挪到 `-9999` 隐藏掉了。元件升级槽是 WCWT 自己的
`CellScrollingUpgradesPanel`（`cellScrollingUpgrades` = `right 2, top 46`，8 格），与本次无关。

## 1. 面板几何

常量（`ScrollingUpgradesPanel`）：

| 常量 | 值 |
|---|---|
| `SLOT_SIZE` | 18 |
| `PADDING` | 5 |
| `SCROLLBAR_WIDTH` | 5 |
| `maxRows`（默认） | 2；WCWT 设 `Math.max(2, getVisibleRows())`，即随「小/中/大终端」的网格行数变化 |

尺寸与摆放：

- 面板宽 = `2 * PADDING + SLOT_SIZE`（= 28），需要滚动时再 `+ SCROLLBAR_WIDTH`（= 33）
- 面板高 = `2 * PADDING + 可见槽数 * SLOT_SIZE`
- 第 i 个槽：`x = 面板x + 1`，`y = 面板y + PADDING + 1 + i * 18`
- 滚动条：AE2 的 `Scrollbar.SMALL`，范围 `0 .. (总槽数 - 可见槽数)`，步长 1，仅在 `总槽数 > maxRows` 时显示
- 悬浮提示：`Upgrades.getTooltipLinesForMachine(...)`，**首行是「兼容升级」**（`GuiText.CompatibleUpgrades`）
- **槽序：第 1 格是奇点槽**（`AE2wtlibSlotSemantics.SINGULARITY`）；该格为空且没装量子桥卡时**整格隐藏**（不占位）
- 真正的升级槽 = 终端的升级库存（`host.getUpgrades()`），数量 = `WTDefinition.upgradeCount`，**默认 2**（`WTDefinitionBuilder` 构造器里 `this.upgradeCount = 2`）

## 2. 底板精灵（逐像素核对）

贴图：`assets/ae2wtlib_api/textures/guis/icons.png`，128×128。
（已整张拷到本工程：`assets/wtct/textures/guis/wcwt/wtlib_icons.png`，坐标保持原样，可直接照抄上表。）

| 用途 | u, v, w, h |
|---|---|
| 固定面板 TOP | 77, 62, 23, 23 |
| 固定面板 MIDDLE（每多一格重复一次） | 77, 85, 23, 18 |
| 固定面板 BOTTOM | 77, 103, 23, 25 |
| 滚动面板 TOP | 48, 62, 29, 23 |
| 滚动面板 MIDDLE | 48, 85, 29, 18 |
| 滚动面板 BOTTOM | 48, 103, 29, 25 |

同表其它可用精灵：按钮底 `(63,0,16,17)` / `(79,0,16,17)` / 悬停 `(95,1,16,16)`；终端切换器 `(0,56,21,72)`；
磁铁 `(0,0)`、垃圾桶 `(0,32)`、样板访问 `(16,0)`、样板编码 `(16,16)`、合成 `(16,32)`；
否/是/上/下/开关 `(32,0)` `(32,16)` `(32,32)` `(32,48)` `(32,64)`；空盔甲格 `(112,0/16/32/48/64)`。

逐列/逐行结构（自绘或校验用）：

- 固定面板 23 列 = `f2f2f2` 白 | 槽内面 `adb0c4` ×17 | 白 | `cbccd4` ×3 | `413f54` 靛
- 滚动面板 29 列 = 白 | 槽内面 ×17 | 白 | `cbccd4` ×2 | 白 | 凹槽 `9a9fb4` ×3 | 白 | `cbccd4` ×2 | 靛
- 顶段：第 0 行 `413f54`、第 1 行 `f2f2f2`（横向外框），第 2 行起进入槽内
- 中段首行 `cbccd4`、次行 `9a9fb4`、其余 `adb0c4`（就是槽的上沿 + 内面）
- 底段：倒数第 2 行 `878fa5`、最后一行 `413f54`（与终端底框同一套收口）

**精灵与槽位的对齐（实测）**：把精灵逐行转储后可以确认，三段精灵里「槽位美术」的位置是
TOP 的**第 5 行**起（上面 5 行就是 PADDING）、MIDDLE/BOTTOM 的**第 0 行**起，而槽位美术是
18×18（1px 边 + 16px 内面 + 1px 边）。也就是说精灵被设计成「槽位 = 精灵原点 / MIDDLE 起点」，
WTLib `updateBeforeRender` 里那句 `slot.x = slotOriginX + 1; slot.y = slotOriginY + 1;` 让槽比美术
多偏了 (1,1)。本移植按美术对齐：槽的 16×16 物品区 = 美术 18×18 的 (1,1) 处，即
`槽 x = 面板x + 1`、`槽 y = 面板y + PADDING + 1 + 18i`，精灵仍用 WTLib 的原偏移（TOP 在
`面板y`、MIDDLE/BOTTOM 在 `面板y + PADDING + 18i`）。

## 3. 卡清单（AE2 的升级卡机制）

上卡方式：AE2 的 `Upgrades.add(card, machineItem, maxSupported, tooltipGroup)` 声明「这台机器支持这张卡、
最多几张」，玩家把卡手动插进升级槽；`isInstalled(card)` 判断是否生效。

| 卡 | 注册处 | 每台上限 | 作用 |
|---|---|---|---|
| 能量卡 `AEItems.ENERGY_CARD` | `UpgradeHelper.addUpgradeToAllTerminals(card, 0)`（0 = 不限） | 不限 | 供电 |
| 量子桥卡 `ae2wtlib:quantum_bridge_card` | `addUpgradeToAllTerminals(card, 1)` | 1 | 无线终端经量子环连网；配合奇点槽里的纠缠奇点使用（「连接范围无限制」） |
| 磁铁卡 `ae2wtlib:magnet_card` | `Upgrades.add(card, 无线合成终端 / 通用终端, 1)` | 1 | 磁铁：关 / 拾取到物品栏 / 存入 ME |
| WCWT 六张（高级编码卡、装饰盔甲卡、饰品栏卡、网络工具卡槽包卡、工具包卡、谐振过载编码器卡） | 各自 `addUpgradeToAllTerminals` | 1 | 插上才出现对应扩展面板（`IExtendedUIHost.ExtendedUIType`） |
| 第三方 addon | `UpgradeHelper.addUpgradeToAllTerminals(...)` | 自定 | — |

终端之间：WTLib 用 `wut/recipe/Upgrade`（通用终端 + 某终端 = 装上该终端）和 `Combine`（两个终端 = 通用终端）
两个配方类实现，卡片本身不参与这些配方。

已提取的物品贴图（`assets/wtct/textures/items/`）：
`wtlib_quantum_bridge_card.png`、`wtlib_magnet_card.png`、
`wtlib_pattern_access_terminal.png`、`wtlib_pattern_encoding_terminal.png`、`wtlib_universal_terminal.png`（均 16×16）；
另有 GUI 底图 `wtlib_magnet_gui.png` / `wtlib_crafting_gui.png` / `wtlib_trash_gui.png`（256×256，备用）。

## 4. 本移植（1.7.10 / GTNH）的落地计划

现状：host（`WirelessDualInterfaceTerminalInventory`）只有 1 格 `UPGRADES`（`ItemPatternRefillInventory`，
样板补充卡），而且只在无线双接口终端里显示（`PatternContainer` 的 (217,110) 槽）；综合终端**没有任何卡槽**。

### 4.1 已定方案（2026-09-18 实施）

「WCWT 默认「奇点槽 + 2 卡槽」+ 随终端高度增长」，落地为 WTLib 自己的公式：

- `WcwtUpgradesInventory`（`com.asdflj.wtct.inventory`）：`CARD_SLOTS = 8` 个卡槽（索引 0..7）
  + 1 个奇点槽（索引 8）。
  - 卡槽接受 AE2 的升级卡（`IUpgradeModule` 且 `getType != null`，即 AE2 自己的
    `PlacableItemType.UPGRADES` 判据），外加 AE2 的 **Basic Card**——本终端的「样板补充卡」，
    它不报升级类型，AE2 自己也按材质判断。
  - 奇点槽只接受 `materials().qESingularity()`（1.7.10 的量子纠缠奇点，`AEFeature.QuantumNetworkBridge`）。
- **数据顺序刻意为「卡在前、奇点在后」**（WCWT 是奇点在前）：老存档 `UPGRADES.#0` 里已经是样板补充卡，
  这样它仍然落在卡槽 0，不需要迁移；面板显示顺序仍是 WCWT 的（奇点在最上）。
- 面板：`GuiWcwtUpgradePanel`（`client/gui/widget`），几何/精灵全部照抄 WTLib；可见槽数 =
  `max(2, rows)`（`rows` 是终端列表行数，随窗口高度变化）。
  - 终端最小高度（rows = 3）→ 可见 3 格 = **奇点槽 + 2 卡槽**，正是 WCWT 的默认外观；
  - 越高看得越多（rows = 6 → 奇点 + 5 卡），超出的用滑块翻页（`maxScroll > 0` 时才画滑块）。
- 位置：WCWT `scrollingUpgrades` = `right 2, top 0`，即 `x = 352`（对 354px 宽的贴图），
  面板挂在终端右框之外（和左侧工具条挂在左框之外对称）；滑块 = `right -17, top 6` → `x = 371`。
- 1.7.10 侧的实现要点：
  - 槽位由容器创建（`AppEngSlot` ×8 + `SlotRestrictedInput(QE_SINGULARITY)` ×1），**客户端**按当前
    滚动偏移摆放；不在视野里的槽 park 到 `-9999`（既不入画也不参与点击），这不是服务端状态，
    和已有的样板缓存/编码区布局同一套做法。
  - 不画 1.7.10 的槽位底图：WTLib 的底板精灵本身就是槽位美术，18×18 正好套住 16×16 的物品区；
    空槽只有奇点槽的占位图标（AE2 `states.png`，0.4 透明度），和 WCWT 一致。
  - 精灵三段：TOP/BOTTOM 各自带槽位美术，MIDDLE 每多一格重复一次；blit 偏移与 WTLib 完全相同
    （见 §2）。

### 4.2 还没做

| 卡 | 1.7.10 可行性 | 状态 |
|---|---|---|
| 样板补充卡（AE2 Basic Card） | ✅ | 已在面板卡槽生效（`hasRefillerUpgrade()` 改为扫描全部卡槽） |
| 量子桥卡 | ✅ 有对应物（量子环 + `MaterialType.QESingularity`） | 未做；做完后奇点槽的「空且无卡则隐藏」规则（`singularityVisible()` 里的 TODO）就能打开 |
| 磁铁卡 | ⚠️ 功能可做，需挂玩家 tick + 注入逻辑 | 未做 |
| 能量卡 | ❌ 1.7.10 AE2 没有能量卡（无线终端直接吃 AE） | 不做 |
| WCWT 六张 | ❌ 各自对应的扩展面板都还没移植 | 先放着 |

另：`PatternContainer`（无线双接口终端）的样板补充卡槽用 `PlacableItemType.UPGRADES`，而该判据拒绝
Basic Card（它不报升级类型），所以那个槽实际放不进补充卡——综合终端的面板卡槽用普通 `AppEngSlot`
（有效性交给库存判断），没有这个问题。

## 5. 相关源码位置（参照工程）

- `ae2wtlib_api/.../api/gui/ScrollingUpgradesPanel.java` — 面板本体（几何、滚动、奇点槽隐藏、提示）
- `ae2wtlib_api/.../api/gui/UpgradeBackground.java` + `Icon.java` — 三段式底板与全部精灵坐标
- `ae2wtlib_api/.../api/terminal/WTMenuHost.java` — 奇点库存（`INV_SINGULARITY`）与量子桥卡判定
- `ae2wtlib_api/.../api/registration/UpgradeHelper.java` — 卡 → 终端 的注册方式
- `src/.../AE2wtlibItems.java` / `AE2wtlib.java` — 卡物品与上限声明（`registerUpgrades`）
- `src/.../wut/recipe/Upgrade.java` / `Combine.java` — 终端合并配方
