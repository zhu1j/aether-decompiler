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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * The bytecode view: a list of bytecode instructions with offset, index, and
 * rendered operand text, with a highlight method for three-view linkage.
 *
 * @author Jerry Zhu (Zeek)
 */
public final class BytecodeView extends VBox {

    /**
     * One displayable bytecode row.
     *
     * @param offset   the bytecode offset
     * @param index    the instruction ordinal
     * @param mnemonic the opcode mnemonic
     * @param operand  the rendered operand, possibly empty
     * @author Jerry Zhu (Zeek)
     */
    public record Row(int offset, int index, String mnemonic, String operand) {
        @Override
        public String toString() {
            return String.format("%4d  %3d  %-14s %s", offset, index, mnemonic, operand);
        }
    }

    private final ListView<Row> list = new ListView<>();

    /**
     * Create the bytecode view.
     */
    public BytecodeView() {
        getStyleClass().add("bytecode-view");
        list.getStyleClass().add("bytecode-list");
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Row row, boolean empty) {
                super.updateItem(row, empty);
                setText(empty || row == null ? null : row.toString());
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        getChildren().add(list);
    }

    /**
     * @param rows the rows to display
     */
    public void setRows(List<Row> rows) {
        ObservableList<Row> data = FXCollections.observableArrayList(rows);
        list.setItems(data);
    }

    /**
     * Highlight a row by index.
     *
     * @param index the instruction ordinal to select
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
}
