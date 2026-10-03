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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;
import java.util.function.Consumer;

/**
 * 字节码视图：一个字节码指令列表，含偏移、索引与渲染后的操作数文本，
 * 并带有一个用于三视图联动的高亮方法。
 *
 * @author Jerry Zhu (Zeek)
 */
public final class BytecodeView extends VBox {

    /**
     * 一行可显示的字节码。
     *
     * @param offset   字节码偏移
     * @param index    指令序号
     * @param mnemonic 操作码助记符
     * @param operand  渲染后的操作数，可能为空
     * @author Jerry Zhu (Zeek)
     */
    public record Row(int offset, int index, String mnemonic, String operand) {
        @Override
        public String toString() {
            return String.format("%4d  %3d  %-14s %s", offset, index, mnemonic, operand);
        }
    }

    private final ListView<Row> list = new ListView<>();
    private Consumer<Row> rowListener;

    /**
     * 创建字节码视图。
     */
    public BytecodeView() {
        getStyleClass().add("bytecode-view");
        list.getStyleClass().add("bytecode-list");
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Row row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                // IDEA 风格分色：偏移（暗）、索引（强调色）、助记符（次强调+粗体）、操作数（正文）。
                setText(null);
                TextFlow flow = new TextFlow(
                        token(String.format("%4d", row.offset()), "tok-offset"),
                        token(String.format("  %3d", row.index()), "tok-index"),
                        token(String.format("  %-14s", row.mnemonic()), "tok-mnemonic"),
                        token(row.operand() == null ? "" : row.operand(), "tok-operand"));
                setGraphic(flow);
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        // 选中字节码行 → 回调，用于三视图联动（联动右侧检查器 + 源码定位）。
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row != null && rowListener != null) {
                rowListener.accept(row);
            }
        });
        getChildren().add(list);
    }

    /**
     * 注册字节码行选中回调。
     *
     * @param listener 回调；接收被选中的行
     */
    public void setOnRowSelected(Consumer<Row> listener) {
        this.rowListener = listener;
    }

    /**
     * @param rows 要显示的行
     */
    public void setRows(List<Row> rows) {
        ObservableList<Row> data = FXCollections.observableArrayList(rows);
        list.setItems(data);
    }

    /**
     * 按索引高亮一行。
     *
     * @param index 要选中的指令序号
     */
    public void highlight(int index) {
        for (Row row : list.getItems()) {
            if (row.index() == index) {
                list.getSelectionModel().select(row);
                list.scrollTo(row);
                return;
            }
        }
    }

    /** 构造一个携带分色样式类的文本片段。 */
    private static Text token(String text, String styleClass) {
        Text t = new Text(text);
        t.getStyleClass().add(styleClass);
        return t;
    }
}
