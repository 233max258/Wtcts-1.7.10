# WCWT 1.3.9 GUI 与交互权威参考（移植用）

来源：`AE2-WCWT-1.3.9` 源码逐文件通读（`WirelessComprehensiveWorkTerminalScreen` / `...Menu` /
`...MenuHost` / `ExtendedPanelLayout` / `wireless_comprehensive_work_terminal.json` / 各 Packet / `PatternMultiplierButton`）。
AP 目标：`Wtcts-29x`（GTNH 2.9.x / 1.7.10）。整理日期：2026-09-14。

> 目的：让"坐标对不准""行为不对"这两类问题一次解决——坐标全部给公式，行为全部给流程。

---

## 一、画布与切片（对齐基准）

| 项 | 值 |
|---|---|
| 背景贴图 | `guis/wcwt/wireless_comprehensive_work_terminal_gui.png`，**512×512** |
| 背景取用区域 | `srcRect = [0, 0, 354, 291]`（整幅参考布局 **354×291**） |
| `slotsPerRow` | **18**（ME 物品列表每行列数） |

`terminalStyle` 切片（全部来自同一张 512×512）：

| 切片 | srcRect (x,y,w,h) | 用途 |
|---|---|---|
| `header` | 0, 0, 355, 17 | 顶部（标题/搜索） |
| `firstRow` | 0, 17, 355, 18 | 列表第一行 |
| `row` | 0, 288, 355, 18 | 列表中间行（重复） |
| `lastRow` | 0, 35, 355, 18 | 列表最后一行 |
| `bottom` | 0, 53, 355, **235** | 列表下方固定区 |

**屏幕高度公式**：`H = 17 + rows*18 + 235`（rows = 当前显示的物品行数）→ 2 行时 **H = 288**。
我方 `GuiComprehensiveWorkTerminal` 的 `ySize = HEADER_H(17) + rows*ROW_H(18) + BOTTOM_H(235)` 与此一致，
所以**任何 WCWT 的 `bottom` 值都能直接换算**：

```
y_local（相对 guiTop） = ySize - bottom          // bottom 锚点：widget 的【顶边】距屏幕底的距离
y_局部（相对底部切片顶） = (ySize - bottom) - (17 + rows*18)
x_local = left
```

---

## 二、坐标规则（`ExtendedPanelLayout.readRect`）

```
width  = widget.width  ?? fallback.width
height = widget.height ?? fallback.height
left   : 有 left 用 left；否则 containerWidth  - right
top    : 有 top  用 top； 否则 containerHeight - bottom     ← 关键：bottom 是"顶边到屏幕底"
容器尺寸 = 当前屏幕的 imageWidth / imageHeight（H 随行数变化）
-9999 = 隐藏到屏幕外
```
另有两类相对坐标：
- **行内按钮**（`manage_upload_pattern` 等）：`top` 是**相对所在行顶部**的偏移；
- **槽位网格**：`slots.<ID>` 给起点 + `grid`（`BREAK_AFTER_3COLS` = 每 3 列换行；`BREAK_AFTER_9COLS`；
  `VERTICAL` 竖排；`HORIZONTAL` 横排）。

---

## 三、全部控件坐标表（原始 JSON，已解析）

### 3.1 顶部（ME 列表区，`top` 绝对坐标）

| 控件 | x | y | w×h | 说明 |
|---|---|---|---|---|
| `verticalToolbar` | 3 | 1 | — | AE2 左侧垂直工具栏锚点 |
| `search` / `inventory_search` | 241 | 4 | 90×12 | 终端搜索框 |
| `scrollbar` / `inventory_scrollbar` | 336 | 18 | — | 物品列表滑块（18 列到 332） |
| `craftingStatus` | 332 | -5 | 20×20 | 合成状态按钮（"锤子"）；`hideEdge:false` → 1.7.10 用**不做 `setHideEdge`** 的 `GuiTabButton`。注意 `GuiMonitor` 只在 `viewCell` 为真时才建它，本终端 `viewCell=false`，必须自己建 |
| `item_display_button` | 79 | 4 | 22×12 | **物品**显示模式 |
| `fluid_display_button` | 104 | 4 | 22×12 | **流体**显示模式 |
| `no_item_fluid_display_button` | 129 | 4 | 22×12 | 隐藏物品/流体 |
| `turn_off_sound_button` | 169 | 3 | 14×14 | 静音（ExtremeSoundMuffler） |
| `wirelessTerminalSettingsButton` | 184 | 3 | 14×14 | 无线终端设置（WTLib） |
| `magnetCardMenuButton` | 199 | 3 | 14×14 | 磁铁卡菜单 |
| `trashButton` | 214 | 3 | 14×14 | 垃圾桶 |
| `pinned_row_overlay` | 7 | 17 | 324×18 | "合成完成置顶"整行覆盖层 |
| `player` | 25 | H-222 | — | 玩家模型渲染位 |
| `viewCells` | W-25 | 0 | — | 显示元件面板 |
| `scrollingUpgrades` | W-2 | 0 | — | 滚动升级面板 |

### 3.2 右侧模式标签（竖直排列，**关键**）

| 控件 | x | y | w×h | 模式 |
|---|---|---|---|---|
| `modeTabButton0` | 331 | **H-232** | 22×22 | 合成样板（`tabCrafting`） |
| `modeTabButton1` | 331 | **H-211** | 22×22 | 处理样板（`tabProcessing`） |
| `modeTabButton2` | 331 | H-190 | 22×22 | 锻造台样板（1.7.10 无） |
| `modeTabButton3` | 331 | H-169 | 22×22 | 切石机样板（1.7.10 无） |
| `modePanel0..5` | 177 | **H-223** | — | 各模式面板的锚点（面板 JSON 各自排版） |

（间隔 21px，`Style.HORIZONTAL`。X=331 → 331..353。）

### 3.3 左侧手动工作区（3×3 合成 / 铁砧 / 锻造台）

| 槽/控件 | x | y | 说明 |
|---|---|---|---|
| `CRAFTING_GRID` | 79 | H-214 | 3×3 输入（`BREAK_AFTER_3COLS`） |
| `CRAFTING_RESULT` | 149 | H-196 | 合成结果槽 |
| `clearCraftingGrid` | 134 | H-214 | 清空合成格 |
| `clearToPlayerInv` | 144 | H-214 | 收回背包 |
| `manual_mode_button0` | 98 | H-168 | 显示"工作台"模式 |
| `manual_mode_button1` / `_2` | 154 | H-214 | 锻造台 / 铁砧模式 |
| `manual_workspace_background` | 78 | H-215 | 88×58 扩展背景 |
| `manual_anvil_name` | 79 | H-213 | 88×12 命名框 |
| `manual_anvil_cost` | 79 | H-178 | 附魔/修复花费 |
| 锻造台/铁砧槽节点 | 79/97/115/149 | H-196 等 | 仅 Java 读取坐标 |
| `wcwtManualPatternSubstitutions` | 134 | H-169 | 工作台模式物品替换 |
| `wcwtManualPatternFluidSubstitutions` | 134 | H-178 | 工作台模式流体替换 |

### 3.4 批量处理区（左侧两行按钮）

| 控件 | x | y | w×h |
|---|---|---|---|
| `Double_button0..3` | 29 / 48 / 67 / 86 | **H-134** | 17×16 |
| `Double_button4..7` | 29 / 48 / 67 / 86 | **H-117** | 17×16 |
| `pattern_Replace1`（物品替换模式） | 152 | H-135 | 14×14 |
| `pattern_Replace2`（流体替换模式） | 152 | H-116 | 14×14 |

**`PatternMultiplierButton.MultiplierType` 顺序（=按钮 0..7）**：
`SWAP("⇄")` `TIMES_2("×2")` `TIMES_3("×3")` `TIMES_5("×5")` / `EQUALS_1("=1")` `DIVIDE_2("÷2")` `DIVIDE_3("÷3")` `DIVIDE_5("÷5")`。

### 3.5 样板编码区（右侧）

| 槽/控件 | x | y | 说明 |
|---|---|---|---|
| `BLANK_PATTERN` | 308 | **H-223** | 空白样板槽 |
| `ENCODED_PATTERN` | 308 | **H-199** | 已编码样板/编辑槽（结果默认落这里，也可放样板回填） |
| `wcwtEncodePattern` | 308 | **H-176** | 16×16 编码按钮（空白样板槽下 30px） |
| `PROCESSING_INPUTS` | 192 | H-216 | 处理输入（3 列换行） |
| `PROCESSING_OUTPUTS` | 277 | H-216 | 处理输出（竖排） |
| `processingPatternModeScrollbar` | 183 | H-216 | h=52 |
| `pattern_crafting_result_slot` | 274 | H-198 | 合成预览结果槽（仅 Java 读取） |
| `wcwtPatternClearPattern` | 运行时定位 | 16×16 | 清除配置 |
| `wcwtPatternSubstitutions` | 运行时定位 | 16×16 | 物品替换 |
| `wcwtPatternFluidSubstitutions` | 运行时定位 | 16×16 | 流体替换（1.7.10 无） |
| `wcwtPatternMergeMaterials` | 清空按钮右侧 | 8×8 | 处理样板合并同类输入 |
| `processingCycleOutput` | 随输出槽定位 | 16×16 | 主产物轮换 |

### 3.6 样板缓存区

| 槽/控件 | x | y | 说明 |
|---|---|---|---|
| `WCWT_PATTERN_CACHE` | 176 | **H-143** | 9 列换行（`BREAK_AFTER_9COLS`）；Java 按滑块只显示部分行 |
| `caching_scrollbar` | 342 | H-143 | h=34 |

> **注意**（2026-09-14 从 WCWT 源码补）：JSON 锚点是 `bottom: 144`，但屏幕代码用的是
> `patternCacheSlotsOriginY() = imageHeight - PATTERN_CACHE_SLOT_BOTTOM(142) + PATTERN_CACHE_SLOT_Y_OFFSET(-1)`
> = **H-143**（基准布局 = 145），比 JSON 锚点低 1px，与滑块顶边齐平。
> 命中测试 `isMouseOverPatternCache` 宽 = `CACHE_COLS * 18 + 12`（= 174，多出的 12px 让滚轮在滑块列也生效）。
> 槽类 = **`RestrictedInputSlot(PlacableItemType.ENCODED_PATTERN)`**（1.21），即**真实槽**——可放入、可取走、
> 空槽显示 40% 不透明度的"样板"图标。**不是** ghost 槽。

### 3.7 样板管理区（WCWT 特色）

| 控件 | x | y | w×h | 说明 |
|---|---|---|---|---|
| `management_page` | 176 | **H-82** | 160×72 | 列表主区（H-82 .. H-10） |
| `manage_scrollbar` | 343 | H-82 | h=72 | 列表滑块 |
| `manage_display_mode` | 175 | H-97 | 14×14 | 显示模式（全部/可见/未满） |
| `manage_displays_slots` | 189 | H-97 | 14×14 | 是否显示样板槽 |
| `automatic_upload` | 203 | H-97 | 14×14 | **自动上传开关** |
| `manage_output_mode` | 217 | H-97 | 14×14 | 输出模式 |
| `manage_search` | 230 | H-107 | 60×12 | 供应器搜索框 |
| `manage_mapping` | 230 | H-96 | 60×12 | 映射编辑框 |
| `increase_mapping` | 291 | H-107 | 30×11 | 增加映射 |
| `heavy_load_mapping` | 322 | H-107 | 30×11 | 重载映射 |
| `delete_mapping` | 291 | H-95 | 30×11 | 删除映射 |
| `manage_cancel` | 322 | H-95 | 30×11 | 映射管理 |
| `manage_upload_pattern` | 285 | 行顶+2 | 14×15 | 行内上传按钮 |
| `manage_open_ui` | 306 | 行顶+2 | 14×15 | 行内打开机器 UI |
| `manage_highlight_provider` | 338 | 行顶+4 | 5×10 | 世界高亮（ExtendedAE） |

**行结构**：表头行 18px（组图标 8×8 @left+2、名称 @left+12 截断 94px、多供应器加 `(n)`）；
槽行 18px，**9 列**、步距 18，槽底图 `wcwt_management.png (0,0,18,18)`，物品画在 `(x, y+1)`；
槽隐藏时整区铺 `background1.png`。

> **几何关系（2026-09-14 更正）**：管理区在**右半边**（x=176..336，H-82..H-10），玩家背包在**左半边**
> （`PLAYER_INVENTORY` x=8 → 8..170），两者是**并排**的，不是"同一块地方的页面"。列表正好落在**缓存区正下方**
> （缓存 H-144..H-108 → 控制行 H-97..H-95 → 管理列表 H-82..H-10），右侧一列自上而下是
> 编码区 / 缓存区 / 控制行 / 管理列表。WCWT 创建页面时并不隐藏玩家背包槽
> （`setSemanticSlotsHidden` 只用于 view cells / 高级编码 / RLOS / 时装护甲 / Curios / 工具包 这些**可选面板**）。
> → **我方移植时不需要"翻页"机制**：我们的 GUI 下半右区（x=176..338、缓存区以下）本来就是空的。

#### 上行/下行数据（供移植参考）
- **供应器列表**：WCWT 由服务端 `PatternProviderListPacket.buildForPlayer(player)` 现扫现发（1.21 用
  `IPatternProviderHost`/网格遍历）；**1.7.10 的等价物我们已经有**：AE2 接口终端的
  `IInterfaceViewable` 注册表 + `PacketInterfaceTerminalUpdate` / `IInterfaceTerminalPostUpdate.postUpdate`，
  每个接口的样板槽就是 `IInterfaceViewable#getPatterns()`（`IInventory`）——我方的
  `ContainerWirelessDualInterfaceTerminal` 已在用这套，"ME 二合一接口终端"的行数据与它同源。
- **槽位同步**：1.21 用 `PatternProviderSlotSyncPacket` 增量同步槽内容；1.7.10 直接读/写那个 `IInventory`
  即可，行数据照抄接口终端的包格式。
- **动作**：`PatternManagementActionPacket.Action` = `EXCHANGE_PROVIDER_SLOT`（左键：空手取 1 / 手持放 1 / 交换）、
  `QUICK_EXTRACT_PROVIDER_SLOT`（Shift 整槽取出）、`QUICK_INSERT_FIRST_PROVIDER`（Shift 编码：按目标解析插入）。
  我方已有等价的 `InterfaceTerminal.PlacePattern`（把已编码样板放进指定供应器槽），即"行内上传按钮"与
  "自动上传落点"两条路都能复用它。
- **上传目标解析**（`resolvePatternUploadTarget`）：① 面板里**选中的供应器** → ② 样板的搜索文本匹配的供应器 →
  ③ 排序后的**第一个**；上传失败按 `patternUploadFailFallbackToEditor` 决定回退到编辑槽还是缓存区。
- 无对应物的控件：`manage_mapping`/`increase_mapping`/`heavy_load_mapping`/`delete_mapping`/`manage_cancel`
  依赖 **ExtendedAE Plus 的 provider search key**；`manage_open_ui` 打开机器 GUI（1.7.10 接口没有 GUI）；
  `manage_highlight_provider` 是世界高亮（ExtendedAE 能力）。

### 3.8 玩家区

| 槽/控件 | x | y | 说明 |
|---|---|---|---|
| `PLAYER_INVENTORY` | 8 | **H-85** | 3 行（9 列换行） |
| `PLAYER_HOTBAR` | 8 | **H-27** | 快捷栏 |
| 护甲槽（WTLib） | 8 | H-223 / H-205 / H-187 / H-169 | 头盔/胸甲/护腿/靴子 |
| `AE2WTLIB_OFFHAND` | 149 | H-174 | 副手 |
| `extended_functions_0..5` | 358 | 48/69/90/111/132/153 | 右侧扩展按钮列（20×17，间距 21） |

### 3.9 区域标题（运行时文字，贴图里没有！）

WCWT 的贴图**不含任何文字**——AE2 1.21 按屏幕 JSON 的 `text` 节点在运行时画。所以这些标签必须自己画。
全部节点（`bottom` 锚点同样走 `y = ySize - bottom`）：

| 节点 | 文案 key | WCWT 文案（zh / en） | x | bottom |
|---|---|---|---|---|
| `comprehensive_work_area` | `item.wcwt.wireless_comprehensive_work_terminal` | （物品名） | 8 | `top: 6` |
| `crafting_grid_title_area` | `gui.ae2.CraftingArea` | 手动合成区 / Crafting Area | 8 | **232** |
| `pattern_encoding_area` | `gui.ae2.PatternEncodingArea` | 样板编码区 / Pattern Encoding Area | 176 | **232** |
| `batch_processing_area` | `gui.ae2.BatchProcessingArea` | 批处理区 / Batch Processing Area | 8 | **149** |
| `pattern_caching_area` | `gui.ae2.PatternCachingArea` | 样板缓存区 / Pattern Caching Area | 176 | **153** |
| `pattern_management_area` | `gui.ae2.PatternManagementArea` | 样板管理区 / Management | 176 | 106 |
| `player_inventory_title` | `container.inventory`（原版） | 物品栏 / Inventory | 8 | **95** |
| `entriesShown` | AE2 条目计数 | — | `right: 26` | `top: 8` |
| `dialog_title` / `crafting_grid_title` / `multiplication_processing_area` / `replacement_mode` / `fluid_replacement_mode` | — | **隐藏**（-9999） | — | — |

- 这些 key 在 **AE2 1.7.10 里不存在**（AE2 1.21 才加的），文案得自己写；我方用 `wtct.gui.area.*`
  命名空间而不是 `gui.ae2.*`，避免和别的 AE2 版本撞 key。文案沿用 WCWT 的覆盖值（如 CraftingArea = 手动合成区）。
- 文字颜色：JSON 没给 color → AE2 默认深灰（`0x404040`），与终端标题一致，不带阴影。
- `pattern_management_area` 我方**不画**：它的页面（样板管理面板）本移植没有，画出来会指向玩家背包。

---

## 四、交互语义（使用情况）

### 4.1 编码流程（服务端 `encode(...)`）
1. 需要"空白样板"来源：`BLANK_PATTERN` 槽，或 `ENCODED_PATTERN` 槽里已有样板（视为编辑/替换）。
2. `createEncodedPattern(mode)` 生成样板 → 写入两段元数据：
   - `ExtendedAePlusPatternMetadata.writeEncoder(pattern, 玩家名)`（ExtendedAE Plus 兼容）；
   - **`PatternUploadMetadata.write(pattern, 上传目标文本)`** ← 关键。
3. 上传目标文本解析顺序（`resolvePatternUploadSearchText`）：
   **① 映射框文本 → ② 配方 ID（NEI/JEI 拉取记录）→ ③ 合成模式默认键 → ④ 处理模式为 null**。
4. **自动上传开启**（`automatic_upload`）：非处理模式先试 *样板矩阵*（ExtendedAE Plus），
   再 `uploadEncodedPatternToMatchingProvider(pattern, 搜索文本)`；
   成功 → 清空编辑槽 + **从 ME 网络补空白样板**（`tryFillBlankPatternFromNetwork`）→ 广播；失败 → 继续走 5。
5. **自动上传关闭 / 上传失败**：结果进 **样板缓存区空槽**（`findEmptyPatternCacheSlot`），随后同样补空白样板。

### 4.1b 编辑槽回填（把已编码样板放回编辑槽）—— 全面对齐 GTNH AE

`ENCODED_PATTERN` 除了接编码结果，还能**放回一份已编码样板让编辑区回填**（JSON 里那句
"也可放入现有已编码样板回填到编辑区"）。**GTNH 的 AE 自己就有这个 API**：

```java
// appeng.api.parts.IPatternTerminal（default 方法，GTNH rv3 反编译核对）
void loadPatternFromItem(ItemStack pattern, World world, IAEStackInventory inputs, IAEStackInventory outputs)
```

它的顺序就是标准流程（我方 `loadEditSlotPattern()` 按这个顺序实现）：

| 步 | 官方做法 | 我方对应 |
|---|---|---|
| 1 | `tag == null` → 退出；`item instanceof ICraftingPatternItem` 才继续 | 同 |
| 2 | 选项从样板 NBT 读：`crafting` / `substitute` / `beSubstitute`，分别 `setCraftingRecipe` / `setSubstitution` / `setCanBeSubstitution` | 同（`restoreEncodingOptions`） |
| 3 | 内容：能解析时用 `details.getAEInputs()/getAEOutputs()`，否则按物品类型分支：`ItemEncodedPattern` → `PatternHelper.loadIAEItemStackFromNBT(list, true, null)`；否则 → `UltimatePatternHelper.loadIAEStackFromNBT(...)` | 一律用这两个读取器（见下） |
| 4 | 先清空 inputs/outputs 两个 inventory，再**按槽位**填（`in[i] → 槽 i`） | 同（清 `clearEncodingArea()` 后按槽位填） |
| 5 | 样板本体不动 → 再按编码键就是覆盖重编 | 同 |

**两处必须补的修正**（AE 的读取器是给合成任务用的，不是给编辑用的，直接用会坏）：

1. **位置**：`PatternHelper.loadIAEItemStackFromNBT` 会**跳过空 compound**（不补 null），后边的条目整体前移；
   而 AE 自己的**编码器**是按槽位读编辑区的（`PatternEncodingHelper#getInputs` 就是 `new IAEStack[size]` 然后
   `out[i] = inv.getAEStackInSlot(i)`，null 保留）。所以回填时要把读取器的结果按**原始列表布局**放回各自的槽，
   否则合成样板的 3×3 布局过不了往返。
2. **数量**：AE 自己的编码器把数量写在 long `Cnt` 里、`Count` 留 0，而 `Platform.loadItemStackFromNBT` 只读 `Count`——
   只有 `PatternHelper` 的**构造器**做了 `if (stackSize == 0) stackSize = getLong("Cnt")` 这个回退。读取器没有这步，所以要自己补
   （本 mod 的编码器是把数量直接写进 `Count`，两种都要认）。

其它相关的 AE 官方入口：
- `IPatternTerminal.isBlankPattern(ItemStack)` / `isEncodedPattern(ItemStack)` / `createBlankPattern()`（静态）。
  注意 `isEncodedPattern` **只认 `ItemEncodedPattern`**，会漏掉 Ultimate 变体，所以"是不是样板"的判断用
  `instanceof ICraftingPatternItem` 更稳。
- `PatternEncodingHelper.encode(IPatternTerminal, IEnergySource, IMEMonitor, BaseActionSource, String, World)` = 官方编码实现。
- `IPatternTerminal$PatternEncodeListener#onEncoded(terminal, pattern)` = 编码完成回调。
- 触发时机：1.7.10 没有 1.21 的 `PatternEncodingLogic`，用「服务端每 tick 比对编辑槽内容」实现，
  顺带也能接住 Shift 点击/从缓存拖入等一切来源。

### 4.2 行内上传按钮（`UPLOAD_CACHE_SLOT`）
按**缓存槽顺序**遍历 → 只处理编码样板 → 插入顺序 = **目标供应器 → 同名供应器组** →
插入方式 = **先叠放同类未满栈、再填空槽** → 缓存栈按接受数量 shrink（空则清槽）→
一次没放完就 **break** → 状态提示 `没有可保存的样板 / 样板已保存到供应器 / 样板保存失败 / 样板供应器不存在`。

> 入库前会 `PatternUploadMetadata.copyWithoutUploadData(pattern)` **剥掉**上传元数据
> （即"目标供应器搜索文本"不会留在供应器里的样板上）。

### 4.3 槽位交互
| 动作 | 语义 |
|---|---|
| `EXCHANGE_PROVIDER_SLOT`（左键） | 空手 → 取出该槽样板到手；手持非编码样板 → 忽略；手持样板+空槽 → 放 1 个并 shrink 手持；手持正好 1 个+槽非空 → **与槽内交换** |
| `QUICK_EXTRACT_PROVIDER_SLOT` | 整槽取出并放回玩家背包 |
| `QUICK_INSERT_FIRST_PROVIDER`（Shift 编码） | 目标供应器优先：选中供应器 → ②样板的搜索文本匹配 → ③排序后的第一个 |

### 4.3b Shift+左键（1.7.10 移植的坑，2026-09-14 修）
WCWT/AE2 1.21 的编码格是**真槽**，我们移植时用了 AE2 的 `SlotFake`（幽灵槽）来当编码格，
于是踩到 **`AEBaseContainer#transferStackInSlot` 的"假槽兜底"**：

1. 玩家背包里的栈 Shift 点击时，先走 `getValidDestinationSlots(true, stack)` = 容器侧**非假槽**里
   `isItemValid` 通过的那些。编码格是 `SlotFake`，在这个筛选里被**显式跳过**，所以普通物品得到空表；
2. 空表且源在玩家侧 → `getValidDestinationFakeSlot(stack)`：**扫全部非玩家侧 `SlotFake`，返回第一个空格**
   （不看 `isItemValid`！）→ `fake.putStack(stack.copy())` → **复制**一份进编码格，**源槽不动**，返回非 null；
3. vanilla `Container.slotClick` 的 Shift 分支发现源槽还是同一物品 → **`retrySlotClick` 再来一次**；
   第二次因为 `getValidDestinationFakeSlot` 见到"已有同款物品"直接返回 null 才停手。

净效果：**Shift+左键凭空复制一个到样板区，原堆叠不减少**（AE2 原生编码终端没有 `SlotFake`，所以从不出现）。

修法（我方，2026-09-14 定稿语义）：
- `ContainerComprehensiveWorkTerminal#getValidDestinationFakeSlot` → **返回 null**，彻底关掉复制兜底；
- **Shift 语义对齐 WCWT/AE2**：
  - **已编码样板 → 进编码区**（`BaseNetworkContainer#transferPatternToSlot` 先把它放进编辑槽；
    编辑槽占用时走 AE2 的槽路由落到缓存区）；
  - **普通物品 → 进终端（ME 网络）**：`BaseNetworkContainer#transferStackInSlot` 在"容器侧没有槽要它"
    （AE2 返回 null）时调新钩子 `storeShiftClickedStack(p, idx)`，
    `ContainerMonitor` 里用现成的 `inject(monitor, power, src, AEItemStack.create(stack))`（= `Platform.poweredInsert`）
    存进 ME 物品存储，装不下的余量写回原槽；**流体包**改走 `injectFluids`（ae2fc 语义）。
    → 这一步是 AE2 `ContainerMEMonitorable#transferStackInSlot` 的 `shiftStoreItem`，
    我方容器链（`… → ContainerMonitor → BaseNetworkContainer → AEBaseContainer`）**不含** `ContainerMEMonitorable`，
    所以此前整套无线终端都**不能**用 Shift 把背包物品存进网络（这是本次补的最大缺口）；
  - `BaseNetworkContainer#transferStackInSlot` 开头加 `Platform.isClient() → null`，与 AE2 一致，不在客户端预测改物品。
- `BaseNetworkContainer#transferPatternToSlot` 原来用 `p.inventory.setInventorySlotContents(slotIndex, null)`
  清源槽，只对玩家槽成立；从**缓存/合成格**Shift 点击时会误删一个无关的玩家槽位而样板仍在原处
  （又是一次"凭空复制"）。改成 `clickSlot.decrStackSize(1)`，由槽自己决定从哪个 inventory 取。

### 4.3c 已编码样板不可堆叠（2026-09-14）
`appeng.items.misc.ItemEncodedPattern` 构造器里是 **`setMaxStackSize(64)`** —— 同 NBT 的样板在 1.7.10
**能叠成一份 64**。AE2 的槽路由用 `slot.getSlotStackLimit()` 决定"一次能塞几个"，
所以只要给槽 `setStackLimit(1)`，Shift 一次就只搬 1 份、也不会叠成一格：
- 样板缓存槽（36 个 `SlotRestrictedInput(ENCODED_PATTERN)`）：**本次加 `setStackLimit(1)`**；
- 编辑槽（`patternSlotOUT`）：本来就有；
- **空白样板槽照旧允许成叠**（`patternSlotIN`）：空白样板是消耗品，一次带一组才好用，
  AE2 原生编码终端同样允许，故不锁。

### 4.4 管理区控制项
- 4 个按钮的状态（自动上传开关 / 显示模式 / 是否显示槽 / 搜索模式）经
  **`PatternManagementUploadSettingPacket(enabled, displayMode, showSlots, searchMode)`** 存到 host 端；
- `display_mode`：全部供应器 / 显示可见供应器 / 显示未满供应器；
- **搜索框**：既是列表过滤，也是"上传目标文本"的来源；
- **映射框 + 4 个映射按钮**：WCWT 本身不存映射表——它接的是 **ExtendedAE Plus 的 provider search key**
  （`PlusMapping.captureRecipeSearchKey / consumeLastProviderSearchKey`，`manage_cancel` = 打开 ExtendedAE Plus 映射界面）。
  → **1.7.10 没有 ExtendedAE Plus，这套映射无对应物**（这是"映射功能"在我方一直做不顺的根本原因）。

### 4.5 批量区（`applyPatternMultiplier`，2026-09-14 补细节）

`Double_button0..7` 的真实作用（`WirelessComprehensiveWorkTerminalMenu.applyPatternMultiplier`）：

| 键 | 作用 |
|---|---|
| `SWAP` ⇄ | `rotateProcessingPatternOutputs`：每个处理样板的**输出轮换一位**（跳过空槽、环形取"下一个非空"，空位保持空），主产物换人 |
| `TIMES_2/3/5` | 输入与输出数量**全部乘以** 2/3/5 |
| `EQUALS_1` =1 | `restoreProcessingPatternRatio`：全部数量**除以它们的最大公约数** → 还原最简整数比。**不是**"把数量都改成 1"，所以 2:1 的配方仍是 2:1 |
| `DIVIDE_2/3/5` | 全部数量 ÷2/÷3/÷5，**要求每个数量都能整除** |

**作用范围**（两处同时改）：
1. 编码区**当前配置**（`applyMultiplierToCurrentProcessingConfig`）——仅当处于 **PROCESSING** 模式；
2. **缓存区所有已编码处理样板**（`modifyPatterns` / `restoreProcessingPatternRatio` / `rotateProcessingPatternOutputs`），
   逐个 `decodePattern` 后重编码，**只处理处理样板**（`instanceof AEProcessingPattern`）。

**校验**（`checkCanModify`，逐样板 all-or-nothing）：乘法 `amount*scale <= 999999 * amountPerUnit`；
除法要求 `amount % scale == 0`。不满足就**整份样板不动**。
客户端配置 `patternMultiplierApplyToEditorProcessing` 可关掉"作用于编辑区"，只留缓存区。

- `pattern_Replace1/2`：切换**物品替换 / 流体替换**"模式"（不是开关本身，是批处理的替换来源切换）；
- `wcwtPatternMaterialsMerge`：处理样板合并同类输入；
- `processingCycleOutput`：主产物轮换（等价于 ⇄）。

### 4.6 缓存区
- `WCWT_PATTERN_CACHE` 36 格（9×4），界面按滑块只显示部分行；
- 滚轮 / 拖拽滑块；`patternSelectionLockedMode` 下点缓存槽=选择锁定（取用目标），普通模式=取出。

---

## 五、移植对齐规则（我方 `GuiComprehensiveWorkTerminal`）

**统一换算（不会再错）**
```
y = guiTop + ySize - bottom        // 例：管理区 H-82 → ySize-82
```
其中 `ySize = 17 + rows*18 + 235`，与 WCWT 完全同构；`bottom` 一律取上表原值。

**已知偏差清单（需要修正）**

| # | 项 | WCWT | 我方现状 | 处理 |
|---|---|---|---|---|
| 1 | 模式标签 | 合成 @H-232、处理 @H-211（**竖直两个位置**，22×22） | ~~两标签同坐标 (331,56) 靠显隐切换~~ | ✅ 2026-09-14 已分开到 textureY 56 / 77，两个常显，选中态由 `GuiWcwtTabButton` 表达 |
| 2 | 替换按钮 | `pattern_Replace1/2` @ (152, H-135/H-116)，14×14，位于左侧批量区 | 放在编码区顶行 | 待办（移到 WCWT 位置） |
| 3 | 编码区按钮 | 编码 (308,H-176)、空白样板 (308,H-223)、已编码样板 (308,H-199) | ~~我方用自造值 ENC_ROW_Y=72 等~~ | ✅ 2026-09-14 复核：这四项本就等于 112/65/89，正确 |
| 4 | 处理输入/输出 | (192,H-216) / (277,H-216) | 核对 | ✅ 计算得 72 / 72，本就正确 |
| 5 | 合成格/结果 | (79,H-214) / (149,H-196) | 核对 | ✅ 74 / 92，本就正确；**但样板编码区的合成矩阵 WCWT 在 x=183（= 192−9），已在 2026-09-14 修正** |
| 6 | 顶部显示模式按钮 | (79/104/129, y=4) 22×12 | **未实现** | 若要完整还原需补 |
| 7 | 缓存区 | (176,**H-143**)、滑块 (342,H-143,h34)、**真实槽** | ~~我方 144 / 145 / 34，且用 `SlotFake` 幽灵槽~~ | ✅ 2026-09-14 修正：Y 改 145；槽改 `SlotRestrictedInput(ENCODED_PATTERN)`（原幽灵槽放不进也拿不出）；命中宽改 174 |
| 8 | 倍增按钮 | (29/48/67/86, H-134/H-117) | 我方 154/171（H=288） | ✅ 一致 |
| 9 | 手动清空 | (134/144, H-214) | 我方 (134,74)（H=288 → H-214 ✓） | ✅ 一致 |
| 10 | 管理区 | (176,H-82,160×72)+控制行+行内按钮 | 曾按此实现后被撤 | 坐标公式本身正确，问题在"是否与背包重叠+页面机制" |

**编码区选项按钮（JSON 里写 `-9999`，全靠运行时定位 —— 2026-09-14 补）**

`getPatternEncodingBackgroundOrigin()`：`left = PROCESSING_INPUTS.left − 16 = 176`，
`top = PROCESSING_INPUTS.top − 7 = H−223`（H=288 时为 65），面板 124×66。按钮全部落在 `原点 + 6`（y=71）：

| 按钮 | 合成模式 | 处理模式 |
|---|---|---|
| `wcwtPatternClearPattern` | 原点+62 → x=238 | 原点+71 → x=247 |
| `wcwtPatternMergeMaterials` | —（不显示） | clear+10 → x=257 |
| `wcwtPatternSubstitutions` | 原点+72 → x=248 | —（不显示） |
| `wcwtPatternFluidSubstitutions` | 原点+82 → x=258 | —（1.7.10 无，用 be-substitution 占位） |

编码滚动条 `processingPatternModeScrollbar`：left **183**、bottom 216、height 52。

**结论（对不准的根因）**
1. 早期我方用的是**自造 top 值**（ENC_ROW_Y / TAB_Y 等），而不是从 `ySize - bottom` 反推；
2. 模式标签被我方做成"同坐标显隐"，而 WCWT 是**竖直两个独立位置**；
3. ~~管理区是与玩家背包同区域的"页面"~~ **更正（2026-09-14）**：管理区在 **x=176..336 的右半边**，
   `PLAYER_INVENTORY`/`HOTBAR` 都在 **x=8 的左半边**，两者**并排不重叠**；WCWT 的 `setSemanticSlotsHidden`
   从不用于管理区（只用于 view cells / 高级编码 / RLOS / 时装护甲 / Curios / 工具包）。所以它是右列
   （编码区→缓存区→控制行→管理列表）里本来就空着的那一段，**不是"翻页"**。

### 5.0 两个坐标空间：`drawBG` 绝对 / `drawFG` 相对（2026-09-14 踩坑）

`GuiContainer.drawScreen` 的真实顺序（已核对 `build/rfg/minecraft-src/.../GuiContainer.java`）：

```
drawGuiContainerBackgroundLayer()      →  未平移：绝对屏幕坐标（AE2 传的 offsetX/offsetY = guiLeft/guiTop）
super.drawScreen()                     →  GuiScreen 的按钮
glPushMatrix(); glTranslatef(guiLeft, guiTop)
   …容器槽位功能渲染（用 slot.xDisplayPosition 这种 GUI 相对值）…
   drawGuiContainerForegroundLayer()   →  drawFG：**GUI 相对坐标**
glPopMatrix()
   …手持物品、原生 tooltip…              →  又是绝对坐标
```

由此：
- 我方 `drawBG(offsetX, offsetY, …)` 里画的东西一律用**绝对**坐标（`blit512(offsetX, …)`、`THGuiTextField.drawTextBox()`）；
- `drawFG(offsetX, offsetY, mouseX, mouseY)` 里画的东西一律用**相对**坐标，但**传进来的 mouseX/mouseY 仍是绝对**的
  （所以要写 `mouseX - guiLeft`）；
- `AEBaseGui.drawHoveringText` / `renderToolTip` 只是转发给 `GuiContainer` 的同名方法，**不做任何补偿**，
  对当前矩阵直接绘制 → **在 drawFG 里必须传相对坐标**，否则 tooltip 会偏移整个 (guiLeft, guiTop)。
  2026-09-14 修的就是这一处（管理区 tooltip）。AE2 自己在 `AEBaseGui.drawScreen` 末尾（`glPopMatrix` 之后）
  处理 tooltip，所以它那边用绝对坐标。

### 5.0b "未知物品"占位（`ItemEncodedPattern`）

样板产物**解析不出来**时，AE2 会造一个**没有 item 的** ItemStack（`ItemEncodedPattern` 里是
`new ItemStack(Blocks.fire)` —— fire 没有对应物品），并 `setStackDisplayName("未知物品" / GuiText.UnknownItem)`。
把它塞进 `RenderItem` 就是**紫黑缺贴图方块**——这就是"图层里有未知物品的贴图"的来源（不是二合一接口终端画的，
但数据是它那条 `PacketInterfaceTerminalUpdate` 链给的）。
**规则**：画样板产物前必须检查 `product != null && product.getItem() != null && !(product instanceof ItemFluidPacket)`，
任一不成立就**退回画样板本体**。

---

## 六、1.7.10 无对应 / 需替代

| WCWT | 1.7.10 情况 |
|---|---|
| 锻造台 / 切石机样板模式（tab2/tab3） | 无这两种机器 |
| 流体替换（`wcwtPatternFluidSubstitutions`） | AE2 1.7.10 无对应 API |
| ExtendedAE Plus：样板矩阵上传、映射键、高亮、打开机器 UI | 无（映射按钮整套失效） |
| WTLib：无线终端设置、护甲槽、副手、滚动升级面板 | 无（可用 Baubles 桥接替代部分） |
| ExtremeSoundMuffler 静音按钮 | 视整合包 |
| `tryFillBlankPatternFromNetwork`（从网络补空白样板） | **可实现**，我方此前跳过 |
| `PatternUploadMetadata`（data component） | 用 **NBT 标签**等价实现 |

---

## 七、样板 NBT 格式（1.7.10 实测，改造成品样板时必须知道）

样例板的 `tag` 里有 `in` / `out`（`NBTTagList` of `NBTTagCompound`）、`crafting`（bool）、
解析失败时 AE2 会写**粘性** `InvalidPattern=true`。**列表位置就是槽位位置**，
未使用的槽 = **空 compound**（`hasNoTags()`）。

单个条目的金额有两种写法，AE2 两种都认（`PatternHelper` 构造函数反编译）：

```java
ItemStack is = Platform.loadItemStackFromNBT(entry);      // 内部：stackSize = entry.getInteger("Count")
if (is != null) {
    if (is.stackSize == 0) is.stackSize = (int) entry.getLong("Cnt");  // 回退
    ...
} else if (!entry.hasNoTags()) { tag.setBoolean("InvalidPattern", true); throw new IllegalStateException(...); }
```

| 写法 | 来源 | 字段 |
|---|---|---|
| `Count` = 真实数量（int） | ae2fc `Util.writeItemStackToNBT` / AE2 `Platform.writeItemStackToNBT`（**本 mod 的编码器用这个**） | `{id, Damage, Count:<int>}` |
| `Count = 0` + `Cnt` = 真实数量（long） | AE2 `AEItemStack.writeToNBT`（1.21 原生编码器） | `{id, Count:0, Cnt:<long>, Req, Craft}` |

流体条目是 ae2fc 的 `ItemFluidPacket` ItemStack：数量在内层 `tag` 里，
统一用 `ItemFluidPacket.getFluidAmount/setFluidAmount` 读写（**不要**看外层 `Count`）。

→ 改写金额时：读 `Count`（非 0 用它，否则用 `Cnt`），写回时 `Count` 写 int，
并且**只有当原条目本来就有 `Cnt` 时才补写 `Cnt`**，这样两种来源的样板都不会被改坏。
实现见 `com.asdflj.wtct.util.PatternScaling`。
