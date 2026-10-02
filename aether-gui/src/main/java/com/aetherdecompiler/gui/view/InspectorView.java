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
package com.aetherdecompiler.gui.view;

import com.aetherdecompiler.api.AetherVersion;
import com.aetherdecompiler.core.cfg.ControlFlowGraph;
import com.aetherdecompiler.core.engine.DecompilerEngine;
import com.aetherdecompiler.core.model.ClassModel;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The right-hand inspector: class metadata, pipeline status, and current-method
 * metrics, plus the author signature footer.
 *
 * @author Jerry Zhu (Zeek)
 */
public final class InspectorView extends VBox {

    private final VBox info = new VBox(2);
    private final VBox metrics = new VBox(6);

    /**
     * Create the inspector.
     */
    public InspectorView() {
        getStyleClass().add("inspector-view");
        setSpacing(14);

        getChildren().add(section("类信息", info));
        getChildren().add(section("流水线状态", buildPipeline()));
        getChildren().add(section("当前方法度量", metrics));

        Label footer = new Label(AetherVersion.PROJECT + " " + AetherVersion.VERSION
                + "\n\u00a9 2026 " + AetherVersion.AUTHOR + " (" + AetherVersion.AUTHOR_PEN_NAME + ")"
                + "\n" + AetherVersion.LICENSE + " \u00b7 " + AetherVersion.AUTHOR_EMAIL);
        footer.getStyleClass().add("inspector-footer");
        getChildren().add(footer);
        setInfo(null);
    }

    private VBox section(String title, Region content) {
        Label header = new Label(title);
        header.getStyleClass().add("panel-header");
        VBox box = new VBox(6, header, content);
        box.getStyleClass().add("inspector-section");
        return box;
    }

    private VBox buildPipeline() {
        VBox box = new VBox(4);
        String[] stages = {"读取字节", "构建模型", "控制流图", "SSA", "AST"};
        for (int i = 0; i < stages.length; i++) {
            Label l = new Label((i < 3 ? "\u2713  " : "\u00b7  ") + stages[i]);
            l.getStyleClass().add(i < 3 ? "stage-done" : "stage-wait");
            box.getChildren().add(l);
        }
        return box;
    }

    /**
     * Update the class-information block.
     *
     * @param model the class model, or {@code null} to clear
     */
    public void setInfo(ClassModel model) {
        info.getChildren().clear();
        if (model == null) {
            info.getChildren().add(plain("\u5c1a\u672a\u6253\u5f00\u7c7b"));
            metrics.getChildren().clear();
            return;
        }
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(3);
        int row = 0;
        row = addRow(grid, row, "类名", model.dottedName());
        row = addRow(grid, row, "版本", "major " + model.majorVersion());
        row = addRow(grid, row, "父类", model.superName() == null ? "-" : model.superName());
        row = addRow(grid, row, "接口", String.valueOf(model.interfaces().size()));
        row = addRow(grid, row, "方法/字段", model.methods().size() + " / " + model.fields().size());
        info.getChildren().add(grid);
    }

    /**
     * Update the method-metrics block from a decompile result.
     *
     * @param result the decompile result, or {@code null}
     */
    public void setMetrics(DecompilerEngine.DecompileResult result) {
        metrics.getChildren().clear();
        if (result == null) {
            return;
        }
        int blocks = 0;
        int edges = 0;
        int insns = 0;
        for (ControlFlowGraph cfg : result.cfgs()) {
            blocks += cfg.blockCount();
            edges += cfg.edgeCount();
            insns += cfg.instructions().size();
        }
        Map<String, String> kv = new LinkedHashMap<>();
        kv.put("方法数", String.valueOf(result.model() == null ? 0 : result.model().methods().size()));
        kv.put("基本块", String.valueOf(blocks));
        kv.put("控制流边", String.valueOf(edges));
        kv.put("指令总数", String.valueOf(insns));
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(3);
        int row = 0;
        for (Map.Entry<String, String> e : kv.entrySet()) {
            Label k = new Label(e.getKey());
            k.getStyleClass().add("kv-key");
            Label v = new Label(e.getValue());
            v.getStyleClass().add("kv-value");
            grid.add(k, 0, row);
            grid.add(v, 1, row);
            row++;
        }
        metrics.getChildren().add(grid);
    }

    private int addRow(GridPane grid, int row, String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("kv-key");
        Label v = new Label(value);
        v.getStyleClass().add("kv-value");
        v.setWrapText(true);
        grid.add(k, 0, row);
        grid.add(v, 1, row);
        return row + 1;
    }

    private Label plain(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("kv-key");
        l.setPadding(new Insets(2, 0, 2, 0));
        return l;
    }
}
