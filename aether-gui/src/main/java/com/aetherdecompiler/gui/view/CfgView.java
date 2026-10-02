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
 * The control-flow graph view: a Canvas-free, node-based renderer.
 *
 * <p>Deliberately built from JavaFX {@code Shape}/{@code Text} nodes rather than
 * a raw {@code Canvas}, so the graph is styleable by CSS skins (each node is a
 * {@code Rectangle} carrying a style class) and so it can be scrolled naturally.
 * Exception edges are drawn distinct from normal edges.</p>
 *
 * <p>It consumes the neutral {@code com.aetherdecompiler.api.CfgView} from the
 * plugin API, referenced by its fully-qualified name because this view class
 * shares the simple name {@code CfgView}. The view therefore never depends on
 * the kernel's concrete CFG type.</p>
 *
 * <p>Story analogy: a wall-map of a road network drawn with removable stencils —
 * each junction is a placard you can repaint when the decor changes.</p>
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
     * Create an empty CFG view.
     */
    public CfgView() {
        getStyleClass().add("cfg-view");
        canvas.getStyleClass().add("cfg-canvas");
        setContent(canvas);
        setFitToWidth(true);
    }

    /**
     * Render a control-flow graph.
     *
     * @param cfg the neutral CFG view (may be {@code null})
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
        // Arrow head at the end point.
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
