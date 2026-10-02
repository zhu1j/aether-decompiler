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

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The class navigator: a searchable tree of the class names in a source.
 *
 * <p>Selecting a class fires a callback so the application can drive the engine;
 * the view itself holds no engine reference.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class ClassTreeView extends VBox {

    /** Callback invoked with the internal class name when a leaf is chosen. */
    public interface ClassSelectListener {
        /**
         * @param internalName the internal binary name, e.g. {@code com/foo/Bar}
         */
        void onSelect(String internalName);
    }

    private final TextField filter = new TextField();
    private final TreeView<String> tree = new TreeView<>();
    private ClassSelectListener listener;
    private List<String> allNames = new ArrayList<>();

    /**
     * Create the navigator.
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
     * @param listener the selection callback
     */
    public void setClassSelectListener(ClassSelectListener listener) {
        this.listener = listener;
    }

    /**
     * Populate the tree with class names.
     *
     * @param internalNames the internal binary names
     * @param sourceLabel   a label for the root node (e.g. the jar name)
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
        // Group by package (everything before the last '/').
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
