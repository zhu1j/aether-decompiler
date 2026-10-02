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
package com.aetherdecompiler.gui.background;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 随仓库内置的默认背景集合。
 *
 * <p>仓库在 {@code aether-gui/src/main/resources/backgrounds/} 下携带一批预览图
 * （{@code bundled/*.jpg|*.gif}）与一份清单（{@code manifest.json}）。首次启动时，
 * 本类把这些内置图 <b>播种（seed）</b> 到用户的背景库目录
 * {@code ~/.aether/backgrounds}，使新用户开箱即有一批可选背景。</p>
 *
 * <p>播种是 <b>幂等</b> 的：只补齐缺失的文件，绝不覆盖用户已存在或已替换的同名文件，
 * 因此用户删掉的默认背景不会被强行塞回（除非整目录被清空重来）。</p>
 *
 * <p>故事类比：随书附赠的一盒样片 —— 第一次打开时摆进你的放映档案，
 * 之后你怎么整理，它都不会擅自插手。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class BundledBackdrops {

    /** 清单在 classpath 上的位置。 */
    private static final String MANIFEST = "/backgrounds/manifest.json";
    /** 内置图在 classpath 上的目录前缀。 */
    private static final String BUNDLE_DIR = "/backgrounds/bundled/";

    // 极简字段提取：清单是我们自己产出的受控格式，逐字段正则足够且无需引入 JSON 依赖。
    private static final Pattern FILE_FIELD =
            Pattern.compile("\"file\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TITLE_FIELD =
            Pattern.compile("\"title\"\\s*:\\s*\"([^\"]*)\"");

    private BundledBackdrops() {
    }

    /**
     * 把内置背景播种到目标目录（幂等）。
     *
     * @param targetDir 用户背景库目录（按需创建）
     * @return 实际新写入的文件数量（已存在的不计）
     */
    public static int seed(Path targetDir) {
        if (targetDir == null) {
            return 0;
        }
        List<String> files = readBundleFiles();
        if (files.isEmpty()) {
            return 0;
        }
        try {
            Files.createDirectories(targetDir);
        } catch (IOException ex) {
            return 0;
        }
        int written = 0;
        for (String file : files) {
            Path dest = targetDir.resolve(file);
            if (Files.exists(dest)) {
                continue;
            }
            try (InputStream in = BundledBackdrops.class.getResourceAsStream(BUNDLE_DIR + file)) {
                if (in == null) {
                    continue;
                }
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                written++;
            } catch (IOException ex) {
                // 单个文件失败不影响其余播种。
            }
        }
        return written;
    }

    /** @return 清单中声明的内置文件名（按声明顺序） */
    private static List<String> readBundleFiles() {
        List<String> files = new ArrayList<>();
        try (InputStream in = BundledBackdrops.class.getResourceAsStream(MANIFEST)) {
            if (in == null) {
                return files;
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = FILE_FIELD.matcher(text);
            while (m.find()) {
                files.add(m.group(1));
            }
        } catch (IOException ex) {
            // 无清单即无内置背景。
        }
        return files;
    }

    /**
     * 读取清单中「文件 → 标题」的映射，供需要展示友好名称时使用。
     *
     * @return 文件名到标题的映射（缺失时为对应文件名）
     */
    public static java.util.Map<String, String> bundleTitles() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        try (InputStream in = BundledBackdrops.class.getResourceAsStream(MANIFEST)) {
            if (in == null) {
                return map;
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher fm = FILE_FIELD.matcher(text);
            Matcher tm = TITLE_FIELD.matcher(text);
            while (fm.find()) {
                String file = fm.group(1);
                String title = tm.find() ? tm.group(1) : file;
                map.put(file, title);
            }
        } catch (IOException ex) {
            // 忽略：无清单时返回空映射。
        }
        return map;
    }
}
