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
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制流图视图：一个不使用 Canvas 的、基于节点的渲染器。
 *
 * <p>刻意由 JavaFX 的 {@code Shape}/{@code Text} 节点构建，而不用原始
 * {@code Canvas}，这样该图便可由 CSS 皮肤设定样式（每个节点是一个携带
 * 样式类的 {@code Rectangle}），也能被自然地滚动。异常边与普通边的
 * 绘制方式不同。</p>
 *
 * <p>它消费插件 API 中中性的 {@code com.aetherdecompiler.api.CfgView}，
 * 以全限定名引用，因为本视图类的简单名也叫 {@code CfgView}。因此该视图
 * 永不依赖内核的具体 CFG 类型。</p>
 *
 * <p>故事类比：一幅用可移除模板绘制的路网墙图 —— 每个路口都是一块可在
 * 装饰变化时重新上色的标牌。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class CfgView extends ScrollPane {

    private static final double NODE_W = 210;
    private static final double NODE_H = 62;
    private static final double V_GAP = 42;
    private static final double X_COL = 120;

    private final Pane canvas = new Pane();

    /**
     * 创建一个空的 CFG 视图。
     */
    public CfgView() {
        getStyleClass().add("cfg-view");
        canvas.getStyleClass().add("cfg-canvas");
        setContent(canvas);
        setFitToWidth(true);
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

        Map<Integer, double[]> pos = new HashMap<>();
        double y = 24;
        for (com.aetherdecompiler.api.CfgView.Block block : cfg.blocks()) {
            pos.put(block.id(), new double[]{X_COL, y});
            y += NODE_H + V_GAP;
        }

        List<com.aetherdecompiler.api.CfgView.Block> blocks = new ArrayList<>(cfg.blocks());
        for (com.aetherdecompiler.api.CfgView.Block block : blocks) {
            drawBlock(cfg, block, pos.get(block.id()));
        }
        for (com.aetherdecompiler.api.CfgView.Block block : blocks) {
            double[] from = pos.get(block.id());
            for (int succId : block.successors()) {
                double[] to = pos.get(succId);
                if (to != null) {
                    drawEdge(from, to, false);
                }
            }
            for (int succId : block.exceptionSuccessors()) {
                double[] to = pos.get(succId);
                if (to != null) {
                    drawEdge(from, to, true);
                }
            }
        }
        canvas.setPrefHeight(y + 24);
        canvas.setPrefWidth(560);
    }

    private void drawBlock(com.aetherdecompiler.api.CfgView cfg,
                           com.aetherdecompiler.api.CfgView.Block block, double[] xy) {
        Rectangle rect = new Rectangle(NODE_W, NODE_H);
        rect.setX(xy[0]);
        rect.setY(xy[1]);
        rect.setArcWidth(14);
        rect.setArcHeight(14);
        rect.getStyleClass().add("cfg-node");
        if (block.isEntry()) {
            rect.getStyleClass().add("cfg-node-entry");
        }
        Text head = new Text(xy[0] + 12, xy[1] + 22, "B" + block.id()
                + (block.isEntry() ? "  \u25b8 entry" : ""));
        head.getStyleClass().add("cfg-node-id");
        head.setFont(Font.font("monospace", 11));
        StringBuilder bodyText = new StringBuilder();
        List<String> insns = cfg.insnTexts();
        for (int i = block.firstInsn(); i <= block.lastInsn() && i < insns.size(); i++) {
            bodyText.append(insns.get(i)).append('\n');
            if (bodyText.length() > 90) {
                break;
            }
        }
        Text body = new Text(xy[0] + 12, xy[1] + 40, bodyText.toString().stripTrailing());
        body.getStyleClass().add("cfg-node-label");
        body.setFont(Font.font("monospace", 10));
        body.setTextAlignment(TextAlignment.LEFT);
        body.setWrappingWidth(NODE_W - 24);
        canvas.getChildren().addAll(rect, head, body);
    }

    private void drawEdge(double[] from, double[] to, boolean exception) {
        double sx = from[0] + NODE_W / 2;
        double sy = from[1] + (to[1] > from[1] ? NODE_H : 0);
        double ex = to[0] + NODE_W / 2;
        double ey = to[1] + (to[1] > from[1] ? 0 : NODE_H);
        Line line = new Line(sx, sy, ex, ey);
        line.getStyleClass().add("cfg-edge");
        if (exception) {
            line.getStyleClass().add("cfg-edge-exception");
        }
        // 终点处的箭头。
        double angle = Math.atan2(ey - sy, ex - sx);
        double size = 8;
        Polygon arrow = new Polygon(
                ex, ey,
                ex - size * Math.cos(angle - Math.PI / 6), ey - size * Math.sin(angle - Math.PI / 6),
                ex - size * Math.cos(angle + Math.PI / 6), ey - size * Math.sin(angle + Math.PI / 6));
        arrow.getStyleClass().add(exception ? "cfg-arrow-exception" : "cfg-arrow");
        canvas.getChildren().addAll(line, arrow);
    }
}
