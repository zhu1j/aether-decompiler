@echo off
REM aether-decompiler - launch the JavaFX desktop studio.
REM Copyright 2026 Jerry Zhu (Zeek) <zhujiejava1@gmail.com>
REM Apache-2.0.  "Run the Code, Run the World!"
REM
REM IMPORTANT: always start the studio through the launcher class
REM (com.aetherdecompiler.gui.AetherLauncher), never through the Application
REM subclass (AetherGuiApp). When JavaFX is supplied on the classpath, the JVM
REM refuses to start an Application subclass directly and aborts with
REM "JavaFX runtime components are missing".

setlocal
set "HERE=%~dp0"
set "JAR=%HERE%aether-gui\target\aether-gui-0.1.0-SNAPSHOT.jar"
if not "%~1"=="" set "JAR=%~1"

if not exist "%JAR%" (
  echo GUI jar not found: %JAR%
  echo Build it first:  mvn -q -pl aether-gui -am -DskipTests package
  exit /b 1
)

java -jar "%JAR%"
endlocal
