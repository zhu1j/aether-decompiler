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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Java source view: a RichTextFX {@link CodeArea} with line numbers and
 * syntax highlighting, plus a click callback for three-view linkage.
 *
 * <p>This view renders whatever text the kernel/plugin produced; it is purely an
 * application-layer widget. It contains no decompilation logic.</p>
 *
 * <p>Story analogy: the sheet of paper held up to the light. The paper shows the
 * text; the printing press that made it is elsewhere.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class CodeEditorView extends VBox {

    /** Callback invoked with the 1-based line the user clicked. */
    public interface LineClickListener {
        /**
         * @param line the 1-based line number
         */
        void onLine(int line);
    }

    private static final String[] KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while", "var", "record", "sealed", "permits", "yield"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "[()]";
    private static final String BRACE_PATTERN = "[{}]";
    private static final String BRACKET_PATTERN = "[\\[\\]]";
    private static final String SEMICOLON_PATTERN = ";";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String CHAR_PATTERN = "'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
    private static final String NUMBER_PATTERN = "\\b\\d[\\d_]*(\\.[\\d_]+)?[fFdDlL]?\\b";
    private static final String ANNOTATION_PATTERN = "@\\w+";

    private static final Pattern HIGHLIGHT = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<CHAR>" + CHAR_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")");

    private final CodeArea codeArea = new CodeArea();
    private LineClickListener lineClickListener;

    /**
     * Create the editor view.
     */
    public CodeEditorView() {
        getStyleClass().add("code-editor");
        codeArea.getStyleClass().add("code-area");
        codeArea.setEditable(false);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));
        codeArea.setOnMouseClicked(evt -> {
            if (lineClickListener != null) {
                int line = codeArea.getCurrentParagraph() + 1;
                lineClickListener.onLine(line);
            }
        });
        VBox.setVgrow(codeArea, Priority.ALWAYS);
        getChildren().add(codeArea);
    }

    /**
     * @param text the source text to display (may be {@code null})
     */
    public void setSource(String text) {
        codeArea.replaceText(text == null ? "" : text);
        codeArea.moveTo(0);
        codeArea.requestFollowCaret();
    }

    /**
     * Highlight a line by selecting it (used for three-view linkage).
     *
     * @param oneBasedLine the 1-based line to focus
     */
    public void focusLine(int oneBasedLine) {
        int para = Math.max(0, Math.min(oneBasedLine - 1, codeArea.getParagraphs().size() - 1));
        int start = codeArea.getAbsolutePosition(para, 0);
        int end = start + codeArea.getParagraph(para).length();
        codeArea.selectRange(start, end);
        codeArea.requestFollowCaret();
    }

    /**
     * @param listener the click listener to register
     */
    public void setLineClickListener(LineClickListener listener) {
        this.lineClickListener = listener;
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = HIGHLIGHT.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastKwEnd = 0;
        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword"
                            : matcher.group("STRING") != null ? "string"
                            : matcher.group("CHAR") != null ? "string"
                            : matcher.group("COMMENT") != null ? "comment"
                            : matcher.group("NUMBER") != null ? "number"
                            : matcher.group("ANNOTATION") != null ? "annotation"
                            : matcher.group("PAREN") != null ? "paren"
                            : matcher.group("BRACE") != null ? "brace"
                            : matcher.group("BRACKET") != null ? "bracket"
                            : matcher.group("SEMICOLON") != null ? "semicolon"
                            : null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
