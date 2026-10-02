# 12 · 任务进度条（空闲灰条 → 真实进度）

> 分类：交互 / 并发 ｜ 轮次：第四轮 ｜ 状态：已修复

## 现象

底部状态栏里有一条**灰色的进度条**，但**每个任务运行时它都不动**——永远是一条空灰条，
看不出任何进度。

## 根因

两个问题叠加：

```mermaid
flowchart TD
    A[进度条常驻显示] --> B[空闲时 ProgressBar(0) 渲染为空灰条]
    C[任务未上报进度] --> D[task.progress 恒为 -1/未绑定]
    B --> E[“灰色没用的条子”]
    D --> E
```

1. **空闲不隐藏**：进度条一直可见且进度为 0，表现为一条多余的灰条；
2. **未绑定/未上报**：后台任务没有调用 `updateProgress(...)`，也没有把
   `task.progressProperty()` 绑到进度条，于是永远没有填充。

## 解决方案

引入统一的「开始/结束」封装，并让各任务按需选择**确定进度**或**不确定（滚动）进度**：

```java
/** 开始任务：indeterminate=true 用滚动动画；否则绑定真实进度。 */
private void beginProgress(Task<?> task, boolean indeterminate) {
    progress.progressProperty().unbind();
    if (indeterminate) {
        progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS); // 滚动动画
    } else {
        progress.setProgress(0);
        progress.progressProperty().bind(task.progressProperty()); // 真实进度
    }
    progress.setVisible(true);
    progress.setManaged(true);   // 占位，避免布局跳动
}

/** 任务结束：隐藏并复位。 */
private void endProgress() {
    progress.progressProperty().unbind();
    progress.setProgress(0);
    progress.setVisible(false);
    progress.setManaged(false);
}
```

接入点：

| 任务 | 进度类型 |
|------|----------|
| 反编译当前类 | 不确定（滚动） |
| 导出全部源码 | **确定**（`updateProgress(done, total)`） |
| 扫描背景库 | 不确定（滚动） |
| 导入背景工程 | 不确定（滚动） |

进度条样式（`base.css`）：

```css
.stage-progress { -fx-accent: -ae-accent; }
.stage-progress > .bar   { -fx-background-color: -ae-accent; }  /* 填充色 = 主题强调色 */
.stage-progress > .track { -fx-background-color: -ae-border; }  /* 轨道色 */
```

## 验证

- 空闲时状态栏**没有**灰条；
- 导出时进度条按文件数**逐步填充**（强调色）；
- 反编译/扫描/导入时显示**滚动动画**，任务结束即隐藏。

## 教训

> 进度反馈的三要素：**该显示时才显示、有真实进度就上报、没真实进度就用不确定动画**。
> 否则进度条只会成为一条「没用的灰条」。
