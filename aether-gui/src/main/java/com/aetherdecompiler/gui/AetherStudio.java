/*
 * aether-decompiler —— 一个独立、可复用的 JVM 反编译引擎。
 * Copyright 2026 Jerry Zhu (Zeek) <zhujiejava1@gmail.com>
 *
 * 依据 Apache License, Version 2.0（下称“本许可证”）授权；
 * 除非遵守本许可证，否则你不得使用本文件。
 * 你可以在以下地址获取本许可证副本：
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，依据本许可证分发的软件
 * 均按“原样（AS IS）”提供，不附带任何明示或默示的担保，
 * 包括但不限于对适销性、特定用途适用性的担保。
 * 关于本许可证下具体权限与限制的表述，请参见本许可证。
 *
 * @author Jerry Zhu (Zeek)
 * “Run the Code, Run the World!”
 */
package com.aetherdecompiler.gui;

import com.aetherdecompiler.api.AetherEvent;
import com.aetherdecompiler.api.AetherVersion;
import com.aetherdecompiler.api.ClassSource;
import com.aetherdecompiler.api.ClassSourcePlugin;
import com.aetherdecompiler.api.IRKind;
import com.aetherdecompiler.api.OptionRegistry;
import com.aetherdecompiler.api.Options;
import com.aetherdecompiler.core.cfg.ControlFlowGraph;
import com.aetherdecompiler.core.engine.DecompilerEngine;
import com.aetherdecompiler.core.engine.DecompilationPipeline;
import com.aetherdecompiler.core.engine.PluginHost;
import com.aetherdecompiler.core.model.Insn;
import com.aetherdecompiler.core.model.MethodModel;
import com.aetherdecompiler.gui.background.Backdrop;
import com.aetherdecompiler.gui.background.BackdropKind;
import com.aetherdecompiler.gui.background.BackdropLibrary;
import com.aetherdecompiler.gui.background.BundledBackdrops;
import com.aetherdecompiler.gui.background.WallpaperProject;
import com.aetherdecompiler.gui.skin.Skin;
import com.aetherdecompiler.gui.skin.SkinManager;
import com.aetherdecompiler.gui.view.AnalysisView;
import com.aetherdecompiler.gui.view.BytecodeView;
import com.aetherdecompiler.gui.view.CfgView;
import com.aetherdecompiler.gui.view.ClassTreeView;
import com.aetherdecompiler.gui.view.CodeEditorView;
import com.aetherdecompiler.gui.view.EventsView;
import com.aetherdecompiler.gui.view.InspectorView;
import com.aetherdecompiler.gui.view.OutputTreeView;
import com.aetherdecompiler.plugins.render.java.NativeJavaDecompiler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Aether Decompiler Studio 窗口。
 *
 * <p>这是桌面工作台的应用层组装根。它持有 {@link DecompilerEngine}、
 * {@link PluginHost} 以及渲染引擎输出的各个部件，并接通三视图联动：点击某一行
 * 源码会高亮对应的字节码行与 CFG 块。</p>
 *
 * <p>它是纯调用方：每一步反编译都走公开的内核/插件 API，因此可以替换为 CLI
 * 或未来的 IDE 插件，而无需改动引擎。</p>
 *
 * <p>本窗口是一个感知背景的外壳。自定义背景 —— 静止图像<em>或</em>循环视频，
 * 包括 Wallpaper&nbsp;Engine 导出的那类壁纸 —— 会作为一层刻意半透明的图层
 * 绘制在工作区之后，并带有用户可调的透明度、模糊与填充模式。由于外壳由 CSS
 * 驱动，自定义 {@code *.css} 样式表与自定义背景都可以在运行期导入，
 * 而无需重新编译任何东西。</p>
 *
 * <h2>启动工作台</h2>
 * <p>规范入口点是 {@link AetherLauncher}，<em>而非</em>本类。本类继承自
 * {@link Application}；当 JavaFX 是通过类路径提供的（就像普通 fat jar 那样）
 * 时，JVM 拒绝直接启动一个 {@code Application} 子类，并以
 * “JavaFX runtime components are missing”中止。{@code AetherLauncher} 是一个
 * 普通类，它唯一的职责就是调用 {@code launch(...)}，这是启动以类路径打包的
 * JavaFX 应用所支持的方式。运行该 jar，或使用随附的 {@code run-gui} 脚本。</p>
 *
 * <p>故事类比：工作台本身。印刷机（内核）与零件供应商（插件）都已交付并安装；
 * 工作台把它们布置成一间人可以真正走进去并工作的房间 —— 并可以在后墙上
 * 挂上任意喜欢的画作或影片。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AetherStudio extends Application {

    /** 背景填充模式：铺满窗口（默认）/ 等比适应 / 拉伸填充 / 原始尺寸。 */
    private static final String FILL_COVER = "铺满窗口";
    private static final String FILL_FIT = "等比适应";
    private static final String FILL_STRETCH = "拉伸填充";
    /** 原始尺寸（1:1）：按源图分辨率 1 比 1 显示，绝不放大，动图最清晰。 */
    private static final String FILL_NATIVE = "原始尺寸 (1:1)";

    private final SkinManager skinManager = new SkinManager(SkinManager.defaultUserSkinDir());
    private final BackdropLibrary backdropLibrary = new BackdropLibrary(BackdropLibrary.defaultRoot());
    private final DecompilerEngine engine = new DecompilerEngine();

    private Stage stage;
    private Scene scene;

    private final CodeEditorView codeView = new CodeEditorView();
    private final BytecodeView bytecodeView = new BytecodeView();
    private final CfgView cfgView = new CfgView();
    private final ClassTreeView classTree = new ClassTreeView();
    private final OutputTreeView outputTree = new OutputTreeView();
    private final EventsView eventsView = new EventsView();
    private final InspectorView inspector = new InspectorView();
    private final AnalysisView analysisView = new AnalysisView();
    private final DecompilationPipeline pipeline = new DecompilationPipeline();

    private final ComboBox<Skin> skinBox = new ComboBox<>();
    private final ProgressBar progress = new ProgressBar(0);
    private final Label statusLabel = new Label("就绪");

    // 背景层。
    private final StackPane rootStack = new StackPane();
    private final StackPane mediaHolder = new StackPane();
    private final ImageView bgImage = new ImageView();
    private final MediaView bgVideo = new MediaView();
    /** 网页型壁纸（Wallpaper Engine type=web）的渲染器。 */
    private final WebView bgWeb = new WebView();
    /** 背景层裁剪框：让“铺满窗口”模式溢出的部分被裁掉，而不是外溢到窗口外。 */
    private final Rectangle bgClip = new Rectangle();
    private final Region scrim = new Region();
    private final Slider bgOpacity = new Slider(0.05, 1.0, Skin.DEFAULT_BACKGROUND_OPACITY);
    private final Slider bgBlur = new Slider(0, 24, 0);
    private final ComboBox<String> bgFill = new ComboBox<>();
    private final GaussianBlur blurEffect = new GaussianBlur(0);
    private final Button clearBackdrop = new Button("清除背景");

    private MediaPlayer videoPlayer;
    private ClassSource currentSource;
    private DecompilerEngine.DecompileResult currentResult;
    private String currentClass;
    private Backdrop currentBackdrop;
    /** 反编译输出的根目录（存放生成的 {@code .java} 文件）。 */
    private Path outputRoot;

    /** 界面是否已完成构建。构建期间皮肤下拉框的初始选择不应触发应用与偏好持久化。 */
    private boolean uiReady;
    /** 填充模式是否应回写偏好。构建期间不写，避免覆盖用户已保存的选择。 */
    private boolean rememberFillChange;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        engine.eventBus().subscribe((AetherEvent ev) -> eventsView.append(ev));

        // 首启播种：把随仓库内置的默认背景图补齐到用户背景库（幂等，不覆盖已有文件）。
        BundledBackdrops.seed(BackdropLibrary.defaultRoot());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(buildTopBar());
        root.setCenter(buildWorkspace());
        root.setBottom(buildStatusBar());

        rootStack.getStyleClass().add("root-stack");
        rootStack.getChildren().addAll(buildBackdropLayer(), scrim, root);

        // 缺陷修复：窗口不再使用固定高度，而是按当前屏幕的“可视区域”自适应，
        // 并预留边距，避免窗口高于屏幕、用户每次运行都要手动调整高度。
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double winW = Math.min(1360, Math.max(900, vb.getWidth() - 80));
        double winH = Math.min(880, Math.max(600, vb.getHeight() - 80));
        scene = new Scene(rootStack, winW, winH);
        classTree.setClassSelectListener(this::openClass);
        outputTree.setFileSelectListener(this::openOutputFile);
        codeView.setLineClickListener(this::onSourceLineClicked);

        loadPrefs();
        refreshOutputTree();
        // 选定并应用初始皮肤：优先使用偏好中保存的皮肤，否则回退到默认皮肤。
        // 必须放在场景创建之后，避免在场景尚未就绪时应用样式表导致空指针。
        Skin initialSkin = skinBox.getSelectionModel().getSelectedItem();
        if (initialSkin == null) {
            initialSkin = skinManager.defaultSkin();
        }
        skinBox.getSelectionModel().select(initialSkin);
        applySkin(initialSkin);
        uiReady = true;

        primaryStage.setTitle(AetherVersion.PROJECT + "  \u00b7  Studio");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 窗口首次显示后再校准一次背景几何：此前场景/布局尺寸可能尚未就绪，
        // 若此时计算，“铺满窗口”会按错误的尺寸得出裁切区域，导致背景只铺满一半。
        primaryStage.widthProperty().addListener((o, a, b) -> applyFillLayout());
        primaryStage.heightProperty().addListener((o, a, b) -> applyFillLayout());
        javafx.application.Platform.runLater(this::applyFillLayout);

        eventsView.append(new AetherEvent(AetherEvent.Phase.PLUGIN, IRKind.BYTES,
                "studio ready \u2014 " + AetherVersion.attribution(), null));
    }

    // --------------------------------------------------------------- 背景

    private Region buildBackdropLayer() {
        bgImage.setSmooth(true);
        bgImage.setPreserveRatio(false);
        bgImage.setVisible(false);

        bgVideo.setPreserveRatio(false);
        bgVideo.setVisible(false);

        // 网页型壁纸渲染器：作为背景时不可交互，且不显示右键菜单。
        bgWeb.setMouseTransparent(true);
        bgWeb.setContextMenuEnabled(false);
        bgWeb.setDisable(true);
        // 关键修复：WebView 在未加载内容时会渲染一张“白页”，叠加半透明后形成整屏
        // 灰白遮罩，把图片/视频壁纸一起盖灰（表现为“有层透明白遮罩”且“应用了没变化”）。
        // 这里默认隐藏，并在页面加载成功后再淡入。
        bgWeb.setVisible(false);
        bgWeb.setOpacity(0);
        bgWeb.getEngine().getLoadWorker().stateProperty().addListener((o, a, b) -> {
            if (b == Worker.State.SUCCEEDED) {
                bgWeb.setOpacity(1);
            }
        });

        mediaHolder.getChildren().addAll(bgImage, bgVideo, bgWeb);
        mediaHolder.getStyleClass().add("bg-layer");
        mediaHolder.setMouseTransparent(true);
        mediaHolder.opacityProperty().bind(bgOpacity.valueProperty());
        mediaHolder.setEffect(blurEffect);

        // 分辨率自适应：窗口尺寸变化时重算三种填充模式的几何。
        rootStack.widthProperty().addListener((o, a, b) -> applyFillLayout());
        rootStack.heightProperty().addListener((o, a, b) -> applyFillLayout());

        // 裁剪框：让“铺满窗口”模式溢出的部分被裁掉，而不是外溢到窗口之外。
        mediaHolder.setClip(bgClip);
        bgClip.widthProperty().bind(rootStack.widthProperty());
        bgClip.heightProperty().bind(rootStack.heightProperty());

        scrim.getStyleClass().add("bg-scrim");
        scrim.setMouseTransparent(true);
        return mediaHolder;
    }

    private void applyBackdrop(Backdrop backdrop, boolean remember) {
        disposeVideo();
        bgImage.setImage(null);
        bgVideo.setMediaPlayer(null);
        bgWeb.getEngine().load(null);
        currentBackdrop = backdrop;

        if (backdrop != null) {
            if (backdrop.kind() == BackdropKind.IMAGE) {
                Image img = loadImage(backdrop.media());
                if (img != null) {
                    bgImage.setImage(img);
                } else {
                    currentBackdrop = null;
                }
            } else if (backdrop.kind() == BackdropKind.WEB) {
                // 网页型壁纸（Wallpaper Engine type=web）：用 WebView 渲染 index.html。
                // 因整目录已复制进背景库，其相对引用的 css/js/img 依然有效。
                if (Files.isRegularFile(backdrop.media())) {
                    if (backdrop.preview() != null && Files.isRegularFile(backdrop.preview())) {
                        Image poster = loadImage(backdrop.preview());
                        if (poster != null) {
                            bgImage.setImage(poster);
                        }
                    }
                    bgWeb.setOpacity(0);
                    bgWeb.getEngine().load(backdrop.media().toUri().toString());
                } else {
                    currentBackdrop = null;
                    statusLabel.setText("网页壁纸入口文件不存在");
                }
            } else if (backdrop.kind() == BackdropKind.VIDEO) {
                // 先铺设预览静帧作为海报，避免视频解码前或解码失败时呈现空白。
                if (backdrop.preview() != null && Files.isRegularFile(backdrop.preview())) {
                    Image poster = loadImage(backdrop.preview());
                    if (poster != null) {
                        bgImage.setImage(poster);
                    }
                }
                try {
                    Media media = new Media(backdrop.media().toUri().toString());
                    videoPlayer = new MediaPlayer(media);
                    videoPlayer.setMute(true);
                    videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    videoPlayer.setOnError(() -> {
                        statusLabel.setText("视频背景播放错误，已回退到预览图");
                        eventsView.append(new AetherEvent(AetherEvent.Phase.PLUGIN, IRKind.BYTES,
                                "video backdrop error: " + backdrop.title(), null));
                    });
                    bgVideo.setMediaPlayer(videoPlayer);
                    videoPlayer.play();
                } catch (RuntimeException ex) {
                    statusLabel.setText("无法播放视频背景（已回退到预览图）: " + ex.getMessage());
                    if (bgImage.getImage() == null) {
                        currentBackdrop = null;
                    }
                }
            }
        }

        // 只显示当前类型的渲染器，避免“白页/空层”叠加形成灰白遮罩。
        boolean isVideo = currentBackdrop != null && currentBackdrop.kind() == BackdropKind.VIDEO
                && videoPlayer != null;
        boolean isWeb = currentBackdrop != null && currentBackdrop.kind() == BackdropKind.WEB;
        bgVideo.setVisible(isVideo);
        bgWeb.setVisible(isWeb);
        bgImage.setVisible(bgImage.getImage() != null);

        boolean active = currentBackdrop != null;
        if (active) {
            if (!rootStack.getStyleClass().contains("bg-active")) {
                rootStack.getStyleClass().add("bg-active");
            }
        } else {
            rootStack.getStyleClass().remove("bg-active");
        }
        clearBackdrop.setDisable(!active);
        bgOpacity.setDisable(!active);
        bgBlur.setDisable(!active);
        bgFill.setDisable(!active);
        if (active) {
            applyFillLayout();
        }
        if (remember) {
            savePrefs();
        }
    }

    private void clearBackdropAction() {
        applyBackdrop(null, true);
        statusLabel.setText("已清除背景");
    }

    private void disposeVideo() {
        if (videoPlayer != null) {
            try {
                videoPlayer.stop();
            } catch (RuntimeException ignored) {
                // 从未启动过的媒体播放器没有什么可停止的。
            }
            videoPlayer.dispose();
            videoPlayer = null;
        }
    }

    /**
     * 依据当前填充模式与窗口尺寸重新计算背景层几何（分辨率自适应）。
     *
     * <p>三种模式：</p>
     * <ul>
     *   <li><b>铺满窗口</b>（默认，cover）—— 保持宽高比放大到完全覆盖窗口，溢出由裁剪框
     *       裁掉；背景始终贴合窗口、不留黑边，也不会被拉伸变形。</li>
     *   <li><b>拉伸填充</b> —— 强制拉满窗口，可能变形。</li>
     *   <li><b>等比适应</b>（contain）—— 保持宽高比缩放到完整可见，可能留黑边。</li>
     * </ul>
     */
    private void applyFillLayout() {
        // 用场景实际尺寸兜底：首次布局前 rootStack 尺寸可能为 0，
        // 若用 1×1 计算几何，背景就会“只铺满一小块/一半”。
        double w = Math.max(1, rootStack.getWidth());
        double h = Math.max(1, rootStack.getHeight());
        if (scene != null) {
            w = Math.max(w, scene.getWidth());
            h = Math.max(h, scene.getHeight());
        }
        String mode = bgFill.getSelectionModel().getSelectedItem();

        Image img = bgImage.getImage();
        double iw = img == null ? 0 : img.getWidth();
        double ih = img == null ? 0 : img.getHeight();

        MediaPlayer mp = videoPlayer;
        double vw = mp == null || mp.getMedia() == null ? 0 : mp.getMedia().getWidth();
        double vh = mp == null || mp.getMedia() == null ? 0 : mp.getMedia().getHeight();

        applyImageGeometry(bgImage, mode, iw, ih, w, h);
        applyMediaGeometry(bgVideo, mode, vw, vh, w, h);

        bgWeb.setPrefWidth(w);
        bgWeb.setPrefHeight(h);
    }

    /**
     * 依据填充模式设置背景图片的几何。
     *
     * 关键修复：铺满窗口（cover）不再依赖 StackPane 居中 + 外层裁剪框，而是把
     * 节点的显示尺寸精确设为窗口尺寸，再用 viewport 把源图居中裁剪到窗口宽高比，
     * 最后拉伸铺满。这样无论窗口与图片比例如何，节点都恰好铺满整个窗口，既不会
     * 出现“只铺满一半/留黑边”，也不会产生形变。
     */
    private static void applyImageGeometry(ImageView iv, String mode, double iw, double ih,
                                           double w, double h) {
        if (iv.getImage() == null) {
            return;
        }
        if (FILL_NATIVE.equals(mode)) {
            // 原始尺寸（1:1）：按源图分辨率显示，绝不放大，动图最清晰。
            iv.setViewport(null);
            iv.setPreserveRatio(true);
            iv.setFitWidth(iw > 0 ? iw : w);
            iv.setFitHeight(ih > 0 ? ih : h);
        } else if (FILL_STRETCH.equals(mode)) {
            iv.setViewport(null);
            iv.setPreserveRatio(false);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
        } else if (FILL_FIT.equals(mode)) {
            // 等比适应：完整可见，允许留边。
            iv.setViewport(null);
            iv.setPreserveRatio(true);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
        } else if (iw <= 0 || ih <= 0) {
            iv.setViewport(null);
            iv.setPreserveRatio(false);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
        } else {
            iv.setViewport(centerCrop(iw, ih, w, h));
            iv.setPreserveRatio(false);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
        }
    }

    /** 视频背景的几何：与图片一致，用 viewport 裁剪实现真正的“铺满窗口”。 */
    private static void applyMediaGeometry(MediaView mv, String mode, double mw, double mh,
                                           double w, double h) {
        if (mv.getMediaPlayer() == null) {
            return;
        }
        if (FILL_NATIVE.equals(mode)) {
            mv.setViewport(null);
            mv.setPreserveRatio(true);
            mv.setFitWidth(mw > 0 ? mw : w);
            mv.setFitHeight(mh > 0 ? mh : h);
        } else if (FILL_STRETCH.equals(mode)) {
            mv.setViewport(null);
            mv.setPreserveRatio(false);
            mv.setFitWidth(w);
            mv.setFitHeight(h);
        } else if (FILL_FIT.equals(mode)) {
            mv.setViewport(null);
            mv.setPreserveRatio(true);
            mv.setFitWidth(w);
            mv.setFitHeight(h);
        } else if (mw <= 0 || mh <= 0) {
            mv.setViewport(null);
            mv.setPreserveRatio(false);
            mv.setFitWidth(w);
            mv.setFitHeight(h);
        } else {
            mv.setViewport(centerCrop(mw, mh, w, h));
            mv.setPreserveRatio(false);
            mv.setFitWidth(w);
            mv.setFitHeight(h);
        }
    }

    /**
     * 计算把源图（iw×ih）按窗口宽高比（w:h）居中裁剪所需的 viewport 矩形。
     * 返回矩形与窗口同比例，配合“拉伸铺满”即可实现无变形的 cover。
     */
    private static Rectangle2D centerCrop(double iw, double ih, double w, double h) {
        double winAspect = w / h;
        double srcAspect = iw / ih;
        if (srcAspect > winAspect) {
            // 源图更宽：裁掉左右两侧。
            double vw = ih * winAspect;
            return new Rectangle2D((iw - vw) / 2.0, 0, vw, ih);
        }
        // 源图更高：裁掉上下两侧。
        double vh = iw / winAspect;
        return new Rectangle2D(0, (ih - vh) / 2.0, iw, vh);
    }

    // ------------------------------------------------------------------- 偏好

    private void loadPrefs() {
        Properties props = readPrefs();
        String skinId = props.getProperty("skin");
        Skin skin = skinId == null ? null : skinManager.byId(skinId);
        if (skin != null) {
            skinBox.getSelectionModel().select(skin);
        }

        String fill = props.getProperty("backgroundFill", FILL_COVER);
        if (bgFill.getItems().isEmpty()) {
            bgFill.setItems(FXCollections.observableArrayList(
                    FILL_COVER, FILL_FIT, FILL_STRETCH, FILL_NATIVE));
        }
        bgFill.getSelectionModel().select(fill);
        applyFillLayout();

        double opacity = parseDouble(props.getProperty("backgroundOpacity"),
                Skin.DEFAULT_BACKGROUND_OPACITY);
        double blur = parseDouble(props.getProperty("backgroundBlur"), 0);
        bgOpacity.setValue(opacity);
        bgBlur.setValue(blur);
        blurEffect.setRadius(blur);

        String outRoot = props.getProperty("outputRoot");
        if (outRoot != null && !outRoot.isBlank()) {
            outputRoot = Path.of(outRoot);
        }

        String bgPath = props.getProperty("background");
        if (bgPath != null && !bgPath.isBlank()) {
            Path p = Path.of(bgPath);
            if (Files.isRegularFile(p)) {
                Backdrop bd = Backdrop.of(p.getFileName().toString(), p, null);
                applyBackdrop(bd, false);
            } else {
                applyBackdrop(null, false);
            }
        } else {
            applyBackdrop(null, false);
        }
    }

    private Properties readPrefs() {
        Properties props = new Properties();
        Path prefs = SkinManager.studioPrefsFile();
        if (Files.isRegularFile(prefs)) {
            try (InputStream in = Files.newInputStream(prefs)) {
                props.load(in);
            } catch (IOException ex) {
                // 忽略损坏的偏好文件；使用默认值。
            }
        }
        return props;
    }

    private void savePrefs() {
        Properties props = new Properties();
        Skin skin = skinBox.getSelectionModel().getSelectedItem();
        if (skin != null) {
            props.setProperty("skin", skin.id());
        }
        if (outputRoot != null) {
            props.setProperty("outputRoot", outputRoot.toAbsolutePath().toString());
        }
        if (currentBackdrop != null) {
            props.setProperty("background", currentBackdrop.media().toAbsolutePath().toString());
            props.setProperty("backgroundOpacity", Double.toString(bgOpacity.getValue()));
            props.setProperty("backgroundBlur", Double.toString(bgBlur.getValue()));
            props.setProperty("backgroundFill", bgFill.getSelectionModel().getSelectedItem());
        }
        try {
            Path prefs = SkinManager.studioPrefsFile();
            Files.createDirectories(prefs.getParent());
            try (OutputStream out = Files.newOutputStream(prefs)) {
                props.store(out, "aether-decompiler studio preferences \u2014 Jerry Zhu (Zeek)");
            }
        } catch (IOException ex) {
            // 持久化偏好是尽力而为的。
        }
    }

    private Image loadImage(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(file)) {
            Image img = new Image(in);
            return img.isError() ? null : img;
        } catch (IOException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ 顶栏

    private Region buildTopBar() {
        Label mark = new Label("\u25c8");
        mark.getStyleClass().add("mark");
        Label brand = new Label(AetherVersion.PROJECT);
        brand.getStyleClass().add("brand");
        Label motto = new Label("\u201c" + AetherVersion.MOTTO + "\u201d");
        motto.getStyleClass().add("motto");

        Label spacer = new Label();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button openJar = new Button("打开 JAR");
        openJar.getStyleClass().add("tool-button");
        openJar.setOnAction(e -> openJarDialog());
        Button openDir = new Button("打开类目录");
        openDir.getStyleClass().add("tool-button");
        openDir.setOnAction(e -> openDirDialog());
        Button decompile = new Button("反编译");
        decompile.getStyleClass().addAll("tool-button", "primary");
        decompile.setOnAction(e -> decompileCurrent());

        // 新增：把整个来源反编译成完整的 .java 文件，按包结构写入输出目录。
        Button exportAll = new Button("导出全部源码");
        exportAll.getStyleClass().addAll("tool-button", "primary");
        exportAll.setTooltip(new Tooltip("将当前来源中的所有类反编译为 .java 文件，输出到设定的输出目录"));
        exportAll.setOnAction(e -> exportAllSources());
        Button setOutDir = new Button("设置输出目录");
        setOutDir.getStyleClass().add("tool-button");
        setOutDir.setOnAction(e -> chooseOutputDir());
        Button openOutDir = new Button("打开输出目录");
        openOutDir.getStyleClass().add("tool-button");
        openOutDir.setOnAction(e -> openFolder(ensureOutputRoot()));

        Button backdropBtn = new Button("\u2b07 背景 / 外观");
        backdropBtn.getStyleClass().addAll("tool-button", "menu");
        backdropBtn.setTooltip(new Tooltip("打开背景库：导入图片、视频或 Wallpaper Engine 工程"));
        backdropBtn.setOnAction(e -> openBackdropDialog());

        skinBox.getStyleClass().add("skin-picker");
        skinBox.setItems(FXCollections.observableArrayList(skinManager.all()));
        skinBox.setCellFactory(v -> new SkinCell());
        skinBox.setButtonCell(new SkinCell());
        skinBox.getSelectionModel().selectedItemProperty().addListener((obs, old, skin) -> {
            // 构建期间的选择不触发应用；只有界面就绪后用户切换皮肤才生效并持久化，
            // 否则会在载入偏好之前把默认皮肤写回偏好文件，覆盖用户已保存的选择。
            if (skin != null && uiReady) {
                applySkin(skin);
                savePrefs();
            }
        });

        HBox bar = new HBox(10, mark, brand, motto, spacer,
                skinBox, backdropBtn, openJar, openDir, decompile,
                exportAll, setOutDir, openOutDir);
        bar.getStyleClass().add("topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // -------------------------------------------------------- 背景对话框

    private void openBackdropDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("背景库 \u00b7 Wallpaper Engine / 图片 / 视频");

        Label title = new Label("背景库");
        title.getStyleClass().add("dialog-title");
        Label hint = new Label("支持 PNG/JPG/GIF 等图片、MP4 等视频，以及 Wallpaper Engine 工程文件夹（含 project.json）。\n"
                + "导入 Wallpaper 工程时请选择<整个工程文件夹>；背景以半透明呈现，可用下方滑块调节透明度、模糊与填充。");
        hint.getStyleClass().add("dialog-hint");
        hint.setWrapText(true);

        // 对话框内的即时反馈：应用/移除/刷新等操作的结果与提示都显示在这里，
        // 让用户明确看到“应用”确实生效（此前按钮被挤出可视区域，看似点不动）。
        Label dialogStatus = new Label("在列表中选择一张背景，然后点击“应用所选”。");
        dialogStatus.getStyleClass().add("dialog-status");
        dialogStatus.setWrapText(true);

        ListView<Backdrop> gallery = new ListView<>();
        gallery.getStyleClass().add("gallery");
        gallery.setCellFactory(v -> new BackdropCell());
        gallery.setPlaceholder(new Label("正在扫描背景库…"));
        // 批量导入大量壁纸时，目录扫描放到后台线程，避免界面长时间无响应。
        reloadGallery(gallery, dialogStatus);

        Button importFile = new Button("导入图片/视频");
        importFile.getStyleClass().add("tool-button");
        importFile.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("导入背景图片或视频");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("图片/视频", "*.png", "*.jpg", "*.jpeg", "*.gif",
                            "*.bmp", "*.webp", "*.mp4", "*.m4v", "*.mov", "*.webm"),
                    new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"),
                    new FileChooser.ExtensionFilter("视频", "*.mp4", "*.m4v", "*.mov", "*.webm"));
            File f = chooser.showOpenDialog(dialog);
            if (f != null) {
                importIntoLibrary(dialog, gallery, f.toPath(), false);
            }
        });

        Button importWe = new Button("导入 Wallpaper 工程");
        importWe.getStyleClass().add("tool-button");
        importWe.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择 Wallpaper Engine 工程文件夹（含 project.json）");
            File dir = chooser.showDialog(dialog);
            if (dir != null) {
                importIntoLibrary(dialog, gallery, dir.toPath(), true);
            }
        });

        Button apply = new Button("应用所选");
        apply.getStyleClass().addAll("tool-button", "primary");
        apply.setOnAction(e -> {
            Backdrop bd = gallery.getSelectionModel().getSelectedItem();
            if (bd == null) {
                dialogStatus.setText("请先在列表中选择一张背景。");
                return;
            }
            applyBackdrop(bd, true);
            String note = "已应用背景: " + bd.title();
            statusLabel.setText(note);
            dialogStatus.setText(note);
        });

        Button remove = new Button("移除所选");
        remove.getStyleClass().add("tool-button");
        remove.setOnAction(e -> {
            Backdrop bd = gallery.getSelectionModel().getSelectedItem();
            if (bd == null) {
                dialogStatus.setText("请先在列表中选择要移除的项。");
                return;
            }
            try {
                backdropLibrary.remove(bd.media());
                if (currentBackdrop != null && currentBackdrop.media().equals(bd.media())) {
                    applyBackdrop(null, true);
                }
                reloadGallery(gallery, dialogStatus);
                dialogStatus.setText("已移除: " + bd.title());
            } catch (IOException ex) {
                statusLabel.setText("移除失败: " + ex.getMessage());
                dialogStatus.setText("移除失败: " + ex.getMessage());
            }
        });

        Button openFolder = new Button("打开背景目录");
        openFolder.getStyleClass().add("tool-button");
        openFolder.setOnAction(e -> openFolder(backdropLibrary.ensureRoot()));

        // 刷新：重新扫描背景目录（用户手动往目录里放了新的工程/图片时很有用）。
        Button refresh = new Button("刷新列表");
        refresh.getStyleClass().add("tool-button");
        refresh.setOnAction(e -> {
            reloadGallery(gallery, dialogStatus);
        });

        HBox importRow = new HBox(8, importFile, importWe, openFolder, refresh);
        importRow.setAlignment(Pos.CENTER_LEFT);

        // 缺陷修复：透明度 / 模糊 / 填充单独占一行，并给标签与控件留足宽度，
        // 中文标签不再被截断成“透…”“等比…”。
        Label opacityCaption = new Label("透明度");
        Label blurCaption = new Label("模糊");
        Label fillCaption = new Label("填充");
        opacityCaption.setMinWidth(Region.USE_PREF_SIZE);
        blurCaption.setMinWidth(Region.USE_PREF_SIZE);
        fillCaption.setMinWidth(Region.USE_PREF_SIZE);
        bgOpacity.setPrefWidth(180);
        bgBlur.setPrefWidth(180);
        bgFill.setPrefWidth(180);
        if (bgFill.getItems().isEmpty()) {
            bgFill.setItems(FXCollections.observableArrayList(
                    FILL_COVER, FILL_FIT, FILL_STRETCH, FILL_NATIVE));
        }
        // 切换填充模式立即重算几何，实现分辨率自适应（默认“铺满窗口”）。
        bgFill.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                applyFillLayout();
                if (rememberFillChange) {
                    savePrefs();
                }
            }
        });
        HBox controls = new HBox(10, opacityCaption, bgOpacity, blurCaption, bgBlur, fillCaption, bgFill);
        controls.setAlignment(Pos.CENTER_LEFT);

        // 缺陷修复：“应用 / 移除”单独一行并右对齐。此前所有控件挤在一行里，
        // 按钮被挤出对话框之外、无法点击，看起来就像“应用点不动”。
        HBox actionRow = new HBox(10, apply, remove);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(actionRow, Priority.ALWAYS);

        VBox box = new VBox(10, title, hint, gallery, importRow, controls, actionRow, dialogStatus);
        box.getStyleClass().add("backdrop-dialog");
        VBox.setVgrow(gallery, Priority.ALWAYS);

        // 缺陷修复：对话框加宽加高，容纳新增的控制行与状态行。
        Scene dscene = new Scene(box, 760, 620);
        skinManager.apply(dscene, skinBox.getSelectionModel().getSelectedItem());
        dialog.setScene(dscene);
        dialog.showAndWait();
    }

    private void importIntoLibrary(Stage dialog, ListView<Backdrop> gallery, Path source, boolean project) {
        // 大批量复制（尤其是整目录的 Wallpaper 工程）放到后台线程，界面保持响应。
        Task<Backdrop> task = new Task<>() {
            @Override
            protected Backdrop call() throws Exception {
                return backdropLibrary.importAny(source);
            }
        };
        beginProgress(task, true);
        task.setOnSucceeded(e -> {
            endProgress();
            Backdrop bd = task.getValue();
            reloadGallery(gallery, null);
            gallery.getSelectionModel().select(bd);
            applyBackdrop(bd, true);
            statusLabel.setText("已导入并应用背景: " + bd.title());
        });
        task.setOnFailed(e -> {
            endProgress();
            Throwable ex = task.getException();
            statusLabel.setText("导入失败: " + (ex == null ? "未知错误" : ex.getMessage()));
        });
        Thread t = new Thread(task, "backdrop-import");
        t.setDaemon(true);
        t.start();
    }

    /**
     * 在后台线程重新扫描背景库，完成后回到 JavaFX 线程刷新列表。
     *
     * <p>用户一次粘贴大量 Wallpaper 工程时，目录扫描可能耗时；放到后台可避免
     * 界面“长时间未响应”。</p>
     *
     * @param gallery 要刷新的列表
     * @param status  可选状态标签（可为 {@code null}）
     */
    private void reloadGallery(ListView<Backdrop> gallery, Label status) {
        if (status != null) {
            status.setText("正在扫描背景库…");
        }
        Task<List<Backdrop>> task = new Task<>() {
            @Override
            protected List<Backdrop> call() {
                return backdropLibrary.list();
            }
        };
        beginProgress(task, true);
        task.setOnSucceeded(e -> {
            endProgress();
            List<Backdrop> items = task.getValue();
            gallery.setItems(FXCollections.observableArrayList(items));
            if (currentBackdrop != null && items.contains(currentBackdrop)) {
                gallery.getSelectionModel().select(currentBackdrop);
            } else if (!items.isEmpty()) {
                gallery.getSelectionModel().selectFirst();
            } else {
                gallery.setPlaceholder(new Label("背景库为空：请导入图片、视频或 Wallpaper 工程。"));
            }
            String note = "背景库共 " + items.size() + " 项";
            statusLabel.setText(note);
            if (status != null) {
                status.setText(note);
            }
        });
        task.setOnFailed(e -> {
            endProgress();
            Throwable ex = task.getException();
            String msg = "扫描背景库失败: " + (ex == null ? "未知错误" : ex.getMessage());
            statusLabel.setText(msg);
            if (status != null) {
                status.setText(msg);
            }
        });
        Thread t = new Thread(task, "backdrop-scan");
        t.setDaemon(true);
        t.start();
    }

    private void openFolder(Path dir) {
        try {
            java.awt.Desktop.getDesktop().open(dir.toFile());
        } catch (Exception ex) {
            statusLabel.setText("目录: " + dir);
        }
    }

    // ------------------------------------------------------ 反编译输出 / 导出

    /** 确保输出目录存在，必要时回退到用户主目录下的默认位置。 */
    private Path ensureOutputRoot() {
        if (outputRoot == null) {
            outputRoot = Path.of(System.getProperty("user.home"), "AetherDecompiled");
        }
        try {
            Files.createDirectories(outputRoot);
        } catch (IOException ignored) {
            // 目录已存在或无法创建；后续写入会给出明确错误。
        }
        return outputRoot;
    }

    /** 让用户选择反编译输出目录，并持久化。 */
    private void chooseOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择反编译输出目录");
        if (outputRoot != null && Files.isDirectory(outputRoot)) {
            chooser.setInitialDirectory(outputRoot.toFile());
        }
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            outputRoot = dir.toPath();
            savePrefs();
            refreshOutputTree();
            statusLabel.setText("输出目录: " + outputRoot);
        }
    }

    /** 重新扫描输出目录并刷新“反编译输出”树。 */
    private void refreshOutputTree() {
        outputTree.setRoot(outputRoot);
    }

    /** 在源码视图中打开一个已反编译的 {@code .java} 文件。 */
    private void openOutputFile(Path file) {
        try {
            codeView.setSource(Files.readString(file));
            statusLabel.setText("查看反编译文件: " + file);
        } catch (IOException ex) {
            statusLabel.setText("读取失败: " + ex.getMessage());
        }
    }

    /**
     * 把当前来源中的每个类反编译为完整的 {@code .java} 文件，按包结构写入
     * 输出目录。这是“反编译成一个完整的 Java 工程”这一诉求的核心动作。
     */
    private void exportAllSources() {
        if (currentSource == null) {
            statusLabel.setText("请先打开 JAR 或目录");
            return;
        }
        Path out = ensureOutputRoot();
        ClassSource source = currentSource;
        List<String> names = new ArrayList<>(source.classNames());
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                int ok = 0;
                for (int i = 0; i < names.size(); i++) {
                    String internal = names.get(i);
                    try {
                        java.util.Optional<byte[]> bytes = source.readClass(internal);
                        if (bytes.isEmpty()) {
                            continue;
                        }
                        String code = NativeJavaDecompiler.decompile(internal, bytes.get());
                        if (code == null || code.isBlank()) {
                            continue;
                        }
                        Path target = out.resolve(internal + ".java");
                        Files.createDirectories(target.getParent());
                        Files.writeString(target, code);
                        ok++;
                    } catch (Exception ignored) {
                        // 单个类失败不影响整体导出。
                    }
                    updateProgress(i + 1, names.size());
                }
                return ok;
            }
        };
        beginProgress(task, false);
        task.setOnSucceeded(e -> {
            endProgress();
            int ok = task.getValue();
            refreshOutputTree();
            statusLabel.setText("已导出 " + ok + " / " + names.size() + " 个 .java 文件 \u2192 " + out);
            eventsView.append(new AetherEvent(AetherEvent.Phase.PLUGIN, IRKind.BYTES,
                    "exported " + ok + " java source file(s)", null));
            openFolder(out);
        });
        task.setOnFailed(e -> {
            endProgress();
            statusLabel.setText("导出失败: " + (task.getException() == null
                    ? "未知错误" : task.getException().getMessage()));
        });
        Thread thread = new Thread(task, "aether-export");
        thread.setDaemon(true);
        thread.start();
    }

    /** 开始一个任务时显示进度条；indeterminate=true 时用滚动的不确定态动画。 */
    private void beginProgress(Task<?> task, boolean indeterminate) {
        progress.progressProperty().unbind();
        if (indeterminate) {
            progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        } else {
            progress.setProgress(0);
            progress.progressProperty().bind(task.progressProperty());
        }
        progress.setVisible(true);
        progress.setManaged(true);
    }

    /** 任务结束后隐藏进度条。 */
    private void endProgress() {
        progress.progressProperty().unbind();
        progress.setProgress(0);
        progress.setVisible(false);
        progress.setManaged(false);
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    // ---------------------------------------------------------------- 工作区

    private Region buildWorkspace() {
        // 左侧并行展示两个结构：上方是待反编译来源（JAR / 目录）的类结构，
        // 下方是反编译输出目录的源码文件结构。
        VBox jarPane = new VBox(classTree);
        jarPane.getStyleClass().add("nav");
        VBox.setVgrow(classTree, Priority.ALWAYS);

        VBox outPane = new VBox(outputTree);
        outPane.getStyleClass().add("nav");
        VBox.setVgrow(outputTree, Priority.ALWAYS);

        SplitPane nav = new SplitPane(jarPane, outPane);
        nav.setOrientation(Orientation.VERTICAL);
        nav.setDividerPositions(0.55);
        nav.getStyleClass().add("nav-split");
        nav.setPrefWidth(320);

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("workspace");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("源码", codeView),
                tab("字节码", bytecodeView),
                tab("控制流图", cfgView),
                tab("分析", analysisView));

        SplitPane centre = new SplitPane(nav, tabs);
        centre.getStyleClass().add("main-split");
        SplitPane.setResizableWithParent(nav, Boolean.FALSE);
        centre.setDividerPositions(0.22);

        VBox inspectorPane = new VBox(inspector);
        inspectorPane.getStyleClass().add("aside");
        inspectorPane.setPrefWidth(260);

        SplitPane root = new SplitPane(centre, inspectorPane);
        root.setDividerPositions(0.80);
        SplitPane.setResizableWithParent(inspectorPane, Boolean.FALSE);
        return root;
    }

    private Tab tab(String title, Region content) {
        content.getStyleClass().add("pane-body");
        Tab t = new Tab(title, content);
        t.setClosable(false);
        return t;
    }

    private Region buildStatusBar() {
        progress.setPrefWidth(180);
        progress.getStyleClass().add("stage-progress");
        // 空闲时不显示进度条：此前它常驻且为 0，表现为一条多余的空灰条。
        progress.setVisible(false);
        progress.setManaged(false);
        Label eventsHeader = new Label("流水线事件");
        eventsHeader.getStyleClass().add("panel-header");
        VBox eventBox = new VBox(2, eventsHeader, eventsView);
        eventBox.getStyleClass().add("event-dock");
        eventBox.setPrefHeight(140);
        VBox.setVgrow(eventsView, Priority.ALWAYS);

        HBox status = new HBox(10, new Label("状态:"), statusLabel, progress);
        status.getStyleClass().add("statusbar");
        status.setAlignment(Pos.CENTER_LEFT);

        return new VBox(eventBox, status);
    }

    // -------------------------------------------------------------------- 皮肤

    private void applySkin(Skin skin) {
        if (scene == null) {
            // 场景尚未创建：此时应用皮肤会被跳过，皮肤在场景就绪后统一应用。
            return;
        }
        skinManager.apply(scene, skin);
        if (skin != null) {
            statusLabel.setText("皮肤: " + skin.name() + "  \u00b7  " + skin.author());
        }
    }

    // -------------------------------------------------------------- 打开来源

    private void openJarDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("打开 JAR / 归档");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java 归档 (*.jar, *.zip)", "*.jar", "*.zip"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            loadSource(file.getAbsolutePath());
        }
    }

    private void openDirDialog() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("打开 class 目录");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            loadSource(dir.getAbsolutePath());
        }
    }

    private void loadSource(String locator) {
        eventsView.clear();
        try {
            ClassSourcePlugin plugin = findJarSourcePlugin();
            if (plugin == null) {
                statusLabel.setText("未找到 source.jar 插件");
                return;
            }
            closeSource();
            currentSource = plugin.open(locator, Options.empty());
            List<String> names = currentSource.classNames();
            classTree.setClasses(names, new File(locator).getName());
            if (outputRoot == null) {
                outputRoot = Path.of(System.getProperty("user.home"),
                        "AetherDecompiled", sanitize(new File(locator).getName()));
            }
            refreshOutputTree();
            statusLabel.setText("已加载: " + currentSource.describe() + "  (" + names.size() + " 个类)");
            inspector.setInfo(null);
            codeView.setSource("// 选择左侧类以查看字节码与结构\n// " + AetherVersion.PROJECT + " \u2014 "
                    + AetherVersion.AUTHOR + " (" + AetherVersion.AUTHOR_PEN_NAME + ")");
            if (!names.isEmpty()) {
                openClass(names.get(0));
            }
        } catch (RuntimeException ex) {
            statusLabel.setText("加载失败: " + ex.getMessage());
            eventsView.append(new AetherEvent(AetherEvent.Phase.PLUGIN, IRKind.BYTES,
                    "load failed: " + ex.getMessage(), null));
        }
    }

    private ClassSourcePlugin findJarSourcePlugin() {
        PluginHost host = new PluginHost(getClass().getClassLoader(), Options.empty(),
                new OptionRegistry(), engine.eventBus());
        for (ClassSourcePlugin p : host.classSources()) {
            if (p.id().equals("source.jar")) {
                return p;
            }
        }
        return host.classSources().isEmpty() ? null : host.classSources().get(0);
    }

    // --------------------------------------------------------------- 反编译

    private void openClass(String simpleOrInternal) {
        if (currentSource == null) {
            return;
        }
        String internal = resolveInternalName(simpleOrInternal);
        currentClass = internal;
        DecompilerEngine.DecompileResult result = engine.decompile(currentSource, internal);
        currentResult = result;
        if (!result.ok()) {
            statusLabel.setText("无法读取类: " + internal);
            return;
        }
        inspector.setInfo(result.model());
        inspector.setMetrics(result);
        renderClass(result);
        statusLabel.setText("已选择: " + internal + "  (" + result.model().methods().size() + " 个方法)");
    }

    private String resolveInternalName(String selected) {
        for (String name : currentSource.classNames()) {
            int slash = name.lastIndexOf('/');
            String simple = slash < 0 ? name : name.substring(slash + 1);
            if (name.equals(selected) || simple.equals(selected)
                    || name.endsWith("/" + selected)) {
                return name;
            }
        }
        return selected;
    }

    private void renderClass(DecompilerEngine.DecompileResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("// ").append(result.model().dottedName()).append('\n');
        sb.append("// 类文件主版本 ").append(result.model().majorVersion()).append('\n');
        sb.append("// ").append(AetherVersion.PROJECT).append(" ").append(AetherVersion.VERSION)
                .append(" \u2014 ").append(AetherVersion.AUTHOR)
                .append(" (").append(AetherVersion.AUTHOR_PEN_NAME).append(")\n\n");
        for (MethodModel m : result.model().methods()) {
            sb.append("    // 方法： ").append(m.name()).append(m.descriptor()).append('\n');
        }
        codeView.setSource(renderJavaSource(result));

        if (!result.cfgs().isEmpty()) {
            renderMethod(result.cfgs().get(0));
        } else {
            bytecodeView.setRows(List.of());
            cfgView.render(null);
        }

        // Phase 2–5：把 SSA / AST 两层中间表示接入界面。分析在后台完成，
        // 结果只读，因此可安全地回填到 JavaFX 视图。
        analysisView.render(pipeline.analyze(result.model()));
    }

    /**
     * 渲染一个类的 Java 源码。
     *
     * <p>缺陷修复：把类的原始字节交给 Java 反编译后端（CFR），得到真正可读的
     * Java 代码，而不是只有方法签名的注释。若后端不可用或反编译失败，则回退为
     * 方法摘要注释，保证源码视图永远有内容。</p>
     *
     * @param result 反编译结果
     * @return 要显示在源码视图中的文本
     */
    private String renderJavaSource(DecompilerEngine.DecompileResult result) {
        try {
            String internal = result.internalName();
            if (currentSource != null) {
                java.util.Optional<byte[]> bytes = currentSource.readClass(internal);
                if (bytes.isPresent()) {
                    String src = NativeJavaDecompiler.decompile(internal, bytes.get());
                    if (src != null && !src.isBlank()) {
                        return src;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // 反编译失败时回退到方法摘要注释。
        }
        StringBuilder fallback = new StringBuilder();
        fallback.append("// ").append(result.model().dottedName()).append('\n');
        fallback.append("// 类文件主版本 ").append(result.model().majorVersion()).append('\n');
        fallback.append("// （Java 源码渲染暂不可用，以下为方法摘要）\n");
        for (MethodModel m : result.model().methods()) {
            fallback.append("//   方法： ").append(m.name()).append(m.descriptor()).append('\n');
        }
        return fallback.toString();
    }

    private void renderMethod(ControlFlowGraph cfg) {
        List<BytecodeView.Row> rows = new ArrayList<>();
        List<Insn> insns = cfg.instructions();
        for (int i = 0; i < insns.size(); i++) {
            Insn insn = insns.get(i);
            rows.add(new BytecodeView.Row(insn.index(), i, insn.mnemonic(), insn.operand()));
        }
        bytecodeView.setRows(rows);
        cfgView.render(cfg);
    }

    private void decompileCurrent() {
        if (currentSource == null) {
            statusLabel.setText("请先打开 JAR 或目录");
            return;
        }
        String target = currentClass != null ? currentClass
                : (currentSource.classNames().isEmpty() ? null : currentSource.classNames().get(0));
        if (target == null) {
            return;
        }
        Task<DecompilerEngine.DecompileResult> task = new Task<>() {
            @Override
            protected DecompilerEngine.DecompileResult call() {
                return engine.decompile(currentSource, target);
            }
        };
        beginProgress(task, true);
        task.setOnSucceeded(e -> {
            endProgress();
            currentResult = task.getValue();
            inspector.setInfo(task.getValue().model());
            inspector.setMetrics(task.getValue());
            renderClass(task.getValue());
            statusLabel.setText("反编译完成: " + target);
        });
        task.setOnFailed(e -> {
            endProgress();
            statusLabel.setText("反编译失败: " + task.getException().getMessage());
        });
        Thread thread = new Thread(task, "aether-decompile");
        thread.setDaemon(true);
        thread.start();
    }

    // ------------------------------------------------------------ 联动

    private void onSourceLineClicked(int line) {
        if (currentResult == null || currentResult.cfgs().isEmpty()) {
            return;
        }
        ControlFlowGraph cfg = currentResult.cfgs().get(0);
        int insnCount = cfg.instructions().size();
        int target = Math.min(line - 1, Math.max(0, insnCount - 1));
        bytecodeView.highlight(target);
        statusLabel.setText("源码行 " + line + " \u2192 指令 " + target);
    }

    private void closeSource() {
        if (currentSource != null) {
            try {
                currentSource.close();
            } catch (RuntimeException ignored) {
                // 关闭一个已经不存在的来源不算错误。
            }
            currentSource = null;
        }
    }

    @Override
    public void stop() {
        savePrefs();
        disposeVideo();
        closeSource();
    }

    private static double parseDouble(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * 一个为皮肤绘制双色色板的组合框单元格。
     *
     * @author Jerry Zhu (Zeek)
     */
    private static final class SkinCell extends ListCell<Skin> {
        @Override
        protected void updateItem(Skin skin, boolean empty) {
            super.updateItem(skin, empty);
            if (empty || skin == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Region swatch = new Region();
            swatch.setPrefSize(14, 14);
            swatch.setMinSize(14, 14);
            swatch.setStyle("-fx-background-radius: 4; -fx-border-radius: 4; -fx-border-width: 1;"
                    + "-fx-background-color: " + skin.swatchBg() + ";"
                    + "-fx-border-color: " + skin.swatchAccent() + ";");
            Label label = new Label(skin.name());
            HBox box = new HBox(8, swatch, label);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(2, 0, 2, 0));
            setText(null);
            setGraphic(box);
        }
    }

    /**
     * 一个图库单元格，展示背景的缩略图、标题与类型。
     *
     * @author Jerry Zhu (Zeek)
     */
    private static final class BackdropCell extends ListCell<Backdrop> {
        @Override
        protected void updateItem(Backdrop bd, boolean empty) {
            super.updateItem(bd, empty);
            if (empty || bd == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            ImageView thumb = new ImageView();
            thumb.setFitWidth(96);
            thumb.setFitHeight(54);
            thumb.setPreserveRatio(true);
            Region frame = new Region();
            frame.getStyleClass().add("bd-thumb");
            frame.setPrefSize(96, 54);
            StackPane holder = new StackPane(frame, thumb);
            holder.setPrefSize(96, 54);

            Path preview = bd.preview();
            Path visual = preview != null ? preview
                    : (bd.kind() == BackdropKind.IMAGE ? bd.media() : null);
            if (visual != null && Files.isRegularFile(visual)) {
                try (InputStream in = Files.newInputStream(visual)) {
                    Image img = new Image(in);
                    if (!img.isError()) {
                        thumb.setImage(img);
                    }
                } catch (IOException ignored) {
                    // 留出占位帧。
                }
            }

            Label name = new Label(bd.title());
            name.getStyleClass().add("bd-name");
            Label kind = new Label(switch (bd.kind()) {
                case VIDEO -> "视频";
                case WEB -> "网页";
                case IMAGE -> "图片";
                default -> "背景";
            });
            kind.getStyleClass().add("bd-kind");
            VBox text = new VBox(2, name, kind);
            text.setAlignment(Pos.CENTER_LEFT);

            // 若有描述（Wallpaper Engine 工程的 description），显示一行淡色副标题。
            if (!bd.description().isBlank()) {
                Label desc = new Label(bd.description());
                desc.getStyleClass().add("bd-desc");
                desc.setMaxWidth(360);
                text.getChildren().add(desc);
            }

            HBox row = new HBox(10, holder, text);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 6, 4, 6));
            // 悬停提示：显示媒体文件的完整路径，便于分辨同名工程。
            setTooltip(new Tooltip(bd.media().toString()));
            setText(null);
            setGraphic(row);
        }
    }
}
