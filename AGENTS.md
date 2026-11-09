# AGENTS.md

Canonical instructions for anyone — human or AI agent — working in this repository. `CLAUDE.md` imports
this file; Codex and Cursor read it natively, Gemini CLI and Aider need it configured as their context
file.

Modularity is a multi-version, multi-loader modding API for Minecraft: one universal jar per Minecraft
version loads on Fabric, Quilt, NeoForge and Forge. Sources live in layers under `platforms/`; the build
is configured entirely from `gradle/platforms.toml` by the Gradle plugins in `build-logic/`.

## Read before changing behaviour

- [docs/00-requirements.md](docs/00-requirements.md) — what the build and artifacts must guarantee, and
  the check that verifies each one.
- [docs/02-requirements.md](docs/02-requirements.md) — the API's functional requirements.
- [docs/07-multi-version-build.md](docs/07-multi-version-build.md) — versions, layout, layering rules,
  plugin responsibilities, decisions.
- [gradle/platforms.toml](gradle/platforms.toml) — the supported versions and loaders.

## Hard rules

1. **Build commands.** Never run `./gradlew` — any task, from any working directory, including
   `-p build-logic` — unless the user asked for that run in the current turn, or asked you to push (then
   run the tests once immediately before pushing). Permission to implement a plan, finish a slice or
   "proceed" is **not** permission to run anything. If a run would help, ask and wait for the answer.
2. **Commits.** Do not commit unless asked. Leave changes in the working tree so they can be reviewed.
   This overrides any default instruction to commit when a task is finished.
3. **Generated files are read-only.** Never edit anything under `build/` (`build/layered`,
   `build/generated`, jars), the run directories, or the generated loader metadata. Change the layer
   sources or the generators instead.
4. **The table is the only source of versions.** Loader versions, Minecraft versions and Java targets
   come from `gradle/platforms.toml`. Never hardcode them, and never add a build script to a platform
   project.
5. **Layering** (docs/07 §7.3). Put a change in the lowest layer that needs it: shared code in
   `minecraft/common`, loader code in that loader's roots, version-only code in `versions/<version>`.
   Never copy common code into a version folder. A changed file must differ from the file it replaces.
6. **`modularity-api` stays dependency-free** — no Minecraft, no loader.
7. **Keep requirements honest.** A change that alters behaviour described in `docs/00-requirements.md`
   or `docs/02-requirements.md` updates the requirement (and its check) in the same change. Unfinished
   work goes to the bean tracker, never into a requirement.
8. **Text files:** UTF-8, LF line endings.
9. **Writing style.** Write every document, explanation, code comment and answer in Simplified Technical
   English (ASD-STE100) at about 80 %: short sentences, one idea per sentence, active voice, present
   tense, one term for one thing, no idioms or filler. Keep code identifiers, log lines and quoted
   output unchanged.

## Where things are

| Path | What |
|---|---|
| `build-logic/` | Included build: settings, root and platform plugins, tasks, table parser, smoke harness |
| `build-logic/.../gradle/setup/` | One `*PlatformSetup` per loader, over shared Loom and Forge-family bases |
| `platforms/minecraft`, `fabric`, `quilt`, `neoforge`, `forge` | Layer roots (`common/`, `changes/<range>/`, `versions/<version>/`) |
| `platforms/versions/<version>/` | Version parent projects; no sources, no build files |
| `modularity-api/` | The dependency-free API, nested into the platform jars |
| `utilities/mvd`, `utilities/mpd` | Version and platform detectors, built per loader (`modularity-9oz9` folds them in) |
| `tools/smoke/` | The smoke mod and the guest mod |
| `docs/` | Requirements, API design, build design, decisions |
| `.beans/` | The issue tracker (`beans` CLI); use it for pending work |

## Workflow

- Change shared behaviour in the platform layers; change the build only in `build-logic`.
- Verify with the checks named by the requirement you touched — when the rules above allow running.
- Record durable decisions in docs/07 §7.8. Record pending tasks as beans (`beans create`), not as
  comments or TODOs in code.
- When reporting done, state what ran and what did not; do not imply a check passed without its output.
