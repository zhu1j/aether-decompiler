# aether-decompiler 技术问题档案

> 本目录系统性汇总 aether-gui 在「从能用 → 好用」三个迭代轮次中修复的每一类技术问题：
> 每篇都给出**现象 → 根因 → 解决方案 → 验证**，并尽量配一张 mermaid 图辅助理解。
>
> 作者：Jerry Zhu (Zeek) ｜ 座右铭：*Run the Code, Run the World!*

---

## 目录

| 编号 | 主题 | 分类 | 一句话根因 |
|------|------|------|-----------|
| [01](01-cfg-redraw.md) | 控制流图重绘 | 可视化 | 顺序堆叠、无分层、边横穿节点 |
| [02](02-backdrop-visibility.md) | 背景「已应用却不显示」 | 渲染/层叠 | 主容器不透明，把壁纸整片盖住 |
| [03](03-java-export.md) | 反编译导出完整 Java 源码 | 功能/IO | 只有单类预览，未落盘为完整工程目录 |
| [04](04-dual-tree-nav.md) | 双目录树（JAR + 反编译输出） | 交互/布局 | 只有输入树，缺少输出目录结构 |
| [05](05-cfg-overlap-fix.md) | 控制流图文字重叠 | 可视化 | 折行高度计算与绘制不一致，文本溢出叠字 |
| [06](06-web-wallpaper.md) | 网页型壁纸黑屏 | 渲染/媒体 | `type=web` 工程需 WebView 渲染，原先无此类型 |
| [07](07-bulk-import.md) | 批量导入大量壁纸卡死 | 并发/性能 | 主线程同步扫描 + 逐项解码缩略图 |
| [08](08-backdrop-resolution.md) | 背景分辨率自适应 | 布局/几何 | 拉伸变形、未按窗口比例贴合 |
| [09](09-javafx-launch.md) | JavaFX 启动类运行报错 | 构建/运行时 | classpath 下 Application 子类不能作主类 |
| [10](10-wallpaper-json-parse.md) | Wallpaper `project.json` 解析错误 | 解析 | 嵌套键 `schemecolor.type` 遮蔽顶层 `type` |
| [11](11-webview-white-veil.md) | 网页壁纸引入的透明白遮罩（回归） | 渲染/层叠 | WebView 未加载时的白页被半透明化成整屏遮罩 |
| [12](12-task-progress.md) | 任务进度条（空闲灰条） | 交互/并发 | 空闲不隐藏 + 任务未上报/绑定进度 |
| [15](15-ast-placeholder-and-gui-contrast.md) | AST 占位符 `/*?*/` 与配色/联动 | 解析/观感/交互 | 操作数分隔符是点而非斜杠，连锁击穿 new 合并与字段命名 |

---

## 按分类速览

```mermaid
mindmap
  root((aether-gui<br/>缺陷档案))
    可视化
      CFG 重绘
      CFG 文字重叠
    渲染与层叠
      背景可见性
      网页壁纸黑屏
    功能与 IO
      导出完整 Java
      双目录树
    并发与性能
      批量导入卡死
    布局与几何
      背景分辨率自适应
    构建与运行时
      JavaFX 启动报错
    解析
      project.json
    回归与交互
      WebView 白遮罩
      任务进度条
```

---

## 迭代轮次与问题对应

```mermaid
graph LR
    A[第一轮<br/>可用性] --> A1[JavaFX 启动排障]
    A --> A2[背景库对话框可交互]
    A --> A3[窗口自适应屏幕]
    A --> A4[导出完整 Java]
    A --> A5[背景可见性]
    B[第二轮<br/>观感增强] --> B1[CFG 重绘]
    B --> B2[双目录树]
    B --> B3[背景透明化规则]
    B --> B4[视频预览兜底]
    C[第三轮<br/>深度修复] --> C1[CFG 文字重叠]
    C --> C2[网页壁纸 WebView]
    C --> C3[批量导入异步化]
    C --> C4[背景 cover 自适应]
    C --> C5[技术文档沉淀]
    D[第四轮<br/>回归修复] --> D1[WebView 白遮罩]
    D --> D2[任务进度条]
```

---

## 修复方法论（可复用的排障套路）

```mermaid
flowchart TD
    S[用户报障 + 截图] --> R1{能否复现?}
    R1 -- 否 --> R2[构造最小样本]
    R1 -- 是 --> R3[定位到具体控件/数据流]
    R2 --> R3
    R3 --> D{根因层次}
    D -- 渲染/层叠 --> V1[检查 styleClass 与层叠顺序]
    D -- 布局/几何 --> V2[核对尺寸计算与绘制是否同一套]
    D -- 并发/性能 --> V3[检查是否在主线程做重活]
    D -- 解析 --> V4[核对嵌套结构解析深度]
    D -- 构建 --> V5[核对运行时约束]
    V1 --> F[最小改动修复]
    V2 --> F
    V3 --> F
    V4 --> F
    V5 --> F
    F --> T[全量构建 + 单测]
    T --> P[推送 + 用户验证]
```

---

## 阅读建议

- 想了解**为什么背景明明「已应用」却看不到**：先读 [02](02-backdrop-visibility.md) 与 [08](08-backdrop-resolution.md)。
- 想了解**图形渲染为什么不叠字**：读 [01](01-cfg-redraw.md) 与 [05](05-cfg-overlap-fix.md)。
- 想了解**桌面程序为什么会被海量文件卡死**：读 [07](07-bulk-import.md)。
- 想了解**JavaFX 工程结构上的坑**：读 [09](09-javafx-launch.md)。
