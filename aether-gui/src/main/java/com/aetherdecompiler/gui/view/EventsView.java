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

import com.aetherdecompiler.api.AetherEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The event-stream view: a live log of pipeline events published on the engine's
 * bus. This is the visible face of hard constraint #8 (every intermediate step
 * is observable).
 *
 * <p>Events may arrive from a background decompilation thread, so ingestion is
 * marshalled onto the JavaFX thread.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class EventsView extends VBox {

    private final ObservableList<String> events = FXCollections.observableArrayList();
    private final ListView<String> list = new ListView<>(events);

    /**
     * Create the event view.
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
     * Append one engine event to the log (thread-safe).
     *
     * @param event the event to log
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

    /** Clear the log. */
    public void clear() {
        Platform.runLater(events::clear);
    }
}
