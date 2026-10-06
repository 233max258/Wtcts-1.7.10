# 综合终端的五张升级卡（除 WCWT 六张扩展 UI 卡）

来源：
- `AE2-Import-Export-Card-26.1.2`（AE2ImportExportCard，NeoForge/1.21，MIT）：导入卡 / 导出卡 /
  方块拾取卡。
- `AE2WirelessTerminalLibrary-main`（AE2WTLib，1.21）：量子桥卡 / 磁铁卡（贴图已在本工程
  `textures/items/wtlib_*.png`）。

> 注：AE2WTLib 的 `UPGRADE_INVENTORY_SIZE` 注释里列的「能源卡」在 1.7.10 没有对应物（无线终端直接吃
> AE），本工程早已用另一套机制解决：`WirelessTerminalEnergyRecipe` 写终端 NBT 的
> `InfinityEnergyCard`，`WirelessObject.hasEnergyCard()` 读它。所以这次不做能量卡物品。

## 1. 卡物品（统一一个类）

`ItemWcwtUpgradeCard`（`common/item/card`），`Kind` 枚举：

| Kind | 贴图 | 卡内升级槽 | 过滤器 |
|---|---|---|---|
| `IMPORT` | `wcwt_card_import.png` | 4 | 18 + 9×容量卡（最多 45） |
| `EXPORT` | `wcwt_card_export.png` | 5 | 同上 |
| `BLOCK_PICKER` | `wcwt_card_block_picker.png` | 0 | — |
| `MAGNET` | `wcwt_card_magnet.png` | 0 | — |
| `QUANTUM_BRIDGE` | `wcwt_card_quantum_bridge.png` | 0 | — |

- 卡是 `IUpgradeModule` 但 `getType()` **返回 null**——它们只属于本终端的卡槽，报类型会变成「任何 AE2
  机器都能塞」。`WcwtUpgradesInventory.isUpgradeCard()` 按类识别它们。
- 注册在 `ItemAndBlockHolder`（`CARD_IMPORT` / `CARD_EXPORT` / `CARD_BLOCK_PICKER` / `CARD_MAGNET` /
  `CARD_QUANTUM_BRIDGE`）。
- 配方照抄 AE2ImportExportCard（`RecipeLoader`）：`ERE / XRA / CRC`，E=工程处理器、R=红石块、
  A=高级卡、C=运算处理器、X=对应总线（方块拾取卡用钻石镐）。
- 卡自己的配置全部存在**卡物品的 NBT** 上（对应 addon 的 DataComponent）：
  `SelectedSlots`（int[40]：36 背包 + 4 护甲）、`Filter`（幽灵过滤器，`CardInventory`）、
  `CardUpgrades`（卡内升级槽，`CardUpgradeInventory`，只收 AE2 卡）。
- **卡内升级槽的数量限制**照 addon 的 `Upgrades.add(...)` 注册值：容量卡最多 3，其它卡各最多 1
  （addon 靠 AE2 的 `Upgrades` 注册表强制，1.7.10 这边 `CardUpgradeInventory.isItemValidForSlot`
  自己数）。未注册的 AE2 卡（如 Basic Card、矿石字典卡）也按「最多 1」处理。
- 磁铁卡另外在卡上存 `MagnetPickup` / `MagnetInsert`（各 27 格幽灵过滤器）与
  `MagnetPickupMode` / `MagnetInsertMode`（true = 白名单，默认黑名单）——WTLib 把这两组存在
  *终端*物品上，这里选择存在卡上，让配置跟着卡走（和导入/导出卡的过滤器一致）。

## 2. 五张卡的行为（1.7.10 落地）

### 2.1 量子桥卡
- WCWT：卡 + 奇点槽里的纠缠奇点 = 终端经量子环连网（范围无限制）。
- 本移植：`WirelessObject.hasInfinityBoosterCard(item)` 变成「NBT 标记 **或** 卡槽里装了量子桥卡」，
  `rangeCheck()` 因此放行——与既有 `WirelessTerminalQuantumBridgeRecipe`（把标记烤进终端）等价，但卡
  可以随时取出。
- **奇点槽的显示规则同时打开了**：`GuiWcwtUpgradePanel.singularityVisible()` = 「槽里有东西」或「装了
  量子桥卡」——正是 WTLib 的 `singularitySlotHidden()`。`refresh()` 每帧检查，装上/取下卡后奇点槽会
  出现/收起并重排面板。

### 2.2 磁铁卡
- 复用本模组既有的 `Constants.MAGNET_MODE_KEY`（背包终端也用这个键，两者共享设置）。
- 模式三档：关 / 吸到身上 / 吸进 ME 网络（`CardTicker.MagnetMode`）。第三档用
  `IStorageHelper.poweredInsert` 直接注入网络，吸不完的落到玩家身上；经验球同样吸附（照
  `MagnetObject`）。
- **两个过滤器**（WTLib 的 `MagnetHost`）：`pickup` 决定「哪些掉落物吸不吸」，`insert` 决定「吸到
  的哪些直接进网络」；两者各带一个白/黑名单开关，**默认黑名单 + 空过滤 = 全吸、全进网络**，与
  WTLib 出厂行为一致。潜行时不工作（WTLib `handleMagnet` 的 `isShiftKeyDown` 早退）。
- 设置界面：头部磁铁按钮 → `GuiType.CARD_MAGNET`（WTLib 的 `MagnetScreen`，`wtlib_magnet_gui.png`
  176×256）。左侧多一个是本移植加的「磁铁模式」按钮（WTLib 靠热键切模式），其余五个按钮
  （黑白名单 ×2、复制上/下、交换）位置就是 WTLib `magnet.json` 的 45/63/81/99/117 @ y=80。

### 2.3 导入卡
- 每 10 tick 跑一次（`CardTicker`）：对每个被标记的背包格子，物品通过过滤器后
  `poweredInsert` 进网络，成功后按实际插入量减少背包堆叠。
- 过滤器语义照抄 addon 的 `AEKeyFilterUtil.passesFilter`：`invert != matches`——**空过滤器什么都不
  导入**，装了反相卡则相反；模糊卡按「同物品（忽略损伤/NBT）」比较（= AE2 `IGNORE_ALL`）。
- 终端自己永远不会被导入。

### 2.4 导出卡
- 每 10 tick：对被标记的背包格子，标记值就是过滤器序号（1..N），把该过滤器物品从网络
  `poweredExtraction` 进这个格子；卡内装了速度卡则一次一整组（64），否则 1 个（与 addon 相同）。

### 2.5 方块拾取卡
- 数量存在**终端** NBT 的 `BlockPickerAmount`（addon 也是这样，只是它有独立界面），头部按钮循环
  1/8/16/32/64。
- 客户端 `BlockPickerKeyHandler` 盯「选取方块」键（1.7.10 该键是鼠标键，编码为 `键码+100`），
  边沿触发时把「正看着的方块坐标」发给服务端；服务端 `CardTicker.pickBlock()` 重新校验
  生存模式、距离、方块是否可拾取、背包里是否已有、是否有空位，然后从装了卡的终端取货并放入背包、
  选中该格。
  - addon 是 mixin 进 `Minecraft#pickBlockOrEntity`；1.7.10 对应的方法是私有的、只被按键调用，因此
    改成客户端 tick 边沿检测（`ClientHelper.onClientTick`），不引入新的 mixin。

### 2.6 驱动方式
`ItemBaseWirelessTerminal.onUpdate()` → `CardTicker.onUpdate()`：终端在谁手里就由谁 tick（快捷栏 /
背包 / 饰品），不需要界面开着。服务端每 10 tick 一次，先读终端 NBT 判断有没有卡，没卡零开销。

## 3. 导入/导出卡的配置界面

- `ContainerCardConfig` + `GuiCardConfig`，几何全照抄 addon 的 `import_card.json` /
  `export_card.json`：过滤器 9×2 起（每个容量卡加一行）、玩家 3 行 + 快捷栏（`bottom 84` / `bottom 26`）、
  护甲列 x=2、卡内升级列用终端同一块 WTLib 面板（`right 2, top 0`）。
- 新增两个 GUI 类型 `GuiType.CARD_IMPORT` / `CARD_EXPORT`（**追加在枚举末尾**，否则
  `CPacketSwitchGuis` 的序号会错位）。
- 背景是 addon 的 `upgrade_0..3.png`（按容量卡数量换高度），标记用 `checkmark.png` / `xmark.png`，
  批量选择用 `mass_select.png`——都原样拷进 `textures/guis/wcwt/card_*`。
- 标记写入走已有的 `CPacketTerminalBtns`（新增 `CardConfig.SetSlots` / `CycleMagnet` /
  `CyclePickerAmount` / `PickBlock` 四个 case），没有新增包类。
- 过滤器槽是 `SlotFake`（幽灵），但 `slotClick` 被重写成「拿着的物品直接变成过滤器、右键清空」，
  因为 AE2 自己的假槽只吃 shift-click，而 addon 是拖拽。

### 3.1 踩过的坑（都改好了，别再犯）

1. **贴图命名空间**：`AEBaseGui.bindTexture(String)` 解析的是 **AE2 的资源**（
   `new ResourceLocation(AppEng.MOD_ID, "textures/" + file)`）。照它写 `bindTexture("guis/wcwt/card_bg_0.png")`
   会让整张背景 —— 以及勾/叉/批量选择 —— 全部变成紫黑「缺失贴图」，界面看起来「全坏了」。
   本工程要用 `this.mc.getTextureManager().bindTexture(new ResourceLocation(Wtct.MODID, "textures/" + file))`
   （`GuiComprehensiveWorkTerminal.bindTextureBack` / `GuiCardConfig.bindCardTexture` 就是这个）。
2. **xSize/ySize 必须在 `super.initGui()` 之前设好**：`GuiContainer.initGui` 用它们算
   `guiLeft/guiTop`，之后再改尺寸只会让界面偏心（配置界面高度随容量卡变，所以这条最容易踩）。
3. **`bindPlayerInventory` 绑的是 y=0/18/36 + 快捷栏 y=58**（不是 MC 的 84/142），
   所以「按底边定位」要写成 `height - bottom + slot.getY()`；`SlotPlayerArmor` 的槽索引是
   **39-i**（39=头盔、36=靴子），标记数组 `SelectedSlots` 用的就是这个编号 —— 用铠甲 *类型*
   （0=头盔）去算会整体差 3 格（点头盔标到靴子）。
4. **槽位凹槽是画在背景贴图里的**：addon 的 `upgrade_*.png` 已经把每个槽的凹槽烤进去了
   （`9a9fb4` 上沿 + `adb0c4` 内面），AE2 不会另外画 `states.png` 的槽底。所以背景一坏，槽也
   一起消失；而槽坐标必须和背景里的凹槽严丝合缝（实测 (23,29) 过滤器、(23,79) 玩家栏、
   (2,70) 护甲列都对上了）。
5. **批量选择按钮的精灵比美术宽 1px**：`mass_select.png` 的图形在 (0,0)~(5,6)，精灵画在
   x=183、可点击区是 x=184 起 4×5（addon 的 `isHovering(24+160, y+1, 4, 5)`）。

### 3.2 对照 addon 引导图补齐的三处（`ae2guide/diagrams/*.png` 是带标注的真实截图）

1. **导出卡的过滤器槽每个都画绿色序号**（右上角、半比例、`0x00FF00`，1 起算）——addon 的
   `extractContents` 对 EXPORT 的每个 FakeSlot 都调 `drawSlotHighlight`。没有它，玩家格上的数字
   无法对应到过滤器。`GuiCardConfig.drawFilterIndices()`。
2. **升级面板的「兼容升级卡」tooltip**（addon `UpgradesPanel.getTooltip`）：`GuiWcwtHintArea`
   盖在面板范围上（不可见、`mousePressed` 恒 false，不抢槽位点击、不播音效），列出该卡接受的
   AE2 卡与数量。
3. **模糊切换按钮**（左工具栏 `left 18, top 1`，装了模糊卡才显示）：`GuiWcwtFuzzyButton`，
   AE2 工具栏板 + AE2 1.21 的 `FUZZY_IGNORE`(64,96)/`FUZZY_PERCENT_99..25` 图标；循环
   IGNORE_ALL → PERCENT_99/75/50/25，模式存卡 NBT 的 `FuzzyMode`（AE2 自己的键）。
   过滤语义：无模糊卡 = 精确同堆叠；IGNORE_ALL = 同物品；PERCENT_x = 同物品且耐久落在同一个
   damage-percentage 桶（`breakPoint = max(1, calculateBreakPoint(maxDamage))`）。
6. **配置界面的卡必须拿「活引用」**：`ItemWcwtUpgradeCard.firstInstalled(terminal, kind)` 是从终端
   NBT 反序列化出来的**副本**，往它 NBT 里写什么都不影响终端——`markDirty()` 只会把列库存里
   （没变的）堆叠写回去，结果就是标记、过滤器、卡内升级**全部写进去又丢掉**，卡看起来「无法使用」。
   正确做法是 `ItemWcwtUpgradeCard.liveCard(升级列库存, kind)`：列里的堆叠是活对象，改完对列
   `markDirty()` 才会写进终端。
7. **空卡槽的提示图标**是 AE2 自己的槽位占位机制：`AppEngSlot.setIIcon(states.png 的 16×16 序号)`，
   只在槽为空时以 0.4 透明度画。卡槽用 `PlacableItemType.UPGRADES.IIcon`（=223，一张淡卡，
   states.png (240,208)），奇点槽由 `SlotRestrictedInput(QE_SINGULARITY)` 自带 175（(240,160) 淡奇点）。

## 4. 还没做

- 导出卡的**合成卡**（`AEItems.CRAFTING_CARD`）：addon 里没货会自动下单合成。1.7.10 需要
  `ICraftingGrid` + `beginCraftingJob`，接口完全不同，留待以后。
- 导入/导出卡的**流体**支持：addon 的过滤器是 AEKey，可以选流体；我们只存物品栈。GTNH 侧可以用
  ae2fc 的流体包/液滴表达，但需要额外映射。
- 方块拾取卡的**数量界面**：addon 有独立的 `block_picker_amount` 界面（AE2 的 NumberEntry），
  1.7.10 没有那个控件，这里用头部按钮循环 1/8/16/32/64 代替。
- 这些卡目前只接到**综合终端**的头部按钮；无线双接口终端（旧界面）还没放按钮。

## 4.1 三个卡槽规则（用户常问）

- **终端面板**（`WcwtUpgradesInventory`，9 格 = 8 卡槽 + 1 奇点槽）：卡槽收「报了 AE2 升级类型
  的 `IUpgradeModule`」+「本模组这五张卡」+「AE2 Basic Card = 样板补充卡」；奇点槽**只收**
  `materials().qESingularity()`（量子纠缠奇点）。面板里奇点槽排在最上面（WCWT 的显示顺序），
  但**数据**顺序是「卡在前、奇点在后」，这样旧存档的样板补充卡不会跳到奇点槽里。
- **卡里的卡槽**（导入卡 4 格 / 导出卡 5 格）：只收 AE2 升级卡，数量限制见 §1。
- 磁铁/方块拾取/量子桥卡没有卡内槽（它们不吃 AE2 升级卡）。

## 5. 相关源码位置（参照工程）

- `AE2-Import-Export-Card-26.1.2/.../mixin/WirelessTerminalItemMixin.java` — 导入/导出的搬运逻辑
  （`tickUpgradeCard` / `importItem` / `exportItemToPlayerSlot`）
- `.../item/UpgradeHost.java` — 卡的配置（选中格、过滤器、卡内升级）与容量卡规则
- `.../container/UpgradeContainerMenu.java` + `screen/UpgradeScreen.java` — 配置界面
- `.../mixin/MinecraftMixin.java` + `util/BlockPickerHandler.java` — 方块拾取卡
- `.../util/AEKeyFilterUtil.java` — 过滤器语义
