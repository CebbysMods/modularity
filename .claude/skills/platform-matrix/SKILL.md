---
name: platform-matrix
description: Produce Modularity's per-platform traceability matrix (Fabric, Quilt, NeoForge, Forge) with ✅/🟡/❌ marks, weights, per-area scores, totals and the four-loader average. Use when the user asks for progress, status, coverage, completeness or a feature matrix across loaders.
---

# Platform progress matrix

Build a traceability matrix of what each loader platform implements, in the shape the owner uses.
Answer in the chat. Write a file only when the user asks for one.

## Hard rules

1. **Never run Gradle.** No `./gradlew`, from any directory (AGENTS.md hard rule 1). A result that only a
   build or a smoke run could prove is **not proven**.
2. **Read only.** Do not edit sources, docs, beans or the table.
3. **No invention.** Every mark and number comes from evidence in the repo. Put the evidence in the
   appendix. If you cannot check a capability, mark it 🟡 and say why.
4. **One term for one thing.** Keep the loader names `Fabric`, `Quilt`, `NeoForge`, `Forge`.

## The ten feature areas (fixed)

The owner fixed this list and these weights. Change them only when the owner asks.

| # | Feature area | Weight |
|---|---|---|
| 1 | Build, jar, metadata, universal jar | 10 |
| 2 | Mod lifecycle (entrypoints, init order, server/client) | 10 |
| 3 | Detection and mod discovery (`LoaderInfo`, `Loaders`, detector sources) | 10 |
| 4 | Content registration (blocks, items, block entities, creative tabs, payload types) | 15 |
| 5 | Networking (custom payloads, both directions) | 10 |
| 6 | Storage (tanks, item/fluid storages, expose/fill) | 5 |
| 7 | Client (render data, dynamic models, HUD, key mappings, entity renderers) | 15 |
| 8 | Mixins (per-loader config, production refmaps) | 10 |
| 9 | Guest mods | 5 |
| 10 | Verification (smoke, shipped jar, multi-version) | 10 |

Total weight: 100.

## Capabilities per area (the counting unit)

Count these sub-capabilities per platform. `share = implemented / total`. Use the same list every time,
so two runs are comparable.

| Area | Capabilities to count |
|---|---|
| 1 | table parsed; layered sources assembled; metadata generated (Fabric, Quilt, NeoForge/Forge TOML); universal jar relocation; `collectJars` |
| 2 | Modularity's own `main`; `client`; `server`; `preLaunch`/`pre_launch`; mod registration; per-mod dispatch for common, client, server (and launch where it exists); init order; failure names the mod |
| 3 | version source; platform source (loader, version, side, development); `LoaderInfo`; `Loaders` seams installed; `(<side>) platform ...` line |
| 4 | blocks; block items; block entity types; items; creative tabs; entity types; sounds; channels; payload types |
| 5 | payload registration; server → client; client → server; channel codec bound |
| 6 | fluid unit; entity fluid tank (expose + fill); entity item storage; `StorageBackend` |
| 7 | render data; dynamic block models; HUD layers; key mappings; entity renderers; client tick; OBJ models; hit particles |
| 8 | mixin config generated for the loader; Modularity's mixins apply; production refmaps |
| 9 | guest discovery; guest injection as a real mod; guest entrypoints run; guests on this loader |
| 10 | server smoke per version; client smoke; shipped-jar smoke; multi-version coverage |

A capability counts as implemented only when the code exists **and** it does real work. A stub that
throws `UnsupportedOperationException` does not count.

## Where the evidence is

| Area | Look at |
|---|---|
| all | `platforms/<loader>/**` (Fabric and Quilt share `loom`, so most code is common), `gradle/platforms.toml` (enabled platforms and versions) |
| stubs | `grep -rn "UnsupportedOperationException" platforms --include=*.java` |
| seams | `platforms/common/common/.../platform/loader/{LoaderHooks,ClientHooks,LoaderInfo,StorageBackend,RenderDataAccess}` — count the methods and compare with each implementation (`ModularityLoomLoaderHooks`, `NeoForgeLoaderHooks`, `NeoForgeClientHooks`, `NeoForgeRenderData`, `NeoForgeStorageBackend`, and the Forge counterparts if present) |
| lifecycle | `ModularityCommon`, `ModularityClient`, `ModularityServer`, `ModularityLaunch`, the loader entrypoint classes (`ModularityLoom{Common,Client,Server,Launch}`, `Modularity{Fabric,Quilt,NeoForge,Forge}*`) |
| detection | `platforms/<loader>/.../detect/`, `platform.detect.DetectionLog` |
| build | `build-logic/src/main/java/.../gradle/` (`ModMetadata`, `PlatformTable`, `UniversalJar`), `build-logic` tests |
| verification | `docs/07-multi-version-build.md` §7.6 and §7.10 (recorded smoke runs), `docs/00-requirements.md` status column, `TODO-NOW.md` open checks, `.beans/` for open verification beans |

Rules for verification evidence:

- A recorded run in `docs/07` §7.10 or a closed check in `TODO-NOW.md` proves the platform.
- An open check, an unrun smoke bean, or a run older than the code change proves nothing: mark 🟡.
- `docs/00` PR-11 to PR-15 statuses are a second opinion, not proof.

## Scoring

For each platform and area:

1. Count the capabilities from the table above. `share = implemented / total`.
2. Mark:
   - ✅ when `share = 1` **and** a recorded run or test proves it.
   - 🟡 when `0 < share ≤ 1` and it is unproven, or `share < 1` but at least half works.
   - ❌ when `share < 0.5`.
3. `points = round(weight × share)`.
4. Platform total = sum of points. Percentage = the total (it is already a percentage of 100).
5. Average = mean of the four platform percentages, rounded to a whole number.

Do not adjust a number to make the total look right. If the evidence is thin, the number is low.

## Output

Default: a boxed table, then the average line, then a short appendix.

```
Marks: ✅ complete · 🟡 partial / implemented but unverified · ❌ missing. The number is score/weight.

┌─────┬──────────────────────────────────────────────────┬────────┬────────┬────────┬──────────┬───────┐
│  #  │                   Feature area                   │ Weight │ Fabric │ Quilt  │ NeoForge │ Forge │
├─────┼──────────────────────────────────────────────────┼────────┼────────┼────────┼──────────┼───────┤
│ 1   │ Build, jar, metadata, universal jar              │ 10     │ ✅ 10  │ ✅ 10  │ ✅ 10    │ ✅ 10 │
├─────┼──────────────────────────────────────────────────┼────────┼────────┼────────┼──────────┼───────┤
│ ... │ ...                                              │ ...    │ ...    │ ...    │ ...      │ ...   │
├─────┼──────────────────────────────────────────────────┼────────┼────────┼────────┼──────────┼───────┤
│     │ Total                                            │ 100    │ 92 %   │ 88 %   │ 34 %     │ 18 %  │
└─────┴──────────────────────────────────────────────────┴────────┴────────┴────────┴──────────┴───────┘

Average across the four loaders: 58 %.
```

Formatting rules:

- Column order and titles: `#`, `Feature area`, `Weight`, `Fabric`, `Quilt`, `NeoForge`, `Forge`.
- A score cell is the mark, one space, the points: `✅ 10`, `🟡 6`, `❌ 0`.
- Pad every cell to its content width. The marks are wide characters in most fonts; if the columns do
  not line up, use a Markdown table instead and say that the boxed form did not fit.
- Last row: empty `#`, `Total`, `100`, then each platform as `NN %`. Then the average line.
- After the table: one line with the date and `git rev-parse --short HEAD` (allowed: not Gradle).
- Appendix: one bullet per 🟡 and ❌ cell: area, platform, what is missing or unproven, and the file,
  bean or open check that shows it. Keep each bullet to one sentence.

## When the user asks to save it

Write to the path the user names. Default name: `PROGRESS.md` at the repo root. Never write under
`build/` (generated, read-only).
