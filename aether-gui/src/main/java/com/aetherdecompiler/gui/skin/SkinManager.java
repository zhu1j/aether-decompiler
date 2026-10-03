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
package com.aetherdecompiler.gui.skin;

import javafx.scene.Scene;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * 发现、持有、导入并应用皮肤与背景图片。
 *
 * <p>用同一套机制支持两种皮肤：</p>
 * <ul>
 *   <li><strong>内置皮肤</strong>，作为类路径上的 {@code /skins/*.css} 随附，
 *       并在 {@link #BUILTIN} 中注册。</li>
 *   <li><strong>自定义皮肤</strong>，由用户放入
 *       {@code ~/.aether/skins/}：任意 {@code *.css} 文件，可选地伴随一个
 *       {@code <name>.skin.properties} 作为元数据。无需重编译，无需改注册表
 *       —— 目录就是扩展点。</li>
 * </ul>
 *
 * <p>除了手动放置文件，工作台还可以在运行期<em>导入</em>它们：
 * {@link #importSkin(Path)} 把选定的 {@code *.css} 复制到用户皮肤目录并重新
 * 加载目录，{@link #importBackground(Path)} 把选定的图片复制到背景目录。二者
 * 都是简单、诚实的文件操作 —— 目录始终是唯一事实来源。</p>
 *
 * <p>故事类比：一个衣柜。店里买来的衣服挂在一根杆上；主人放进去的定制款挂在
 * 另一根杆上。无论怎样，穿衣照的还是同一面镜子。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class SkinManager {

    /** 始终在任何皮肤之下应用的共享布局样式表。 */
    public static final String BASE_CSS = "styles/base.css";

    /** 导入背景所接受的图片扩展名。 */
    private static final List<String> IMAGE_EXT =
            List.of(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp");

    /** 内置皮肤元组：id、name、author、accent、bg、css 资源。 */
    private static final String[][] BUILTIN = {
            {"midnight-aether", "Midnight Aether", "Jerry Zhu (Zeek)", "#34e0c8", "#0b0f14", "skins/midnight-aether.css"},
            {"obsidian-amber", "Obsidian Amber", "Jerry Zhu (Zeek)", "#f5b544", "#14110c", "skins/obsidian-amber.css"},
            {"matrix-green", "Matrix Terminal", "Jerry Zhu (Zeek)", "#39ff14", "#030705", "skins/matrix-green.css"},
            {"nebula-violet", "Nebula Violet", "Jerry Zhu (Zeek)", "#b18cff", "#0d0a17", "skins/nebula-violet.css"},
            {"solar-light", "Solar Light", "Jerry Zhu (Zeek)", "#007a6e", "#f4f6f8", "skins/solar-light.css"},
    };

    private final Path userSkinDir;
    private final List<Skin> skins = new ArrayList<>();

    /**
     * 构建一个管理器，加载内置皮肤与任何用户皮肤。
     *
     * @param userSkinDir 用于扫描自定义皮肤的目录（可能不存在）
     */
    public SkinManager(Path userSkinDir) {
        this.userSkinDir = userSkinDir;
        reload();
    }

    /** 由内置皮肤加上用户皮肤目录重建皮肤目录。 */
    public void reload() {
        skins.clear();
        loadBuiltin();
        loadUser();
    }

    private void loadBuiltin() {
        ClassLoader cl = SkinManager.class.getClassLoader();
        for (String[] row : BUILTIN) {
            URL url = cl.getResource(row[5]);
            if (url != null) {
                skins.add(Skin.of(row[0], row[1], row[2], row[3], row[4], url, true));
            }
        }
    }

    private void loadUser() {
        if (userSkinDir == null || !Files.isDirectory(userSkinDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(userSkinDir)) {
            files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".css"))
                    .sorted()
                    .forEach(this::addUserSkin);
        } catch (IOException ex) {
            // 用户目录缺失或不可读不算错误。
        }
    }

    private void addUserSkin(Path cssFile) {
        String fileName = cssFile.getFileName().toString();
        String baseName = stripExtension(fileName);
        Properties meta = readMeta(cssFile.resolveSibling(baseName + ".skin.properties"));
        String name = meta.getProperty("name", baseName);
        String author = meta.getProperty("author", "user");
        String accent = meta.getProperty("accent", "#34e0c8");
        String bg = meta.getProperty("bg", "#0b0f14");
        try {
            Skin skin = Skin.of("user-" + baseName, name, author, accent, bg,
                    cssFile.toUri().toURL(), false);
            // 由元数据附属文件声明的可选逐皮肤默认背景。
            String background = meta.getProperty("background");
            if (background != null && !background.isBlank()) {
                Path bgPath = cssFile.getParent().resolve(background.trim());
                if (Files.isRegularFile(bgPath)) {
                    double opacity = parseDouble(meta.getProperty("backgroundOpacity"),
                            Skin.DEFAULT_BACKGROUND_OPACITY);
                    skin = skin.withBackground(bgPath.toUri().toURL(), opacity);
                }
            }
            skins.add(skin);
        } catch (IOException ex) {
            // 跳过无法读取的皮肤文件。
        }
    }

    private Properties readMeta(Path metaFile) {
        Properties props = new Properties();
        if (Files.isRegularFile(metaFile)) {
            try (InputStream in = Files.newInputStream(metaFile)) {
                props.load(in);
            } catch (IOException ex) {
                // 回退到默认值。
            }
        }
        return props;
    }

    // -------------------------------------------------------------------- 导入

    /**
     * 把一个 {@code *.css} 样式表作为自定义皮肤导入。
     *
     * <p>该文件（以及可选的 {@code <name>.skin.properties} 附属文件）会被复制到
     * 用户皮肤目录，目录被重新加载，并返回所得的皮肤，以便调用方立即选中它。
     * 用户的原始文件绝不被修改。</p>
     *
     * @param source 选定的 {@code *.css} 文件
     * @return 已注册的自定义皮肤
     * @throws IOException 若文件无法复制
     */
    public Skin importSkin(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("not a readable file: " + source);
        }
        String fileName = source.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".css")) {
            throw new IOException("not a css file: " + fileName);
        }
        Files.createDirectories(userSkinDir);
        Path dest = userSkinDir.resolve(fileName);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);

        String baseName = stripExtension(fileName);
        Path metaSrc = source.resolveSibling(baseName + ".skin.properties");
        if (Files.isRegularFile(metaSrc)) {
            Files.copy(metaSrc, userSkinDir.resolve(baseName + ".skin.properties"),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        reload();
        Skin imported = byId("user-" + baseName);
        return imported != null ? imported : all().get(all().size() - 1);
    }

    /**
     * 把一张图片导入为可复用的背景。
     *
     * @param source 选定的图片文件
     * @return 背景目录内已存储的背景路径
     * @throws IOException 若文件无法复制
     */
    public Path importBackground(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("not a readable file: " + source);
        }
        String fileName = source.getFileName().toString();
        if (!isImage(fileName)) {
            throw new IOException("not an image file: " + fileName);
        }
        Path dir = ensureBackgroundDir();
        Path dest = dir.resolve(fileName);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    /** @return 某个文件名是否具有受支持的图片扩展名 */
    public static boolean isImage(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String ext : IMAGE_EXT) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /** @return 背景目录中已存储的全部背景图片 */
    public List<Path> backgrounds() {
        Path dir = defaultBackgroundDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> isImage(p.getFileName().toString()))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    // -------------------------------------------------------------------- 访问

    /** @return 发现的所有皮肤，内置的在前 */
    public List<Skin> all() {
        return Collections.unmodifiableList(skins);
    }

    /**
     * @param id 皮肤 id
     * @return 匹配的皮肤，若无则返回 {@code null}
     */
    public Skin byId(String id) {
        for (Skin s : skins) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** @return 第一个内置皮肤，用作启动默认值 */
    public Skin defaultSkin() {
        return skins.isEmpty() ? null : skins.get(0);
    }

    /**
     * 把一款皮肤应用到某个场景：共享的基础布局加上该皮肤样式表。
     *
     * @param scene 要换装的场景
     * @param skin  要应用的皮肤
     */
    public void apply(Scene scene, Skin skin) {
        if (scene == null) {
            // 防御性处理：场景尚未创建时安全跳过，避免空指针。
            return;
        }
        scene.getStylesheets().clear();
        URL base = SkinManager.class.getClassLoader().getResource(BASE_CSS);
        if (base != null) {
            scene.getStylesheets().add(base.toExternalForm());
        }
        if (skin != null) {
            scene.getStylesheets().add(skin.externalForm());
        }
    }

    // --------------------------------------------------------------------- 路径

    /** @return 约定的皮肤根目录 {@code ~/.aether} */
    public static Path aetherHome() {
        return Path.of(System.getProperty("user.home", "."), ".aether");
    }

    /** @return 约定的用户皮肤目录 {@code ~/.aether/skins} */
    public static Path defaultUserSkinDir() {
        return aetherHome().resolve("skins");
    }

    /** @return 约定的背景目录 {@code ~/.aether/backgrounds} */
    public static Path defaultBackgroundDir() {
        return aetherHome().resolve("backgrounds");
    }

    /** @return 工作台偏好文件 {@code ~/.aether/studio.properties} */
    public static Path studioPrefsFile() {
        return aetherHome().resolve("studio.properties");
    }

    /**
     * 确保用户皮肤目录存在，使“打开皮肤文件夹”这一交互始终有处可指。
     *
     * @return 用户皮肤目录
     */
    public static Path ensureUserSkinDir() {
        return ensureDir(defaultUserSkinDir());
    }

    /**
     * 确保背景目录存在。
     *
     * @return 背景目录
     */
    public static Path ensureBackgroundDir() {
        return ensureDir(defaultBackgroundDir());
    }

    private static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            // 非致命：选择器只会列出内置皮肤。
        }
        return dir;
    }

    // ------------------------------------------------------------------- 辅助

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private static double parseDouble(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
