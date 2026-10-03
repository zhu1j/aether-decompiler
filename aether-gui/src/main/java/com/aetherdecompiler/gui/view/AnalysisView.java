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

import com.aetherdecompiler.api.SourceMapping;
import com.aetherdecompiler.api.SourceTree;
import com.aetherdecompiler.core.ast.JavaAstRenderer;
import com.aetherdecompiler.core.engine.DecompilationPipeline;
import com.aetherdecompiler.core.mapping.DefaultSourceMapping;
import com.aetherdecompiler.core.model.ClassModel;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
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

    /** 每个方法在 AST 文本中的起始行偏移 + 其源码映射，用于“选中指令 → 定位 AST 行”。 */
    private final List<MethodRegion> astRegions = new ArrayList<>();

    /**
     * 单个方法在 AST 文本区中占据的行区间与其源码映射。
     *
     * @param startLine 该方法内容首行在 AST 文本区中的 1 基行号
     * @param mapping   该方法渲染时产生的源码映射（提供指令 → 文本的反查）
     */
    private record MethodRegion(int startLine, DefaultSourceMapping mapping) {
    }

    /**
     * 创建分析视图。
     */
    public AnalysisView() {
        getStyleClass().add("analysis-view");

        // 缺陷修复：此前把 labeled(...) 返回的 VBox 又套进一层 VBox，内层 VBox 拿不到
        // 纵向拉伸，内容只按“内容自身高度”顶在上半部分，整块分析区看起来“只显示了一半”。
        // 这里直接使用已含“标题 + 代码区”的 VBox 作为分割面板的两项，让内容铺满全高。
        VBox ssaPane = labeled("SSA 形式（phi 节点 / 定值-使用）", ssaArea);
        VBox astPane = labeled("AST → Java（结构化控制流）", astArea);
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
        astRegions.clear();
        int astLine = 1;
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
                // 操作数栈提升（C1）：展示隐式栈被符号化后的规模与汇合处的栈 phi。
                var lift = m.ssa().stackLift();
                ssa.append("    栈提升: 最大栈深 ").append(lift.maxDepth())
                        .append("，栈单元 ").append(lift.cellCount())
                        .append("，栈 phi ").append(lift.phiCount()).append('\n');
                lift.phis().forEach(phi -> ssa.append("    ").append(phi.display()).append('\n'));
                if (lift.isApproximate()) {
                    ssa.append("    （近似：").append(lift.approximateReason()).append("）\n");
                }
            }
            SourceTree tree = renderer.render(m.ast());
            if (tree != null && tree.content() != null) {
                // 缺陷修复：记录每个方法在 AST 文本区中的起始行与其源码映射，
                // 使“选中源码行/指令”能够反向定位到 AST 栏中的对应文本并高亮。
                if (tree.mapping() instanceof DefaultSourceMapping dsm) {
                    astRegions.add(new MethodRegion(astLine, dsm));
                }
                String content = tree.content();
                ast.append(content).append('\n');
                astLine += countLines(content) + 1;
            }
        }
        if (methodCount == 0) {
            ssa.append("（无具体方法）\n");
        }
        ssaArea.replaceText(ssa.toString());
        astArea.replaceText(ast.length() == 0 ? "// （无可渲染的 AST）\n" : ast.toString());
    }

    /** 统计一段文本包含的行数（以 {@code '\n'} 计）。 */
    private static int countLines(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    /**
     * 按指令索引把 AST 文本区滚动到对应位置并选中该段文本。
     *
     * <p>缺陷修复：用户点击源码某一行时，此前 AST 栏毫无反应。现在借助每个方法渲染时
     * 产出的源码映射，把“字节码指令索引”反查为“AST 文本区中的行列”，滚动过去并高亮，
     * 让联动真正闭环。</p>
     *
     * @param insnIndex 目标指令索引；{@code < 0} 表示清空选中
     */
    public void focusInsn(int insnIndex) {
        if (insnIndex < 0 || astRegions.isEmpty()) {
            clearSelection();
            return;
        }
        for (MethodRegion region : astRegions) {
            List<SourceMapping.Span> spans = region.mapping().atInsn(insnIndex);
            if (spans.isEmpty()) {
                continue;
            }
            SourceMapping.Span s = spans.get(0);
            int line = region.startLine() + (s.generatedStartLine() - 1);
            int lineEnd = region.startLine() + (s.generatedEndLine() - 1);
            String[] lines = astArea.getText().split("\n", -1);
            if (line < 1 || line > lines.length) {
                continue;
            }
            int start = offsetOfLine(lines, line) + Math.max(0, s.generatedStartCol());
            int endLine = Math.min(Math.max(lineEnd, line), lines.length);
            int end = offsetOfLine(lines, endLine) + Math.max(0, s.generatedEndCol());
            start = clamp(start, 0, astArea.getLength());
            end = clamp(end, start, astArea.getLength());
            astArea.selectRange(start, end);
            astArea.requestFollowCaret();
            astArea.requestFocus();
            return;
        }
        clearSelection();
    }

    /** 清空 AST 文本区的选择。 */
    private void clearSelection() {
        astArea.deselect();
    }

    private static int offsetOfLine(String[] lines, int oneBasedLine) {
        int off = 0;
        for (int i = 0; i < oneBasedLine - 1 && i < lines.length; i++) {
            off += lines[i].length() + 1;
        }
        return off;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
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
