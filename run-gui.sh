#!/usr/bin/env bash
#
# aether-decompiler — launch the JavaFX desktop studio.
# Copyright 2026 Jerry Zhu (Zeek) <zhujiejava1@gmail.com>
# Apache-2.0.  "Run the Code, Run the World!"
#
# IMPORTANT: always start the studio through the launcher class
# (com.aetherdecompiler.gui.AetherLauncher), never through the Application
# subclass (AetherGuiApp). When JavaFX is supplied on the classpath, the JVM
# refuses to start an Application subclass directly and aborts with
# "JavaFX runtime components are missing".
#
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="${1:-$HERE/aether-gui/target/aether-gui-0.1.0-SNAPSHOT.jar}"

if [[ ! -f "$JAR" ]]; then
  echo "GUI jar not found: $JAR" >&2
  echo "Build it first:  mvn -q -pl aether-gui -am -DskipTests package" >&2
  exit 1
fi

exec java -jar "$JAR" "$@"
