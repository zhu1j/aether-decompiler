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
import com.aetherdecompiler.core.engine.PluginHost;
import com.aetherdecompiler.core.model.Insn;
import com.aetherdecompiler.core.model.MethodModel;
import com.aetherdecompiler.gui.background.Backdrop;
import com.aetherdecompiler.gui.background.BackdropKind;
import com.aetherdecompiler.gui.background.BackdropLibrary;
import com.aetherdecompiler.gui.background.WallpaperProject;
import com.aetherdecompiler.gui.skin.Skin;
import com.aetherdecompiler.gui.skin.SkinManager;
import com.aetherdecompiler.gui.view.BytecodeView;
import com.aetherdecompiler.gui.view.CfgView;
import com.aetherdecompiler.gui.view.ClassTreeView;
import com.aetherdecompiler.gui.view.CodeEditorView;
import com.aetherdecompiler.gui.view.EventsView;
import com.aetherdecompiler.gui.view.InspectorView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
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
public final class AetherGuiApp extends Application {

    /** 用于填充模式下拉框的背景透明度预设。 */
    private static final String FILL_STRETCH = "拉伸填充";
    private static final String FILL_FIT = "等比适应";

    private final SkinManager skinManager = new SkinManager(SkinManager.defaultUserSkinDir());
    private final BackdropLibrary backdropLibrary = new BackdropLibrary(BackdropLibrary.defaultRoot());
    private final DecompilerEngine engine = new DecompilerEngine();

    private Stage stage;
    private Scene scene;

    private final CodeEditorView codeView = new CodeEditorView();
    private final BytecodeView bytecodeView = new BytecodeView();
    private final CfgView cfgView = new CfgView();
    private final ClassTreeView classTree = new ClassTreeView();
    private final EventsView eventsView = new EventsView();
    private final InspectorView inspector = new InspectorView();

    private final ComboBox<Skin> skinBox = new ComboBox<>();
    private final ProgressBar progress = new ProgressBar(0);
    private final Label statusLabel = new Label("就绪");

    // 背景层。
    private final StackPane rootStack = new StackPane();
    private final StackPane mediaHolder = new StackPane();
    private final ImageView bgImage = new ImageView();
    private final MediaView bgVideo = new MediaView();
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

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        engine.eventBus().subscribe((AetherEvent ev) -> eventsView.append(ev));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(buildTopBar());
        root.setCenter(buildWorkspace());
        root.setBottom(buildStatusBar());

        rootStack.getStyleClass().add("root-stack");
        rootStack.getChildren().addAll(buildBackdropLayer(), scrim, root);

        scene = new Scene(rootStack, 1360, 860);
        classTree.setClassSelectListener(this::openClass);
        codeView.setLineClickListener(this::onSourceLineClicked);

        loadPrefs();
        applySkin(skinManager.defaultSkin());

        primaryStage.setTitle(AetherVersion.PROJECT + "  \u00b7  Studio");
        primaryStage.setScene(scene);
        primaryStage.show();

        eventsView.append(new AetherEvent(AetherEvent.Phase.PLUGIN, IRKind.BYTES,
                "studio ready \u2014 " + AetherVersion.attribution(), null));
    }

    // --------------------------------------------------------------- 背景

    private Region buildBackdropLayer() {
        bgImage.setSmooth(true);
        bgImage.setPreserveRatio(false);
        bgImage.fitWidthProperty().bind(rootStack.widthProperty());
        bgImage.fitHeightProperty().bind(rootStack.heightProperty());

        bgVideo.setPreserveRatio(false);
        bgVideo.fitWidthProperty().bind(rootStack.widthProperty());
        bgVideo.fitHeightProperty().bind(rootStack.heightProperty());

        mediaHolder.getChildren().addAll(bgImage, bgVideo);
        mediaHolder.getStyleClass().add("bg-layer");
        mediaHolder.setMouseTransparent(true);
        mediaHolder.opacityProperty().bind(bgOpacity.valueProperty());
        mediaHolder.setEffect(blurEffect);

        scrim.getStyleClass().add("bg-scrim");
        scrim.setMouseTransparent(true);
        return mediaHolder;
    }

    private void applyBackdrop(Backdrop backdrop, boolean remember) {
        disposeVideo();
        bgImage.setImage(null);
        bgVideo.setMediaPlayer(null);
        currentBackdrop = backdrop;

        if (backdrop != null) {
            if (backdrop.kind() == BackdropKind.IMAGE) {
                Image img = loadImage(backdrop.media());
                if (img != null) {
                    bgImage.setImage(img);
                } else {
                    currentBackdrop = null;
                }
            } else if (backdrop.kind() == BackdropKind.VIDEO) {
                try {
                    Media media = new Media(backdrop.media().toUri().toString());
                    videoPlayer = new MediaPlayer(media);
                    videoPlayer.setMute(true);
                    videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    videoPlayer.setOnError(() -> statusLabel.setText("视频背景播放错误"));
                    bgVideo.setMediaPlayer(videoPlayer);
                    videoPlayer.play();
                } catch (RuntimeException ex) {
                    statusLabel.setText("无法播放视频背景: " + ex.getMessage());
                    currentBackdrop = null;
                }
            }
        }

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

    private void updateFillMode(String mode) {
        boolean fit = FILL_FIT.equals(mode);
        bgImage.setPreserveRatio(fit);
        bgVideo.setPreserveRatio(fit);
        if (fit) {
            bgImage.fitWidthProperty().unbind();
            bgImage.fitHeightProperty().unbind();
            bgVideo.fitWidthProperty().unbind();
            bgVideo.fitHeightProperty().unbind();
        }
    }

    // ------------------------------------------------------------------- 偏好

    private void loadPrefs() {
        Properties props = readPrefs();
        String skinId = props.getProperty("skin");
        Skin skin = skinId == null ? null : skinManager.byId(skinId);
        if (skin != null) {
            skinBox.getSelectionModel().select(skin);
        }

        String fill = props.getProperty("backgroundFill", FILL_STRETCH);
        bgFill.getSelectionModel().select(fill);
        updateFillMode(fill);

        double opacity = parseDouble(props.getProperty("backgroundOpacity"),
                Skin.DEFAULT_BACKGROUND_OPACITY);
        double blur = parseDouble(props.getProperty("backgroundBlur"), 0);
        bgOpacity.setValue(opacity);
        bgBlur.setValue(blur);
        blurEffect.setRadius(blur);

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
        Button openDir = new Button("打开目录");
        openDir.getStyleClass().add("tool-button");
        openDir.setOnAction(e -> openDirDialog());
        Button decompile = new Button("反编译");
        decompile.getStyleClass().addAll("tool-button", "primary");
        decompile.setOnAction(e -> decompileCurrent());

        Button backdropBtn = new Button("\u2b07 背景 / 外观");
        backdropBtn.getStyleClass().addAll("tool-button", "menu");
        backdropBtn.setTooltip(new Tooltip("打开背景库：导入图片、视频或 Wallpaper Engine 工程"));
        backdropBtn.setOnAction(e -> openBackdropDialog());

        skinBox.getStyleClass().add("skin-picker");
        skinBox.setItems(FXCollections.observableArrayList(skinManager.all()));
        skinBox.setCellFactory(v -> new SkinCell());
        skinBox.setButtonCell(new SkinCell());
        skinBox.getSelectionModel().selectedItemProperty().addListener((obs, old, skin) -> {
            if (skin != null) {
                applySkin(skin);
                savePrefs();
            }
        });
        skinBox.getSelectionModel().select(skinManager.defaultSkin());

        HBox bar = new HBox(10, mark, brand, motto, spacer,
                skinBox, backdropBtn, openJar, openDir, decompile);
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
                + "背景以半透明呈现，可用下方滑块调节透明度与模糊。");
        hint.getStyleClass().add("dialog-hint");
        hint.setWrapText(true);

        ListView<Backdrop> gallery = new ListView<>();
        gallery.getStyleClass().add("gallery");
        gallery.setItems(FXCollections.observableArrayList(backdropLibrary.list()));
        gallery.setCellFactory(v -> new BackdropCell());
        if (currentBackdrop != null) {
            gallery.getSelectionModel().select(currentBackdrop);
        } else if (!gallery.getItems().isEmpty()) {
            gallery.getSelectionModel().selectFirst();
        }

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
            applyBackdrop(bd, true);
            if (bd != null) {
                statusLabel.setText("已应用背景: " + bd.title());
            }
        });

        Button remove = new Button("移除所选");
        remove.getStyleClass().add("tool-button");
        remove.setOnAction(e -> {
            Backdrop bd = gallery.getSelectionModel().getSelectedItem();
            if (bd == null) {
                return;
            }
            try {
                backdropLibrary.remove(bd.media());
                if (currentBackdrop != null && currentBackdrop.media().equals(bd.media())) {
                    applyBackdrop(null, true);
                }
                gallery.setItems(FXCollections.observableArrayList(backdropLibrary.list()));
            } catch (IOException ex) {
                statusLabel.setText("移除失败: " + ex.getMessage());
            }
        });

        Button openFolder = new Button("打开背景目录");
        openFolder.getStyleClass().add("tool-button");
        openFolder.setOnAction(e -> openFolder(backdropLibrary.ensureRoot()));

        HBox importRow = new HBox(8, importFile, importWe, openFolder);
        importRow.setAlignment(Pos.CENTER_LEFT);
        HBox actionRow = new HBox(8, apply, remove);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(actionRow, Priority.ALWAYS);

        HBox controls = new HBox(8,
                new Label("透明度"), bgOpacity, new Label("模糊"), bgBlur, new Label("填充"), bgFill);
        controls.setAlignment(Pos.CENTER_LEFT);
        bgOpacity.setPrefWidth(120);
        bgBlur.setPrefWidth(120);
        bgFill.setItems(FXCollections.observableArrayList(FILL_STRETCH, FILL_FIT));

        VBox box = new VBox(10, title, hint, gallery, importRow,
                new HBox(10, controls, actionRow));
        box.getStyleClass().add("backdrop-dialog");
        VBox.setVgrow(gallery, Priority.ALWAYS);

        Scene dscene = new Scene(box, 620, 560);
        skinManager.apply(dscene, skinBox.getSelectionModel().getSelectedItem());
        dialog.setScene(dscene);
        dialog.showAndWait();
    }

    private void importIntoLibrary(Stage dialog, ListView<Backdrop> gallery, Path source, boolean project) {
        try {
            Backdrop bd = backdropLibrary.importAny(source);
            List<Backdrop> items = new ArrayList<>(gallery.getItems());
            items.add(bd);
            gallery.setItems(FXCollections.observableArrayList(items));
            gallery.getSelectionModel().select(bd);
            applyBackdrop(bd, true);
            statusLabel.setText("已导入并应用背景: " + bd.title());
        } catch (IOException ex) {
            statusLabel.setText("导入失败: " + ex.getMessage());
        }
    }

    private void openFolder(Path dir) {
        try {
            java.awt.Desktop.getDesktop().open(dir.toFile());
        } catch (Exception ex) {
            statusLabel.setText("目录: " + dir);
        }
    }

    // ---------------------------------------------------------------- 工作区

    private Region buildWorkspace() {
        VBox nav = new VBox(classTree);
        nav.getStyleClass().add("nav");
        nav.setPrefWidth(300);
        VBox.setVgrow(classTree, Priority.ALWAYS);

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("workspace");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("源码", codeView),
                tab("字节码", bytecodeView),
                tab("控制流图", cfgView));

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
        codeView.setSource(sb.toString());

        if (!result.cfgs().isEmpty()) {
            renderMethod(result.cfgs().get(0));
        } else {
            bytecodeView.setRows(List.of());
            cfgView.render(null);
        }
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
        progress.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(e -> {
            progress.progressProperty().unbind();
            progress.setProgress(0);
            currentResult = task.getValue();
            inspector.setInfo(task.getValue().model());
            inspector.setMetrics(task.getValue());
            renderClass(task.getValue());
            statusLabel.setText("反编译完成: " + target);
        });
        task.setOnFailed(e -> {
            progress.progressProperty().unbind();
            progress.setProgress(0);
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
            Label kind = new Label(bd.kind() == BackdropKind.VIDEO ? "视频" : "图片");
            kind.getStyleClass().add("bd-kind");
            VBox text = new VBox(2, name, kind);
            text.setAlignment(Pos.CENTER_LEFT);

            HBox row = new HBox(10, holder, text);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 6, 4, 6));
            setText(null);
            setGraphic(row);
        }
    }
}
