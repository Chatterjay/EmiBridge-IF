# EmiBridge: Industrial Foregoing

**Industrial Foregoing 配方适配器，将 JEI 配方桥接到 EMI，支持 NeoForge 1.21.1**

作为 EmiBridge 框架的适配器模组，将 Industrial Foregoing 的机器配方从 JEI 翻译为 EMI 配方格式。

## 功能

- **配方桥接** — 将以下 IF 机器配方显示在 EMI 中：
  - 消融室（Dissolution Chamber）
  - 流体提取器（Fluid Extractor）
  - 激光钻机（Laser Drill Ore/Fluid）
  - 石材工厂（Stone Work）
  - 生物反应器（Bio-reactor）
  - 机器产出（Machine Produce）：胶乳处理、污泥精炼、堆肥机等
  - 洗矿/发酵/筛矿
- **内置黑名单** — 默认跳过 EMI 已原生支持的配方分类，可通过配置文件调整
- **中文本地化** — 完整的 EMI 分类名中文翻译

## 依赖

- EmiBridge 1.0-SNAPSHOT
- Industrial Foregoing 3.6.38+
- NeoForge >=21.1.230
- JEI 19.27.0.340+
- EMI 1.1.24+1.21.1

## 内置黑名单

默认跳过的配方分类（在 `IndustrialForegoingAdapter_blacklist.json` 中配置）：

- `/fluid_extractor`
- `/stone_work`
- `/dissolution`
- `/laser_ore`
- `/stone_work_generator`
- `/laser_fluid`

可通过 `emibridge-common.toml` 的 `adapterBlacklist` 补充更多黑名单。

## 命令

详见 EmiBridge 核心模组的 `/emibridge` 命令。

## 许可证

GNU AGPL 3.0
