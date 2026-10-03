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

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * “反编译输出”导航器：把输出目录里生成的 {@code .java} 文件按目录结构
 * 呈现成一棵树。
 *
 * <p>它和左侧上方的 {@link ClassTreeView} 并行存在：上方显示待反编译来源
 * （JAR / class 目录）的类结构，本视图显示反编译<em>结果</em>的源码文件
 * 结构。选中一个 {@code .java} 叶子会触发回调，由应用把该文件内容加载进
 * 源码视图。</p>
 *
 * <p>它刻意复用 {@code class-tree-view} 的样式类，从而与类导航器保持
 * 一致的外观，同时保留自己的 {@code output-tree-view} 身份。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class OutputTreeView extends VBox {

    /** 选中某个 {@code .java} 文件时调用的回调。 */
    public interface FileSelectListener {
        /**
         * @param file 被选中的 {@code .java} 文件
         */
        void onSelect(Path file);
    }

    private final TextField filter = new TextField();
    private final TreeView<Path> tree = new TreeView<>();
    private FileSelectListener listener;
    private Path root;
    private List<Path> allFiles = new ArrayList<>();

    /**
     * 创建输出导航器。
     */
    public OutputTreeView() {
        getStyleClass().addAll("class-tree-view", "output-tree-view");
        Label header = new Label("反编译输出");
        header.getStyleClass().add("panel-header");
        filter.setPromptText("搜索文件\u2026");
        filter.getStyleClass().add("filter-field");
        filter.textProperty().addListener((obs, old, val) -> rebuild(val));
        VBox.setVgrow(tree, Priority.ALWAYS);

        tree.setCellFactory(v -> new TreeCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : display(item));
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && val.isLeaf() && val.getValue() != null && listener != null) {
                listener.onSelect(val.getValue());
            }
        });

        getChildren().addAll(header, filter, tree);
    }

    /**
     * @param listener 文件选择回调
     */
    public void setFileSelectListener(FileSelectListener listener) {
        this.listener = listener;
    }

    /**
     * 重新扫描输出目录并重建该树。
     *
     * @param outputRoot 输出根目录（可为 {@code null}）
     */
    public void setRoot(Path outputRoot) {
        this.root = outputRoot;
        this.allFiles = new ArrayList<>();
        if (outputRoot != null && Files.isDirectory(outputRoot)) {
            try (Stream<Path> walk = Files.walk(outputRoot)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".java"))
                        .forEach(allFiles::add);
            } catch (IOException ignored) {
                // 目录可能正在被写入；忽略本次扫描错误，下次再刷。
            }
        }
        rebuild(filter.getText());
    }

    private String display(Path p) {
        if (root != null && p.startsWith(root) && !p.equals(root)) {
            return root.relativize(p).toString();
        }
        Path fn = p.getFileName();
        return fn == null ? p.toString() : fn.toString();
    }

    private void rebuild(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Path> filtered = new ArrayList<>();
        for (Path p : allFiles) {
            if (q.isEmpty() || p.toString().toLowerCase().contains(q)) {
                filtered.add(p);
            }
        }
        tree.setRoot(buildTree(filtered));
    }

    private TreeItem<Path> buildTree(List<Path> files) {
        TreeItem<Path> rootItem = new TreeItem<>(root != null ? root : Path.of("(未设置输出目录)"));
        rootItem.setExpanded(true);
        if (root == null) {
            return rootItem;
        }
        Map<String, TreeItem<Path>> dirs = new LinkedHashMap<>();
        for (Path f : files) {
            Path rel = root.relativize(f);
            TreeItem<Path> parent = rootItem;
            Path acc = root;
            for (int i = 0; i < rel.getNameCount() - 1; i++) {
                acc = acc.resolve(rel.getName(i).toString());
                TreeItem<Path> dir = dirs.get(acc.toString());
                if (dir == null) {
                    dir = new TreeItem<>(acc);
                    dir.setExpanded(true);
                    parent.getChildren().add(dir);
                    dirs.put(acc.toString(), dir);
                }
                parent = dir;
            }
            parent.getChildren().add(new TreeItem<>(f));
        }
        return rootItem;
    }
}
