# Step4 · 01 Span 与锚点（本质）

> TEACH 第 1 步：一句话本质 ＋ 第 2 步：俯瞰框架。

## 一句话本质

> **SourceMapping = 一张对照表，让「我看到的这行代码」与「真正执行的那条字节码」互相指认。**

## Span：一段映射的基本单位

一个 `Span` 同时记录三侧信息（本项目 `SourceMapping.Span`）：

```text
Span
├── 生成侧：generatedStartLine/Col → generatedEndLine/Col    （渲染出的文本坐标）
├── 目标侧：insnStart → insnEnd                              （字节码指令区间）
└── 溯源侧：sourceLine                                        （原始源码行，可选）
```

> 「半开区间」约定：`insnEnd` 是**不含尾**的（与 `List.subList` 一致）。统一半开区间
> 能避免大量「差一」错误——这是卡点 ⚠️2。

## 卡点 ⚠️1：锚点不是事后补的

**错误直觉**：先渲染出整段文本，再用正则去文本里找「这行对应哪条指令」。
**为什么错**：渲染文本里的行号、结构是打印的产物，反解析它既脆弱又易错。

**正解**：锚点从 AST 出生就带着。本项目每个 `AstNode` 都记录：

```java
private final int firstInsn;   // 我由第几条指令开始生成
private final int lastInsn;    // 到第几条指令结束
```

于是渲染器只要在**写每一行的同时**，把该行与该节点的区间登记进映射即可——见
[02 边渲染边映射](02-边渲染边映射.md)。**映射是渲染的副产品，不是二次解析的结果。**

## 俯瞰：从哪来、到哪去

```text
AstNode(firstInsn,lastInsn)
        │ 渲染时携带
        ▼
JavaAstRenderer.emitLine(node, text)
        │ 登记一个 Span
        ▼
DefaultSourceMapping  ──►  atGenerated(line,col) → Span
                      └──►  atInsn(idx) → [Span...]
```

## 现实场景

- **反编译器**：点击源码跳字节码（本项目 Phase4）。
- **调试器**：断点行 ↔ 机器指令。
- **教学**：解释「这行 Java 编译成了什么」。
- **覆盖率工具**：报告「哪行执行了」需要原始行映射（`LineNumberTable`）。

## 一句话记住

```text
Span = 生成坐标 + 指令区间 + 原始行。
映射的锚点，来自 AST 节点天生携带的 firstInsn/lastInsn。
```

- [下一步：02 · 边渲染边映射](02-边渲染边映射.md)
