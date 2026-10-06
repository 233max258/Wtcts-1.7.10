# 综合工作终端 · 待办清单（对照 WCWT 1.3.9）

对象：`Wtcts-29x`（GTNH 2.9.x / 1.7.10）对齐 `AE2-WCWT-1.3.9`（1.21 AE2）。
更新日期：2026-09-14。前置说明见 `docs/wcwt-gap-analysis.md`。

状态图例：✅ 已完成 ｜ ↩️ 已回滚待重做 ｜ ⬜ 未开始 ｜ ⛔ 不适用

---

## 一、状态总览

| 项 | 状态 | 工作量 | 备注 |
|---|---|---|---|
| 综合终端本体（18 列 ME 列表 / 搜索 / 切片布局） | ✅ | — | GuiComprehensiveWorkTerminal |
| 编码区（合成 3×3 + 处理 3 列滚动 / 3 输出） | ✅ | — | |
| 编码 → 自动进样板缓存（满则进背包） | ✅ | — | |
| 样板缓存区 36 格 + 滚动 | ✅ | — | NBT `pattern_cache` 持久化 |
| 批处理倍增器 8 键（⇄ ×2 ×3 ×5 ／ =1 ÷2 ÷3 ÷5） | ✅ | — | ⇄ = 主副产物轮换 |
| 合并材料开关 | ✅ | — | 服务端每 tick 合并同型输入 |
| 物品替换 / 可被替换开关 | ✅ | — | @GuiSync 同步，图标随状态 |
| 清除配置按钮 | ✅ | — | 1.21 风格图标（(224,200)） |
| NEI 配方搬运进编码区（含流体） | ✅ | — | THComprehensiveWorkTerminal |
| 手动 3×3 合成格 + 清空/回收 | ✅ | — | |
| 无线二合一接口终端（本 mod 原生） | ✅ | — | WCWT 没有，属本 mod 优势 |
| 样板供应器管理面板 | ↩️ | 大 | 见 P0-1，需先定位上次启动期异常 |
| 复制 / 替换样板 | ⬜ | 小 | P0-2 |
| 主产物轮换独立按钮 | ⬜ | 极小 | P0-3（逻辑已存在） |
| 显示过滤按钮组（物品/流体/其他 + 视图单元） | ⬜ | 中 | P1-4 |
| 收藏夹 | ⬜ | 中 | P1-5 |
| 补货 Restock | ⬜ | 中 | P1-6 |
| 垃圾桶 | ⬜ | 小 | P1-7 |
| 磁吸（终端内开关） | ⬜ | 中 | P1-8（核心已有） |
| 样板改名（终端内） | ⬜ | 小 | P2-9（可复用 GuiRenamer） |
| 切石/锻造样板、流体替换、工具箱、Polymorph | ⛔ | — | 1.7.10 无对应 |

---

## 二、P0（建议紧接着做）

### P0-1 样板供应器管理面板 ↩️

**目标**：在综合终端里浏览/搜索/排序网络内所有样板供应器，并直接管理其样板、接收"编码后自动上传"。

**关键事实**
- 综合终端与二合一终端**共用同一个 host**：`WirelessDualInterfaceTerminalInventory`（已实现 AE2 `IInterfaceTerminal`）。
- 因此供应器行同步（`PacketInterfaceTerminalUpdate` → `IInterfaceTerminalPostUpdate.postUpdate`）、PlacePattern 自动填充、样板修改器注入/抽取、倍增全部是**现成机制**。
- 增量只在两处：①用综合终端的 host 打开二合一终端视图（走 `GuiType` + `ItemGuiBridge` + `CPacketSwitchGuis`，需支持 Baubles 槽位）；
  ②在综合终端 GUI 内实现行列表渲染（可抽 `GuiBaseInterfaceWireless` 的 MasterList）/ 或独立视图。

**上次尝试的失败点（必读）**
- 上一轮实现后在**启动期出现 server thread 异常**（会话日志已被后续运行覆盖，未能留证），遂按用户要求全面回滚。
- 因此本轮必须**分步启用并逐步验证**：
  1. 先只加 `GuiType` 入口 + 独立视图（不动综合终端容器）→ 启动验证；
  2. 再在综合终端容器里挂 `ContainerInterfaceTerminal` delegate + `detectAndSendChanges()` → 启动验证；
  3. 最后接入"编码完成 → 按映射上传到供应器"。
- 重点怀疑方向：delegate 容器在**同一 tick 被重复 `detectAndSendChanges`**、`ContainerInterfaceTerminal` 构造要求 `IInterfaceTerminal` 强转、Baubles bridge 路径的 `ContainerOpenContext` 槽位。

**涉及文件**：`inventory/gui/GuiType.java`、`client/gui/container/ContainerComprehensiveWorkTerminal.java`、
`client/gui/GuiComprehensiveWorkTerminal.java`、`client/gui/GuiWirelessDualInterfaceTerminal.java`、`util/Util.java`。

**验收**：终端内能看到供应器行、能放入样板；关 GUI 重开仍在；启动日志无异常。

### P0-2 复制 / 替换样板 ⬜

**目标**：把样板缓存 / 背包里的样板复制到编码区或另一槽位；用当前编码结果替换已有样板。
**要点**：纯 NBT 搬运，服务端执行，复用 `IPatternContainer` 加两个方法（如 `copyPattern(fromSlot, toSlot)`）。
注意 rv3-1050 的坑：判定样板有效性必须走 `ICraftingPatternItem#getPatternForItem` 虚分发，禁止硬编码 `new PatternHelper(...)`。
**验收**：缓存槽样板 → 一键复制到编码输出；替换后 NBT 与原样板一致。

### P0-3 主产物轮换独立按钮 ⬜

**目标**：给已实现的 `rotateOutputs()`（现挂在 ⇄ 键上）补一个独立图标入口。
**要点**：新增 `GuiWcwtStateButton`/`GuiImgButton`，发 `PatternTerminal.SwapOutputs`（协议已存在）。**零新协议**。
**验收**：点击后三输出槽轮转一格，⇄ 键行为不变。

---

## 三、P1（按性价比排序）

### P1-4 显示过滤按钮组 ⬜
**目标**：终端内切换 物品 / 流体 / 其他类型 显示，以及视图单元（ViewCell）开关。
**现状**：容器的 view cells 过滤链路已通（`ContainerComprehensiveWorkTerminal` 已引用 ViewCells），缺 GUI 侧按钮与设置同步。
**要点**：参考双接口终端自己的 `typeFilters` 实现，用 `@GuiSync` 或 `Settings` 通道同步；按钮复用 `GuiWcwtStateButton`。

### P1-5 收藏夹 ⬜
**目标**：终端内标记常用物品，单独视图/过滤展示。
**要点**：NBT 存物品清单（同终端持久化），列表侧加一个 filter 开关。

### P1-6 补货 Restock ⬜
**目标**：把背包某物品补到指定数量（从 ME 网络拉取）。
**要点**：服务端执行，需要"数量输入 + 目标槽位"；可复用 `GuiAmount` 的数量输入交互。

### P1-7 垃圾桶 ⬜
**目标**：终端内销毁物品。
**要点**：小控件 + 一个服务端销毁动作；注意与世界交互的权限校验。

### P1-8 磁吸（终端内开关）⬜
**现状**：本 mod 已有磁吸核心（`api/MagnetObject.java`、`Constants.MAGNET_MODE_KEY`），缺终端内界面/开关。
**要点**：把既有磁吸开关暴露成终端按钮 + 状态同步即可，无需重写吸取逻辑。

---

## 四、P2

### P2-9 样板改名（终端内）⬜
本 mod 已有 `GuiRenamer`，把它接进综合终端（或复用铁砧命名逻辑）即可。

---

## 五、⛔ 不适用（1.7.10 无对应，不再评估）

- 切石机 / 锻造台样板模式（1.7.10 无这两种机器）
- 流体替换开关（1.7.10 AE2 无此 API）
- Curios / CosmeticArmor 面板（可用 Baubles 桥接替代，属新功能非移植）
- AE2 工具箱面板、滚动升级面板、Polymorph 配方冲突兼容
- JEI/EMI 专用通道（NEI 已覆盖基本搬运）
- 依赖特定 mod 的兼容项（ExtendedAE / AppliedMekanistics / LightningTech 等）

---

## 六、工作流备忘（每次改代码）

1. 改代码 → 先 `compileJava` 验证 → 再 `spotlessApply` → 再 `compileJava` 复验（顺序不能反，spotless 会删解析失败的 import）。
2. 判断编译结果只看 `BUILD SUCCESSFUL` / `BUILD FAILED`，不要用管道后的 `$?`。
3. 重启客户端前必须**先杀掉 runClient 进程**（否则 gradle 全局锁报 `FileAccessTimeJournal` 错误）。
4. 同一文件的多次 Edit **必须串行**发送，否则后一次会覆盖前一次的改动且都报成功。
