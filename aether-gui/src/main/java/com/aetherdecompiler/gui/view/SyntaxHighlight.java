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

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 共享的 Java 语法高亮器。
 *
 * <p>源码视图与分析视图都需要“按 Java 语法给文本分色”，若各自维护一份正则就会
 * 出现两套实现悄悄漂移的风险。这里把词法规则收敛成单一来源：两个视图都调用
 * {@link #compute(String)}，配色则统一交给 CSS 皮肤里的 {@code .keyword} /
 * {@code .string} / {@code .comment} 等样式类决定，因此换皮肤时无需改动任何 Java 代码。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
final class SyntaxHighlight {

    private SyntaxHighlight() {
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
    private static final String COMMENT_PATTERN = "//[^\\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
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

    /**
     * 计算一段 Java 文本的样式区间。
     *
     * @param text 源码文本
     * @return 可直接交给 RichTextFX 的样式区间
     */
    static StyleSpans<Collection<String>> compute(String text) {
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
