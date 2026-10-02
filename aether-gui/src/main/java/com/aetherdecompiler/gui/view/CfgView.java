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

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制流图视图：一个不使用 Canvas 的、基于节点的渲染器。
 *
 * <p>本轮重绘解决了旧版的三个观感问题：</p>
 * <ol>
 *   <li><b>节点高度固定导致文字溢出</b> —— 现在每个节点的高度按该基本块的
 *       指令行数动态计算，长指令按近似列宽折行，绝不会溢出边框。</li>
 *   <li><b>单列“一条线”布局</b> —— 现在用从入口基本块出发的 BFS 层次做
 *       分层布局：同一层的基本块并排成一行，层与层之间垂直堆叠，图结构
 *       一眼可见。</li>
 *   <li><b>粗糙的直连边</b> —— 现在用正交折线（先下后横再下）连接，前向
 *       边居中直落，回边（指向更上层）从右侧绕行，末端一律带箭头。</li>
 * </ol>
 *
 * <p>刻意由 JavaFX 的 {@code Shape}/{@code Text} 节点构建，而不用原始
 * {@code Canvas}，这样该图便可由 CSS 皮肤设定样式，也能被自然地滚动与
 * 平移。它消费插件 API 中中性的 {@code com.aetherdecompiler.api.CfgView}，
 * 以全限定名引用，因为本视图类的简单名也叫 {@code CfgView}。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class CfgView extends ScrollPane {

    private static final double NODE_W = 300;
    private static final double HEADER_H = 30;
    private static final double LINE_H = 16;
    private static final double PAD_BOTTOM = 14;
    private static final double H_GAP = 72;
    private static final double V_GAP = 84;
    private static final double MARGIN = 40;
    private static final double BODY_WRAP = NODE_W - 28;
    private static final int MAX_WRAP_CHARS = 46;
    private static final int MAX_LINES_PER_NODE = 16;

    private final Pane canvas = new Pane();

    /**
     * 创建一个空的 CFG 视图。
     */
    public CfgView() {
        getStyleClass().add("cfg-view");
        canvas.getStyleClass().add("cfg-canvas");
        setContent(canvas);
        setFitToWidth(false);
        setFitToHeight(false);
        setPannable(true);
        canvas.setMinWidth(680);
        canvas.setMinHeight(440);
    }

    /**
     * 渲染一个控制流图。
     *
     * @param cfg 中性的 CFG 视图（可为 {@code null}）
     */
    public void render(com.aetherdecompiler.api.CfgView cfg) {
        canvas.getChildren().clear();
        if (cfg == null || cfg.blocks().isEmpty()) {
            return;
        }

        List<com.aetherdecompiler.api.CfgView.Block> blocks = new ArrayList<>(cfg.blocks());
        List<String> insns = cfg.insnTexts();

        // 1) 用 BFS 层次给每个基本块分层。
        Map<Integer, Integer> level = computeLevels(blocks);
        int maxLevel = 0;
        for (int l : level.values()) {
            maxLevel = Math.max(maxLevel, l);
        }
        List<List<com.aetherdecompiler.api.CfgView.Block>> rows = new ArrayList<>();
        for (int i = 0; i <= maxLevel; i++) {
            rows.add(new ArrayList<>());
        }
        for (com.aetherdecompiler.api.CfgView.Block b : blocks) {
            rows.get(level.get(b.id())).add(b);
        }
        for (List<com.aetherdecompiler.api.CfgView.Block> row : rows) {
            row.sort(Comparator.comparingInt(com.aetherdecompiler.api.CfgView.Block::id));
        }

        // 2) 逐块计算显示行与动态高度。
        Map<Integer, List<String>> bodyLines = new HashMap<>();
        Map<Integer, double[]> size = new HashMap<>();
        for (com.aetherdecompiler.api.CfgView.Block b : blocks) {
            List<String> ls = wrapInsns(insns, b);
            bodyLines.put(b.id(), ls);
            double h = HEADER_H + Math.max(1, ls.size()) * LINE_H + PAD_BOTTOM;
            size.put(b.id(), new double[]{NODE_W, h});
        }

        // 3) 逐层摆放：同层并排，层间垂直堆叠。
        Map<Integer, double[]> pos = new HashMap<>();
        double y = MARGIN;
        for (List<com.aetherdecompiler.api.CfgView.Block> row : rows) {
            if (row.isEmpty()) {
                continue;
            }
            double rowH = 0;
            double x = MARGIN;
            for (com.aetherdecompiler.api.CfgView.Block b : row) {
                double h = size.get(b.id())[1];
                pos.put(b.id(), new double[]{x, y});
                x += NODE_W + H_GAP;
                rowH = Math.max(rowH, h);
            }
            y += rowH + V_GAP;
        }

        double maxRight = MARGIN;
        for (double[] p : pos.values()) {
            maxRight = Math.max(maxRight, p[0] + NODE_W);
        }

        // 4) 先画边（位于节点之下），再画节点。
        for (com.aetherdecompiler.api.CfgView.Block b : blocks) {
            double[] from = pos.get(b.id());
            double[] fs = size.get(b.id());
            for (int s : b.successors()) {
                drawEdge(from, fs, pos.get(s), size.get(s), maxRight, false);
            }
            for (int s : b.exceptionSuccessors()) {
                drawEdge(from, fs, pos.get(s), size.get(s), maxRight, true);
            }
        }
        for (com.aetherdecompiler.api.CfgView.Block b : blocks) {
            drawBlock(b, pos.get(b.id()), size.get(b.id()), bodyLines.get(b.id()));
        }

        double bottom = y;
        for (com.aetherdecompiler.api.CfgView.Block b : blocks) {
            double[] p = pos.get(b.id());
            bottom = Math.max(bottom, p[1] + size.get(b.id())[1]);
        }
        canvas.setPrefWidth(Math.max(maxRight + MARGIN, 680));
        canvas.setPrefHeight(Math.max(bottom + 60, 440));
    }

    private Map<Integer, Integer> computeLevels(List<com.aetherdecompiler.api.CfgView.Block> blocks) {
        Map<Integer, List<Integer>> succ = new HashMap<>();
        com.aetherdecompiler.api.CfgView.Block entry = null;
        for (com.aetherdecompiler.api.CfgView.Block b : blocks) {
            if (b.isEntry() && entry == null) {
                entry = b;
            }
            List<Integer> targets = new ArrayList<>();
            for (int s : b.successors()) {
                targets.add(s);
            }
            for (int s : b.exceptionSuccessors()) {
                targets.add(s);
            }
            succ.put(b.id(), targets);
        }
        if (entry == null) {
            entry = blocks.get(0);
        }

        Map<Integer, Integer> level = new HashMap<>();
        Deque<Integer> queue = new ArrayDeque<>();
        level.put(entry.id(), 0);
        queue.add(entry.id());
        while (!queue.isEmpty()) {
            int u = queue.poll();
            int next = level.get(u) + 1;
            for (int v : succ.getOrDefault(u, List.of())) {
                if (!level.containsKey(v)) {
                    level.put(v, next);
                    queue.add(v);
                }
            }
        }
        // 不可达（或被异常处理遮蔽）的基本块统一放到最后一行。
        int fallback = 0;
        for (int l : level.values()) {
            fallback = Math.max(fallback, l + 1);
        }
        for (com.aetherdecompiler.api.CfgView.Block b : blocks) {
            level.putIfAbsent(b.id(), fallback);
        }
        return level;
    }

    private List<String> wrapInsns(List<String> insns, com.aetherdecompiler.api.CfgView.Block block) {
        List<String> out = new ArrayList<>();
        for (int i = block.firstInsn(); i <= block.lastInsn() && i < insns.size(); i++) {
            String s = insns.get(i);
            if (s == null) {
                continue;
            }
            String rest = s;
            while (rest.length() > MAX_WRAP_CHARS) {
                out.add(rest.substring(0, MAX_WRAP_CHARS));
                rest = rest.substring(MAX_WRAP_CHARS);
            }
            out.add(rest);
            if (out.size() >= MAX_LINES_PER_NODE) {
                out.add("…");
                break;
            }
        }
        if (out.isEmpty()) {
            out.add("(无指令)");
        }
        return out;
    }

    private void drawBlock(com.aetherdecompiler.api.CfgView.Block block,
                           double[] xy, double[] size, List<String> lines) {
        Rectangle rect = new Rectangle(size[0], size[1]);
        rect.setX(xy[0]);
        rect.setY(xy[1]);
        rect.setArcWidth(16);
        rect.setArcHeight(16);
        rect.getStyleClass().add("cfg-node");
        if (block.isEntry()) {
            rect.getStyleClass().add("cfg-node-entry");
        }

        // 标题与正文之间的分隔线。
        Rectangle sep = new Rectangle(size[0] - 2, 1);
        sep.setX(xy[0] + 1);
        sep.setY(xy[1] + HEADER_H - 6);
        sep.getStyleClass().add("cfg-node-sep");

        Text head = new Text(xy[0] + 14, xy[1] + 20,
                "B" + block.id() + (block.isEntry() ? "   \u25B8 entry" : ""));
        head.getStyleClass().add("cfg-node-id");
        head.setFont(Font.font("monospace", 12));

        canvas.getChildren().addAll(rect, sep, head);

        double ty = xy[1] + HEADER_H + 8;
        for (String line : lines) {
            Text t = new Text(xy[0] + 14, ty, line);
            t.getStyleClass().add("cfg-node-label");
            t.setFont(Font.font("monospace", 11));
            t.setWrappingWidth(BODY_WRAP);
            canvas.getChildren().add(t);
            ty += LINE_H;
        }
    }

    private void drawEdge(double[] from, double[] fs, double[] to, double[] ts,
                          double maxRight, boolean exception) {
        if (to == null || ts == null) {
            return;
        }
        double sx = from[0] + fs[0] / 2;
        double sy = from[1] + fs[1];
        double ex = to[0] + ts[0] / 2;
        double ey = to[1];

        Polyline poly = new Polyline();
        if (ey > sy) {
            // 前向边：先下到中缝，横向，再下落进目标顶部。
            double midY = (sy + ey) / 2;
            poly.getPoints().addAll(sx, sy, sx, midY, ex, midY, ex, ey);
        } else {
            // 回边：从右侧绕行，避免穿过其它节点。
            double rightX = maxRight + 30;
            poly.getPoints().addAll(
                    sx, sy,
                    sx, sy + 16,
                    rightX, sy + 16,
                    rightX, ey - 18,
                    ex, ey - 18,
                    ex, ey);
        }
        poly.setFill(null);
        poly.getStyleClass().add("cfg-edge");
        if (exception) {
            poly.getStyleClass().add("cfg-edge-exception");
        }

        // 末端箭头：一律向下指向目标节点顶部。
        Polygon arrow = new Polygon(ex, ey, ex - 5, ey - 9, ex + 5, ey - 9);
        arrow.getStyleClass().add(exception ? "cfg-arrow-exception" : "cfg-arrow");

        canvas.getChildren().addAll(poly, arrow);
    }
}
