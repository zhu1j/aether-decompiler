/*
 * aether-decompiler — an independent, reusable JVM decompilation engine.
 * Copyright 2026 Jerry Zhu (Zeek) <zhujiejava1@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Jerry Zhu (Zeek)
 * "Run the Code, Run the World!"
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
import com.aetherdecompiler.gui.skin.Skin;
import com.aetherdecompiler.gui.skin.SkinManager;
import com.aetherdecompiler.gui.view.BytecodeView;
import com.aetherdecompiler.gui.view.CfgView;
import com.aetherdecompiler.gui.view.ClassTreeView;
import com.aetherdecompiler.gui.view.CodeEditorView;
import com.aetherdecompiler.gui.view.EventsView;
import com.aetherdecompiler.gui.view.InspectorView;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
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

/**
 * The Aether Decompiler Studio window.
 *
 * <p>This is the application-layer composition root for the desktop studio. It
 * owns the {@link DecompilerEngine}, the {@link PluginHost}, and the widgets that
 * render the engine's output, and it wires the three-view linkage: clicking a
 * source line highlights the corresponding bytecode row and CFG block.</p>
 *
 * <p>It is a pure caller: every decompilation step goes through the public
 * kernel/plugin API, so it could be replaced by the CLI or a future IDE plug-in
 * without touching the engine.</p>
 *
 * <p>The window is a backdrop-aware shell: a custom background image, imported by
 * the user, is painted as a deliberately translucent layer beneath the work area
 * (its opacity is user-adjustable), while a themed scrim keeps code legible.
 * Because the shell is CSS-driven, a custom {@code *.css} stylesheet and a custom
 * backdrop can both be imported at runtime without recompiling anything.</p>
 *
 * <p>Story analogy: the studio itself. The presses (kernel) and the parts
 * suppliers (plugins) are delivered and installed; the studio arranges them into
 * a room a person can actually walk through and work in — and hang whatever
 * painting they like on the back wall.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AetherGuiApp extends Application {

    private final SkinManager skinManager = new SkinManager(SkinManager.defaultUserSkinDir());
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

    // Backdrop layers.
    private final StackPane rootStack = new StackPane();
    private final ImageView bgImage = new ImageView();
    private final Region scrim = new Region();
    private final Slider bgOpacity = new Slider(0.05, 1.0, Skin.DEFAULT_BACKGROUND_OPACITY);
    private final Button clearBackdrop = new Button("清除背景");

    private ClassSource currentSource;
    private DecompilerEngine.DecompileResult currentResult;
    private String currentClass;
    private Path currentBackgroundFile;

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

    // --------------------------------------------------------------- backdrop

    private Region buildBackdropLayer() {
        bgImage.setSmooth(true);
        bgImage.setPreserveRatio(false);
        bgImage.setMouseTransparent(true);
        bgImage.fitWidthProperty().bind(rootStack.widthProperty());
        bgImage.fitHeightProperty().bind(rootStack.heightProperty());
        bgImage.opacityProperty().bind(bgOpacity.valueProperty());

        StackPane layer = new StackPane(bgImage);
        layer.getStyleClass().add("bg-layer");
        layer.setMouseTransparent(true);

        scrim.getStyleClass().add("bg-scrim");
        scrim.setMouseTransparent(true);
        return layer;
    }

    private void setBackdrop(Image image, Path file, double opacity, boolean remember) {
        bgImage.setImage(image);
        currentBackgroundFile = (image == null) ? null : file;
        if (image != null) {
            bgOpacity.setValue(opacity);
        }
        boolean active = image != null;
        if (active) {
            if (!rootStack.getStyleClass().contains("bg-active")) {
                rootStack.getStyleClass().add("bg-active");
            }
        } else {
            rootStack.getStyleClass().remove("bg-active");
        }
        clearBackdrop.setDisable(!active);
        bgOpacity.setDisable(!active);
        if (remember) {
            savePrefs();
        }
    }

    private void clearBackdropAction() {
        setBackdrop(null, null, bgOpacity.getValue(), true);
        statusLabel.setText("已清除背景图");
    }

    // ------------------------------------------------------------------- prefs

    private void loadPrefs() {
        Properties props = readPrefs();
        // Skin.
        String skinId = props.getProperty("skin");
        Skin skin = skinId == null ? null : skinManager.byId(skinId);
        if (skin != null) {
            skinBox.getSelectionModel().select(skin);
        }
        // Backdrop.
        String bgPath = props.getProperty("background");
        double opacity = parseDouble(props.getProperty("backgroundOpacity"),
                Skin.DEFAULT_BACKGROUND_OPACITY);
        if (bgPath != null && !bgPath.isBlank()) {
            Path p = Path.of(bgPath);
            Image img = loadImage(p);
            if (img != null) {
                setBackdrop(img, p, opacity, false);
            }
        } else {
            setBackdrop(null, null, opacity, false);
        }
    }

    private Properties readPrefs() {
        Properties props = new Properties();
        Path prefs = SkinManager.studioPrefsFile();
        if (Files.isRegularFile(prefs)) {
            try (InputStream in = Files.newInputStream(prefs)) {
                props.load(in);
            } catch (IOException ex) {
                // Ignore a corrupt prefs file; defaults apply.
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
        if (currentBackgroundFile != null) {
            props.setProperty("background", currentBackgroundFile.toAbsolutePath().toString());
            props.setProperty("backgroundOpacity", Double.toString(bgOpacity.getValue()));
        }
        try {
            Path prefs = SkinManager.studioPrefsFile();
            Files.createDirectories(prefs.getParent());
            try (OutputStream out = Files.newOutputStream(prefs)) {
                props.store(out, "aether-decompiler studio preferences \u2014 Jerry Zhu (Zeek)");
            }
        } catch (IOException ex) {
            // Persisting prefs is best-effort.
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

    // ------------------------------------------------------------------ top bar

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

        // ---- skin / backdrop customisation group ----
        Button importBtn = new Button("\u2b07 导入皮肤/背景");
        importBtn.getStyleClass().add("tool-button");
        importBtn.setTooltip(new Tooltip("导入 *.css 皮肤，或导入图片作为半透明背景"));
        importBtn.setOnAction(e -> importDialog());

        Button openSkinDir = new Button("\u2728 皮肤目录");
        openSkinDir.getStyleClass().add("tool-button");
        openSkinDir.setTooltip(new Tooltip("打开 ~/.aether/skins 放置自定义皮肤"));
        openSkinDir.setOnAction(e -> openSkinFolder());

        Label opacityLabel = new Label("背景透明");
        opacityLabel.getStyleClass().add("opacity-label");
        bgOpacity.getStyleClass().add("backdrop-slider");
        bgOpacity.setPrefWidth(110);
        bgOpacity.setTooltip(new Tooltip("背景图不透明度（半透明）"));
        bgOpacity.setDisable(true);
        bgOpacity.valueProperty().addListener((obs, o, n) -> {
            if (currentBackgroundFile != null) {
                savePrefs();
            }
        });

        clearBackdrop.getStyleClass().add("tool-button");
        clearBackdrop.setDisable(true);
        clearBackdrop.setOnAction(e -> clearBackdropAction());

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
                skinBox, importBtn, openSkinDir, opacityLabel, bgOpacity, clearBackdrop,
                openJar, openDir, decompile);
        bar.getStyleClass().add("topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ---------------------------------------------------------------- workspace

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

    // -------------------------------------------------------------------- skins

    private void applySkin(Skin skin) {
        skinManager.apply(scene, skin);
        if (skin != null) {
            statusLabel.setText("皮肤: " + skin.name() + "  \u00b7  " + skin.author());
        }
    }

    private void openSkinFolder() {
        Path dir = SkinManager.ensureUserSkinDir();
        try {
            java.awt.Desktop.getDesktop().open(dir.toFile());
        } catch (Exception ex) {
            statusLabel.setText("自定义皮肤目录: " + dir);
        }
    }

    // ---------------------------------------------------------- import dialog

    private void importDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入皮肤 (*.css) 或 背景图片");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("全部可导入 (*.css, 图片)",
                        "*.css", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"),
                new FileChooser.ExtensionFilter("皮肤样式表 (*.css)", "*.css"),
                new FileChooser.ExtensionFilter("背景图片", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        String name = path.getFileName().toString();
        try {
            if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".css")) {
                Skin imported = skinManager.importSkin(path);
                skinBox.setItems(FXCollections.observableArrayList(skinManager.all()));
                skinBox.getSelectionModel().select(imported);
                statusLabel.setText("已导入皮肤: " + imported.name());
            } else if (SkinManager.isImage(name)) {
                Path stored = skinManager.importBackground(path);
                Image img = loadImage(stored);
                if (img == null) {
                    statusLabel.setText("图片无法解码: " + name);
                    return;
                }
                setBackdrop(img, stored, Skin.DEFAULT_BACKGROUND_OPACITY, true);
                statusLabel.setText("已导入背景图 (半透明): " + stored.getFileName());
            } else {
                statusLabel.setText("不支持的文件类型: " + name);
            }
        } catch (IOException ex) {
            statusLabel.setText("导入失败: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------- open source

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

    // --------------------------------------------------------------- decompile

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
        sb.append("// class file major version ").append(result.model().majorVersion()).append('\n');
        sb.append("// ").append(AetherVersion.PROJECT).append(" ").append(AetherVersion.VERSION)
                .append(" \u2014 ").append(AetherVersion.AUTHOR)
                .append(" (").append(AetherVersion.AUTHOR_PEN_NAME).append(")\n\n");
        for (MethodModel m : result.model().methods()) {
            sb.append("    // method: ").append(m.name()).append(m.descriptor()).append('\n');
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

    // ------------------------------------------------------------ linkage

    private void onSourceLineClicked(int line) {
        if (currentResult == null || currentResult.cfgs().isEmpty()) {
            return;
        }
        ControlFlowGraph cfg = currentResult.cfgs().get(0);
        // Line number is a coarse link: map to a proportional instruction index.
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
                // Closing a source that is already gone is not an error.
            }
            currentSource = null;
        }
    }

    @Override
    public void stop() {
        savePrefs();
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
     * A combo-box cell that paints a two-colour swatch for a skin.
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
}
