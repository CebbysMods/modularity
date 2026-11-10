# Build and smoke commands

Two commands replace the eight per-loader calls:

````
./gradlew -Pmodularity.platforms=21.1 buildAll
./gradlew -Pmodularity.platforms=21.1 smokeAll
````

The first command builds the four 21.1 platforms (Fabric, Quilt, NeoForge, Forge) and the universal
jar. The second command runs the server and client smoke of the same four platforms. The selector
`-Pmodularity.platforms=<version>` accepts a bare version and selects every loader of that version.

## What already works

Both commands already work today. The exploration found the pieces below in place.

| Piece | Where | State |
|---|---|---|
| Bare version selector (`=21.1` → every loader of 21.1, enabled or not) | `ModularitySettingsPlugin.apply`, lines 36–55 | implemented; documented in docs/07 §7.1 line 63 |
| `buildAll` = `build` of every included project | `ModularityRootPlugin`, lines 44–50 | implemented |
| Universal jar per version | parent `build` depends on the parent `jar` (`ModularityVersionPlugin`, lines 55–83) | implemented |
| `smokeAll` = `<parent>:smoke` for every included version | `ModularityRootPlugin`, lines 63–69 | implemented |
| `<parent>:smoke` = `smoke` of every child | `ModularityVersionPlugin.aggregate("smoke", ...)` | implemented |
| platform `smoke` = `smokeServer` + `smokeClient` when the table sets `smoke-client` | `SmokeSetup.apply`, lines 187–194 | implemented |
| one game at a time across the whole build | `SmokeSetup.apply` lines 142–147 and 169–170, `SmokeRunService` with `maxParallelUsages = 1` | implemented |

The memory problem recorded in docs/07 §7.8 ("a single `smokeAll` was killed for low system memory")
is already answered at the game-run level: the runs use one shared service with one slot. The
decision text still describes the old behaviour.

## Changes

1. **Extract the selector and test it.** Add
   `build-logic/src/main/java/lv/cebbys/modularity/gradle/PlatformSelection.java` with
   `static List<PlatformTable.Platform> select(PlatformTable table, String value)`. Move the body of
   `ModularitySettingsPlugin.apply` (lines 36–55) into it unchanged: `none` gives an empty list,
   `all` gives `table.enabled()`, and any other value is a comma list of `<loader>-<name>` ids or bare
   `<name>` versions. An unknown value throws `IllegalArgumentException` with the value and the valid
   ids and versions. `ModularitySettingsPlugin` keeps reading the property and calls `select`;
   behaviour does not change. Improve the error message while moving it: it must name the accepted
   form and the known ids and versions, not only `(see gradle/platforms.toml)`.
2. **Test the selector.** Add
   `build-logic/src/test/java/lv/cebbys/modularity/gradle/PlatformSelectionTest.java`, in the style of
   `LayersTest` (plain JUnit 5, no Gradle runner). Build the fixture with `PlatformTable.parse(...)`
   from a small inline TOML string, as `TableAndMetadataTest` already does; no file writes. Cases:
   `all` gives only `enabled` rows; `none` gives empty; `fabric-21.1` gives one platform; `21.1`
   gives every loader of 21.1, including a row with `enabled = false`; a `21.1,none`-style mix with
   whitespace is trimmed and both values are accepted; `21.9` (unknown) throws
   `IllegalArgumentException` and the message names `21.9`.
3. **Make `smokeAll` say what it does.** In `ModularityRootPlugin`, set the description to "Runs the
   smoke tests of every included platform, one game at a time". No dependency changes.
4. **Docs.** Add the two commands to `README.md` lines 40–46 next to the existing
   `build -Pmodularity.platforms=...` lines, and state that a bare version selects every loader of
   that version. In `docs/07-multi-version-build.md` §7.1 line 63, keep the text and add that the
   same value drives `buildAll` and `smokeAll`. In §7.4 task list (line ~175), note that game runs
   share one service slot, so `smokeAll` runs the loaders one after the other. In §7.8 decisions
   (line 262), amend "Final smoke run one platform per Gradle call": it is still true for CI (one
   job per platform), but the reason has changed, because game runs are serialized, so a local
   `smokeAll` is safe; CI keeps one job per platform for parallel wall-clock time, not memory.
   `docs/00-requirements.md` PR-15 stays correct; no text change.
5. **Beans and risks.** Record this plan as a bean epic with one child per step (selector and test,
   `smokeAll` wording, docs, verification). Add every risk below to `TODO-NOW.md` as an open check.
   Do not close the epic before the verification run passes.

Order: (1) `PlatformSelection` extraction, no behaviour change; (2) `PlatformSelectionTest`;
(3) `smokeAll` description and README/docs; (4) verification run on request, then bean progress
notes.

## Verification (run only on request)

````
./gradlew -p build-logic test
./gradlew -Pmodularity.platforms=21.1 buildAll
./gradlew -Pmodularity.platforms=21.1 smokeAll --continue
````

Expected:

- The selector test and the existing plugin tests pass.
- `buildAll` builds the four 21.1 platforms and writes one universal jar to
  `platforms/versions/21.1/build/libs/`.
- `smokeAll` runs eight game runs in sequence (server then client per platform) and writes
  `build/smoke/{server,client}.txt` in each platform project. `--continue` runs every platform even
  after a failure, which is needed while NeoForge and Forge smoke results are still open.
- Each passing platform logs `[modularity] platform <LOADER> <version>, Minecraft 1.21.1, <SIDE>`.

## Risks to check before closing

- **Memory.** Serialization covers the game runs, but the Gradle daemon, Loom caches and the four
  platform builds in one invocation still compete. If the run dies, use `smokeAll` per loader.
- **Client on a headless machine.** `smokeAll` runs clients for the 21.1 rows (`smoke-client = true`).
  It needs a display; CI keeps `xvfb`.
- **Disabled rows.** A bare version selects the disabled rows of that version on purpose (porting).
  For 21.1 every row is enabled, so this plan does not change what runs today.
- **Failure order.** `smokeAll` stops at the first failing platform unless `--continue` is given.
  The docs must say so.
- **Forge and NeoForge smoke are expected to fail** until `modularity-lw0r` and `modularity-8ywo`
  pass. A red `smokeAll` on 21.1 is not a regression of this plan.

## Out of scope

- `collectJars` in `buildAll`.
- CI workflow changes.
- The Forge-family launch stage (anvil) and the launch-stage docs.
