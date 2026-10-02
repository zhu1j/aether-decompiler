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

import com.aetherdecompiler.api.AetherEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * 事件流视图：发布在引擎总线上流水线事件的实时日志。这是硬性约束 #8
 * （每一步中间结果都可观测）的可见一面。
 *
 * <p>事件可能来自后台反编译线程，因此摄入被编组到 JavaFX 线程上。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class EventsView extends VBox {

    private final ObservableList<String> events = FXCollections.observableArrayList();
    private final ListView<String> list = new ListView<>(events);

    /**
     * 创建事件视图。
     */
    public EventsView() {
        getStyleClass().add("events-view");
        list.getStyleClass().add("events-list");
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeIf(c -> c.startsWith("stage-"));
                if (!empty && item != null) {
                    getStyleClass().add("stage-row");
                }
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        getChildren().add(list);
    }

    /**
     * 向日志追加一个引擎事件（线程安全）。
     *
     * @param event 要记录的事件
     */
    public void append(AetherEvent event) {
        String line = "[" + event.phase() + "/" + event.kind() + "]  " + event.message();
        Platform.runLater(() -> {
            events.add(line);
            if (events.size() > 2000) {
                events.remove(0);
            }
            list.scrollTo(events.size() - 1);
        });
    }

    /** 清空日志。 */
    public void clear() {
        Platform.runLater(events::clear);
    }
}
