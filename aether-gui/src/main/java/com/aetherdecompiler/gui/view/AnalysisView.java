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
package com.aetherdecompiler.gui.view;

import com.aetherdecompiler.api.SourceTree;
import com.aetherdecompiler.core.ast.JavaAstRenderer;
import com.aetherdecompiler.core.engine.DecompilationPipeline;
import com.aetherdecompiler.core.model.ClassModel;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;

import java.util.List;

/**
 * 「分析」视图：把内核的 SSA 与 AST 两层中间表示直观地展示出来。
 *
 * <p>这是 Phase 2–5 成果的 GUI 出口。上半区展示静态单赋值形式（每个方法的 phi
 * 节点、定值-使用映射），下半区展示由 AST 渲染出的 Java 结构。两者都来自
 * {@link DecompilationPipeline}——因此视图所见即引擎所算，不存在“界面自己另算一份”
 * 的双份实现风险。</p>
 *
 * <p>展示面板使用与源码视图相同的 RichTextFX {@link CodeArea}，并复用
 * {@link SyntaxHighlight} 做语法分色，这样 SSA / AST 文本不再是单调的白字，
 * 而是与源码视图一致的彩色高亮。</p>
 *
 * <p>设计上刻意保持只读：本视图<strong>不</strong>修改任何模型，只把不可变的分析
 * 结果转成文本。因此它可以安全地在后台线程生产、在 JavaFX 线程消费。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AnalysisView extends SplitPane {

    private final CodeArea ssaArea = text();
    private final CodeArea astArea = text();

    /**
     * 创建分析视图。
     */
    public AnalysisView() {
        getStyleClass().add("analysis-view");

        VBox ssaPane = new VBox(labeled("SSA 形式（phi 节点 / 定值-使用）", ssaArea));
        VBox astPane = new VBox(labeled("AST → Java（结构化控制流）", astArea));
        VBox.setVgrow(ssaArea, Priority.ALWAYS);
        VBox.setVgrow(astArea, Priority.ALWAYS);
        getItems().addAll(ssaPane, astPane);
        setOrientation(javafx.geometry.Orientation.VERTICAL);
        setDividerPositions(0.5);
        setPlaceholder();
    }

    /** 创建一个只读、不折行、带语法高亮的文本区。 */
    private static CodeArea text() {
        CodeArea area = new CodeArea();
        area.getStyleClass().add("analysis-text");
        area.setEditable(false);
        area.setWrapText(false);
        area.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(i -> area.setStyleSpans(0, SyntaxHighlight.compute(area.getText())));
        return area;
    }

    private VBox labeled(String title, CodeArea area) {
        Label header = new Label(title);
        header.getStyleClass().add("panel-header");
        VBox box = new VBox(header, area);
        VBox.setVgrow(area, Priority.ALWAYS);
        return box;
    }

    private void setPlaceholder() {
        ssaArea.replaceText("// 打开一个类后，这里会展示每个方法的 SSA 形式。\n");
        astArea.replaceText("// 打开一个类后，这里会展示由 AST 渲染出的 Java 结构。\n");
    }

    /**
     * 用一个类的分析结果刷新视图。
     *
     * @param analysis 流水线类分析结果；可为 {@code null}
     */
    public void render(DecompilationPipeline.ClassAnalysis analysis) {
        if (analysis == null || analysis.model() == null) {
            setPlaceholder();
            return;
        }
        ClassModel model = analysis.model();
        StringBuilder ssa = new StringBuilder();
        ssa.append("// ").append(model.dottedName()).append(" — SSA 形式\n");
        StringBuilder ast = new StringBuilder();
        JavaAstRenderer renderer = new JavaAstRenderer();
        int methodCount = 0;
        for (DecompilationPipeline.MethodAnalysis m : analysis.methods()) {
            methodCount++;
            ssa.append("\n=== ").append(m.method().name()).append(m.method().descriptor())
                    .append(" ===\n");
            if (m.ssa() == null) {
                ssa.append("  （SSA 已禁用）\n");
            } else {
                ssa.append("  槽位数: ").append(m.ssa().slotCount())
                        .append("，phi 数: ").append(m.ssa().phiCount()).append('\n');
                m.ssa().phis().forEach(phi -> ssa.append("    phi  ").append(phi.display()).append('\n'));
                m.ssa().definitions().forEach((idx, def) ->
                        ssa.append("    [").append(idx).append("] 定义 ").append(def.name()).append('\n'));
            }
            SourceTree tree = renderer.render(m.ast());
            if (tree != null && tree.content() != null) {
                ast.append(tree.content()).append('\n');
            }
        }
        if (methodCount == 0) {
            ssa.append("（无具体方法）\n");
        }
        ssaArea.replaceText(ssa.toString());
        astArea.replaceText(ast.length() == 0 ? "// （无可渲染的 AST）\n" : ast.toString());
    }

    /**
     * 便捷方法：直接展示一批方法分析。
     *
     * @param methods 方法分析列表
     */
    public void renderMethods(List<DecompilationPipeline.MethodAnalysis> methods) {
        if (methods == null || methods.isEmpty()) {
            setPlaceholder();
            return;
        }
        DecompilationPipeline.ClassAnalysis wrapper =
                new DecompilationPipeline.ClassAnalysis(methods.get(0).owner(), methods);
        render(wrapper);
    }
}
