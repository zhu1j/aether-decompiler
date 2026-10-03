# Step3 · 01 为什么需要 AST（本质）

> TEACH 第 1 步：一句话本质 ＋ 第 2 步：俯瞰框架。

## 一句话本质

> **AST = 把「一堆跳转指令」翻译成「一棵有层次的语句树」。**

## 从「线性」到「层次」的痛

字节码是**线性**的：一串指令，靠 `goto`/`if_icmpgt` 的**目标序号**来回跳。

```text
  0: iconst_0
  1: istore_1
  2: iload_0
  3: ifle 14        ; 若 n<=0 跳到 14
  6: ...            ; 循环体
 10: goto 2         ; 跳回条件
 14: iload_1
 15: ireturn
```

人脑读它，需要自己把 `goto 2` 和 `ifle 14` 在脑子里「折叠」成：

```java
int sum = 0;
for (int i = 1; i <= n; i++) { sum += i; }
return sum;
```

**AST 就是把这次折叠显式地做出来**：把「线性 + 跳转」还原成「嵌套 + 层次」。

## 一条硬性设计原则：AST 不含关键字

本项目的 `AstNode` 只描述**结构**，绝不包含 `if`/`while`/`;` 这些**打印细节**：

```java
// Expr 里是这样：
new Expr.Binary(insn, "+", left, right)      // 只记「加」，不记「a + b」

// 而不是这样（错误示范）：
"a + b;"                                      // 混入了语法记号
```

为什么？因为**同一棵 AST** 可以被渲染成 Java、Kotlin、伪代码或 DOT 图。一旦把 Java 的
关键字焊进节点，就失去了这种可能性。渲染是 `RenderPlugin`（`JavaAstRenderer`）的职责。

> 故事类比：AST 是建筑结构的抽象模型（墙、梁、门洞），它不说「这是砖砌的」；
> 用什么材料打印，是打印机的事。

## 俯瞰：两类节点

```text
AstNode（公共基类，携带 firstInsn / lastInsn）
├── Expr   求一个值：Const Local Binary Unary Assign Call New Cast
│          ArrayLoad ArrayStore InstanceOf Cond Incr Opaque
└── Stmt   做一件事：Block ExprStmt If While DoWhile Switch
           Return Throw TryCatch Label Goto Nop
```

**为什么每个节点都带指令区间？** 这是为 Phase4 源码映射埋的锚点：
只要节点还记得「我是由第几条到第几条指令生成的」，就能在「生成文本 ↔ 字节码」
之间做双向跳转。**映射不是事后补的，而是从 AST 出生就带着的基因。**

## 现实场景

- **反编译器**：必须有 AST 才能输出像样的源码（本项目 Phase3）。
- **IDE 重构**：重命名/提取方法，本质是对 AST 做变换。
- **Lint / 静态检查**：规则就是「在 AST 上找某种模式」。
- **代码生成**：先建 AST 再打印，比字符串拼接可靠得多。

## 本项目的落地

```text
com.aetherdecompiler.core.ast.AstNode
   → kind() 返回 IRKind.AST
   → children() 支持遍历整棵树
   → firstInsn()/lastInsn() 供源码映射
```

- [下一步：02 · 表达式重建](02-表达式重建.md)
