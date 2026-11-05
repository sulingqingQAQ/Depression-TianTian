# Depression-TianTian

语言：**| [English](README.md) | > 简体中文 < |**

![banner](banner.png)

> 本仓库为 **Depression-TianTian（DTT）1.21.1（NeoForge / Fabric）移植版**。原作者为 **GRAINALCOHOL**（原仓库：[GRAINALCOHOL/depression-tiantian](https://github.com/GRAINALCOHOL/depression-tiantian)），本分支（`neoforge-1.21.1`）的移植与修复由 **sulingqingQAQ** 完成。其余版本与更新请以原作者发布为准。

## 📌 前置

| 依赖 | 说明 |
|---|---|
| [Depression](https://github.com/sulingqingQAQ/Depression) | **必需**，建议 0.2.3+1.21.1（配套移植版） |
| Architectury API | **必需**，13.0.11（请勿使用不兼容的大版本） |
| Fabric API | Fabric 端必需（Loader 0.18.4 / API 0.116.15） |
| NeoForge | NeoForge 端 21.1.219 |
| Java | 21 |

规划联动起源（Origins），规划兼容 AppleSkin。

## 📑 介绍

Depression-TianTian（以下简称本模组）是一个扩展抑郁症（Depression）模组（以下简称原版模组）的附属模组。

它修改了原版模组的一部分内容，增加了更多可配置选项，最重要的是**增强了日记系统**，也可以作为一个**库模组**供其它创作者使用。

它的诞生原因之一，是原作者认为原版模组的日记内容太少、文案表演感太重，于是"报复性"地添加了大量日记内容。

## 💖 玩家向功能

### 增强的日记生成系统

- 根据玩家**当日行为数据**（击杀怪物、驯服动物、宠物死亡、死亡次数等）与**心理健康状态**（健康 / 轻度 / 中度 / 重度抑郁 / 躁狂相）动态生成个性化日记。
- 日记由开头、天气、话题、通用、结尾等段落组装，包含话题与感受等多维度内容，并支持**变体**避免重复。
- 话题带权重计算（今日 / 昨日 / 长期统计三层），同一话题不会连续反复出现。
- 提供**温柔模式**等日记相关配置。
- ⚠️ 增强日记由配置项 `enhanced_diary_generator` 控制（**默认关闭**），需要开启后生效。

### 提示消息系统

特定情境下向玩家发送沉浸式提示文案，例如：

- 身处黑暗、淋雨、在宠物附近、在唱片机旁、重置重生点；
- 厌食及其加重；
- PTSD 形成 / 发作 / 极端 / 缓解 / 消散各阶段。

消息带冷却与发送节奏控制，不会刷屏。

### 躯体化症状强化

- 新增**厌食**状态效果；抑郁严重时隐藏饥饿值栏，让心理问题的躯体表现更直观。

### 游戏性保护机制

- 双脚离地时自动阻止闭眼（避免摔落等意外）；
- 战斗状态下可配置阻止闭眼 / 打盹；
- 紧张性木僵可配置为不锁键盘（改为给予负面效果）。

### 更多恢复途径与高度可配置

- 附近方块、宠物、唱片机的情绪恢复机制均可配置（间隔、模式）；击杀怪物也可恢复情绪。
- 心理医生商品的价格 / 库存 / 解锁等级可配置。
- PTSD 触发距离、恢复速率可配置。
- 精神特质选择界面可禁用。
- 配置为**服务端 / 客户端分离的 JSON 文件**，首次运行自动生成默认配置。

## 🗺️ 开发者向（库模组）

本模组同时是一个让原版模组代码更易用的库模组，并对日记系统做了系统性重构，方便添加或修改内容。详见[原 WIKI 页面](https://github.com/GRAINALCOHOL/depression-tiantian/wiki)。

- **事件系统**（`api/event/`，基于 Architectury Event）：精神健康变化、双相极性切换、PTSD 各阶段（部分可取消）、厌食 / 进食打断 / 闭睁眼 / 精神疲劳 / 失眠、战斗与禅定状态进出等事件。
- **包装类**（`api/wrapper/`）：`MentalHealthStatus`、`MentalIllnessStatus`、`PTSDLevel`、`Severity`、`EmotionLevel` 等语义化枚举，替代魔法数值。
- **辅助类**（`api/helper/`）：`MentalStatusHelper`、`PTSDHelper`、`EmotionHelper`。
- **日记扩展点**：注册表化的话题（essential / stat）、统计键与提示消息（`DTTRegistries`），可注册自己的日记话题与统计来源。

## ❔ 版本号含义

- 0.0.x ~ 0.1.x：开发版本
- 0.2.x：增强的日记系统未实装版本
- 0.3.x：正式版本，内容已基本完善
- 本移植分支版本：**1.0.1+1.21.1**（移植版本号，非上游版本线）

## 💭 FAQ

**Q: 其它 Minecraft 版本？**
A: 上游目前没有这样的打算，但鼓励第三方移植（本仓库即为 1.21.1 移植）。

**Q: Forge / NeoForge / Quilt 版本？**
A: 上游原本仅 Fabric；**本移植分支已提供 NeoForge 与 Fabric 双端构建**。

**Q: 增强日记没生效？**
A: 检查服务端配置中的 `enhanced_diary_generator` 是否已设为 `true`（默认关闭）。

## ℹ️ 其它 & 鸣谢

- DTT 原作者：**GRAINALCOHOL**，原仓库：[GRAINALCOHOL/depression-tiantian](https://github.com/GRAINALCOHOL/depression-tiantian)。
- Depression 作者：[Block-137（MC百科）](https://www.mcmod.cn/author/32591.html)。
- 美术资源：[食猫兽杨子默（MC百科）](https://www.mcmod.cn/author/32592.html)。
- 文案贡献者：[泡芙Orz（MC百科）](https://www.mcmod.cn/author/40349.html)、[李天昊（MC百科）](https://www.mcmod.cn/author/33502.html)、[ki_ecao（MC百科）](https://www.mcmod.cn/author/40348.html)。
- 模组名字来源于一首音乐：[TIAN TIAN](https://music.163.com/song?id=2707332868)。
- 本分支（`neoforge-1.21.1`）移植与修复：**sulingqingQAQ**（含 NeoForge 适配、英文本地化编码修复等，详见提交记录）。
- 许可证：GPL-3.0。
