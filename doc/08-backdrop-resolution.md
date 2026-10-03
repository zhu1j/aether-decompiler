# 08 · 背景分辨率自适应

> 分类：布局 / 几何 ｜ 轮次：第三轮 ｜ 状态：已修复

## 现象

背景图像与窗口比例不一致时：要么被**拉伸变形**，要么**留有黑边**，不够「贴合、自然」。
用户希望背景随窗口分辨率自适应。

## 根因

初版把 `ImageView` / `MediaView` 的 `fitWidth` / `fitHeight` 直接 **bind** 到窗口宽高，
且 `preserveRatio = false`——即**始终强制拉伸**，丢失宽高比。缺少按比例计算的填充策略。

## 解决方案

引入三种填充模式，并在窗口尺寸变化时**重算几何**：

| 模式 | 语义 | 计算 |
|------|------|------|
| **铺满窗口**（默认，cover） | 保持比例放大到完全覆盖，溢出裁掉 | `scale = max(w/iw, h/ih)` |
| **拉伸填充** | 强制拉满，可能变形 | `fit = w × h` |
| **等比适应**（contain） | 保持比例完整可见，可能留边 | `scale = min(w/iw, h/ih)` |

```mermaid
flowchart TD
    R[窗口尺寸变化] --> F[applyFillLayout]
    F --> M{填充模式?}
    M -- cover --> C[scale=max(w/iw,h/ih)<br/>放大覆盖 + 裁剪框裁边]
    M -- stretch --> S[fitWidth=w, fitHeight=h]
    M -- contain --> K[scale=min(w/iw,h/ih)<br/>完整可见]
    C --> A[设置 ImageView/MediaView 尺寸]
    S --> A
    K --> A
```

关键工程点：

- `rootStack.width/heightProperty()` 变化时触发 `applyFillLayout()`；
- 媒体真实尺寸：`Image.getWidth/Height`、`MediaPlayer.getMedia().getWidth/Height`；
- **裁剪框**：cover 模式会溢出窗口，用 `bgClip`（`Rectangle` 绑定窗口宽高）作
  `mediaHolder.setClip(...)`，把溢出部分裁掉，避免画面外溢；
- 网页壁纸 `WebView` 直接铺满窗口。

## 边界处理

- 媒体尺寸未知（`iw/ih ≤ 0`）时退回窗口尺寸，避免除零；
- 尺寸变化监听只重算几何，不重新解码媒体，开销极小。

## 验证

- 拖动窗口大小，背景始终贴合窗口、比例正常、无变形；
- 切换三种模式行为符合预期。

## 教训

> 「自适应」不是「拉伸填满」，而是**按比例 + 裁剪/留边**三选一；默认选 cover 最贴合自然。
