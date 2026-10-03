# 15. AST 占位符（`/*?*/`）与 GUI 观感 / 联动修复

> 现象 → 根因 → 方案 → 验证。本文记录一次“一个符号害了一整片输出”的典型排障。

## 现象

分析视图输出的 AST→Java 里到处是占位符：

```java
v0.Object.<init>();
if (v1 != null) {
    /*?*/v0.d.a = v1;              // 字段写入带 ? 前缀
    return;
} else {
    new java.lang.IllegalArgumentException().IllegalArgumentException.<init>();  // new 未合并
    throw /*?*/?;                  // 裸 ?
}
...
return /*?*//*?*/v0.d.a[v1].e.a;   // 字段链两个 ?
```

同时用户反馈：字节码 / 分析视图在深色皮肤上是刺眼的**灰白底**，且点击源码右侧「小窗口」无任何联动。

## 根因（一个点号，三处连锁）

关键在操作数分隔符。`AsmClassParser` 把方法 / 字段操作数拼成 **`owner.name desc`**（`owner` 内部才用斜杠），例如：

```
java/lang/Object.<init> ()V
com/Fixture$Node.e Lcom/Fixture$Leaf;
```

但 `ExpressionBuilder` 早期版本用**斜杠**去切分简单名：

```java
int slash = sig.lastIndexOf('/');
name = sig.substring(slash + 1);   // → "Object.<init>" 而不是 "<init>"
```

于是三处连锁失效：

| 症状 | 直接原因 |
|------|----------|
| `new` 未合并成 `new T(...)` | `"<init>".equals(name)` 恒为 false（name 是 `Object.<init>`） |
| 字段名被塞进类前缀（`.IllegalArgumentException.<init>`、`Node.e`） | 字段简单名从斜杠后取，多带类名 |
| `throw /*?*/?;` | `<init>` 判断失败 → 方法返回值不压栈 → `athrow` 弹到空栈 → 兜底 `Opaque("?")` |

而 `/*?*/` 前缀本身来自 `Expr.Opaque.render()` —— 它是「未能精确重建」的**标记**，不是乱码：设计上用来提示“此处表达式是近似还原”。

## 方案

1. **统一分隔符为点**（根因修复）：`buildCall` 与 `fieldOwner` / `simpleField` 全部按 `owner.name:desc` 切分。
2. **补真实表达式节点**，替掉 `Opaque` 兜底：
   - `Expr.FieldAccess`（getfield/getstatic/putfield/putstatic）
   - `Expr.ArrayLength`（`arraylength`）
   - `Expr.NewArray`（`newarray`/`anewarray`）
3. **构造合并**：`new` → 紧随的 `<init>` 调用合并为 `new T(args)`；`this` 上的 `<init>` 渲染为惯用 `super(...)`。
4. **`Opaque` 去掉 `/*?*/` 前缀**（保留兜底能力，但不污染观感）。
5. **字符串常量加引号**，`new` 用简单类名（IDEA 风格观感）。

## 观感与联动修复

| 问题 | 方案 |
|------|------|
| 字节码 / 分析区灰白底 | 分析区补 `.analysis-text` 皮肤令牌样式；字节码行改用 `TextFlow` 分色（偏移暗 / 索引强调 / 助记符次强调加粗 / 操作数正文），全部基于 `-ae-*` 令牌 → 6 套皮肤自适应 |
| 流水线看不出变化 | `InspectorView.setPipelineState()` 按真实产物动态点亮 SSA/AST（此前写死只亮前 3 段） |
| 点源码右侧无反应 | 新增 `InspectorView.setSelection()` + `BytecodeView` 选中回调，打通「源码 → 字节码 → 检查器」**双向**联动 |

## 验证

用与用户截图同形态的样例（字段链 + 数组 + 构造抛异常）跑通流水线：

```
CONTAINS_PLACEHOLDER=false      // /*?*/ 零残留
CONTAINS_STRAY_QMARK=false      // 裸 ? 零残留
字段链正确：v0.d[v1].e.a
构造正确：super(); / new IllegalArgumentException("null")
```

全量 `javac`（api + core + plugins + cli + gui，79 文件）EXIT=0。

## 教训

> 解析层拼出的“展示字符串”和消费层按什么分隔符解析，必须由**同一处契约**约束。
> 一个分隔符不一致，会穿透到 `new` 合并、字段命名、栈平衡三处，表象各异却同源。
> 排障要点：先在**真实字节码**（`javap -c`）确认指令序列，再回到拼接处核对契约，而不是逐个症状打补丁。
