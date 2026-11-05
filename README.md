# Depression-TianTian

Language: **| >English< | [简体中文](README-zh_cn.md) |**

![banner](banner.png)

> This repository is the **1.21.1 (NeoForge / Fabric) port** of Depression-TianTian (DTT). Original author: **GRAINALCOHOL** (upstream: [GRAINALCOHOL/depression-tiantian](https://github.com/GRAINALCOHOL/depression-tiantian)). The `neoforge-1.21.1` branch is ported and maintained by **sulingqingQAQ**. For other versions and updates, please refer to the upstream releases.

## 📌 Prerequisites

| Dependency | Notes |
|---|---|
| [Depression](https://github.com/sulingqingQAQ/Depression) | **Required**, 0.2.3+1.21.1 recommended (matching port) |
| Architectury API | **Required**, 13.0.11 (do not use incompatible major versions) |
| Fabric API | Required on Fabric (Loader 0.18.4 / API 0.116.15) |
| NeoForge | 21.1.219 on the NeoForge side |
| Java | 21 |

Planned integration with Origins, planned compatibility with AppleSkin.

## 📑 Introduction

Depression-TianTian (hereinafter referred to as this mod) is a mod that extends the Depression mod (hereinafter referred to as the original mod).

It modifies some content of the original mod, adds more configurable options, and most importantly, **enhances the diary system**. It can also serve as a **library mod** for other creators.

One of the reasons for its creation is that the original author felt the original mod's diary content was too limited and the writing felt too performative — so a large amount of diary content was added "retaliatively".

## 💖 For Players

### Enhanced diary generation

- Generates personalized diaries from **daily behavior data** (mob kills, taming, pet deaths, death counts, etc.) and **mental health state** (healthy / mild / moderate / severe depression / manic phase).
- Diaries are assembled from opening, weather, topics, general and closing paragraphs, with **variants** to avoid repetition.
- Topics are weighted from three layers of statistics (today / yesterday / long-term), with history-based deduplication.
- A **gentle mode** and other diary options are available.
- ⚠️ The enhanced diary is gated by the `enhanced_diary_generator` config option (**off by default**) — enable it to take effect.

### Hint message system

Immersive hint texts in specific situations:

- Being in darkness, in rain, near pets, near a jukebox, resetting spawn point;
- Anorexia and its worsening;
- PTSD forming / onset / extreme / remission / dispersal stages.

Messages have cooldowns and pacing control, so they never spam.

### Somatic symptoms

- Adds the **anorexia** status effect; the hunger bar is hidden when depression gets severe, making somatic symptoms more visible.

### Gameplay protection

- Automatically prevents eye closure when the player's feet are off the ground;
- Optionally blocks eye closure / napping in combat;
- Catatonia can be configured to not lock the keyboard (negative effects instead).

### More recovery options & configurability

- Mood recovery from nearby blocks, pets and jukeboxes is configurable (interval, mode); killing mobs can also restore mood.
- Psychologist trades: price / stock / unlock level are configurable.
- PTSD trigger distance and recovery rate are configurable.
- The mental trait selection screen can be disabled.
- **Server/client-separated JSON configs**, generated with defaults on first run.

## 🗺️ For Other Creators

This is also a library mod that makes the original mod's code easier to use, with a systematically refactored diary system. See the [upstream WIKI](https://github.com/GRAINALCOHOL/depression-tiantian/wiki).

- **Events** (`api/event/`, via Architectury Event): mental health changes, bipolar polarity switches, PTSD stages (some cancellable), anorexia / eating interruption / eye close & open / mental fatigue / insomnia, combat and zen state transitions, and more.
- **Wrappers** (`api/wrapper/`): semantic enums such as `MentalHealthStatus`, `MentalIllnessStatus`, `PTSDLevel`, `Severity`, `EmotionLevel` instead of magic numbers.
- **Helpers** (`api/helper/`): `MentalStatusHelper`, `PTSDHelper`, `EmotionHelper`.
- **Diary extension points**: registry-based topics (essential / stat), daily-stat keys and hint messages (`DTTRegistries`) — register your own diary topics and stat sources.

## ❔ Version numbers

- 0.0.x ~ 0.1.x: development versions
- 0.2.x: enhanced diary system not yet implemented
- 0.3.x: release versions, mostly feature-complete
- This port branch: **1.0.1+1.21.1** (port versioning, not the upstream line)

## 💭 FAQ

**Q: Other Minecraft versions?**
A: No upstream plans, but third-party ports are encouraged (this repository is the 1.21.1 port).

**Q: Forge/NeoForge/Quilt versions?**
A: Upstream was Fabric-only; **this port branch provides both NeoForge and Fabric builds**.

**Q: Enhanced diary not working?**
A: Check that `enhanced_diary_generator` is set to `true` in the server config (off by default).

## ℹ️ Other & Credits

- DTT original author: **GRAINALCOHOL** — [upstream repository](https://github.com/GRAINALCOHOL/depression-tiantian).
- Depression author: [Block-137 (MC百科)](https://www.mcmod.cn/author/32591.html).
- Art resources: [食猫兽杨子默 (MC百科)](https://www.mcmod.cn/author/32592.html).
- Text contributors: [泡芙Orz (MC百科)](https://www.mcmod.cn/author/40349.html), [李天昊 (MC百科)](https://www.mcmod.cn/author/33502.html), [ki_ecao (MC百科)](https://www.mcmod.cn/author/40348.html).
- The mod name comes from a piece of music: [TIAN TIAN](https://music.163.com/song?id=2707332868).
- `neoforge-1.21.1` port & fixes: **sulingqingQAQ** (NeoForge adaptation, English localization encoding fixes, etc. — see commit history).
- License: GPL-3.0.
