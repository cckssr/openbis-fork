# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

openBIS is an enterprise scientific data management system. It is a Gradle 8.14 monorepo with 45+ modules spanning Java servers, JavaScript/TypeScript UIs, Python APIs, and Matlab integrations.

**Requirements:** JDK 17, PostgreSQL 15

## Build Commands

### Full installer build (from repo root)
```bash
cd app-openbis-installer/
./gradlew clean
./gradlew build -x test "-Dorg.gradle.jvmargs=--add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED"
```
Output: `app-openbis-installer/targets/gradle/distributions/openBIS-installation-standard-technologies-SNAPSHOT-rXXXXXXXXXX.tar.gz`

### Build a single Java module
```bash
cd <module-name>/
./gradlew build -x test
```

### V3 JavaScript API bundle (needed by Admin UI)
```bash
cd api-openbis-javascript/
./gradlew bundleOpenbisStaticResources
```

### Admin UI (ui-admin)
```bash
cd ui-admin/
npm install
npm run dev          # Dev server at http://localhost:9999/admin
npm run build        # Production build
npm test             # Jest tests
npm run lint         # ESLint
npm run lint:fix     # ESLint with auto-fix
```

## Running Tests

### Java tests (TestNG)
Tests use TestNG with suites defined in `sourceTest/java/tests.xml` per module.
```bash
cd <module-name>/
./gradlew test
```
JVM args for tests: `--add-opens` flags are applied automatically via `build/javaproject.gradle`. Tests run with timezone `Europe/Zurich` and max heap 8192m.

**IntelliJ note:** Tests must use IntelliJ's test runner, not Gradle's. Also disable "Use --release option for cross-compilation" in compiler settings.

### JavaScript tests (ui-admin)
```bash
cd ui-admin/
npm test
```

## Development Environment

### Starting local servers
From the `build/` Gradle project in IntelliJ:
1. `openBISDevelopmentEnvironmentASPrepare` — one-time setup
2. `openBISDevelopmentEnvironmentASStart` — Application Server on port 8888
3. `openBISDevelopmentEnvironmentDSSStart` — Data Store Server

### Admin UI development
1. Build the JS API bundle (see above)
2. Start AS and DSS
3. `cd ui-admin && npm install && npm run dev`
4. Open http://localhost:9999/admin (proxies to AS at localhost:8888)

### Core plugin development
Plugins are symlinked into server directories. For ELN-LIMS:
```bash
# In server-application-server/source/core-plugins/
ln -s ../../../ui-eln-lims/src/core-plugins/eln-lims
ln -s ../../../ui-admin/src/core-plugins/admin
ln -s ../../../core-plugin-openbis/dist/core-plugins/xls-import
# Add to core-plugins.properties, then repeat similar symlinks for server-original-data-store
```
- Java changes require server restart
- JavaScript changes require browser refresh
- Jython changes auto-refresh

## Project Source Layout

**Java modules** use a non-standard directory structure:
- Main source: `source/java/` (not `src/main/java/`)
- Test source: `sourceTest/java/` (not `src/test/java/`)
- Build output: `targets/gradle/` (not `build/`)
- TestNG suite: `sourceTest/java/tests.xml`

## Module Architecture

**Dependency flow:** `lib-*` → `api-*` → `server-*` → `app-*`

- **lib-*** (32 modules): Internal libraries — `lib-commonbase` and `lib-common` are the foundation; others handle DB, auth, logging, mail, archiving, HDF5, JSON, RDF, etc.
- **api-***: API facades — Java (`api-openbis-java`), JavaScript (`api-openbis-javascript`), TypeScript (`api-openbis-typescript`), Python/PyBIS (`api-openbis-python3-pybis`), Matlab
- **server-application-server**: Main AS (Jetty-based, Spring, Hibernate, PostgreSQL)
- **server-original-data-store**: Data Store Server (DSS)
- **server-screening**, **server-data-store**, **server-ro-crate** (JDK17+), **server-external-data-store**: Additional server modules
- **ui-admin**: React 18 / MUI 6 Admin UI (Webpack, Babel, Jest)
- **ui-eln-lims**: Electronic Lab Notebook & LIMS UI (JavaScript webapp)
- **core-plugin-openbis**: Core plugins (xls-import, imaging, imaging-nanonis)
- **app-openbis-installer**: Aggregates everything into the distribution (IzPack)
- **test-***: Integration test modules

The central Gradle configuration lives in `build/` — `settings.gradle` includes all modules, `javaproject.gradle` is the template applied to every Java module, and `repository.gradle` configures the custom Ivy repository.

## Commit Message Format

Commit messages **must** include an issue number matching `SSDM-XXXXX:` or `BIS-XXX:`. A git hook in `docs/hooks/commit-msg` enforces this.

**UI separation rule:** Files in `ui-admin/` and `ui-eln-lims/` must be committed separately from non-UI files.

Install hooks: `cp ./docs/hooks/* ./.git/hooks/`

## Code Style

Import IntelliJ code style from `docs/codestyle/SIS_Conventions_IntelliJ_V3.xml`. Copyright headers use Apache 2.0 license (preset at `docs/copyright/Copyright_IntelliJ.xml`).
