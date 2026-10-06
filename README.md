# AE2实用 (WTCT)

把 AE2 的仓库、手动合成、样板编码与批处理收进一台无线终端，并补上一整套升级卡与可调吞吐的扩展总线。

*Bundles AE2's terminal, crafting, pattern encoding and pattern management into one wireless terminal, plus a full set of upgrade cards and throughput-tunable extended buses.*

面向 **Minecraft 1.7.10 / GT: New Horizons 2.9.0-RC-1** 的 AE2 扩展模组。modid `wtct`。

---

## 功能

### ME 综合工作终端
一件物品装下多套界面：仓库、手动合成、样板编码、批处理合成。需要 **ME 无线收发器**接入网络，支持无线使用。

### 样板缓存区与样板管理区
终端内与样板编码配套的两块区域。

- **缓存区** —— 与编辑槽分离，编码后暂存的样板可在缓存区内滚动整理；支持收藏置顶、数量的零值显示（物品量为 0 时仍显示该物品而非留空）。
- **管理区** —— 顶部是「供应器搜索」框，直接填配方类型键（如 `gt.recipe.assembler`）即可定位接口；映射表维护「配方类型 ↔ 供应器搜索词」，可增删改并落盘保存。
- **自动上传** —— 编码完成的样板按**机器族**自动送进对应供应器。同一台机器的多个电压等级（如基础／进阶／…组装机）算作**一个目标**，取其中空位最多者；不同机器（如组装机与电路组装机）**互不串味**。
- **扩展布局** —— 可把列表放大到占满整块缓存区。

### ME 扩展总线（一个物品三种形态）
`ME 扩展输入总线` ／ `ME 扩展输出总线` ／ `ME 扩展存储总线`

- 过滤槽由原版 9 格扩展到 **63 格**
- 带速度倍率，支持物品与流体
- 输出总线同时支持模糊匹配、矿物词典与缺料自动合成
- 节拍自适应：有货要搬时逐级提速到 **1 tick**，空闲时退到 **60 tick** 看一次

### 升级卡（移植 WCWT / AE2WTLib）
`ME 导入卡` ｜ `ME 导出卡` ｜ `ME 磁铁卡` ｜ `ME 方块拾取卡` ｜ `ME 量子桥卡` ｜ `ME 能源卡`

### ME 无线收发器
为无线终端提供跨维度网络接入。

### 终端充电
手持终端可直接在 **GregTech** 机器上充电（不限电压档），也可走 **IndustrialCraft 2** 通道充电。

---

## 注册物品一览

| 注册名 | 显示名 |
|---|---|
| `item.comprehensive_work_terminal` | ME综合工作终端 |
| `item.ex_io_bus` | ME扩展输入总线 |
| `item.ex_io_bus.export` | ME扩展输出总线 |
| `item.ex_io_bus.storage` | ME扩展存储总线 |
| `item.wcwt_card_import` | ME导入卡 |
| `item.wcwt_card_export` | ME导出卡 |
| `item.wcwt_card_magnet` | ME磁铁卡 |
| `item.wcwt_card_block_picker` | ME方块拾取卡 |
| `item.wcwt_card_quantum_bridge` | ME量子桥卡 |
| `item.wcwt_card_energy` | ME能源卡 |
| `tile.wtct.network_hub` | ME无线收发器 |

---

## 兼容性

| 项 | 版本 |
|---|---|
| Minecraft | 1.7.10 |
| GT: New Horizons | **2.9.0-RC-1** |
| Applied Energistics 2 (Unofficial) | `rv3-beta-1073-GTNH` |
| GregTech 5 Unofficial | `5.09.54.183` |
| AE2FluidCraft-Rework | `1.5.110-gtnh` |
| GTNHLib | `0.11.51` |
| NotEnoughItems | `2.8.145-GTNH` |
| Baubles-Expanded | `2.2.24-GTNH` |

当前版本 **1.0.23-rc1**。版本号格式为 `<主>.<次>.<修订>-rc<N>`，其中 `rc<N>` 指的是**对齐的 GTNH 版本**（`rc1` = GTNH 2.9.0-RC-1），不随改动变化；版本号唯一来源是 `build.gradle.kts` 里的 `extra["modVersion"]`。

---

## 构建

```bash
./gradlew build --offline --no-configuration-cache
```

产物位于 `build/libs/wtct-<版本>.jar`。

> **`libs/` 目录必须保留。** 其中 `hbmaeaddon-1.4-dev.jar` 与 `programmablehatches-0.1.3p50-dev.jar` 是不在 Maven 上的编译期依赖，缺失会直接编译失败。

---

## 来源与致谢

本模组是 **WCWT / AE2WTLib（Minecraft 1.21）** 向 GTNH 1.7.10 的移植，界面布局与升级卡体系参照 1.21 版实现；项目骨架与 Gradle 构建脚本继承自 [asdflj/Wtcts](https://github.com/asdflj/Wtcts)，感谢原作者。

---

## 许可证

[GNU General Public License v3.0](LICENSE)
