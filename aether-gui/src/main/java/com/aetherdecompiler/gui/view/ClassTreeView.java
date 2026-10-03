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
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * 类导航器：某个来源中类名的可搜索树。
 *
 * <p>选中一个类会触发回调，以便应用驱动引擎；视图自身不持有引擎引用。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class ClassTreeView extends VBox {

    /** 选中叶子时以内部类名调用的回调。 */
    public interface ClassSelectListener {
        /**
         * @param internalName 内部二进制名，例如 {@code com/foo/Bar}
         */
        void onSelect(String internalName);
    }

    private final TextField filter = new TextField();
    private final TreeView<String> tree = new TreeView<>();
    private ClassSelectListener listener;
    private List<String> allNames = new ArrayList<>();

    /**
     * 创建导航器。
     */
    public ClassTreeView() {
        getStyleClass().add("class-tree-view");
        Label header = new Label("结构");
        header.getStyleClass().add("panel-header");
        filter.setPromptText("搜索类\u2026");
        filter.getStyleClass().add("filter-field");
        filter.textProperty().addListener((obs, old, val) -> rebuild(val));
        VBox.setVgrow(tree, Priority.ALWAYS);
        getChildren().addAll(header, filter, tree);

        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && val.isLeaf() && listener != null) {
                String name = val.getValue();
                if (name != null && !name.isEmpty()) {
                    listener.onSelect(name);
                }
            }
        });
    }

    /**
     * @param listener 选择回调
     */
    public void setClassSelectListener(ClassSelectListener listener) {
        this.listener = listener;
    }

    /**
     * 用类名填充该树。
     *
     * @param internalNames 内部二进制名
     * @param sourceLabel   根节点的标签（例如 jar 名）
     */
    public void setClasses(List<String> internalNames, String sourceLabel) {
        this.allNames = new ArrayList<>(internalNames);
        tree.setRoot(buildRoot(allNames, sourceLabel));
        rebuild(filter.getText());
    }

    private void rebuild(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String n : allNames) {
            if (q.isEmpty() || n.toLowerCase().contains(q)) {
                filtered.add(n);
            }
        }
        String label = tree.getRoot() == null ? "source" : tree.getRoot().getValue();
        TreeItem<String> root = buildRoot(filtered, label);
        tree.setRoot(root);
    }

    private TreeItem<String> buildRoot(List<String> names, String label) {
        TreeItem<String> root = new TreeItem<>(label);
        root.setExpanded(true);
        // 按包分组（最后一个斜杠之前的所有内容）。
        java.util.Map<String, TreeItem<String>> packages = new java.util.LinkedHashMap<>();
        for (String name : names) {
            int slash = name.lastIndexOf('/');
            String pkg = slash < 0 ? "(default)" : name.substring(0, slash);
            TreeItem<String> pkgItem = packages.computeIfAbsent(pkg, p -> {
                TreeItem<String> item = new TreeItem<>(p);
                item.setExpanded(true);
                root.getChildren().add(item);
                return item;
            });
            String simple = slash < 0 ? name : name.substring(slash + 1);
            pkgItem.getChildren().add(new TreeItem<>(simple));
        }
        return root;
    }
}
