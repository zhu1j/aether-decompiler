# 14 · 项目整体总结（aether-decompiler）

> 一份面向「整体把握」的总结：项目是什么、走到哪、怎么构成的、如何继续。
>
> 作者：Jerry Zhu (Zeek) ｜ *Run the Code, Run the World!*

---

## 1. 一句话简介

> **aether-decompiler 是一个用 Java 从零构建的、可插拔的 JVM 反编译引擎 ——
> 从字节码一路走到可读的源码结构，SSA 与 AST 都是自己实现的。**

它不是又一个「调 CFR 的壳」，而是一台**透明、可教学、可扩展**的分析引擎：CFG、支配树、
SSA、AST、源码映射、流水线缓存与并行，全部在仓库内可见、可读、可改。

---

## 2. 项目定位与价值

| 维度 | 说明 |
|------|------|
| 定位 | 反编译**引擎**（不是成品工具）；内核与前端解耦 |
| 受众 | 学习代码分析技术的人（本项目本身就是学习载体） |
| 卖点 | ASM 隔离干净、模型不可变、插件契约冻结、SSA/AST 自研 |
| 座右铭 | *Run the Code, Run the World!* |

---

## 3. 发展历程（Phase 0 → 5）

```text
Phase0  契约冻结    配置模型 / 异常模型 / SourceMapping 接口 / 4 扩展点
Phase1  控制流地基  字节码→ClassModel；CFG（前导点切分）；支配树（CHK 迭代）
Phase2  SSA         支配边界 + phi 插入 + 变量版本化（Cytron 经典算法）
Phase3  AST         表达式重建（栈模拟）+ 控制结构恢复（回边/汇合点/降级）
Phase4  SourceMapping  Span + 双向查询 + 边渲染边映射
Phase5  流水线       缓存 + 类级并行 + 事件；GUI「分析」视图点亮 SSA/AST
```

---

## 4. 架构总览

```text
┌───────────────────────────── 前端 ─────────────────────────────┐
│  aether-cli（命令行）        aether-gui（JavaFX Studio）        │
│                              └─ 源码/字节码/CFG/分析 四视图       │
└───────────────▲───────────────────────────────▲───────────────┘
                │ 通过引擎/Pipeline 使用内核      │
┌───────────────┴───────────────────────────────┴───────────────┐
│                          aether-core                            │
│   AsmClassParser → ClassModel → CfgBuilder → DominatorTree      │
│        → SsaBuilder(SSA) → AstBuilder(AST) → SourceMapping      │
│        → DecompilationPipeline（缓存/并行/事件）                 │
└───────────────────────────────▲────────────────────────────────┘
                                │ 实现接口
┌───────────────────────────────┴────────────────────────────────┐
│                        aether-plugin-api                        │
│  ClassSourcePlugin · AstTransformPlugin · RenderPlugin ·        │
│  AnalysisPlugin · AetherEvent/EventBus · Options · AetherException │
└───────────────────────────────▲────────────────────────────────┘
                                │ 实现接口
                      aether-plugins（JAR 来源 / DOT / CFR）
```

---

## 5. 核心模块清单

| 模块 | 职责 | 关键类 |
|------|------|--------|
| `aether-plugin-api` | 契约与事件 | `ClassSourcePlugin` `RenderPlugin` `AetherEvent` |
| `aether-core` | 引擎内核 | `AsmClassParser` `CfgBuilder` `DominatorTree` `SsaBuilder` `AstBuilder` `DecompilationPipeline` |
| `aether-plugins/plugin-source-jar` | JAR 类来源 | `JarClassSource` |
| `aether-plugins/plugin-render-dot` | DOT 图渲染 | `DotRenderPlugin` |
| `aether-plugins/plugin-render-java` | Java 渲染（CFR 后端 + 自研渲染） | `NativeJavaDecompiler` `JavaAstRenderer` |
| `aether-cli` | 命令行入口 | `Main` |
| `aether-gui` | JavaFX 工作室 | `AetherLauncher` `AetherStudio` `AnalysisView` |

---

## 6. 关键技术点（本项目自研）

1. **ASM 唯一隔离接缝**：`AsmClassParser` 是唯一接触 ASM 的类。
2. **不可变模型**：所有中间产物构造即冻结 → 缓存/并行安全的根基。
3. **支配树**：Cooper–Harvey–Kennedy 迭代算法。
4. **支配边界 + phi**：Cytron 经典 SSA 构造，含异常边处理。
5. **版本栈前缀重命名**：沿支配树 DFS，栈顶即到达定值。
6. **栈模拟表达式重建**：`Deque<Expr>` 精确模拟操作数栈，Opaque 兜底。
7. **控制结构恢复**：回边识别 + 汇合点求解 + 不可规约诚实降级。
8. **源码映射**：AST 携带指令锚点，渲染时同步产出双向映射。
9. **流水线**：缓存（`putIfAbsent`）+ 类级并行（注入池）+ 事件发布。

---

## 7. 质量保障

- **构建**：Maven 7 模块，`mvn -o clean install` 通过；
- **测试**：CFG + SSA + AST + 流水线单测全部通过；
- **健壮性**：Opaque 兜底 / 降级 / 异常包装 / 空输入不崩；
- **文档**：`doc/` 技术问题档案 14 篇 + `doc/learn/` Step1–5 学习体系。

---

## 8. 当前边界与已知局限（诚实声明）

- 表达式重建为**保守近似**：字段访问、`dup`/`swap`、`switch`、三元/短路布尔尚未精细化；
- 自研 Java 渲染面向**可读性与映射**，不承诺「输出即可编译」；
- 需要「可编译输出」时，走 CFR 渲染插件；
- 未对不可信输入做尺寸上限（生产前应补）。

---

## 9. 如何继续（路线图建议）

| 阶段 | 目标 |
|------|------|
| 近期 | 字段/`switch`/`new` 精细化；短路布尔与三元识别；不可信输入上限 |
| 中期 | 类型推断层（配合 SSA 的栈类型）；AstTransformPlugin 实际范例 |
| 长期 | 自研 Java 渲染达到「接近可编译」；增量分析；多语言输出（Kotlin） |

---

## 10. 学习资料导航

```text
doc/learn/
├── Step1  架构入门（字节码/内核/插件/流水线/GUI/排错）
├── Step2  SSA（支配边界/phi/版本化）   ← 代码分析核心
├── Step3  AST（表达式重建/控制结构恢复） ← 代码分析核心
├── Step4  SourceMapping（双向映射）
└── Step5  流水线（缓存/并行/事件）
doc/
├── 13-code-review.md   整体评审
└── 14-project-summary.md 本文件
```

---

## 11. 一句话收尾

> **从字节码到源码结构，每一层都自己想清楚、写出来、验证过。**
> 这不只是「做一个反编译器」，而是把「代码分析技术」真正内化了一遍。

*Copyright 2026 Jerry Zhu (Zeek) · Apache-2.0 · Run the Code, Run the World!*
