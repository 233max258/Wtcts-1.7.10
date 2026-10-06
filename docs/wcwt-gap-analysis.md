# WCWT 1.3.9（1.21）与本移植版功能差距清单

对照对象：`AE2-WCWT-1.3.9`（Wireless Comprehensive Work Terminal，1.21 AE2）。
本移植版：`Wtcts-29x`（GTNH 2.9.x / 1.7.10）。分析日期：2026-09-13。

## 一、已实现（与 WCWT 对应）

| WCWT 功能 | 本移植版状态 |
|---|---|
| 综合工作终端本体（ME 列表 18 列、搜索、终端风格切片布局） | ✅ GuiComprehensiveWorkTerminal |
| 样板编码 · 合成模式（3×3 矩阵 + 结果预览） | ✅ |
| 样板编码 · 处理模式（3 列输入可滚动 + 3 输出） | ✅ |
| 编码按钮（编码 → 自动进样板缓存/背包） | ✅ |
| 清除配置按钮（1.21 S_CLEAR 图标） | ✅ |
| 样板缓存区 36 格（4 行×9，两行可见 + 滚动） | ✅ |
| 批处理倍增器 8 键（⇄ ×2 ×3 ×5 / =1 ÷2 ÷3 ÷5，WCWT 底板贴图） | ✅（⇄ = 主副产物轮换） |
| 合并材料开关（相同输入合并为一格） | ✅ |
| 物品替换 / 可被替换开关 | ✅（可被替换为 1.7.10 AE2 风格） |
| NEI 配方搬运进编码区（含流体，THComprehensiveWorkTerminal） | ✅（对应 WCWT 的 JEI/EMI pull，1.7.10 用 NEI） |
| 手动 3×3 合成格 + 清空/回收 | ✅ |
| 无线二合一接口终端（本 mod 原有，WCWT 无） | ✅ |

## 二、缺失 · 建议移植（1.7.10 可行、价值高）

1. **样板供应器管理面板**（PatternProviderListPacket / PatternProviderSorts / PatternProviderFocusPacket / PatternManagementActionPacket）
   —— WCWT 的核心卖点：在终端里浏览/搜索/排序网络中所有样板供应器并直接管理其样板。工作量最大。
2. **复制样板 / 替换样板**（CopyPatternPacket / ReplacePatternPacket）—— 配合缓存区很实用，工作量小。
3. **主产物轮换独立按钮**（CycleProcessingOutputPacket，processingCycleOutput）—— 现在 ⇄ 已实现同一轮换逻辑，缺的是带图标的独立入口。
4. **显示过滤按钮组**（ItemDisplayButton / FluidDisplayButton / OtherTypesDisplayButton / ViewCellsToggleButton）
   —— 综合终端目前没有暴露物品/流体/其他类型过滤与视图单元开关（双接口终端有自己的 typeFilters）。
5. **收藏夹**（WcwtFavorites / FavoriteItemsButton）—— 终端内收藏常用物品。
6. **补货 Restock**（WcwtRestockState / WcwtRestockAmountsPacket / WcwtServerPlayerRestockMixin）—— 快捷把背包补到指定数量。
7. **垃圾桶**（WcwtTrashMenu / WcwtTrashScreen）—— 终端内销毁物品。
8. **磁吸 Magnet**（WcwtMagnetScreen / WcwtMagnetMenu / WcwtMagnetHotkeyAction）—— 范围吸取掉落物。
9. **样板改名（手动工作区铁砧命名）**（ManualAnvilNamePacket）—— 本 mod 另有 GuiRenamer，可打通到终端内。

## 三、缺失 · 可选（依赖特定 mod / 场景）

- 共振闪电样板编码（LightningTech 兼容：ResonatingLightningPatternCodingPanel / ResonatingLightningPatternActionPacket）
- ExtendedAE / AE2 扩展兼容（ExtendedAeHighlight、ExtendedAePlusUploadCompat、ExtendedAePlusPatternMetadata）
- AppliedMekanistics、CrystalScience、NeoEco、MegaCells、AE2WTLib-Plus 等上传兼容
- 批量压缩截断按钮（BulkCompressionCutoffButton，依赖批量压缩类 mod）
- 网络工具远程访问工具箱（WcwtToolkitNetworkTool* / WcwtRemoteMenuAccess / WcwtRemoteMenuMixin / SimulatedNetworkToolMenuHost）
- 拾取方块入终端（WcwtPickBlockPacket / WcwtMinecraftPickBlockMixin）
- 服务端/客户端配置（WcwtServerConfig / WcwtClientConfig）与命令（WcwtCommands）
- 菜单保护 Mixin（WcwtMenuProtectionMixin：终端打开时防止拿走终端本体）—— 1.7.10 有同类需求
- 万能终端合成配方（WcwtUniversalTerminalCombineRecipe）

## 四、缺失 · 1.7.10 无对应（不适用）

- 切石机样板模式、锻造台样板模式（1.7.10 没有这两种机器）
- 流体替换开关（1.7.10 AE2 无此 API；已知）
- Curios / CosmeticArmor 面板（可考虑桥接 Baubles 做"饰品面板"替代）
- AE2 工具箱面板（ToolboxPanel）、滚动升级面板（1.7.10 AE2 无此概念）
- Polymorph 配方冲突兼容
- JEI/EMI 专用通道（书签键、pull 按钮、部分搬运错误提示）—— NEI 已覆盖基本搬运

## 五、建议实施顺序

1. 复制/替换样板（小）→ 2. 显示过滤按钮组（中）→ 3. 主产物轮换独立按钮（小）→
4. 收藏夹（中）→ 5. 补货 / 垃圾桶 / 磁吸（各自独立，中小）→ 6. 样板供应器管理面板（大，最后攻坚）
