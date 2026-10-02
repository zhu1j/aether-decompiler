# aether-decompiler

> **"Run the Code, Run the World!"** — Jerry Zhu (Zeek)

**English** · [中文](README.zh-CN.md)

An independent, reusable **JVM decompilation engine library**. The kernel models
bytecode as a mathematical object; the CLI and GUI are only applications layered
on top and are never part of the kernel.

**Author:** Jerry Zhu (Zeek) — _pen name_ Zeek · zhujiejava1@gmail.com
**License:** Apache-2.0 · **Language:** Java 17+ · **Build:** Maven

---

## Table of contents

- [Why](#why)
- [Architecture (strict, one-way layering)](#architecture-strict-one-way-layering)
- [Modules](#modules)
- [Requirements](#requirements)
- [Build](#build)
- [Run — CLI](#run--cli)
- [Run — GUI Studio](#run--gui-studio)
  - [Skins](#skins)
  - [Custom backdrop](#custom-backdrop)
- [Project layout](#project-layout)
- [Design invariants](#design-invariants)
- [Roadmap](#roadmap)
- [License](#license)

---

## Why

Most decompilers are built as a monolith: parsing, modelling, structuring and
rendering all live in one place, and the "UI" is welded to the engine. This
project inverts that. The kernel owns only the **mathematical model of bytecode**;
everything optional — how classes are found, how the IR is transformed, how the
result is rendered, what analyses run — is pushed down into plugins at four fixed
extension points. The CLI and the desktop GUI are then just callers of the same
public API.

That means a new front-end, a new output format, or a new analysis can be added
**without touching the kernel, and without recompiling it**.

## Architecture (strict, one-way layering)

```
Application  (aether-cli / aether-gui / future IDE plugin)   <- callers only
    |
Plugin host  (Java native ServiceLoader, no Spring/Guava)
    |  fixed 4 extension points:
    |    ClassSourcePlugin  AstTransformPlugin  RenderPlugin  AnalysisPlugin
    |
Kernel       (aether-core)   <- stable core, ONLY dependency: ASM (wrapped)
    |   model / cfg / ssa / type / ast / engine
    |
ASM          (the kernel's only third-party dependency)
```

Dependencies point **one way, downward**. The kernel knows nothing about the
applications; the applications know only the kernel's and the plugin API's public
surfaces.

## Modules

| Module | Role |
|---|---|
| `aether-plugin-api` | The fixed plugin contract. Tiny and dependency-free. Plugins depend only on this. |
| `aether-core` | The kernel: immutable model, CFG, dominator tree, engine. Depends only on ASM. |
| `aether-plugins/plugin-source-jar` | `ClassSourcePlugin` that reads jar / zip / directory sources. |
| `aether-plugins/plugin-render-dot` | `RenderPlugin` that emits Graphviz DOT for a control-flow graph. |
| `aether-cli` | Command-line application; discovers plugins, drives the engine. |
| `aether-gui` | JavaFX desktop studio: RichTextFX editor, three-view linkage, pluggable skins, custom backdrop. |

## Requirements

- **JDK 17 or newer** (the toolchain targets 17; the build was verified on JDK 25).
- **Apache Maven 3.8+**.
- To view generated control-flow graphs: any **Graphviz** install (`dot`).

## Build

```bash
mvn clean install
```

This builds all modules, runs the unit tests, and produces runnable fat jars for
the CLI and the GUI.

## Run — CLI

```bash
java -jar aether-cli/target/aether-cli-0.1.0-SNAPSHOT.jar <input.jar|dir> --outputdir out/
```

The CLI prints the author banner and motto, lists discovered plugins, streams
pipeline events, and writes one DOT file per method (`render.dot`). Feed a DOT
file to Graphviz to view the control-flow graph:

```bash
dot -Tsvg out/com/example/Foo.class/method.dot -o cfg.svg
```

## Run — GUI Studio

```bash
java -jar aether-gui/target/aether-gui-0.1.0-SNAPSHOT.jar
```

The desktop studio is a pure caller of the kernel, exactly like the CLI. It
renders the engine's output in three linked views — **bytecode**, **control-flow
graph**, and a **RichTextFX source view** — inside a themable shell with a class
tree navigator, a pipeline-events dock, and an inspector.

### Skins

The look is fully decoupled from layout. `styles/base.css` owns structure and the
default colour tokens; each skin overrides only those tokens (plus optional
signature flourishes). Five skins ship built in:

| Id | Name |
|---|---|
| `midnight-aether` | Midnight Aether (default) |
| `obsidian-amber` | Obsidian Amber |
| `matrix-green` | Matrix Terminal |
| `nebula-violet` | Nebula Violet |
| `solar-light` | Solar Light |

**Custom skins need no recompile.** Drop any `*.css` into `~/.aether/skins/`
(optionally with a `<name>.skin.properties` sidecar carrying
`name` / `author` / `accent` / `bg`) and it appears in the studio's skin picker.
You can also use the **⬇ 导入皮肤/背景** button to import a `.css` file at
runtime; it is copied into the user skin directory and selected immediately.

### Custom backdrop

The studio can show a background image behind the work area, rendered
**semi-transparently** so code stays legible:

- Use **⬇ 导入皮肤/背景** and pick a `*.png` / `*.jpg` / `*.jpeg` / `*.gif` /
  `*.bmp` / `*.webp` image. It is copied into `~/.aether/backgrounds/` and shown
  immediately.
- Adjust **背景透明** (backdrop opacity) with the slider — default `0.35`,
  range `0.05`–`1.0`. A themed scrim sits between the image and the work area so
  text contrast is preserved.
- **清除背景** removes the current image.
- Skin and backdrop choices are remembered in `~/.aether/studio.properties`.

A skin's `.skin.properties` sidecar may also declare a default backdrop:

```properties
name=My Skin
author=Someone
accent=#7ad0ff
bg=#0b0f14
background=my-backdrop.png
backgroundOpacity=0.30
```

## Project layout

```
aether-decompiler/
├─ pom.xml                       parent POM (attribution, ASM version, modules)
├─ LICENSE                       Apache-2.0
├─ NOTICE                        attribution + motto
├─ README.md
├─ aether-plugin-api/            fixed plugin contract (dependency-free)
├─ aether-core/                  the kernel (ASM is its only dependency)
│  └─ src/main/java/com/aetherdecompiler/core/
│     ├─ model/                  immutable bytecode model
│     ├─ asm/                    the single ASM seam (AsmClassParser)
│     ├─ cfg/                    CFG builder + dominator tree
│     ├─ engine/                 decompiler engine + plugin host
│     ├─ event/                  event bus
│     └─ source/                 directory / in-memory class sources
├─ aether-plugins/
│  ├─ plugin-source-jar/         jar/zip/directory class source
│  └─ plugin-render-dot/         Graphviz DOT renderer
├─ aether-cli/                   command-line application
└─ aether-gui/                   JavaFX desktop studio
```

## Design invariants

- **The kernel hardcodes no Java syntax.** Structure lives in the generic model;
  Java-specific facts are derived by transform plugins.
- **The only third-party dependency is ASM, and it never leaks.** All ASM use is
  confined to a single seam (`AsmClassParser`), so the dependency can be swapped.
- **Models are immutable**, and configuration is an immutable snapshot
  (`Options`) produced from a plugin-populated `OptionRegistry`.
- **Class loading is lazy / on demand** through the `ClassSource` interface — the
  engine never eagerly loads an entire archive.
- **The kernel is stateless and thread-safe.** Parallel scheduling is an
  upper-layer concern, not a kernel one.
- **Observability is an event bus**, not logging scattered through the core.
- **One root exception** (`AetherException`) with a stable `ErrorCode` enum.

## Roadmap

- [x] **Phase 0** — ASM thin wrapper, immutable models, lazy class sources, engine, tests
- [x] **Phase 1** — BasicBlock splitting, CFG, dominator tree, exception edges, DOT plugin
- [ ] **Phase 2** — SSA + JVM stack type inference
- [ ] **Phase 3** — Structured reconstruction (goto elimination)
- [ ] **Phase 4** — Plugin ecosystem (lambda, generics, AST beautify)
- [~] **Phase 5** — CLI (done) + GUI Studio (done: three-view shell, pluggable skins &
      backdrop); cache & parallel scheduling pending

## License

Apache License 2.0 — see [LICENSE](LICENSE). Copyright 2026 **Jerry Zhu (Zeek)**.

> _"Run the Code, Run the World!"_
