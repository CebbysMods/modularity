---
# modularity-hxnf
title: Loader installers and launch commands for Fabric, Quilt, Forge, NeoForge
status: todo
type: feature
priority: high
tags:
    - launcher
    - loaders
created_at: 2026-11-09T07:58:45Z
updated_at: 2026-11-09T07:59:04Z
parent: modularity-xbj0
blocked_by:
    - modularity-1vw1
    - modularity-000m
---

Install each loader into the cache and build its exact client command. Verified against the real installers and
version JSON; do not assume Forge and NeoForge share a shape.

## Installers (version from gradle/platforms.toml)
- Fabric: `java -jar fabric-installer.jar client -dir <installDir> -mcversion 1.21.1 -loader 0.20.1 -noprofile`
  -> version id `fabric-loader-0.20.1-1.21.1`.
- Quilt: `java -jar quilt-installer.jar install client 1.21.1 0.30.2 --install-dir=<installDir> --no-profile`.
- Forge 52.1.21: write `launcher_profiles.json` (`{"profiles":{}}`), run
  `java -jar forge-installer.jar --installClient <installDir>` with cwd = install dir. Version JSON:
  `mainClass=net.minecraftforge.bootstrap.ForgeBootstrap`, args `--launchTarget forge_client`; the patched client
  is the local artifact `net.minecraftforge:forge:1.21.1-52.1.21:client` (empty download URL) under `libraries/`.
  Do **not** append the vanilla client jar; there is no `-DlibraryDirectory` and no module path.
- NeoForge 21.1.265: `java -jar neoforge-installer.jar --install-client <installDir>` (cwd = install dir).
  `mainClass=cpw.mods.bootstraplauncher.BootstrapLauncher`; JVM args include
  `-DignoreList=client-extra,${version_name}.jar`, `-DlibraryDirectory=${library_directory}`,
  `-p ${library_directory}/cpw/mods/bootstraplauncher/...`, `--add-modules ALL-MODULE-PATH`. Copy the original
  client jar to `versions/neoforge-21.1.265/neoforge-21.1.265.jar`, append it **last** on the classpath, set
  `version_name=neoforge-21.1.265` so `ignoreList` matches, and add `--offlineDeveloperMode`.

## Classes
- `LoaderInstall` interface + `FabricInstall`, `QuiltInstall`, `ForgeInstall`, `NeoForgeInstall` returning
  `InstalledLoader(versionId, versionJson, installDir, localArtifacts)`.
- `InstalledLauncher` — merge manifests, resolve libraries (install dir first, cache second), build the classpath
  including the per-loader client-jar rule, extract natives (`natives_directory`), expand placeholders, produce
  `LaunchCommand`.
- Launch args: `--username Smoke --uuid <offline uuid> --accessToken 0 --userType legacy --gameDir <run dir>
  --assetsDir <cache>/assets --assetIndex <indexId> --width 854 --height 480 --quickPlaySingleplayer smoke`.
- JVM binary from the **Java 21** toolchain (`JavaToolchainService`): Gradle runs on 25, Forge/NeoForge 1.21.1
  will not start on 25.

## Acceptance
- Unit-testable parts covered by fixtures; each installer runs offline on a second invocation.
- A hand-built command for each loader starts the client on a vanilla install (no mods yet).
