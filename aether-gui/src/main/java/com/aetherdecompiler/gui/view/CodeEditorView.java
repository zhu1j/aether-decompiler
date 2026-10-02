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
 * Java 源码视图：一个带行号与语法高亮的 RichTextFX {@link CodeArea}，
 * 外加一个用于三视图联动的点击回调。
 *
 * <p>该视图渲染内核/插件产出的任意文本；它纯粹是一个应用层部件，
 * 不含任何反编译逻辑。</p>
 *
 * <p>故事类比：举向灯光的那张纸。纸显示文本；印刷它的印刷机在别处。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class CodeEditorView extends VBox {

    /** 以用户点击的、从 1 开始的行号调用的回调。 */
    public interface LineClickListener {
        /**
         * @param line 从 1 开始的行号
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
     * 创建编辑器视图。
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
     * @param text 要显示的源码文本（可为 {@code null}）
     */
    public void setSource(String text) {
        codeArea.replaceText(text == null ? "" : text);
        codeArea.moveTo(0);
        codeArea.requestFollowCaret();
    }

    /**
     * 通过选中某一行来高亮它（用于三视图联动）。
     *
     * @param oneBasedLine 要聚焦的、从 1 开始的行号
     */
    public void focusLine(int oneBasedLine) {
        int para = Math.max(0, Math.min(oneBasedLine - 1, codeArea.getParagraphs().size() - 1));
        int start = codeArea.getAbsolutePosition(para, 0);
        int end = start + codeArea.getParagraph(para).length();
        codeArea.selectRange(start, end);
        codeArea.requestFollowCaret();
    }

    /**
     * @param listener 要注册的点击监听器
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
