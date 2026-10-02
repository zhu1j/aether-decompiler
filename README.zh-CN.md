# aether-decompiler

> **"Run the Code, Run the World!"（跑起来吧，代码即世界！）** —— Jerry Zhu (Zeek)

[English](README.md) · **中文**

一个**独立、可复用的 JVM 反编译引擎库**。内核只负责把字节码建模成一个数学对象；
CLI 与 GUI 只是架在其上的应用层，永远不属于内核。

**作者：** Jerry Zhu（Zeek，笔名）· zhujiejava1@gmail.com
**许可：** Apache-2.0 · **语言：** Java 17+ · **构建：** Maven

---

## 目录

- [为什么做这个项目](#为什么做这个项目)
- [架构（严格、单向分层）](#架构严格单向分层)
- [模块](#模块)
- [环境要求](#环境要求)
- [构建](#构建)
- [运行 —— CLI](#运行--cli)
- [运行 —— GUI 工作台](#运行--gui-工作台)
  - [皮肤（Skins）](#皮肤skins)
  - [自定义背景图](#自定义背景图)
- [工程结构](#工程结构)
- [设计约束（不变量）](#设计约束不变量)
- [路线图](#路线图)
- [许可](#许可)

---

## 为什么做这个项目

大多数反编译器都是单体结构：解析、建模、结构化重建和渲染全都挤在一起，"界面"
也被焊死在引擎上。本项目反过来做。内核只拥有**字节码的数学模型**；其余一切可选
能力——类从哪里来、IR 如何变换、结果如何渲染、跑哪些分析——都被下沉到四个固定
扩展点的插件里。CLI 和桌面 GUI 因此只是同一套公共 API 的调用方。

这意味着新增一个前端、一种输出格式或一项分析，都可以**不改内核、不重新编译内核**
地完成。

## 架构（严格、单向分层）

```
应用层    (aether-cli / aether-gui / 未来的 IDE 插件)     <- 仅作为调用方
    |
插件宿主  (Java 原生 ServiceLoader，不引入 Spring/Guava)
    |  固定 4 个扩展点：
    |    ClassSourcePlugin  AstTransformPlugin  RenderPlugin  AnalysisPlugin
    |
内核      (aether-core)   <- 稳定核心，唯一依赖：ASM（已被隔离包装）
    |   model / cfg / ssa / type / ast / engine
    |
ASM       (内核唯一的第三方依赖)
```

依赖方向**严格单向、自上而下**。内核完全不知道应用层的存在；应用层只认识内核与
插件 API 的公开接口。

## 模块

| 模块 | 职责 |
|---|---|
| `aether-plugin-api` | 固定的插件契约。体积极小、零依赖。插件只依赖它。 |
| `aether-core` | 内核：不可变模型、CFG、支配树、引擎。仅依赖 ASM。 |
| `aether-plugins/plugin-source-jar` | `ClassSourcePlugin`，读取 jar / zip / 目录源。 |
| `aether-plugins/plugin-render-dot` | `RenderPlugin`，为控制流图输出 Graphviz DOT。 |
| `aether-cli` | 命令行应用；发现插件、驱动引擎。 |
| `aether-gui` | JavaFX 桌面工作台：RichTextFX 编辑器、三视图联动、可插拔皮肤、自定义背景。 |

## 环境要求

- **JDK 17 或更高**（工具链目标为 17；构建已在 JDK 25 上验证通过）。
- **Apache Maven 3.8+**。
- 查看生成的控制流图需要任意 **Graphviz** 安装（`dot`）。

## 构建

```bash
mvn clean install
```

该命令会构建所有模块、运行单元测试，并为 CLI 与 GUI 生成可直接运行的 fat jar。

## 运行 —— CLI

```bash
java -jar aether-cli/target/aether-cli-0.1.0-SNAPSHOT.jar <input.jar|dir> --outputdir out/
```

CLI 会打印作者 Banner 与座右铭，列出发现的插件，流式输出流水线事件，并为每个方法
写出一份 DOT 文件（`render.dot`）。把它交给 Graphviz 即可查看控制流图：

```bash
dot -Tsvg out/com/example/Foo.class/method.dot -o cfg.svg
```

## 运行 —— GUI 工作台

```bash
java -jar aether-gui/target/aether-gui-0.1.0-SNAPSHOT.jar
```

桌面工作台与 CLI 一样，是内核的纯调用方。它把引擎输出渲染成三个联动视图——
**字节码**、**控制流图**、**RichTextFX 源码视图**——并配有可换肤的外壳、
类树导航器、流水线事件停靠区和检查器面板。

### 皮肤（Skins）

外观与布局完全解耦。`styles/base.css` 负责结构与默认色彩令牌；每套皮肤只覆盖这些
令牌（外加可选的标志性点缀）。内置 5 套皮肤：

| Id | 名称 |
|---|---|
| `midnight-aether` | Midnight Aether（默认） |
| `obsidian-amber` | Obsidian Amber |
| `matrix-green` | Matrix Terminal |
| `nebula-violet` | Nebula Violet |
| `solar-light` | Solar Light |

**自定义皮肤无需重新编译。** 把任意 `*.css` 放进 `~/.aether/skins/`（可附带
`<名称>.skin.properties` 元数据，写 `name` / `author` / `accent` / `bg`），下次启动
就会出现在皮肤选择器里。也可以点击 **⬇ 导入皮肤/背景** 按钮在运行时导入 `.css`
文件：它会被复制进用户皮肤目录并立即选中。

### 自定义背景图

工作台可以在工作区背后显示一张背景图，并以**半透明**形式呈现，从而保证代码始终清晰：

- 点击 **⬇ 导入皮肤/背景**，选择一张 `*.png` / `*.jpg` / `*.jpeg` / `*.gif` /
  `*.bmp` / `*.webp` 图片。它会被复制进 `~/.aether/backgrounds/` 并立即显示。
- 用 **背景透明** 滑块调节背景不透明度——默认 `0.35`，范围 `0.05`–`1.0`。图片与
  工作区之间有一层主题遮罩，用来保持文字对比度。
- **清除背景** 可移除当前背景图。
- 皮肤与背景的选择会记忆在 `~/.aether/studio.properties`。

皮肤的 `.skin.properties` 元数据还可以声明一个默认背景：

```properties
name=My Skin
author=Someone
accent=#7ad0ff
bg=#0b0f14
background=my-backdrop.png
backgroundOpacity=0.30
```

> 注：以上元数据键名沿用英文，以便与配置文件解析保持一致。

## 工程结构

```
aether-decompiler/
├─ pom.xml                       父 POM（署名、ASM 版本、模块声明）
├─ LICENSE                       Apache-2.0
├─ NOTICE                        署名 + 座右铭
├─ README.md                     英文说明（默认）
├─ README.zh-CN.md               中文说明
├─ aether-plugin-api/            固定插件契约（零依赖）
├─ aether-core/                  内核（ASM 是其唯一依赖）
│  └─ src/main/java/com/aetherdecompiler/core/
│     ├─ model/                  不可变字节码模型
│     ├─ asm/                    唯一 ASM 隔离层（AsmClassParser）
│     ├─ cfg/                    CFG 构建器 + 支配树
│     ├─ engine/                 反编译引擎 + 插件宿主
│     ├─ event/                  事件总线
│     └─ source/                 目录 / 内存类来源
├─ aether-plugins/
│  ├─ plugin-source-jar/         jar/zip/目录类来源
│  └─ plugin-render-dot/         Graphviz DOT 渲染器
├─ aether-cli/                   命令行应用
└─ aether-gui/                   JavaFX 桌面工作台
```

## 设计约束（不变量）

- **内核不硬编码任何 Java 语法。** 结构存在于通用模型中；Java 特有的事实由变换插件
  派生。
- **唯一的第三方依赖是 ASM，且它永不外泄。** 所有 ASM 用法都被收敛在唯一一个隔离层
  （`AsmClassParser`）内，因此该依赖可以被替换。
- **模型不可变**，配置是不可变快照（`Options`），由插件填充的 `OptionRegistry`
  生成。
- **类加载是惰性 / 按需的**，通过 `ClassSource` 接口实现——引擎绝不会一次性加载整个
  归档。
- **内核无状态且线程安全。** 并行调度是上层关注点，而不是内核关注点。
- **可观测性通过事件总线**，而不是散落在内核各处的日志。
- **单一根异常**（`AetherException`）搭配稳定的 `ErrorCode` 枚举。

## 路线图

- [x] **Phase 0** —— ASM 薄包装、不可变模型、惰性类来源、引擎、测试
- [x] **Phase 1** —— BasicBlock 切分、CFG、支配树、异常边、DOT 插件
- [ ] **Phase 2** —— SSA + JVM 栈类型推断
- [ ] **Phase 3** —— 结构化重建（消除 goto）
- [ ] **Phase 4** —— 插件生态（lambda、泛型、AST 美化）
- [~] **Phase 5** —— CLI（已完成）+ GUI 工作台（已完成：三视图外壳、可插拔皮肤与
      背景）；缓存与并行调度待做

## 许可

Apache License 2.0 —— 见 [LICENSE](LICENSE)。Copyright 2026 **Jerry Zhu (Zeek)**。

> _"Run the Code, Run the World!"_
