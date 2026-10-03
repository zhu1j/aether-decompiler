# Step2 · 02 支配边界与 phi（核心卡点）

> TEACH 第 3 步：微观串行——只解决「phi 该插在哪」这一个卡点。

## 先抛一个猜测（猜测试错）

> 你是不是以为：「只要一个基本块有**多条前驱**，就给它插 phi」？

**方向对，但不精确。** 若真这么做，会在无数「其实只有一个到达版本」的汇合点插入
多余的 phi，既冗余又难看。真正的判据是**支配边界**。

---

## 卡点 ⚠️1：支配边界（Dominance Frontier）

### 定义（读三遍）

> 节点 `d` 属于节点 `b` 的支配边界 `DF(b)`，当且仅当：
> **`b` 支配 `d` 的某条前驱，却 `b` 不严格支配 `d` 本身。**

直观理解：`DF(b)` 是「`b` 的影响力刚好失效的边界」。如果变量在 `b` 被定义，
那么这条定义「管得到」`b` 支配的所有地方；一旦越过 `DF(b)`，就可能出现**另一条**
不受 `b` 支配的路径也来定义同一个变量——那里就是**必须汇合**的地方。

### 为什么它正好是 phi 的位置

- 若变量只在 `b` 定义，而 `b` 支配了到达 `x` 的所有路径 → `x` 处只有一个版本，**不需要 phi**；
- 若存在到达 `x` 的路径不受 `b` 支配 → 该路径可能带着另一个版本 → **`x` 需要 phi**。

而「存在不受 b 支配的路径」正是 `x ∈ DF(b)` 的定义。**证毕。**

### 算法：沿支配树向上爬（Cooper-Harvey-Kennedy）

```text
for 每个有 ≥2 前驱的块 b:
    for 每条前驱 p:
        runner = p
        while runner != idom(b):
            DF(runner) += b
            runner = idom(runner)
```

对照本项目 `DominanceFrontier.of(...)`：

```java
int idomB = dom.immediateDominator(b);
for (int p : preds) {
    int runner = p;
    while (runner != -1 && runner != idomB) {
        if (!df.get(runner).contains(b)) df.get(runner).add(b);
        runner = dom.immediateDominator(runner);
    }
}
```

> ⚠️ **易误判**：这里的「前驱」必须**包含异常边**。本项目把 `successors()` 与
> `exceptionSuccessors()` 合并成 `reachablePredecessors(...)`，否则 try/catch 区域的
> 支配边界会算漏。

---

## 卡点 ⚠️3：phi 操作数要「按来路边对齐」

phi 不是一个「求和」节点，而是一个「**按你从哪条边进来的，就取哪个操作数**」的开关：

```text
B3:  v1_3 = phi(v1_1, v1_2)     // 两条前驱：B1 → 取 v1_1；B2 → 取 v1_2
```

因此 phi 必须持有：

1. **前驱块列表**（顺序敏感）；
2. **与之逐项对应的操作数数组**。

本项目 `PhiNode` 正是这么设计的：

```java
private final List<Integer> predecessorBlocks;
private final SsaVariable[] operands;          // 与上表一一对应

public SsaVariable operandFrom(int predecessorBlock) { ... }   // 按块查操作数
void setOperand(int edgeIndex, SsaVariable operand) { ... }    // 重命名阶段逐边填充
```

> ⚠️ **易误判**：如果某块有前驱 `[B1, B2]`，你却按 `[B2, B1]` 填操作数，
> 结果就完全反了。所以本项目在重命名阶段用 `preds.get(t)` 做**下标对齐**：

```java
List<Integer> tPreds = preds.get(t);
for (int idx = 0; idx < tPreds.size(); idx++) {
    if (tPreds.get(idx) == blockId) phi.setOperand(idx, operand);
}
```

---

## phi 放置：迭代上溯

仅靠 `DF` 还不够——插了 phi 的块本身也是「定义」，它可能又落在别人的支配边界上，
于是要**迭代**：

```text
work = 该槽所有「原始定义块」
while work 非空:
    b = work.pop()
    for d in DF(b):
        if d 还没有该槽的 phi:
            插入 phi(d, 槽)
            若 d 不是原始定义块: work.push(d)     // 传播
```

对照 `SsaBuilder`：

```java
Deque<Integer> work = new ArrayDeque<>(defBlocks.get(s));
while (!work.isEmpty()) {
    int b = work.poll();
    for (int d : df.frontierOf(b)) {
        if (placed.get(s).add(d)) {
            int v = versionCounter[s]++;
            blockPhis.get(d).add(new PhiNode(d, s, v, preds.get(d)));
            if (!defBlocks.get(s).contains(d) && inList.add(d)) work.add(d);
        }
    }
}
```

---

## 小结（一句话记住）

```text
支配边界 = 影响力失效的边界 = 必须汇合的地方 = phi 的家。
phi 的操作数 = 为每条前驱边准备一个「按来路取值」。
```

- [上一步：01 · 为什么需要 SSA](01-SSA本质.md)
- [下一步：03 · 版本化与重命名](03-版本化与重命名.md)
