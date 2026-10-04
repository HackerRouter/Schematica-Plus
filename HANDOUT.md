# Schematica Plus: cloud AI development handoff

```yaml
document_schema: schematica-plus-ai-handoff/1
created_local_date: 2026-10-01
timezone: Asia/Singapore
audience: successor coding AI with access to the user's GitHub repository
language: English operational context; communicate with the user in Chinese
repository: https://github.com/HackerRouter/Schematica-Plus
branch_at_handoff: master
last_completed_code_commit: 85b531732ccf3cf1066ef6421d813b0b833dacb8
remote_master_observed_at_handoff: 65e612320614f01165a20e115f3440b43b543f08
local_completed_commits_ahead_of_remote: 6
implementation_state: PAUSED_BY_USER_FOR_HANDOFF
latest_completed_validation: build + Checkstyle + 334 tests; 0 failures/errors/skips
validation_excludes: the nine-file uncommitted REBUILD WIP embedded below; native Minecraft/GTNH tests
```

## 0. Resume protocol and evidence rules

This file is task context, not a claim that the port is finished. The user paused development to migrate this and subsequent work to a Claude cloud session. At creation time no source edits were made to finish the interrupted REBUILD work. The handoff document is intended to be committed separately; it embeds a recoverable patch for that unfinished work. No push or release was performed for this handoff.

1. Inspect the actual branch, commit history, working tree and any new user instructions. Do not overwrite later work. Fetch the user's intended branch if necessary. A cloud session cannot see unpushed local commits or Windows files merely because it has GitHub access.
2. Ensure the six completed commits listed below are present. The patch assumes code at `85b5317` (or the subsequent documentation-only handoff commit). If only remote `65e6123` is available, obtain the newer commits from the user; the patch alone does NOT contain those six commits.
3. Read this file, `compatibility/README.md`, `compatibility/gtnh-mods.json`, current implementation and `TESTING.md`. `compatibility/ui-port.md` is a chronological journal: later phases supersede earlier "pending" statements. README also contains known stale statements. Do not use a grep of old documentation as an authoritative current feature list.
4. Recover the embedded WIP only if those changes are absent. Run `git apply --check`, inspect the patch, then apply. If already applied, skip it. If partially applied or conflicting, reconcile against current code; do not force or discard user changes.
5. Reproduce the completed baseline build separately from WIP verification. Existing test reports describe the completed code, not the current WIP. Finish and test each coherent portion before committing. Never present selectable but unwired REBUILD/GRID entries as implemented.
6. The latest active implementation objective is tool/hotkey/mode parity with upstream Litematica, including REBUILD. Continue that objective unless the user changes priorities. This file does not authorize publishing a release or pushing to a protected branch.

Status vocabulary used here:

- `IMPLEMENTED_HEADLESS`: implementation exists; automated checks passed; native game integration may still fail.
- `WIP_UNWIRED`: partial source exists, no completed behavior or post-edit validation.
- `OPEN`: known missing behavior or a reported defect without sufficient evidence of resolution.
- `REGRESSION_REQUIRED`: a fix exists but this scenario still needs actual game/pack testing.
- `PARITY_AUDIT`: exact upstream equivalence has not been established; investigate before asserting a defect.
- `UPSTREAM_PLACEHOLDER`: even the chosen upstream reference does not implement the advertised operation.
- `LIMITATION`: intrinsic version/protocol/context constraints; explain accurately instead of claiming a universal fix.

## 1. User requirements that persist across sessions

```yaml
product:
  name: Schematica Plus
  ui_display_brand: Schematica+
  mod_id: schematica_plus
  incompatible_mod_id: schematica
  maintainer_credit: HackerRouter
  homepage: https://github.com/HackerRouter/Schematica-Plus
  requested_release_name: Beta 1.0
  configured_base_version: 1.0.0-beta.1
  legacy_java_package: com.github.lunatrius.schematica
target:
  minecraft: 1.7.10
  forge: 10.13.4.1614
  mappings: MCP stable_12
  bytecode: Java 8
  packs: [GTNH 2.8.4, GTNH 2.9.0-RC-1]
  litematica_reference: 26.1.2-0.27.8
  matching_malilib_reference: 26.1.2-0.28.8
workflow:
  commit_each_major_completed_portion: true
  comments: avoid unnecessary comments; keep necessary comments concise
  preserve_licenses_and_attribution: true
  approach: inspect original implementation, port backend and UI behavior faithfully, validate
```

- Full Litematica UI fidelity is the goal: button positions, widths/grouping, icons, hover/click feedback, menus, config pages, tool modes, tool effects, interaction semantics and hotkeys. The user explicitly rejected treating the UI as merely an approximate visual redesign. Functional backends are now the priority; do not enable decorative no-op controls.
- Use **all original upstream Litematica translation keys**, with MaLiLib keys for MaLiLib controls, whenever semantics match. Keep key names intact. Retain all imported locales. Do not invent a parallel set of Plus keys for already translated upstream functions.
- Displayed functional references such as "Litematica Menu" must become "Schematica+ Menu" across locales. Preserve genuine `.litematic` format names and original attribution. Branding must be reproducible through the translation importer, not hand edits that regeneration erases.
- Credits must include HackerRouter and fit the Forge screen using line breaks. The description must briefly describe the mod's actual functions. Do not remove upstream credits.
- `.litematic` files use the L icon; `.schematic` files use S; `.schemplus` uses its separate texture `src/main/resources/assets/schematica_plus/textures/gui/schemplus.png`. The user edited that texture in `65e6123`: preserve it. Do not replace it with the original S copy.
- Forge mod-list logo path was fixed to `assets/schematica/logo.png`, without a leading slash. Legacy resource/package namespaces are intentional; do not mass-rename them to the new mod ID.
- Original/GTNH Schematica with ID `schematica` must be explicitly rejected when installed together with Plus. LunatriusCore utilities are internal; external LunatriusCore may coexist for other mods, but is no longer required by this mod.
- Keep both `.schematic` and `.schemplus` reading and saving behavior; `.litematic` import remains supported. Do not relabel one format with another extension. Explain automatic `.schemplus` upgrades for extended block IDs/Plus metadata.
- Requested paste settings: suppress block updates **default false**; only replace existing air **default false**. Already implemented; retain semantics and protocol limitations.
- Capture/preview intent: block appearance, connections, orientation and lamp on/off state should match capture in singleplayer and multiplayer, while supported animations continue. The user explicitly wants more than GT pipe connections. Investigate relevant GTNH mods, but do not promise reconstruction of server-private data, all outside neighbors or every renderer.
- Support native source-placeable fluid buckets and special GTNH tech containers in the printer, including eligible flasks/cells. A tank/capsule being able to store fluid alone does not establish native placement support.
- User explicitly deferred the rare GT purple-black `.name` block corruption until connection rotation was addressed. Keep the defect in the backlog; do not silently mark it fixed.
- Release workflow: only upload the main runtime mod JAR, not dev/source/API JARs. Timestamp format requested `DD/MM/YYYY hh:mm:ss`; implementation uses Singapore time and 24-hour `'%d/%m/%Y %H:%M:%S'`. Tags trigger releases. Ordinary branch push triggers build/test, not release.
- The user prefers work to continue autonomously within the requested scope and concise Chinese progress/final messages. Avoid unnecessary approval loops. Do not fabricate game-testing results.

## 2. Cloud bootstrap and upstream provenance

Use repository-relative paths from here onward; Windows `D:` paths in this document identify unavailable local evidence only. Reference repositories need not be placed inside the project or committed into it.

```sh
git status --short --branch
git log --oneline -12
git fetch origin --tags
# Clone external references outside the working repository, e.g. ../references/.
mkdir -p ../references
git clone --branch 26.1.2-0.27.8 --depth 1 https://github.com/sakura-ryoko/litematica.git ../references/litematica
git clone --branch 26.1.2-0.28.8 --depth 1 https://github.com/sakura-ryoko/malilib.git ../references/malilib
chmod +x gradlew
./gradlew build --no-daemon --max-workers=2
python3 tools/import_translations.py --check --litematica ../references/litematica --malilib ../references/malilib
```

Build requirements:

- Wrapper: Gradle 8.13. GTNH settings convention plugin `1.0.38`, RetroFuturaGradle through the convention plugin. Build files: `settings.gradle`, `build.gradle`, `gradle.properties`, `dependencies.gradle`, `repositories.gradle`.
- Match CI: JDK 8 toolchain available, JDK 21 (Zulu in release CI) as the Gradle JVM. Modern syntax is enabled through Jabel; output remains JVM 8. Do not assume an installed Java 8 alone can run this Gradle wrapper.
- First cloud build needs network access for Gradle, GTNH Maven, Minecraft/Forge artifacts, mappings and dependencies (including compile-only LOTR from CurseMaven). The old local `--offline` success relied on a populated cache. Do not use offline mode for a fresh environment, or mistake download restrictions for source failures.
- Matching Minecraft/Forge source is generated under `build/rfg/minecraft-src/java`; use it to verify MCP names, patched Forge behavior and renderer/input details. Do not port modern names mechanically or depend on a private Windows source path.
- Tests are JUnit 4.13.2. Plain JUnit intentionally avoids launching a Forge world. Checkstyle remains enabled. Spotless is disabled in the current repo configuration; don't silently reformat the whole codebase.
- Build outputs: `build/libs/`, reports: `build/test-results/test/` and `build/reports/tests/test/`. A git-derived version suffix can occur on untagged builds.
- `.github/workflows/build-and-test.yml` calls GTNH's reusable workflow. `.github/workflows/release-tags.yml` builds on tag push and expects `build/libs/schematica_plus-${TAG}.jar`. Preserve main-JAR-only selection. Avoid creating/pushing tags just to test handoff or a WIP feature.

Reference identities verified during handoff:

| Purpose | Repository / exact revision | Evidence |
| --- | --- | --- |
| Current project | https://github.com/HackerRouter/Schematica-Plus | `origin`, master; local/remote SHAs above |
| Exact modern Litematica port target | https://github.com/sakura-ryoko/litematica/tree/4c256b9fed7473f00040ac2c258f14449d1f4fe7 | Remote tag `26.1.2-0.27.8`; downloaded `gradle.properties`, `config/Hotkeys.java`, `util/SchematicUtils.java`, `en_us.json` match the supplied snapshot after CRLF normalization. Not a byte audit of every file. |
| Matching MaLiLib | https://github.com/sakura-ryoko/malilib/tree/d194eb689d60f13ba39df4ab525dca64b1cf4478 | Local checkout HEAD and remote tag `26.1.2-0.28.8` agree |
| Original Litematica lineage | https://github.com/maruohon/litematica | Attribution/lineage; use the pinned fork above for parity |
| Original MaLiLib lineage | https://github.com/maruohon/malilib | Attribution/lineage |
| GTNH Schematica lineage | https://github.com/GTNewHorizons/Schematica | Original GTNH family; current implementation is substantially changed |
| Embedded core utilities | https://github.com/GTNewHorizons/LunatriusCore/tree/48cf796e6be47d3f3eb57cf0bc0febbe7b53cc82 | Verified local remote/HEAD; embedded code is relocated |
| Freecam reproduction reference | https://github.com/GTNewHorizons/Freecam/tree/f56a98b73c9ce34a5f7d7ac1dc10ef7ec21c1d56 | Verified local remote/HEAD |
| GTNH pack manifest 2.8.4 | https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/2.8.4/README.md | Registry source |
| GTNH pack manifest RC1 | https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/2.9.0-RC-1/README.md | Registry source |
| 1.7.10 UI design reference | https://github.com/GTNewHorizons/ModularUI | No new runtime dependency was adopted |
| Widget/focus precedent | https://github.com/GTNewHorizons/ModularUI2/tree/40fdbdabab7767999b84fda0dc5e9ff47e7edb0b | Source investigated for UI foundation |
| UI/render-state precedent | https://github.com/GTNewHorizons/OpenModsLib/tree/e5e8177f2dd2297d5aea6bb733955a67ffa63c95 | Source investigated for UI foundation |
| Build plugin | https://github.com/GTNewHorizons/RetroFuturaGradle | Check actual resolved plugin version if debugging build infrastructure |
| Shared CI | https://github.com/GTNewHorizons/GTNH-Actions-Workflows | Workflow dependency |

The initially supplied MaLiLib folder was `26.1.1-0.28.2`. The supplied Litematica snapshot requires `>=0.28.8`, so the port intentionally uses the matching `26.1.2-0.28.8` reference. Do not regress to 0.28.2 or silently update both references to newest releases.

All GTNH mod links/version pins are in `compatibility/gtnh-mods.json` (256 union entries, 231 for 2.8.4 and 242 for RC1, 90 entries with acquired source refs). The repository-index appendix below copies its URLs and exact refs for offline AI context. Source acquisition is NOT proof of complete code audit or native compatibility.

## 3. Architecture map

All paths below are relative to `src/main/java/com/github/lunatrius/schematica/` unless qualified.

| Concern | Entry points |
| --- | --- |
| Mod/lifecycle/identity | `SchematicaPlus.java`, `reference/Reference.java`, `proxy/{CommonProxy,ClientProxy}.java`, root `gradle.properties`, `src/main/resources/mcmod.info` |
| Settings | `handler/ConfigurationHandler.java`, `handler/RenderColors.java`, `handler/BlockInfoHudSettings.java`, `client/gui/GuiModConfig.java`, `client/gui/config/` |
| UI foundation | `client/gui/framework/` (`UiScreen`, `UiInput`, `UiPanel`, `UiRowList`, `UiSprite`, `UiTranslations`, drawing boundary) |
| Input / ASM | `asm/{InputPlugin,InputTransformer}.java`, `client/input/{Hotkey,HotkeyEngine,HotkeyHooks,HotkeyStore,Hotkeys}.java`, `handler/client/InputHandler.java` |
| Tool behavior / world edits | `tool/ToolManager.java`, `tool/ToolMode.java`, `tool/ToolHandler.java`, `tool/ToolSelectionActions.java`, `tool/WorldEditTask.java`, `tool/WorldEditJob.java`, `handler/WorldEditQueue.java` |
| World MOVE | `tool/{BlockMoveTransaction,WorldMoveJob,WorldMoveController}.java`, `network/message/{MessageMoveRequest,MessageMoveResult,MessageCapabilities}.java` |
| Area selections | `client/selection/`, `client/gui/save/GuiAreaSelection*.java`, `world/storage/RegionSelection.java` |
| Loaded sources / placements | `client/world/{SchematicSourceData,SchematicWorld}.java`, other source/library/subregion/transform classes in that directory, `client/gui/placement/` |
| Schematics / NBT | `world/schematic/`, `world/storage/`, `nbt/ClientVisualState.java`, optional `compat/` adapters |
| Rendering | `client/renderer/{RendererSchematicGlobal,RendererSchematicChunk,SchematicFrustum,RenderUpdateScheduler,SchematicRenderPass,SchematicTileRenderContext}.java` |
| HUD / verification | `client/renderer/hud/`, `client/renderer/VerifierOverlayRenderer.java`, `client/gui/GuiSchematicVerifier.java`; locate verifier model and task registry by class name |
| File UI | `client/gui/browser/`, `client/gui/load/`, `client/gui/save/GuiSchematicSourceSave.java` |
| Printer / placement rules | `client/printer/` and `client/printer/registry/`; search native item placement handlers before introducing another protocol |
| Regression tests | `src/test/java/` mirrors feature packages; inspect existing pure models before adding Forge-dependent tests |

Persistence invariants:

- Loaded source and placement are distinct. Multiple placements can share one immutable source snapshot; placement transforms must not mutate it. Unload source vs remove placement has different effects.
- Per-world/server/dimension session identity matters. `LoadedSchematics.json`, `AreaSelection.json`, render-layer/session storage and older coordinates migration must not leak to another world. Old unreadable/future schemas must not be overwritten silently. Preserve unknown JSON fields where the existing code does so.
- AreaSelection v5 stores Normal and Simple independently. Older v2-v4 and original coordinate-only data migrate. Subregion-placement JSON v2 preserves signed anchors; v1 migration maintains world geometry.
- Capture/edit size bounds: 16,777,216 blocks, 32,767 per axis, 1,048,576 X/Y rows; NBT 128 MiB allocation budget and depth 64; world Y=0..255. World MOVE has the stricter 1,048,576 enclosing-cell bound. Validate before allocating/writing.
- Canonical tile/entity NBT and client-only visual payload are separate. Partial client packets must not erase full inventories/machine state on later saves or paste. Preview machinery must not tick real machine logic, mutate static registries/shared storage, send network packets, play machine sounds or alter the actual world.
- Transforms compose global operations, placement/subregion origin/anchor and local operations. Work from preserved source data; do not repeatedly rotate already-transformed NBT. Last overlapping region controls the composed preview; air can overwrite earlier content.
- Missing chunks are not air. Scans must distinguish unloaded/unreadable data from completed/correct results.

## 4. Completed code baseline (do not implement again)

### Most recent six commits (not on remote at handoff)

| Commit | State / behavior |
| --- | --- |
| `ebdd3cb` | `IMPLEMENTED_HEADLESS`: task manager for save/world-edit tasks, removal/cancellation and lifecycle handling |
| `82f82fc` | `IMPLEMENTED_HEADLESS`: verifier classifications, ignore pairs, bounded scans, GUI, task integration, incremental rechecks from client world notifications |
| `707fba5` | `IMPLEMENTED_HEADLESS`: selected verifier marker rendering, nearest look selection, comparison overlays/HUD |
| `7494b0a` | `IMPLEMENTED_HEADLESS`: displayed UI branding, Forge logo path, nearest-target pick-block fallback; 313 tests then |
| `fd53c9b` | `IMPLEMENTED_HEADLESS`: MaLiLib-style advanced hotkey engine, UI/settings persistence and tool input alignment; 327 tests then |
| `85b5317` | `IMPLEMENTED_HEADLESS`: server-authoritative world MOVE plus rollback/request/result protocol; 334 tests total |

Earlier implemented portions include internal LunatriusCore utilities; bounded save/download/world-edit work; block-ID/NBT safety; per-world lifecycle; multiple independent previews; camera/frustum/render fixes; native fluids; optional GTNH visual adapters; independent mod identity; paste options; UI foundation and Litematica layout ports; file browsing/basic file management; source/placement separation; material scans/list/export; render layers; color editor; all upstream translation catalogs; multi-box Normal/Simple selections; origins and signed anchors; subregion placement controls; placement locks/enabled/render/enclosing boxes; area analysis; button sounds/hover frames; block info lines HUD.

### Advanced input (`fd53c9b`)

- `coreModClass = asm.InputPlugin`. Manifest must include `FMLCorePluginContainsFMLMod: true` and the InputPlugin FQCN. Transformer redirects LWJGL `Keyboard.next`/`Mouse.next` polling in Minecraft and GuiScreen only. No MaLiLib runtime/mixin dependency was added. Native Forge/other-coremod interaction is untested.
- Chords support keyboard and mouse codes, order, extra keys, PRESS/RELEASE/BOTH, INGAME/GUI/ANY, exclusive/cancel-further and allow-empty settings. Held modifiers use held state, not a fourth HOLD callback mode. Longest matching chords get priority. Context/focus transitions suppress still-held keys until release; consumed input must not leave vanilla keys stuck.
- `config/schematica_plus_hotkeys.json` v1, atomic UTF-8 save, unknown entries preserved; unreadable/future files protected. Old custom options.txt bindings migrate once; unchanged legacy defaults become upstream defaults. No legacy KeyBinding registration / old hard-coded `ToolItemHandler` remains.
- Defaults: M release = menu; M+C configs; M+S selections; M+P placements; M+L materials; M+V verifier; Ctrl+Alt+S save; keypad `*` area editor; keypad `-` placement configuration. Execute is **unbound**, not Enter. Tool defaults to stick and AREA_SELECTION.
- Tool held: Ctrl+wheel mode cycling; Alt+wheel nudge; configurable grow/grab modifiers. Left/right place corners or placement/subregion origin; middle selects. Area sneak = adjacent hit face; placement/world move non-sneak = adjacent hit face. Grab+wheel changes grab distance, moves selected manual origin/whole area, or performs MOVE according to mode/precedence.
- Alt/Shift+middle picks primary/secondary block state for relevant modes. This can work without holding the tool when tool functionality is enabled. Pick-block falls through to vanilla for outside/nearer real-world targets; inspect fresh rays instead of stale objectMouseOver.
- Area add/delete, set origin/corners, move entire area, grow/shrink and selected-corner behavior have backends. `toolItemEnabled`, `executeRequireHoldingTool` (field `executeRequireTool`), `pickBlockEnabled` exist.

### World MOVE (`85b5317`)

- Real creative world operation using selected area, not moving a schematic placement. Triggered by grab modifier + corner keys / scroll / Move Entire Selection; do not route it through Execute as if it were Paste.
- Source + destination block/meta/server tile NBT snapshots; overlap and disjoint-region mask; clear then place then updates. Cancellation/write failure attempts restoration. Permissions, disconnect/dimension loss cancel safely through the transaction. Normal server shutdown drains rollback before saving.
- Non-player entities relocate after block success, including hanging anchors. Client selection moves only after a successful result and only if the original area still exists with unchanged geometry.
- Separate capability flag appended to `MessageCapabilities`; old server flag defaults false. New messages use discriminators 6/7. Requests are bounded, queued to server tick, checked for creative + command permission level 2; no unmodded-server fallback with incomplete client NBT.
- Limits: no disk transaction log/crash recovery; actual worlds keep ticking during multi-tick work; mod callback/absolute-link/scheduled-tick behavior is not universally transactional. Shutdown rollback may be expensive. Test tile reattachment, overlapping inventories and entity behavior in the game.

### Current source/file/verification facts

- Unedited Save Source preserves original bytes and format, including unknown source fields. `.litematic` is import-only except copying original source bytes. WIP edited-source save is a separate unfinished path below.
- `.litematic` independent overlapping region payloads, signed Size anchors, local entities and old v1 wrappers are imported; modern block/entity conversion is necessarily partial for 1.7.10. Independent payloads/signed-anchor semantics do **not** yet round-trip completely through `.schemplus` or the server download format; those carry combined content/named bounds.
- Materials: bounded counts of placement, schematic file or selected real-world area through `MaterialCache` build items, item/meta/NBT identity, filter/sort/ignore/inventory/multiplier, TXT/CSV/JSON exports, the Raw Materials recipe export and the Material List info HUD. Area-analysis exports report incomplete reads.
- Verifier: registry ID + metadata comparison, category filters, exact expected/found ignore pairs, selected markers, incremental world-update rechecks, pause/resume/reset and invalidation on geometry/layers/world change. Do not list all auto-refresh as missing. Tile NBT/entities/modern block-tag equivalence are outside the current comparison model.
- Layer ranges: All/Single/Range/Below/Above, X/Y/Z world coordinates, persisted per session; preview/printer/material scope supported; legacy local Y filter intersects. General Paste still uses the whole placement.

## 5. Interrupted REBUILD work: exact scope and next actions

> **Cloud continuation update (2026-10-01, branch `claude/pensive-darwin-kdveqy`).** The embedded WIP was applied and completed in three commits on top of `1c7413e`: editable sources + placement patching, REBUILD input/operations, and save/unsaved UX. Status is now `IMPLEMENTED_HEADLESS` (build + Checkstyle + 344 JUnit tests, 0 failures); no native game run was possible. Implemented: in-memory working copy per source (`SchematicSourceData.editable/changed/unsaved`), source cells for flat and independent regions with composite mirroring (`SourceEditor`), inverse/forward placement mapping with last-enabled-region ownership (`SourceBlockPosition`), state transforms through the same `PlacementState` path as composition (`CellState`), per-cell patching of all placements of the source (<= 4096 cells) or recomposition via `ClientProxy.refreshSource`, all upstream attack/use operations and the six modifiers plus `schematicEditReplaceSelection` (`tool/SchematicRebuild`), held `ItemBlock` placement simulated in a detached one-block world (`CellState.Scratch`), `.schemplus` save of edited sources (overlapping independent regions refused), orange name/notice icon and discard confirmation in Loaded Schematics. `GRID_PASTE` was later implemented from the 1.12.2 Litematica (see P2).
> The rebuild targeting overlays (simple and directional, MaLiLib face-part layout) and their three `RenderColors` rows are implemented (`client/renderer/RebuildOverlayRenderer`).
> Bulk edits (break/replace all, break all except, replace block type, fill air) and replace-selection run as `tool/RebuildJobs` tasks: the first ~6 ms slice runs on the click, the rest in ~6 ms slices per client tick; changes go to the working copy in batches of 65,536 cells and placements are patched or recomposed once at the end; long jobs appear in the Task Manager (kind Edit Schematic) and can be removed, keeping changes already made. Only one such job runs at a time (a second one is refused and falls through). Replace-selection has no size limit, as upstream.
> Still `OPEN` for REBUILD: persisting unsaved edits across sessions (they are memory-only); copying real-world tile NBT in replace-selection (default tile entities are created). Press consumption follows upstream exactly (user decision): attack/use are cancelled only when the edit is carried out; a refused edit (placement not selected, no owning region, identical replacement, error) lets the vanilla action reach the real world. Targeting matches `getGenericTrace` (fluids included, schematic wins ties, single break range = reach + 1). Remaining intentional difference: replace-selection skips unloaded chunks instead of treating them as air. Placements keep showing the old content until a long job finishes.
> Cloud build notes: Maven Central returned HTTP 429 through the session proxy; a user-level init script `~/.gradle/init.d/central-mirror.gradle` pointing at `https://maven-central.storage-download.googleapis.com/maven2/` was used (not committed). Tests with CJK file names need a UTF-8 locale (`LC_ALL=C.UTF-8`); without it 6 tests fail with `InvalidPathException`.

Status: `WIP_UNWIRED`, **no build or test was run after these edits**. Seven modified tracked files and two new files were present. Complete patch is embedded at the end. Existing local source files remain untouched by handoff creation.

| File relative to Java package | Partial implementation / risk |
| --- | --- |
| `client/gui/save/GuiSchematicSourceSave.java` | Uses source `saveExtension()` and `save()` instead of direct original snapshot writes |
| `client/input/Hotkeys.java` | Registers six empty REBUILD modifiers; no editing actions connected |
| `client/world/SchematicSourceData.java` | Optional edited ISchematic, clone/edit, edited instantiate, modified flag, edited `.schemplus` save path; no edit callers yet |
| `client/world/SchematicWorld.java` | `sourceData()` and `refreshSource()` to rebuild regions and bump content revision |
| `tool/ToolMode.java` | Adds GRID_PASTE and REBUILD in original ordering; currently selectable without working behavior |
| `world/schematic/SchematicFileSnapshot.java` | Captures an ISchematic via SchematicAlpha into bounded compressed `.schemplus`; not tested for loss of unsupported metadata/independent regions |
| `world/storage/SchematicCopies.java` | Deep-ish copy for flat/multiregion sources, tiles/entities/origin; independent+flat entity duplication was adjusted but still needs tests |
| NEW `client/world/SourceBlockPosition.java` | Inverse global/local transforms and last-overlap-owner lookup into original region coordinates; no callers/tests |
| NEW `tool/RebuildDirection.java` | MaLiLib-style hit-face outer-quarter direction calculation; no callers/tests |

Next REBUILD design work (planned, not written):

1. Trace upstream `event/InputHandler`, `event/KeyCallbacks`, `util/SchematicUtils`, `util/RayTraceUtils`, `util/PositionUtils`, `tool/ToolMode`. Use vanilla attack/use binding codes after earlier hotkey/tool handling has declined the event. REBUILD should edit in-memory schematic content, never silently edit the real world. Upstream allows this without holding the tool.
2. Use fresh nearest schematic ray (respect actual world occlusion, active render/layer behavior) and invert placement/global/subregion transforms to original source coordinates. Respect overlap ownership and disabled regions. Also invert metadata/directional tile state as needed; 1.7.10 metadata is not a modern BlockState property set.
3. Break one; break in targeted direction; break all matching; break all except targeted state. Place adjacent using held ItemBlock, or empty hand plus picked primary block. Holding a non-block item should not invent a replacement. Directional place/replace, replace all and replace-with-properties need explicit semantics.
4. Upstream face-center direction differs from outer-quarter direction. Break/replace points into the target; placement starts in adjacent air. Upstream directional traversal stops at state changes/limits (inspect original 10000 cap and render/chunk conditions).
5. `schematicEditReplaceSelection` is an action copying an actual world area selection into a schematic (`saveAreaSelectionToSchematic`), not simply another held modifier. It was not added in WIP.
6. Deep-copy then publish edited source consistently, refresh all placements sharing that source, retain each placement's geometry/settings, invalidate render/material/verifier caches appropriately. Maintain independent region payloads and flat composite. Do not mutate immutable raw source data or unrelated placements.
7. Budget large edits over client ticks; snapshot world/source/revision and handle cancellation/staleness. A proposed 4096 cells/~4 ms budget is a design suggestion, not existing REBUILD code.
8. Implement save/reload/unsaved-changes UX without accidental original-file overwrite. WIP defaults edited content to `.schemplus`, but independent-region loss must be resolved or explicitly handled before shipping. Disk/session persistence of edits, copying and reloading still need deliberate semantics.
9. Tests: transform inverse and directional hit zones; overlaps/air/masks; clone isolation and entity counts; mutation atomicity/stale jobs; all placements refreshed; save round-trip and unchanged-byte copy preserved; native mouse/GL/game behavior.

`GRID_PASTE`: present only as an enum entry in the pinned upstream source; no execution branch was found there. Classify as `UPSTREAM_PLACEHOLDER`, not a completed operation. Actual repeating placement grids are a distinct feature and need their own backend/UI. Hide/disable/explain unavailable actions instead of shipping silent no-ops.

## 6. Remaining feature/parity backlog

This is the reconciled known backlog, not a promise that every upstream Configs field has been audited. Use the exact upstream source to extend the coverage matrix. Translation-key presence is not feature implementation.

### P1: Finish the active tool/input objective

- `IMPLEMENTED_HEADLESS` / partly `OPEN`: REBUILD operations, edited source lifecycle and save integration are done (see the section 5 update); cross-session persistence of unsaved edits remains (upstream does not persist them either).
- `IMPLEMENTED_HEADLESS` (2026-10-01): every appendix-B hotkey is registered with working behavior, including the five Schematic VCS hotkeys (P2).
- `IMPLEMENTED_HEADLESS`: Visuals master switches (`handler/VisualSettings`), all render toggle hotkeys with MaLiLib toggle messages, held ghost/overlay inversion, overlay through blocks, and the Block Info Overlay (without inventory previews). Still `OPEN`: overlay outline widths, inventory overlays.
- `PARITY_AUDIT` (partly done): scroll precedence now matches `InputHandler.handleMouseScroll` (grow modifier always consumes with upstream messages, nudge consumes only when something moved, `reverseOperationModeDirection`); `selectionModeCycle` cycles the corner mode / Delete target / paste replace mode instead of Normal/Simple; pickBlockFirst/Last follow `shouldPickBlock`; add/delete selection box print upstream messages and delete removes a selected manual origin first. Paste replace mode is upstream `pasteReplaceBehavior` (None/All/With non-air, default None); a legacy `pasteOnlyAir` is migrated once (true → None, false → With non-air, which was the old non-air-only paste). All now also clears world blocks where the schematic has air, inside enabled regions. Remaining: key conflict behavior, action contexts and a full Tool HUD comparison.
- `IMPLEMENTED_HEADLESS`: Execute no longer cancels a running edit (busy is refused; remove tasks in the Task Manager), Fill/Replace without picked blocks fall through like upstream, Fill/Replace/Delete use only the selected box when one is selected, and Delete can target the selected placement (`ToolMode.deleteUsesPlacement`, toggled with selectionModeCycle).
- `IMPLEMENTED_HEADLESS`: cloneSelection (with `cloneAtOriginalPosition`), saveAreaAsInMemorySchematic (in-memory sources, not persisted), placement rotation/mirror hotkeys, replace-selection, Delete placement target.
- `PARITY_AUDIT`: Input plugin in installed obfuscated JAR, other coremods, GUI/focus transitions, custom vanilla mouse/keyboard bindings, held repeats and releases, no stuck attack/use keys. Investigate RELEASE matching when extra keys change a chord; exclusivity across GUI transitions; old custom I binding migration; user feedback on failed hotkey persistence. These are review targets, not confirmed runtime bugs.

### P2: Remaining original feature systems

- `IMPLEMENTED_HEADLESS` (2026-10-01): Easy Place following upstream's default legacy path (`client/printer/EasyPlace`: easyPlaceMode, easyPlaceUseKey with hold, easyPlaceFirst/furthest-before-vanilla, swap interval, 2 s position cache, swing hand, vanilla reach, slab completion, fluids through the printer's native containers), Placement Restriction with placementRestrictionWarn, Sign Text Paste, the Status Info HUD (statusInfoHud/Auto) and the five related hotkeys, all with upstream keys. Without a Plus server behavior equals upstream protocol "None" (the printer's side/half/extra-click rules; facing follows the player). Still `OPEN`: post-rewrite Easy Place options, inventory pick-block slot configs.
- `IMPLEMENTED_HEADLESS` (2026-10-01): accurate placement protocol, the Plus counterpart of easyPlaceProtocol v3. `easyPlaceProtocolVersion` (upstream key and values; auto/v3/v2 use the protocol, slabs_only only for slabs, none disables it). When the server advertises `supportsAccuratePlacement` (`MessageCapabilities`), Easy Place and the printer send `MessagePlacementIntent` (position, block name, metadata) before each click, no longer refuse wrong player facing, and fall back to any solid side. The server (`handler/AccuratePlacement`, config `accuratePlacementEnabled`) applies the metadata in `BlockEvent.PlaceEvent` only for the same block at that position within 2 s, and only for Litematica-whitelisted properties expressed as metadata bits per block class (facing, slab half, axis, rail shape, repeater delay, comparator mode, open; quartz pillar axis); powered/age/content bits, multi-block placements and unknown modded blocks are left as placed, and the dropped item/damage must not change.
- `IMPLEMENTED_HEADLESS` (2026-10-01): Schematic VCS / projects (`client/projects`, `client/gui/projects`), gated by `unhideSchematicVCS` like upstream. Projects browser (JSON files under the schematic directory: create/load/delete/close, project info), project manager (version list with hover, save version, area editor, move origin, place to world, delete area, browser, close), version prompt (name + description), main-menu button with the upstream warning, the five VCS hotkeys plus scroll cycling, Execute pasting the current version, `schematicVcsDeleteMode` with delete-by-placement (`WorldEditJob.Kind.DELETE_PLACEMENT`, render-layer clamped), Tool HUD/Status HUD lines and the project origin outline. While a project is open its own Normal/Simple selections replace the world's (`AreaSelections.setOverride`), the area browser is disabled and tool clicks always edit the area. Versions are saved as numbered `.schemplus` files beside the project JSON and checked out as transient placements that the world session does not save; the open project per world is kept in `SchematicProjects.json`. The project origin outline is drawn on its own (width 4) and area lines are 3 px in project mode, 1.5 px otherwise, as upstream. The version description uses the new multi-line `UiTextArea` (`TextAreaModel`: soft wrap, Enter for line breaks, 512 characters, 8 lines). Every delete (Delete tool, VCS area delete, Entire Volume pre-paste clear, delete-by-placement) removes non-player entities in its boxes on the integrated server (`TaskFillArea.directRemoveEntities`); 1.7.10 has no `@e` selector, so command-based deletes on remote servers keep entities and say so. Difference: project JSON stores Plus selection-library data, so upstream `.json`/`.litematic` projects do not load.
- `IMPLEMENTED_HEADLESS` (2026-10-01): grid/repeated placements and Grid Paste, ported from the 1.12.2 Litematica (maruohon `liteloader_1.12.2`: `GridSettings`, `GridPlacementManager`, `PlacementGridSettingsScreen`, `gridPasteCurrentPlacementToWorld`) because the pinned 26.1.2 source only has the unused `GRID_PASTE` enum. `client/world/GridSettings` (size never below the placement's enclosing box, repeat counts per direction, saved in LoadedSchematics.json, carried over reloads); `client/world/GridPlacements` keeps repeated copies only inside the loaded area (render distance + 1 chunk, ±512 Y), recreating them when the base changes; copies share the base's blocks, region state and settings (`SchematicWorld.repeatedCopy`), are rendered and targeted (Easy Place, restriction, block info, pick block) through `ClientProxy.visiblePlacements()`, but are never saved, listed or selectable. Placement configuration has the `Grid/Repeat: ON/OFF` button (Shift toggles), `GuiPlacementGridSettings` edits size and repeat counts, hotkey `openPlacementGridSettingsScreen`. The `GRID_PASTE` tool mode is selectable again: Execute pastes the selected placement and its existing copies one after another (paste replace mode and HUD as Paste); without an enabled grid it pastes only the placement with a note. Plus keys hold the 1.12.2 texts that the 26.1.2 catalog lacks.
- `IMPLEMENTED_HEADLESS` (2026-10-01): schematic manager Edit / Import / Export (`GuiSchematicManager`, `GuiSchematicConvert`, `world/schematic/SchematicFiles`). Edit (.litematic only) renames or changes the author in the metadata, or sets the preview from the next screenshot-key press (`SchematicPreview`, centered square scaled to 120x120 ARGB like upstream; right click cancels; Ctrl+Alt+Shift uses `thumb.png` beside the file). Edits patch the file's NBT in place (long arrays kept), so blocks and version never change; files whose long arrays sit inside lists are refused. Import saves any readable file as .litematic (a .litematic source keeps name, author, description, preview, creation time); Export (litematic only) writes .schematic (auto .schemplus), V4 (1.12.2) .litematic (Plus label replacing upstream's V6 entry) or a vanilla .nbt structure; both have Ignore entities and Shift to overwrite. The browser info panel shows upstream metadata lines, version/schema (`DataVersions` from MaLiLib's Schema list) and the preview for .litematic and .nbt, read off-thread and cached until refresh/close.
- `IMPLEMENTED_HEADLESS` (2026-10-01): Load browser Material List (`MaterialListSchematic`: reads the file without placing it, counts every block as missing like upstream, Shift picks sub-regions through the MaLiLib-style `GuiStringListSelection`, title `material_list.schematic`) and Rename Schematic (.litematic metadata name, in place).
- `IMPLEMENTED_HEADLESS` (2026-10-01): all four save-screen checkboxes. Save from schematic world reads the enabled placements (first placement holding a block wins; block entities and entities are copied with offsets) through `world/chunk/CaptureSource` instead of the world, and runs on the client. Visible blocks only keeps blocks with a face not covered by an opaque, solid-sided neighbour (`LitematicaSchematic.isExposed`); Include support blocks also keeps hidden blocks under repeaters, comparators, snow layers, carpets and under falling blocks that are visible or hold such blocks (`isSupport`).
- `IMPLEMENTED_HEADLESS` (2026-10-01): `.litematic` writing in the 1.12.2 Litematica layout (maruohon `liteloader_1.12.2`: Version 4, MinecraftDataVersion 1343, Metadata with preview, per-region Position/Size/BlockStatePalette/BlockStates/TileEntities/Entities/PendingBlockTicks, entity Pos relative to the region). Each vanilla block is written as the exact 1.12 state from `assets/schematica/data/legacy_block_states.txt`, generated by `tools/generate_legacy_block_states.py` from the Minecraft 1.13.2 flattening table that modern Litematica uses to upgrade version < 5 files, so 1.12.2 Litematica reads it natively and modern Litematica converts it. Modded blocks keep their registry name with `SchematicaPlusMeta`. Block entity, entity and item NBT go through `LegacyNbt` (1.11 ids, string item ids, sign JSON, spawner SpawnData/SpawnPotentials, HandItems/ArmorItems, wither skeleton / zombie villager / horse splits) because data version 1343 tells readers to apply no older fixes. Reading files with version < 5 or data version < 1631 uses the same table backwards (exact 1.7.10 block/meta; 1.8+ states and stone/sponge variants go through the modern translator) and `LegacyNbt` in reverse. Save Schematic offers .schematic / .schemplus / .litematic; vanilla structures (.nbt, 1.12 layout written, 1.12 or modern read, first of several palettes) can be loaded, imported and exported.
- `OPEN`: Remaining 1.7.10→1.12 NBT gaps: potion/spawn-egg item data (1.9 changes), Riding→Passengers, banners/shulkers do not exist here. Sponge `.schem` import is not implemented.
- `IMPLEMENTED_HEADLESS` (2026-10-01): `.schemplus` stores independent (also overlapping) region contents as `SchematicaPlusRegionData` (version 1: one nested Alpha schematic per region with its own blocks, block entities, entities and signed origin, in region order) next to the flat data that older readers still use; writing forces the extended encoding, reading rebuilds a `MultiRegionSchematic`. Edited sources with overlapping independent regions now save instead of being refused. Server downloads: the client acknowledges region support (`MessageDownloadBeginAck.supportsRegions`), the server sends the compressed region payload after the block chunks in 30000-byte `MessageDownloadRegions` slices (8 per tick, 32 MiB limit) and the client restores the regions before saving; clients without support get the update-client message for such schematics.
- `IMPLEMENTED_HEADLESS` (2026-10-01): material list HUD, cache and Raw Materials. A material list is now an object (`client/gui/material/MaterialList`) that outlives its screen: the placement's list stays with the placement (settings `type`/`sort_criteria`/`sort_reverse`/`hide_available`/`multiplier` saved in LoadedSchematics.json as `materialList`, carried over reloads), and `MaterialLists` remembers the last viewed list like DataManager. The Material List hotkey reopens it, or the selected placement's list when there is none; the placement configuration button, the load browser and Analyze Area set it; changing the selected placement or the world forgets it. Counting continues in the background each client tick. Info HUD toggle draws the missing items (missing minus inventory, or the multiplied total) below the info HUD text at the info HUD alignment/offsets, refreshed every 2 s, with `materialListHudMaxLines`, `materialListHudScale`, `materialListHudItemCountsColor`, and also inside GUIs when `renderMaterialListInGuis`. `highlightBlockInInventory` outlines container slots holding the item of the looked-at schematic block in `hightlightBlockInInventoryColor`. `MaterialCache` maps block+meta to build items like upstream (door tops, bed heads, double-plant tops, piston heads/moving pistons, portals, fire need nothing; farmland is dirt; still water/lava and Forge fluid sources are buckets, flowing fluids nothing; double slabs count 2, snow layers count their layers; flower pots are pot + plant), caching only blocks without tile entities because 1.7.10 mods pick by tile entity; Clear cache empties it. `materialListIgnoreState` compares only the block. Raw Materials (`RawMaterialExport`, `RecipeIndex`) resolves each material through its first crafting recipe (shaped/shapeless/ore) or else its first smelting recipe, skipping recipes that take the item itself, stopping at loops and at base materials (items with a reversible 4/9 packing recipe, i.e. ingots, nuggets, gems), and writes `raw_material_list_recipe_details` (when `materialListRecipeDetails`), `raw_material_list_recipe_steps` and `raw_material_list_simplified` (packed into blocks + remainder) to the dumps folder; Shift = missing only. Items are `modid:name[:damage]`. Differences: no stonecutter in 1.7.10 so Alt has nothing to choose; counts are total counts without the multiplier, as upstream.
- `IMPLEMENTED_HEADLESS` (2026-10-01): file-based area selections like Litematica's SelectionManager. Every normal selection is a JSON file in Litematica's format (`name`, `current`, `boxes` with `pos1`/`pos2`, explicit `origin`, plus a `schematica_plus` object for the guide/origin/corner state) under `<schematics>/area_selections_per_world/<world or server>/area_selections` (config `areaSelectionsPerWorld`, upstream key, default on; off uses `<schematics>/area_selections`), so selection files can be organized in folders and copied to or from Litematica. `AreaSelection.json` keeps only each world's mode, corner mode, simple selection and selected file (version 6); version ≤ 5 data is moved into files on the first save (names that exist get a number). Names are unique per folder; renaming renames the file, removing deletes it. The selection manager browses folders (root, up, create directory, current path, search) and creates selections in the current folder. Schematic projects keep their selections inside the project file.
- `PARITY_AUDIT`: Full verifier parity beyond the current per-placement scans/markers. Verify multiple-placement scheduling/retention and original HUD behavior. Registry+metadata comparison is implemented; modern equivalence rules and any useful 1.7.10 replacement need separate design. NBT/entity comparison was not promised by upstream parity alone.
- `IMPLEMENTED_HEADLESS` (2026-10-04): Different blocks (`enableDifferentBlocks`, default off) through `util/BlockGroups`, the 1.7.10 form of MaLiLib's replaceable block groups (stairs, slabs, double slabs, walls, fences, fence gates, doors, trapdoors, buttons, pressure plates, standing/wall signs, logs, leaves, planks, saplings, flowers, flower pots, anvils, beds, carpets, wool, glass, glass panes, terracotta, ores, by block class so mod subclasses join). Colors and wood types are metadata in 1.7.10, so the bits that change `damageDropped` are the variant and the rest is the state: another block or variant of the same group with the same state is a different block, otherwise wrong state. Overlay `schematicOverlayTypeDiffBlock` with `schematicOverlayColorDiffBlock`, verifier "Diff Blocks" category/button/status, and per-type toggles `schematicOverlayTypeMissing/WrongBlock/WrongState`. Overlay lines use `schematicOverlayOutlineWidth` (1.0) or `schematicOverlayOutlineWidthThrough` (3.0) when drawn through blocks; extra blocks are no longer drawn as wrong blocks when `schematicOverlayTypeExtra` is off.
- `OPEN`: Remaining visual/config behaviors: `schematicOverlayModelOutline/Sides` (model-shaped overlay) and other HUD/config toggles from the config audit.
- `PARITY_AUDIT`: All Configs Generic/InfoOverlays/Visuals/Colors options against the pinned upstream class; Plus currently exposes its implemented Forge properties and a subset of equivalents, not the entire upstream configuration system. Preserve Plus-only printer/save options while adding meaningful original options.
- `UPSTREAM_PLACEHOLDER`: Subregion Slice control is deliberately unavailable, matching the selected upstream placeholder. Do not falsely report it as completed or prioritize invented semantics as a user requirement.

### P3: Integration, persistence and finish work

- `IMPLEMENTED_HEADLESS` (2026-10-01): remote full-NBT edit protocol, the Plus counterpart of Servux paste. On a server advertising `supportsRemoteEdit`, Paste/Fill/Replace/Delete/delete-by-placement/VCS edits are encoded by `WorldEditJob.write` (registry-name palette, metadata, region mask, tile and entity NBT, entity-removal boxes, replace behavior, without-updates), gzip-compressed (≤32 MiB) and uploaded in 30000-byte `MessageEditUpload` slices by `handler/client/RemoteEditClient` (Task Manager stage Upload, cancellable). `handler/RemoteEdits` reassembles per player, decodes off-thread with an NBT size limit, validates every size/name/index (`WorldEditJob.read`), checks creative + op level 2 like MOVE and runs the job in the server `WorldEditQueue`; `MessageEditStatus` returns accepted/progress/finished/failed/rejected with Litematica finish messages from the server. Config `remoteEditsEnabled`. Other servers keep the `/setblock` fallback (no NBT, no update suppression, entities kept, reports commands sent).
- `PARITY_AUDIT`: Bounds, stale callbacks, closing screens, task cancellation/disconnect, dimension transitions, concurrent source reload, deleted files and shared-source edits. Maintain bounded budgets and no false success on partial/unreadable work.
- `OPEN`: Pixel/layout parity and native input checks at GUI scales 1/2/3/Auto, 320x240, Unicode fonts and all locales. Long labels, button widths, icon hover border, click sounds and modal focus must remain correct. Existing hand-built adapters may differ from modern MaLiLib under resource packs/fonts; compare actual behavior.
- `OPEN`: Refresh stale README/tutorial/default-key statements, old chronological phase limitations and relevant THIRD_PARTY_NOTICES wording. README currently still says N/Enter, single-key/advanced hotkeys pending, and contains contradictory older source/placement descriptions. The import notices also predate deliberate display-brand substitutions. Preserve historical context but add an accurate current capability summary.
- `OPEN`: Validate a release JAR in both fixed GTNH versions with/without Angelica/Freecam and on a dedicated server. No cloud/headless test can establish actual GL/render/tile-mod compatibility.

## 7. Reported bugs, fixes present, and unresolved compatibility work

| ID / status | User report / required regression | Current evidence / likely entry points |
| --- | --- | --- |
| B01 `OPEN`, user-deferred | Very few random GT cables/pipes paste as purple-black invalid `.name` blocks, no special logs, success message shown | Sample `test12222_server.schemplus` is local-only. Never confirmed fixed. Inspect registry ID/meta, GT tile/machine subtype NBT, rotation/save/reload and placement order; don't assume all purple texture reports have the same cause. |
| B02 `REGRESSION_REQUIRED` / user previously said still present | GT and EnderIO connection directions wrong after rotation in both preview and pasted world, including bare pipes | `a344717` and `deab157` transform connections/side settings in preview and saved NBT. No final user/native confirmation. Sample `enderio.schematic`; test all rotations/mirrors, resave/reload, global+subregion composition, filters/covers/modes. Treat an ongoing reproduction as open. |
| B03 `REGRESSION_REQUIRED` | Schematic partly/fully disappears on switching/moving/rotating an instance and at certain player-relative position intervals/subchunks | Render-cache scheduling/camera/frustum fixes `8e49e7d`, `195f5fd`, `4306adc`; test multiple instances, negative coordinates, 16-block boundaries, frustum edges and all camera positions. |
| B04 `REGRESSION_REQUIRED` | Leaving a dimension does not fully unload one schematic; returning makes it reappear | Session/world identity cleanup and persisted removals `afd3d71`; distinguish intentionally restored saved placement from stale unloaded object. Test repeated dimensions/servers/disconnect and unload persistence. |
| B05 `REGRESSION_REQUIRED` | Cauldron inner face/cut surfaces missing | Forge render-pass/GL state fixes `4306adc`; test inside/outside, fluid levels, alpha, both passes and TESRs. |
| B06 `REGRESSION_REQUIRED` | With Freecam, schematic moves together with camera | Active camera/matrix changes `195f5fd`; compare actual player vs render-view entity, interpolation and origin translations. |
| B07 `REGRESSION_REQUIRED` | ProjectRed Illumination lights turn into HarvestCraft fish traps, mage campfires, Galacticraft telepads or nothing | ID/nibble/format fixes `7a908a0`, `0f70806`, automatic extended save `4daba6a`, multipart visual adapters. Check numeric IDs above 4095 and registry-name remapping independently from visual streams. |
| B08 `REGRESSION_REQUIRED` | IC2 crops on crop sticks preview as purple-black blocks but paste normally | `115d86d` stops schematic chunks being reported empty to IC2 rendering; native crop texture and growth state checks remain. |
| B09 `REGRESSION_REQUIRED` | Client save error in Sep 30 17:04 log | Extended-ID format upgrade and world-context changes exist; re-test same build/selection and both output choices. Preserve exact logged exception if sample is supplied. Do not invent it from the filename. |
| B10 `REGRESSION_REQUIRED` | Creative middle-click with a selected schematic ignores outside-world blocks | `7494b0a` nearest fresh trace + vanilla fallback; hotkey implementation subsequently replaced input path. Regression must include misses, closer real blocks/entities, same-position blocks, disabled preview and Freecam. |
| B11 `REGRESSION_REQUIRED` | No click sounds, missing icon hover frames, mod-list icon missing | `a90d41d`, `7e0bc4c`, `7494b0a`; ensure new controls inherit behavior and logo loads from installed JAR. |
| B12 `PARITY_AUDIT` | Tool effects/hotkeys still not all original Litematica | Most recent explicit user correction. Section 6/P1 is the active priority; don't stop after cosmetic key names. |

Earlier crash log at Sep 30 14:00 was discussed and the user said it seemed fine. Keep it as historical evidence, not a confirmed current mod crash without re-reading the log.

### GTNH visual/container coverage already present

Read `compatibility/README.md` and concrete `compat/` classes before adding duplicate adapters. Existing source-backed routes include:

- GregTech native update/custom fields, pipe connection masks/covers/redstone/disabled fluid sides; AE2 and AE2 Fluid Crafting cable/part/client fields and TileCableBus replacement; ForgeMultipart/ProjectRed native part factories and writeDesc/readDesc.
- Forestry and Railcraft streams; BuildCraft render/pluggable/pipe streams; LogisticsPipes streams; Galacticraft annotated client fields/relative links; OpenComputers identity preservation; JABBA side-effect-free capture.
- EnderStorage/Translocators lids/tank/attachments and selected safe interpolation; IronChest display stacks/lids; Binnie machine/flower RenderInfo; MalisisDoors animation completion; Steve's Addons RF sides; Thaumic Exploration client seals/lids without shared-storage mutations.
- Native canonical NBT/S35 paths inspected for EnderIO, StorageDrawers, ArchitectureCraft, Carpenter's Blocks, LittleTiles, OpenBlocks/OpenModsLib, bdlib/ae2stuff/gendustry, FloodLights, OpenModularTurrets, Botania/Botanic Horizons, Amazing Trophies, NH core baby chests and IC2 Experimental 2.2.828.
- Printer native item paths: registered ItemBucket (including EnderIO/TConstruct subclasses), Railcraft/Forestry buckets, GT volumetric flasks, IC2 universal fluid cells. Need >=1000 mB, valid source-placeable fluid, proper aim/reach and native inventory/container handling. Don't synthesize placement for Forestry cans/capsules, Galacticraft canisters or Botania fluid-removal-only buckets.

### Still incomplete / cannot be assumed

- `OPEN`: AE2 multipart orientation, GT extended machine facings, other custom directional encodings, absolute-coordinate links in NBT/streams, links to blocks outside the selection. Native block rotation is not enough for every tile.
- `LIMITATION`: Multiplayer capture can only know synchronized client state. Singleplayer capture uses integrated-server data. Preserve all available render state separately, but don't manufacture unsynchronized inventories, genetics or private machine state. Older files lacking visual data require recapture.
- `LIMITATION`: World-time/frame-based animation and selected safe client animation are supported; private tick counters, transient particles/sounds/network events, target entities and simulation-dependent effects are not generally reproducible. Do not solve this by ticking real machines in the schematic world.
- `OPEN`: Outside-neighbor/context, layer-filtered neighbor lookups, biome/tint, original light environment, resource pack/shader differences. User intent is visual fidelity; further scoped adapters/context capture may be needed.
- `LIMITATION`: Binary stream adapters carry version fingerprints; mismatch must skip unsafe visual payload while retaining canonical NBT. Opaque internal numeric IDs cannot be promised portable across arbitrary packs/world registries.
- `OPEN`: Source audit gaps: as recorded on 2026-09-30, Catwalks-2, Galaxy-Space-GTNH, harvestcraft and Automagy-GTNH public source URLs were unavailable. Recheck current availability before asserting that remains true. Binary-only Thaumcraft, Witchery, ExtraUtilities and BiblioCraft have not received exhaustive tile-render audits.
- `PARITY_AUDIT`: MOVE does not provide crash-safe atomicity, full scheduled-tick transfer or every mod's absolute links. Snapshot/clear/place spans ticks while other world logic can run. Test cancellation and restoration with actual machine tile instances, mutable inventories and source/destination overlap; no headless result proves these cases.

## 8. Translation and UI maintenance contract

- Canonical generated catalogs: `src/main/resources/assets/schematica_plus_litematica/lang/*.lang`. 13 Litematica locales + 12 MaLiLib locale inputs produce 13 bundled locale files. Original translator credits retained. Plus-specific fallback strings live under the legacy Schematica asset locale files.
- Generator: `tools/import_translations.py`, no third-party Python packages. Version targets and the semantic `BRAND_KEYS`/brand substitutions are explicit. Run `--check` with the pinned checkouts after relevant edits.
- Reuse exact existing upstream keys with correct parameter order/count and formatting codes. A key that exists but describes a different operation is not an acceptable shortcut. Only genuinely Plus-specific behavior/limitations/errors need new keys; keep at least English and Chinese usable and preserve fallback behavior.
- Do not blind-replace every occurrence of `litematica` in source or language keys. Format identifiers, resource locations, attribution, imported file names and APIs are distinct from displayed product branding.
- Upstream locale coverage does not automatically translate vanilla/other-mod text into a locale absent from 1.7.10. Missing native strings may fall back to English.
- Audit new error/status/hotkey/tool/HUD strings alongside each feature. Ensure formatting survives conversion to legacy `.lang` (escaped newline/backslash, percent placeholders and Unicode). Test English/Chinese and long-text locales in the actual UI.
- Plus key audit (done): every remaining `schematica.*` key in `assets/schematica/lang` is referenced and has no upstream equivalent (printer, server commands/permissions, task backends/stages, 1.7.10 limits, `.schemplus`, MOVE protocol, file-operation safety, UI demo). Usage-only hints that upstream does not show were dropped instead of translated; locales whose strings all became unused were deleted (`de_DE`, `en_GB`, `ja_JP`, `ko_KR`, `no_NO` keep printer/server/command strings). `ConfigTranslationsTest` checks that every visible config label/comment resolves in English and Chinese. The importer repairs upstream placeholders broken as `%§`/trailing `%` (uk_UA paste messages).
- Existing custom controls must continue to use shared sound/hover/focus behavior. Keep Litematica atlas coordinates intact and retain the user-customized schemplus texture.
- Preserve `THIRD_PARTY_NOTICES.md`, SPDX headers and bundled MIT/LGPL/GPL texts. Most original repo code is MIT; adapted Litematica/MaLiLib material is LGPL-3.0 with file-level notices. "Few comments" does not mean deleting license notices.

## 9. Validation and acceptance obligations

Latest completed code (`85b5317`): build + Checkstyle, 334 JUnit tests, zero failures/errors/skips. These are headless tests. WIP has no corresponding validation. Do not run an unchanged full suite repeatedly merely to report activity; rerun relevant and required checks after actual changes.

Required focused regression groups for continuation:

1. REBUILD source copy/inverse transforms/overlap/persistence before enabling its UI. Flat and independent regions, sparse masks, negative coordinates, signed anchors, local+global rotations/mirrors, NBT and entities.
2. Input install-level manifest/ASM checks, M release versus chords, GUI context, focus loss, modified vanilla controls, modifier precedence, consumed press/release and normal outside-schematic pick fallback.
3. MOVE overlap in both directions, gaps/air, capture failure/write failure, cancellation at every phase, disconnect/permissions/dimension changes, normal server shutdown, missing chunks, entity/hanging behavior, native GT/IC2/AE2 inventories. Be explicit about abrupt process termination limitations.
4. Save/import/download/resave round-trip across `.schematic`, extended-ID `.schemplus`, independent `.litematic`, old source versions; source bytes unchanged when unedited; full NBT not replaced by partial visual updates; overwrite/rename/stale source safety.
5. Rendering with both target packs, vanilla/Angelica as applicable, Freecam, multiple placements, 16-block boundaries, real-camera/ghost position changes, dimensions, cauldron interior, IC2 crops, GT/EnderIO rotations, PR multipart lights and safe animations.
6. Server/singleplayer fluid placement inventory consumption, container return, <1000 mB, server-denied actions, reach/visibility, Nether evaporation, avoiding tanks/TE interactions; old printer behavior remains functional while Easy Place is introduced.
7. Paste defaults, air-only on current destination state, silent paste behavior with sand/redstone/fluids/TEs, no unsupported silent `/setblock` fallback, pending versus actual success messages.
8. UI/resizing/locales, long labels, hover frame/click sound, task removal, material incomplete counts, verifier incremental rechecks and stale invalidation, source/placement/session separation.

`TESTING.md` already contains detailed scenarios. Reconcile older historical test instructions with newer features instead of deleting valuable regression cases. If the cloud cannot start a graphical Minecraft/GTNH environment, report that specific limitation and provide a testable build; do not claim visual equivalence or successful gameplay.

## 10. Local-only evidence unavailable to the cloud

These paths existed on the previous Windows workstation, not in GitHub. Ask for the specific artifact only when needed to reproduce that defect; continue independent source work meanwhile. Do not invent their contents or expect local-drive mounts.

```text
D:\MC\GTNH\schematics\test12222_server.schemplus
  rare GT invalid .name/purple-black paste sample
D:\MC\GTNH\schematics\enderio.schematic
  EnderIO rotation/connection sample
D:\MC\常用端（HMCL）\minecraft-exported-logs-2026-09-30T14-00-09.log
  earlier crash, subsequently described by user as seeming okay
D:\MC\常用端（HMCL）\minecraft-exported-logs-2026-09-30T17-04-43.log
  client schematic save failure
D:\CODE_PROJECT\Python\TMC_AD\SOURCECODE\1.7.10_src
  user-supplied raw source; prefer generated RFG Forge/MCP source for actual build names
D:\CODE_PROJECT\litematica-26.1.2-0.27.8
D:\CODE_PROJECT\malilib-26.1.1-0.28.2
D:\CODE_PROJECT\malilib-26.1.2-0.28.8
D:\CODE_PROJECT\GTNH-compat-sources
  local references are replaceable with the exact GitHub pins in this handoff/manifest
```

User authorized obtaining relevant public mod source repositories. On cloud, clone them outside the working repository as needed. Local HEADs of GT5/EnderIO/etc may not equal both target-pack releases; use manifest `source_refs` for version-specific conclusions.

## 11. Suggested continuation sequence (not an additional user promise)

1. Recover baseline + WIP and verify build environment. Finish one coherent REBUILD backend slice with real input, source lifetime, tests and matching translations; commit it.
2. Finish REBUILD bulk/directional/save/overlay work in reviewable portions; then remaining tool operations and existing-feature hotkeys/config parity. Resolve native input regressions reported by the user promptly.
3. Complete highest-impact remaining backend groups: Easy Place/restrictions, materials HUD, save/file metadata/conversion, grid/projects as selected by the user. Keep disabled placeholders honest while their dependencies are absent.
4. Return to unresolved GT corruption and remaining transform/context adapters using actual samples and the fixed pack versions. Keep the rotation defect open whenever user reproduction still fails.
5. Maintain translations, documentation and native regression notes with each feature. Finish all remaining upstream parity by a tracked feature-by-feature comparison, not by counting imported strings or screen classes.

## 12. Machine-generated inventories and WIP recovery

The following appendices are generated from the actual handoff workspace. They distinguish missing registrations from incomplete behavior. The embedded patch contains only the interrupted nine-file source WIP, not this document or the six completed commits.

<!-- HANDOFF_GENERATED_APPENDICES -->

### A. Completed commits to make available in the cloud

```text
ebdd3cb719f4d74ec79632d7c19a00064701529b Implement Litematica task manager for saves and world edits
82f82fcbfc5c536bf43888a32ffadced0f76bf23 Implement schematic verifier backend, results UI and task integration
707fba5e63b562ab5f602e9921e1f47a086b4731 Implement verifier marker selection and comparison overlays
7494b0a2489c1ff57e9821965aa53e06aa7e3fb5 Fix UI branding, Forge logo path and pick-block fallback
fd53c9b6bc296e580ab3c2591be3eb799016157a Port MaLiLib hotkeys and align configurable tool input
85b531732ccf3cf1066ef6421d813b0b833dacb8 Implement server-authoritative world move tool with rollback
```

### B. Upstream hotkeys not registered in the WIP tree

30 of 78 original hotkey IDs are absent. Six additional REBUILD IDs are present only as unwired WIP modifiers; the completed baseline is missing those six as well. Defaults below are upstream strings, not new bindings to apply blindly.

```json
[
  {
    "id": "cloneSelection",
    "upstream_default": ""
  },
  {
    "id": "easyPlaceUseKey",
    "upstream_default": "BUTTON_2"
  },
  {
    "id": "easyPlaceFirst",
    "upstream_default": ""
  },
  {
    "id": "easyPlaceToggle",
    "upstream_default": ""
  },
  {
    "id": "invertGhostBlockRenderState",
    "upstream_default": ""
  },
  {
    "id": "invertOverlayRenderState",
    "upstream_default": ""
  },
  {
    "id": "openGuiSchematicProjects",
    "upstream_default": ""
  },
  {
    "id": "pickBlockLast",
    "upstream_default": ""
  },
  {
    "id": "renderOverlayThroughBlocks",
    "upstream_default": "RIGHT_CONTROL"
  },
  {
    "id": "saveAreaAsInMemorySchematic",
    "upstream_default": ""
  },
  {
    "id": "schematicEditReplaceSelection",
    "upstream_default": ""
  },
  {
    "id": "schematicPlacementRotation",
    "upstream_default": ""
  },
  {
    "id": "schematicPlacementMirror",
    "upstream_default": ""
  },
  {
    "id": "schematicVCSDeleteBlockByPlacement",
    "upstream_default": ""
  },
  {
    "id": "schematicVersionCycleModifier",
    "upstream_default": ""
  },
  {
    "id": "schematicVersionCycleNext",
    "upstream_default": ""
  },
  {
    "id": "schematicVersionCyclePrevious",
    "upstream_default": ""
  },
  {
    "id": "toggleAllRendering",
    "upstream_default": "M,R"
  },
  {
    "id": "toggleAreaSelectionBoxesRendering",
    "upstream_default": ""
  },
  {
    "id": "toggleSchematicRendering",
    "upstream_default": "M,G"
  },
  {
    "id": "toggleInfoOverlayRendering",
    "upstream_default": ""
  },
  {
    "id": "toggleOverlayRendering",
    "upstream_default": ""
  },
  {
    "id": "toggleOverlayOutlineRendering",
    "upstream_default": ""
  },
  {
    "id": "toggleOverlaySideRendering",
    "upstream_default": ""
  },
  {
    "id": "togglePlacementBoxesRendering",
    "upstream_default": ""
  },
  {
    "id": "togglePlacementRestriction",
    "upstream_default": ""
  },
  {
    "id": "toggleSchematicBlockRendering",
    "upstream_default": ""
  },
  {
    "id": "toggleSignTextPaste",
    "upstream_default": ""
  },
  {
    "id": "toggleTranslucentRendering",
    "upstream_default": ""
  },
  {
    "id": "toggleVerifierOverlayRendering",
    "upstream_default": ""
  }
]
```

### C. Config parity audit candidates (mechanical, not confirmed missing behavior)

The following upstream Configs.java option names have no literal-name occurrence in current Java source. Aliases, equivalent legacy properties or intentionally unsupported modern features may exist. Conversely a name that occurs in Java may still be disabled/unwired. Audit semantics and defaults before deciding implementation status. Commented-out upstream declarations are excluded.

```json
[
  "debugHudMode",
  "easyPlaceProtocolVersion",
  "pasteNbtRestoreBehavior",
  "pasteReplaceBehavior",
  "pasteLayerBehavior",
  "placementReplaceBehavior",
  "placementRestrictionWarn",
  "schematicVcsDeleteMode",
  "selectionCornersMode",
  "customSchematicBaseDirectoryEnabled",
  "areaSelectionsPerWorld",
  "changeSelectedCornerOnMove",
  "cloneAtOriginalPosition",
  "commandDisableFeedback",
  "commandFillMaxVolume",
  "commandFillNoChunkClamp",
  "commandLimitPerTick",
  "commandNameClone",
  "commandNameFill",
  "commandNameSetblock",
  "commandNameSummon",
  "commandTaskInterval",
  "commandUseWorldEdit",
  "commandUseStrict",
  "debugHudPMThreads",
  "debugHudWorld",
  "debugLogging",
  "deduplicateSchematicEntities",
  "datafixerMode",
  "datafixerDefaultSchema",
  "displayFileOpsFeedback",
  "easyPlaceClickAdjacent",
  "easyPlaceFirst",
  "easyPlaceHoldEnabled",
  "easyPlaceMode",
  "easyPlacePostRewrite",
  "easyPlaceSinglePlayerHandling",
  "easyPlaceSinglePlayerValidation",
  "easyPlaceSwapInterval",
  "easyPlaceSwingHand",
  "easyPlaceVanillaReach",
  "entityDataSync",
  "entityDataSyncBackup",
  "entityDataSyncCacheTimeout",
  "fixChestMirror",
  "fixRailRotation",
  "fixStairsMirror",
  "generateLowercaseNames",
  "itemUsePacketCheckBypass",
  "layerModeFollowsPlayer",
  "loadEntireSchematics",
  "pasteAlwaysUseFill",
  "pasteIgnoreBlockEntitiesEntirely",
  "pasteIgnoreBlockEntitiesFromFill",
  "pasteIgnoreCommandLimitWithNbtRestore",
  "pasteIgnoreEntities",
  "pasteIgnoreInventories",
  "pasteToMcFunctionFiles",
  "pasteUseFillCommand",
  "pasteUsingCommandsInSp",
  "pasteUsingServux",
  "pickBlockAvoidDamageable",
  "pickBlockAvoidTools",
  "pickBlockShulkers",
  "pickBlockableSlots",
  "placementRestriction",
  "placementManagerThreadCount",
  "renderThreadNoTimeout",
  "reverseOperationModeDirection",
  "serverNbtRequestRate",
  "signTextPaste",
  "toolItemComponents",
  "translationLanguage",
  "translationMode",
  "unhideSchematicVCS",
  "enableAreaSelectionBoxesRendering",
  "enablePlacementBoxesRendering",
  "enableRendering",
  "enableSchematicBlocksRendering",
  "enableSchematicFluidRendering",
  "enableSchematicOverlayCulling",
  "enableSchematicRendering",
  "enableSchematicEntityHitboxes",
  "enableSchematicFakeLighting",
  "ignoreExistingFluids",
  "ignoreExistingBlocks",
  "ignorableExistingBlocks",
  "overlayReducedInnerSides",
  "placementBoxSideAlpha",
  "renderAOModernEnable",
  "renderAreaSelectionBoxSides",
  "renderEnableTranslucentResorting",
  "renderCollidingSchematicBlocks",
  "renderFakeLightingLevel",
  "renderPlacementBoxSides",
  "renderPlacementEnclosingBox",
  "renderPlacementEnclosingBoxSides",
  "renderSchematicEntities",
  "renderSchematicTileEntities",
  "renderTranslucentBlockInnerSides",
  "schematicOverlayModelOutline",
  "schematicOverlayModelSides",
  "schematicOverlayRenderThroughBlocks",
  "defaultSelectionMode",
  "toolHudAlignment",
  "blockInfoOverlayEnabled",
  "statusInfoHud",
  "statusInfoHudAuto",
  "toolHudOffsetX",
  "toolHudOffsetY",
  "toolHudScale",
  "warnDisabledRendering"
]
```

### D. GTNH source URL/version/ref registry

One JSON object per manifest entry, copied from compatibility/gtnh-mods.json. `source` is the manifest source/download URL; not every entry is an open GitHub repository. Missing `source_refs` means no acquired revision is recorded, not that source does not exist. Names can change between target packs. Use the pack version and exact ref together; do not substitute current default branches.

```jsonl
{"name":"Advanced Solar Panel For 1.7.10 (Unofficial)","source":"https://www.curseforge.com/minecraft/mc-mods/advancedsolarpanels","2.8.4":"1.7.10 Edition","2.9.0-RC-1":"1.7.10 Edition"}
{"name":"AdventureBackpack2","source":"https://github.com/GTNewHorizons/AdventureBackpack2","2.8.4":"1.3.13-GTNH","2.9.0-RC-1":"1.4.26-GTNH","source_refs":{"1.3.13-GTNH":"9557d1d3665c0c34e788a0bc4fa2fcb7b4fd7c39","1.4.26-GTNH":"a04116dcd2bcd5f07156041252712b0de7fd19d0"}}
{"name":"AE2FluidCraft-Rework","source":"https://github.com/GTNewHorizons/AE2FluidCraft-Rework","2.8.4":"1.4.120-gtnh","2.9.0-RC-1":"1.5.110-gtnh","source_refs":{"1.4.120-gtnh":"64d2987c2d8a9595d0c9d926b60d402fd0a49d7d","1.5.110-gtnh":"6e8dd013bd4ec123cf74daaecf581534d87f2391"}}
{"name":"AE2NoUltimatePatterns","source":"https://github.com/GTNewHorizons/AE2NoUltimatePatterns","2.8.4":"1.0.1"}
{"name":"ae2stuff","source":"https://github.com/GTNewHorizons/ae2stuff","2.8.4":"0.9.9-GTNH","source_refs":{"0.9.9-GTNH":"1d6b95cf014ede7491bc5a5b3c649fde467be418"}}
{"name":"AFSU","source":"https://github.com/GTNewHorizons/AFSU","2.8.4":"1.3.1-GTNH","2.9.0-RC-1":"1.3.2-GTNH","source_refs":{"1.3.1-GTNH":"24bdb63e27542b3dd33c0a746614f083280d0fad","1.3.2-GTNH":"8c896b8a0c32c4b2419d5ca55e8a9008ee1ad3de"}}
{"name":"AkashicTome","source":"https://github.com/GTNewHorizons/AkashicTome","2.8.4":"1.2.6","2.9.0-RC-1":"1.2.7"}
{"name":"AlchemyGrate","source":"https://github.com/GTNewHorizons/AlchemyGrate","2.8.4":"1.3.1-GTNH","2.9.0-RC-1":"1.3.2-GTNH","source_refs":{"1.3.1-GTNH":"1312c4208f89126d38a8d611128e9454cebac0ed","1.3.2-GTNH":"ae0d12aeea70247038ac6fd3781bb45cd71595ba"}}
{"name":"Amazing-Trophies","source":"https://github.com/GTNewHorizons/Amazing-Trophies","2.8.4":"1.3.8","2.9.0-RC-1":"1.4.10","source_refs":{"1.3.8":"a9deb8704f84fd6d3fb94fcd36dc9b8f36a219ae","1.4.10":"7dc34cd53274fb7d225df22181e21d170c071d50"}}
{"name":"amunra","source":"https://github.com/GTNewHorizons/amunra","2.8.4":"0.8.2","2.9.0-RC-1":"0.8.14","source_refs":{"0.8.2":"7ee2411bfa2abcbe8a7bf3d4053c1d012b86bc8d","0.8.14":"dc3ff7f8d7bbfe1f39cc53324ea78e5dbc221bba"}}
{"name":"Angelica","source":"https://github.com/GTNewHorizons/Angelica","2.8.4":"1.0.0-beta66b","2.9.0-RC-1":"2.2.19","source_refs":{"1.0.0-beta66b":"7059ee0eecbee9cc66a3b9a301bf42d7242669b3","2.2.19":"53776684c8dd8cf1c915f24023f94fdc1b88ac56"}}
{"name":"AngerMod","source":"https://github.com/GTNewHorizons/AngerMod","2.8.4":"0.9.0","2.9.0-RC-1":"1.0.7"}
{"name":"AppleCore","source":"https://github.com/GTNewHorizons/AppleCore","2.8.4":"3.3.7","2.9.0-RC-1":"3.3.13"}
{"name":"Applied-Energistics-2-Unofficial","source":"https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial","2.8.4":"rv3-beta-695-GTNH","2.9.0-RC-1":"rv3-beta-1073-GTNH","source_refs":{"rv3-beta-1073-GTNH":"151550f6d558a663eee792f0bfa22e12a53c0e54","rv3-beta-695-GTNH":"9e7cf61f368a42a806d70ffb21308bf26a818659"}}
{"name":"Archaicfix","source":"https://github.com/embeddedt/ArchaicFix","2.8.4":"0.7.7","2.9.0-RC-1":"0.8.0"}
{"name":"ArchitectureCraft","source":"https://github.com/GTNewHorizons/ArchitectureCraft","2.8.4":"1.11.6","2.9.0-RC-1":"1.12.17","source_refs":{"1.11.6":"33c345b82b065ebe4c8ffadcd9505872e55ad994","1.12.17":"d8f9d93c0690aa15a0a149110380ff15d61fa72b"}}
{"name":"AsieLib","source":"https://github.com/GTNewHorizons/AsieLib","2.8.4":"0.7.0","2.9.0-RC-1":"0.7.2"}
{"name":"Automagy","source":"https://www.curseforge.com/minecraft/mc-mods/automagy","2.8.4":"0.28.2"}
{"name":"Avaritia","source":"https://github.com/GTNewHorizons/Avaritia","2.8.4":"1.77","2.9.0-RC-1":"1.99","source_refs":{"1.77":"24ed442f275bc5d356a79d5a378e8fa305681003","1.99":"f02c7bfe69b4eff4c0187b4808d5cc581f22c1e9"}}
{"name":"Avaritiaddons","source":"https://github.com/GTNewHorizons/Avaritiaddons","2.8.4":"1.9.3-GTNH","2.9.0-RC-1":"1.9.6-GTNH","source_refs":{"1.9.3-GTNH":"5d7dd38fb122aaad3f3c51e3aa7c05a6d37665e0","1.9.6-GTNH":"530be59e89c5e90a430c1df527899a88f67ff3ed"}}
{"name":"Backhand","source":"https://github.com/GTNewHorizons/Backhand","2.8.4":"1.7.7","2.9.0-RC-1":"1.8.15"}
{"name":"Battlegear2-for-Backhand","source":"https://github.com/GTNewHorizons/Battlegear2-for-Backhand","2.8.4":"1.5.9-backhand","2.9.0-RC-1":"1.6.8-backhand"}
{"name":"Baubles-Expanded","source":"https://github.com/GTNewHorizons/Baubles-Expanded","2.8.4":"2.1.19-GTNH","2.9.0-RC-1":"2.2.24-GTNH"}
{"name":"bdlib","source":"https://github.com/GTNewHorizons/bdlib","2.8.4":"1.11.0-GTNH","2.9.0-RC-1":"1.11.0-GTNH","source_refs":{"1.11.0-GTNH":"870d288f830396af6f7d507e76d96972efd00f86"}}
{"name":"BeeBetterAtBees-GTNH","source":"https://github.com/GTNewHorizons/BeeBetterAtBees-GTNH","2.8.4":"0.4.3-GTNH","2.9.0-RC-1":"0.4.7-GTNH"}
{"name":"BetterAchievements","source":"https://github.com/GTNewHorizons/BetterAchievements","2.8.4":"0.3.0","2.9.0-RC-1":"0.3.3"}
{"name":"BetterBuildersWands","source":"https://github.com/GTNewHorizons/BetterBuildersWands","2.8.4":"0.13.3-GTNH","2.9.0-RC-1":"0.13.12-GTNH"}
{"name":"BetterCrashes","source":"https://github.com/GTNewHorizons/BetterCrashes","2.8.4":"1.4.0-GTNH","2.9.0-RC-1":"1.4.7-GTNH"}
{"name":"BetterLoadingScreen","source":"https://github.com/GTNewHorizons/BetterLoadingScreen","2.8.4":"1.7.2-GTNH","2.9.0-RC-1":"1.7.18-GTNH"}
{"name":"BetterP2P","source":"https://github.com/GTNewHorizons/BetterP2P","2.8.4":"1.3.3","2.9.0-RC-1":"1.4.7"}
{"name":"BetterQuesting","source":"https://github.com/GTNewHorizons/BetterQuesting","2.8.4":"3.7.15-GTNH","2.9.0-RC-1":"3.8.87-GTNH"}
{"name":"BiblioCraft: BiblioWoods Biomes O'Plenty Edition","source":"https://www.curseforge.com/minecraft/mc-mods/bibliocraft-bibliowoods-biomes-oplenty-edition","2.8.4":"1.9","2.9.0-RC-1":"1.9"}
{"name":"BiblioCraft: BiblioWoods Forestry Edition","source":"https://www.curseforge.com/minecraft/mc-mods/bibliocraft-bibliowoods-forestry-edition","2.8.4":"1.7","2.9.0-RC-1":"1.7"}
{"name":"BiblioCraft: BiblioWoods Natura Edition","source":"https://www.curseforge.com/minecraft/mc-mods/bibliocraft-bibliowoods-natura-edition","2.8.4":"1.5","2.9.0-RC-1":"1.5"}
{"name":"BiblioCraft","source":"https://www.curseforge.com/minecraft/mc-mods/bibliocraft","2.8.4":"1.11.7","2.9.0-RC-1":"1.11.7"}
{"name":"Binnie","source":"https://github.com/GTNewHorizons/Binnie","2.8.4":"2.5.24","2.9.0-RC-1":"2.6.48","source_refs":{"2.5.24":"5135c0c3c9f19db8f87f5ad14231177115da4fc3","2.6.48":"eb36ee92607754a385e24638b64ad18723c21d23"}}
{"name":"Biomes O' Plenty","source":"https://www.curseforge.com/minecraft/mc-mods/biomes-o-plenty","2.8.4":"2.1.0.2308","2.9.0-RC-1":"2.1.0.2308"}
{"name":"BlockLimiter","source":"https://github.com/GTNewHorizons/BlockLimiter","2.8.4":"0.7.0","2.9.0-RC-1":"0.7.0"}
{"name":"BlockRenderer6343","source":"https://github.com/GTNewHorizons/BlockRenderer6343","2.8.4":"1.3.17","2.9.0-RC-1":"1.4.23","source_refs":{"1.3.17":"000d4ab83463ccaae7a398790127bd3367810561","1.4.23":"732916247ba85b92874dc3aa7bf76aa26578a9e8"}}
{"name":"BloodArsenal","source":"https://github.com/GTNewHorizons/BloodArsenal","2.8.4":"1.4.10","2.9.0-RC-1":"1.5.12","source_refs":{"1.4.10":"f1c5c9fa285c59b58d5bb51064a68cc6e8b85375","1.5.12":"890824be106a655972820f48f852edfbbb8ec23b"}}
{"name":"BloodMagic","source":"https://github.com/GTNewHorizons/BloodMagic","2.8.4":"1.7.52","2.9.0-RC-1":"1.9.13","source_refs":{"1.7.52":"7c2046c0c111b24bee7cdbfa21288aff91e5c5d5","1.9.13":"6776676ef209d8c142a1a159519c40d06c7e0bc7"}}
{"name":"Botania","source":"https://github.com/GTNewHorizons/Botania","2.8.4":"1.12.28-GTNH","2.9.0-RC-1":"1.13.36-GTNH","source_refs":{"1.12.28-GTNH":"ac1864b8263b72cc3a7483285e78c03ba04907e4","1.13.36-GTNH":"c802a448e4f4b3cbe4bc26d749a164a3fb178325"}}
{"name":"Botanic-horizons","source":"https://github.com/GTNewHorizons/Botanic-horizons","2.8.4":"1.11.27-GTNH","2.9.0-RC-1":"1.12.10-GTNH","source_refs":{"1.11.27-GTNH":"b767822ca5a991aac5ff3b2f70cec3ce006d8206","1.12.10-GTNH":"45f458e0c27b15622685e06aa4503d1c4d9f82bd"}}
{"name":"BrandonsCore","source":"https://github.com/GTNewHorizons/BrandonsCore","2.8.4":"1.2.0-GTNH","source_refs":{"1.2.0-GTNH":"65b42b1a6b5e24f245aa2216a4194effbcc6a713"}}
{"name":"Bug-Torch","source":"https://github.com/jss2a98aj/BugTorch","2.8.4":"1.2.14","2.9.0-RC-1":"1.3.1"}
{"name":"BuildCraft","source":"https://github.com/GTNewHorizons/BuildCraft","2.8.4":"7.1.44","2.9.0-RC-1":"7.1.63","source_refs":{"7.1.44":"2d30f3648ffd748c2f4dbf133ff973885eda92e8","7.1.63":"213a3fcb93c88310dfa879b643b23f78f059c999"}}
{"name":"BuildCraftCompat","source":"https://github.com/GTNewHorizons/BuildCraftCompat","2.8.4":"7.1.18","2.9.0-RC-1":"7.1.22"}
{"name":"BuildCraftOilTweak","source":"https://github.com/GTNewHorizons/BuildCraftOilTweak","2.8.4":"1.1.1","2.9.0-RC-1":"1.1.3"}
{"name":"CarpentersBlocks","source":"https://github.com/GTNewHorizons/CarpentersBlocks","2.8.4":"3.7.0-GTNH","2.9.0-RC-1":"3.7.3-GTNH","source_refs":{"3.7.0-GTNH":"a2f7a2c8c94d0431eadd2e244137d9ea36dca013","3.7.3-GTNH":"0ef22db308928e53fb67bccb5647af7d5e744556"}}
{"name":"Catwalks-2","source":"https://github.com/GTNewHorizons/Catwalks-2","2.8.4":"2.4.0-GTNH","2.9.0-RC-1":"2.4.2-GTNH","source_access":"Public source URL unavailable on 2026-09-30; not source-verified."}
{"name":"Chisel","source":"https://github.com/GTNewHorizons/Chisel","2.8.4":"2.16.15-GTNH","2.9.0-RC-1":"2.17.33-GTNH","source_refs":{"2.16.15-GTNH":"1a044729224cdee63ac05d63f2acc7cba809bdf8","2.17.33-GTNH":"3d3260cd71b8d0d4282ad17eb04c60ef401ee89a"}}
{"name":"ChiselTones","source":"https://github.com/GTNewHorizons/ChiselTones","2.8.4":"1.2.0-GTNH","2.9.0-RC-1":"1.2.0-GTNH"}
{"name":"CodeChickenCore","source":"https://github.com/GTNewHorizons/CodeChickenCore","2.8.4":"1.4.10","2.9.0-RC-1":"1.4.22","source_refs":{"1.4.10":"63f36ca3838a148fc69f6ee88fe9bf6d579bb409","1.4.22":"a9643dba1cfa0ab8f19c299096f79e377d672d7d"}}
{"name":"CoFH Core","source":"https://www.curseforge.com/minecraft/mc-mods/cofh-core","2.8.4":"3.1.4-329","2.9.0-RC-1":"3.1.4-329"}
{"name":"Compact Kinetic Generators","source":"https://forum.industrial-craft.net/thread/12724-ic2-exp-1-7-10-compact-kinetic-generators/","2.8.4":"1.0","2.9.0-RC-1":"1.0"}
{"name":"Computronics","source":"https://github.com/GTNewHorizons/Computronics","2.8.4":"1.9.3-GTNH","2.9.0-RC-1":"1.9.10-GTNH","source_refs":{"1.9.3-GTNH":"ae0cc9ff8afcbd00412f340d817b7ce9dd398edb","1.9.10-GTNH":"7e94219667f95354e9f721c0b4532173681a70e9"}}
{"name":"Controlling","source":"https://github.com/GTNewHorizons/Controlling","2.8.4":"2.1.2","2.9.0-RC-1":"2.1.10"}
{"name":"CookingForBlockheads","source":"https://github.com/GTNewHorizons/CookingForBlockheads","2.8.4":"1.4.4-GTNH","2.9.0-RC-1":"1.4.14-GTNH","source_refs":{"1.4.4-GTNH":"ed8f4639a3c66e4333e0659672391b051b8a524e","1.4.14-GTNH":"720e45c368dccbb32599fe3fe4f803c69ed46cd4"}}
{"name":"CoreTweaks","source":"https://github.com/GTNewHorizons/CoreTweaks","2.8.4":"0.3.3.6-GTNH","2.9.0-RC-1":"0.3.4.7-GTNH"}
{"name":"Craft-Presence","source":"https://www.curseforge.com/minecraft/mc-mods/craftpresence/","2.8.4":"2.6.2","2.9.0-RC-1":"2.7.1"}
{"name":"CraftTweaker","source":"https://github.com/GTNewHorizons/CraftTweaker","2.8.4":"3.4.2","2.9.0-RC-1":"3.4.8"}
{"name":"CropLoadCore","source":"https://github.com/GTNewHorizons/CropLoadCore","2.8.4":"0.2.0"}
{"name":"Crops-plus-plus","source":"https://github.com/GTNewHorizons/Crops-plus-plus","2.8.4":"1.8.11","source_refs":{"1.8.11":"e195d2bb427e41664300513dace7a467103807a0"}}
{"name":"Custom-Main-Menu","source":"https://github.com/GTNewHorizons/Custom-Main-Menu","2.8.4":"1.12.2","2.9.0-RC-1":"1.14.1"}
{"name":"Darkerer","source":"https://github.com/GTNewHorizons/Darkerer","2.8.4":"1.0.6","2.9.0-RC-1":"1.1.0"}
{"name":"Default-Configs","source":"https://github.com/GTNewHorizons/Default-Configs","2.8.4":"1.3.0","2.9.0-RC-1":"1.3.2"}
{"name":"DefaultServerList","source":"https://github.com/GTNewHorizons/DefaultServerList","2.8.4":"1.6.5","2.9.0-RC-1":"1.7.4"}
{"name":"DefaultWorldGenerator","source":"https://github.com/GTNewHorizons/DefaultWorldGenerator","2.8.4":"0.4.0","2.9.0-RC-1":"0.4.0"}
{"name":"Draconic-Evolution","source":"https://github.com/GTNewHorizons/Draconic-Evolution","2.8.4":"1.4.27-GTNH","2.9.0-RC-1":"1.5.33-GTNH","source_refs":{"1.4.27-GTNH":"31050d93ae039126a493882b40f99bf578b8a2de","1.5.33-GTNH":"95453b430e9a2a5d737fa5c7913500c83ae53fac"}}
{"name":"DummyCore","source":"https://github.com/GTNewHorizons/DummyCore","2.8.4":"1.20.0","2.9.0-RC-1":"1.21.0"}
{"name":"DuraDisplay","source":"https://github.com/GTNewHorizons/DuraDisplay","2.8.4":"1.3.4","2.9.0-RC-1":"1.4.6"}
{"name":"Electro-Magic-Tools","source":"https://github.com/GTNewHorizons/Electro-Magic-Tools","2.8.4":"1.6.18","2.9.0-RC-1":"1.7.25","source_refs":{"1.6.18":"6b3eaa9474279b12ed79842aa8d212facf51366c","1.7.25":"9ac1008c31595233a72fa8a73c5b0732844fa44b"}}
{"name":"EnderCore","source":"https://github.com/GTNewHorizons/EnderCore","2.8.4":"0.4.8","2.9.0-RC-1":"0.5.15"}
{"name":"EnderIO","source":"https://github.com/GTNewHorizons/EnderIO","2.8.4":"2.9.28","2.9.0-RC-1":"2.10.46","source_refs":{"2.10.46":"a8197e0026c29bfaca35b239729b3d957acca7c0","2.9.28":"09001f524c167c0d1e059d56308a00900cef3383"}}
{"name":"EnderStorage","source":"https://github.com/GTNewHorizons/EnderStorage","2.8.4":"1.7.7","2.9.0-RC-1":"1.8.7","source_refs":{"1.7.7":"8b8b118550c38f66808b743f098a9d289a5f17a6","1.8.7":"b77b19cfd94e68bd85585af0750c94a3c7f16351"}}
{"name":"EnderZoo","source":"https://github.com/GTNewHorizons/EnderZoo","2.8.4":"1.3.3","2.9.0-RC-1":"1.3.6"}
{"name":"EnhancedLootBags","source":"https://github.com/GTNewHorizons/EnhancedLootBags","2.8.4":"1.2.8","2.9.0-RC-1":"1.3.5"}
{"name":"Et-Futurum-Requiem","source":"https://github.com/GTNewHorizons/Et-Futurum-Requiem","2.8.4":"2.6.2.25-GTNH","2.9.0-RC-1":"2.6.59-GTNH","source_refs":{"2.6.2.25-GTNH":"0332aa2d171d7700d5a0958739749b00ab2c7823","2.6.59-GTNH":"65407a439b57fba00e9a0b7d9f5abc564b53743c"}}
{"name":"Eternal-Singularity","source":"https://github.com/GTNewHorizons/Eternal-Singularity","2.8.4":"1.2.3","2.9.0-RC-1":"1.4.3"}
{"name":"Extra Utilities","source":"https://www.curseforge.com/minecraft/mc-mods/extra-utilities","2.8.4":"1.2.12","2.9.0-RC-1":"1.2.12"}
{"name":"FindIt","source":"https://github.com/GTNewHorizons/FindIt","2.8.4":"1.4.0","2.9.0-RC-1":"1.4.6"}
{"name":"FloodLights","source":"https://github.com/GTNewHorizons/FloodLights","2.8.4":"1.5.4","2.9.0-RC-1":"1.5.6","source_refs":{"1.5.4":"d61d862c85f8d37ebd62ffc6c2b8775e36790b24","1.5.6":"7b8a805df5b2014869a59c3fbbc90b873d7b6a15"}}
{"name":"ForbiddenMagic","source":"https://github.com/GTNewHorizons/ForbiddenMagic","2.8.4":"0.8.4-GTNH","2.9.0-RC-1":"0.9.17-GTNH","source_refs":{"0.8.4-GTNH":"13888802acc0ecacab1ae7d4ca3887919f03d3ab","0.9.17-GTNH":"f03f14284fa9295c9d6e691503dfb778d7995e57"}}
{"name":"ForestryMC","source":"https://github.com/GTNewHorizons/ForestryMC","2.8.4":"4.10.17","2.9.0-RC-1":"4.11.38","source_refs":{"4.10.17":"bf94b22345b6f016772f212a2c8b7f7e10f81c87","4.11.38":"0bd9a7978a3852c3a815e41a16e678176344aa3d"}}
{"name":"Forgelin","source":"https://github.com/GTNewHorizons/Forgelin","2.8.4":"2.0.3-GTNH","2.9.0-RC-1":"2.0.3-GTNH"}
{"name":"ForgeMultipart","source":"https://github.com/GTNewHorizons/ForgeMultipart","2.8.4":"1.7.2","2.9.0-RC-1":"1.7.15","source_refs":{"1.7.15":"780825446525f0a45599e1823e9e80dc45c83177","1.7.2":"a36120fc91ae6de017402df6ccf162ef819f76d1"}}
{"name":"ForgeRelocation","source":"https://github.com/GTNewHorizons/ForgeRelocation","2.8.4":"0.3.4","2.9.0-RC-1":"0.3.7"}
{"name":"ForgeRelocationFMP","source":"https://github.com/GTNewHorizons/ForgeRelocationFMP","2.8.4":"0.2.0","2.9.0-RC-1":"0.2.0"}
{"name":"Gadomancy","source":"https://github.com/GTNewHorizons/Gadomancy","2.8.4":"1.4.8","2.9.0-RC-1":"1.5.16","source_refs":{"1.4.8":"3bd7b600e798f150e89ba276a53b122bd78c87a5","1.5.16":"56d29f156a9ac02d61b2c71390bd0ce4a0e2cd8e"}}
{"name":"Galacticraft","source":"https://github.com/GTNewHorizons/Galacticraft","2.8.4":"3.3.13-GTNH","2.9.0-RC-1":"3.4.34-GTNH","source_refs":{"3.3.13-GTNH":"31ae0924615e08fee8c7cb6db28212ca828488ea","3.4.34-GTNH":"fbdc41b97e8178ac07e58ccaa93da2474b1e2664"}}
{"name":"Galaxy-Space-GTNH","source":"https://github.com/GTNewHorizons/Galaxy-Space-GTNH","2.8.4":"1.1.121-GTNH","2.9.0-RC-1":"1.1.142-GTNH","source_access":"Public source URL unavailable on 2026-09-30; not source-verified."}
{"name":"gendustry","source":"https://github.com/GTNewHorizons/gendustry","2.8.4":"1.9.4-GTNH","2.9.0-RC-1":"1.9.13-GTNH","source_refs":{"1.9.4-GTNH":"ab03c46d13f98e6283d65e982223b2c2604a0fd2","1.9.13-GTNH":"e4af5e5cacd2088c8cad6af42f5862069ce13546"}}
{"name":"Gravitation-Suite-Neo","source":"https://github.com/GTNewHorizons/Gravitation-Suite-Neo","2.8.4":"1.3.6","2.9.0-RC-1":"1.3.16"}
{"name":"Gravitation-Suite-old","source":"https://forum.industrial-craft.net/thread/6915-ic2-exp-1-7-10-gravitation-suite-v2-0-3/","2.8.4":"2.0.3","2.9.0-RC-1":"2.0.3"}
{"name":"GT5-Unofficial","source":"https://github.com/GTNewHorizons/GT5-Unofficial","2.8.4":"5.09.51.482","2.9.0-RC-1":"5.09.54.183","source_refs":{"5.09.51.482":"e48304dd8f49be6df8e395c835780002bfb4e898","5.09.54.183":"f8453bce28793713d314f6dcfa5ac551d700efd6"}}
{"name":"GTNH-TC-Wands","source":"https://github.com/GTNewHorizons/GTNH-TC-Wands","2.8.4":"1.4.6","2.9.0-RC-1":"1.4.14"}
{"name":"GTNHLib","source":"https://github.com/GTNewHorizons/GTNHLib","2.8.4":"0.7.10","2.9.0-RC-1":"0.11.51"}
{"name":"Hardcore-Ender-Expansion","source":"https://github.com/GTNewHorizons/Hardcore-Ender-Expansion","2.8.4":"1.12.16-GTNH","2.9.0-RC-1":"1.12.28-GTNH","source_refs":{"1.12.16-GTNH":"7e83dab82a1804ce9fb80669737d7f379c3b6ad6","1.12.28-GTNH":"abfc9640f622389a81ca6e1c6b6acce928c0f44b"}}
{"name":"harvestcraft","source":"https://github.com/GTNewHorizons/harvestcraft","2.8.4":"1.3.2-GTNH","2.9.0-RC-1":"1.3.15-GTNH","source_access":"Exact public source unavailable on 2026-09-30; not source-verified."}
{"name":"Healer","source":"https://www.curseforge.com/minecraft/mc-mods/healer","2.8.4":"1.2.1","2.9.0-RC-1":"1.2.1"}
{"name":"HelpFixer","source":"https://github.com/GTNewHorizons/HelpFixer","2.8.4":"1.3.0","2.9.0-RC-1":"1.3.2"}
{"name":"Hodgepodge","source":"https://github.com/GTNewHorizons/Hodgepodge","2.8.4":"2.6.112","2.9.0-RC-1":"2.7.211"}
{"name":"HoloInventory","source":"https://github.com/GTNewHorizons/HoloInventory","2.8.4":"2.5.5-GTNH","2.9.0-RC-1":"2.5.17-GTNH"}
{"name":"Hunger-Overhaul","source":"https://www.curseforge.com/minecraft/mc-mods/hunger-overhaul","2.8.4":"1.0.0.104","2.9.0-RC-1":"1.0.0.104"}
{"name":"HydroEnergy","source":"https://github.com/GTNewHorizons/HydroEnergy","2.8.4":"1.4.10","2.9.0-RC-1":"1.4.25","source_refs":{"1.4.10":"8bec0e7c650843a16ad8a7b8ffdbe6ce551cd3fb","1.4.25":"21f2095b47c9019fb1d989426fa2bb3eb6ff5ed3"}}
{"name":"IC2 Crop-Breeding Plugin","source":"https://www.curseforge.com/minecraft/mc-mods/ic2-nei-crop-plugin","2.8.4":"1.3.1"}
{"name":"IFU","source":"https://github.com/GTNewHorizons/IFU","2.8.4":"1.11.2","2.9.0-RC-1":"1.12.4","source_refs":{"1.11.2":"d315bf904ee1ed342c896733c779d6627b93d654","1.12.4":"be0be08cdde7071ae0d96cf9acf60d176826a69d"}}
{"name":"IguanaTweaksTConstruct","source":"https://github.com/GTNewHorizons/IguanaTweaksTConstruct","2.8.4":"2.6.6","2.9.0-RC-1":"2.7.12"}
{"name":"Industrial Craft 2","source":"https://www.curseforge.com/minecraft/mc-mods/industrial-craft","2.8.4":"2.2.2.828","2.9.0-RC-1":"2.2.2.828"}
{"name":"Infernal-Mobs","source":"https://github.com/GTNewHorizons/Infernal-Mobs","2.8.4":"1.10.3-GTNH","2.9.0-RC-1":"1.10.7-GTNH"}
{"name":"InGame-Info-XML","source":"https://github.com/GTNewHorizons/InGame-Info-XML","2.8.4":"2.8.30","2.9.0-RC-1":"2.9.7"}
{"name":"InventoryBogoSorter","source":"https://github.com/GTNewHorizons/InventoryBogoSorter","2.8.4":"1.2.68-GTNH","2.9.0-RC-1":"1.3.54-GTNH"}
{"name":"ironchest","source":"https://github.com/GTNewHorizons/ironchest","2.8.4":"6.1.6","2.9.0-RC-1":"6.1.13","source_refs":{"6.1.6":"cdbe7ce4774dcb77b642724c6546900fde591873","6.1.13":"7681237c5c3739558404b7e9e13f670575cee599"}}
{"name":"IronChestMinecarts","source":"https://github.com/GTNewHorizons/IronChestMinecarts","2.8.4":"1.2.0","2.9.0-RC-1":"1.2.1"}
{"name":"IronTankMinecarts","source":"https://github.com/GTNewHorizons/IronTankMinecarts","2.8.4":"1.0.12","2.9.0-RC-1":"1.0.12"}
{"name":"Irontanks","source":"https://github.com/GTNewHorizons/Irontanks","2.8.4":"1.4.2","2.9.0-RC-1":"1.4.7","source_refs":{"1.4.2":"1b54bf98253c09de2eb0ddad54666bdcb119ef40","1.4.7":"f3132d21f44ca6aa53ad2dd83a1e9edfb84d74cb"}}
{"name":"Jabba","source":"https://github.com/GTNewHorizons/Jabba","2.8.4":"1.5.10","2.9.0-RC-1":"1.5.23","source_refs":{"1.5.10":"9bcbe98f0f219a50b03b767305ec701996b8839e","1.5.23":"b04a103eac175e7c93f26c67ff486251239cb93e"}}
{"name":"JourneyMap Server","source":"https://www.curseforge.com/minecraft/mc-mods/journeymap-server","2.8.4":"1.0.5","2.9.0-RC-1":"1.0.5"}
{"name":"JourneyMap","source":"https://www.curseforge.com/minecraft/mc-mods/journeymap","2.8.4":"5.2.10-fairplay","2.9.0-RC-1":"5.2.23-fairplay"}
{"name":"LittleTiles","source":"https://github.com/GTNewHorizons/LittleTiles","2.8.4":"1.5.14-GTNH","2.9.0-RC-1":"1.6.53","source_refs":{"1.5.14-GTNH":"f0a42138331ef8f5484d239704b7d21c27f9d77e","1.6.53":"8cfc14b732472602ea290cd132895434f557a866"}}
{"name":"LogisticsPipes","source":"https://github.com/GTNewHorizons/LogisticsPipes","2.8.4":"1.4.24-GTNH","2.9.0-RC-1":"1.5.36-GTNH","source_refs":{"1.4.24-GTNH":"de7e137ef5ac8c863d2e1b40004707987def7833","1.5.36-GTNH":"df8e3fb02ca098601052a397cac2b1cba2bbd3d9"}}
{"name":"LootGames","source":"https://github.com/GTNewHorizons/LootGames","2.8.4":"2.2.0.1","2.9.0-RC-1":"2.2.14","source_refs":{"2.2.0.1":"76792688313c6eec1f1ef91fa22c031b2ea57f49","2.2.14":"960cfc7760b3b65bb13727ea972ec3ccdf5e0f92"}}
{"name":"LunatriusCore","source":"https://github.com/GTNewHorizons/LunatriusCore","2.8.4":"1.2.1-GTNH","2.9.0-RC-1":"1.2.1-GTNH"}
{"name":"lwjgl3ify","source":"https://github.com/GTNewHorizons/lwjgl3ify","2.8.4":"2.1.16","2.9.0-RC-1":"3.0.35"}
{"name":"MagicBees","source":"https://github.com/GTNewHorizons/MagicBees","2.8.4":"2.9.4-GTNH","2.9.0-RC-1":"2.10.12-GTNH"}
{"name":"MalisisDoors","source":"https://github.com/GTNewHorizons/MalisisDoors","2.8.4":"1.18.2-GTNH","2.9.0-RC-1":"1.19.11-GTNH","source_refs":{"1.18.2-GTNH":"87f8481cd12dc353d6214a559d0fb70d008185d3","1.19.11-GTNH":"b0d85584bdd457da0d58fcd565b64b6086627f97"}}
{"name":"Mantle","source":"https://github.com/GTNewHorizons/Mantle","2.8.4":"0.5.1","2.9.0-RC-1":"0.5.4"}
{"name":"MatterManipulator","source":"https://github.com/GTNewHorizons/MatterManipulator","2.8.4":"0.0.51-GTNH","2.9.0-RC-1":"0.1.59-GTNH","source_refs":{"0.0.51-GTNH":"7dd0d50533d4dc6d1b66a2ef0287a64f1679eaae","0.1.59-GTNH":"e1cac40d2b19ca8569bb8f3838253b1fc7ac0ff4"}}
{"name":"Minecraft-Backpack-Mod","source":"https://github.com/GTNewHorizons/Minecraft-Backpack-Mod","2.8.4":"2.5.10-GTNH","2.9.0-RC-1":"2.6.17-GTNH"}
{"name":"Minetweaker-Gregtech-5-Addon","source":"https://github.com/GTNewHorizons/Minetweaker-Gregtech-5-Addon","2.8.4":"2.3.1","2.9.0-RC-1":"2.3.4"}
{"name":"Mobs-Info","source":"https://github.com/GTNewHorizons/Mobs-Info","2.8.4":"0.5.6-GTNH"}
{"name":"ModernMarkings","source":"https://github.com/GTNewHorizons/ModernMarkings","2.8.4":"0.3.12-1.7.10","2.9.0-RC-1":"0.3.13-1.7.10"}
{"name":"ModTweaker","source":"https://github.com/GTNewHorizons/ModTweaker","2.8.4":"0.12.0","2.9.0-RC-1":"0.14.0"}
{"name":"ModularUI2","source":"https://github.com/GTNewHorizons/ModularUI2","2.8.4":"2.2.18-1.7.10","2.9.0-RC-1":"2.3.91-1.7.10"}
{"name":"ModularUI","source":"https://github.com/GTNewHorizons/ModularUI","2.8.4":"1.2.20","2.9.0-RC-1":"1.3.4"}
{"name":"Morpheus","source":"https://www.curseforge.com/minecraft/mc-mods/morpheus","2.8.4":"1.6.21"}
{"name":"MrTJPCore","source":"https://github.com/GTNewHorizons/MrTJPCore","2.8.4":"1.3.4","2.9.0-RC-1":"1.3.7"}
{"name":"Natura","source":"https://github.com/GTNewHorizons/Natura","2.8.4":"2.8.9","2.9.0-RC-1":"2.8.24","source_refs":{"2.8.9":"120ed3e848b14ad524ccc5dedd6b37849a4704d9","2.8.24":"4f88c066592894c9b896c89a442c4305723f587d"}}
{"name":"NaturesCompass","source":"https://github.com/GTNewHorizons/NaturesCompass","2.8.4":"1.5.0-GTNH","2.9.0-RC-1":"1.5.2-GTNH"}
{"name":"Navigator","source":"https://github.com/GTNewHorizons/Navigator","2.8.4":"1.0.17","2.9.0-RC-1":"1.1.10"}
{"name":"nei-custom-diagram","source":"https://github.com/GTNewHorizons/nei-custom-diagram","2.8.4":"1.7.5","2.9.0-RC-1":"1.8.35"}
{"name":"NEI-Integration","source":"https://github.com/GTNewHorizons/NEI-Integration","2.8.4":"1.5.0","2.9.0-RC-1":"1.5.3"}
{"name":"neiaddons","source":"https://github.com/GTNewHorizons/neiaddons","2.8.4":"1.17.0","2.9.0-RC-1":"1.18.6"}
{"name":"NetherPortalFix","source":"https://github.com/GTNewHorizons/NetherPortalFix","2.8.4":"1.4.0","2.9.0-RC-1":"1.4.0"}
{"name":"NewHorizonsCoreMod","source":"https://github.com/GTNewHorizons/NewHorizonsCoreMod","2.8.4":"2.7.268","2.9.0-RC-1":"2.9.76","source_refs":{"2.7.268":"44f836e6dd19a9c31619830abbf3ba46df987825","2.9.76":"63dfdedc938af2f1281563bde0363bd8d6e755b6"}}
{"name":"Nodal-Mechanics","source":"https://github.com/GTNewHorizons/Nodal-Mechanics","2.8.4":"1.3.1-GTNH","2.9.0-RC-1":"1.3.4-GTNH","source_refs":{"1.3.1-GTNH":"ec63a19f4879f5e043515cad4d4d4c9b5ac00b50","1.3.4-GTNH":"db058cff7cc885c542e6aa910bd7ac8e886cd066"}}
{"name":"NotEnoughEnergistics","source":"https://github.com/GTNewHorizons/NotEnoughEnergistics","2.8.4":"1.7.14","2.9.0-RC-1":"1.7.45"}
{"name":"NotEnoughIds","source":"https://github.com/GTNewHorizons/NotEnoughIds","2.8.4":"2.1.10"}
{"name":"NotEnoughItems","source":"https://github.com/GTNewHorizons/NotEnoughItems","2.8.4":"2.8.44-GTNH","2.9.0-RC-1":"2.8.145-GTNH"}
{"name":"Nuclear-Control","source":"https://github.com/GTNewHorizons/Nuclear-Control","2.8.4":"2.6.20","2.9.0-RC-1":"2.7.14","source_refs":{"2.6.20":"3aa30bb8cd139f7bc424806c19b3a5de802fd034","2.7.14":"9d702bc39704ee0e3d8bc48a6099c0acb92ee389"}}
{"name":"Nutrition","source":"https://github.com/GTNewHorizons/Nutrition","2.8.4":"0.1.3","2.9.0-RC-1":"1.0.1"}
{"name":"oauth","source":"https://github.com/GTNewHorizons/oauth","2.8.4":"1.3.3-GTNH","2.9.0-RC-1":"1.3.4-GTNH"}
{"name":"OCGlasses","source":"https://github.com/GTNewHorizons/OCGlasses","2.8.4":"1.6.1-GTNH","2.9.0-RC-1":"1.6.9-GTNH"}
{"name":"OpenBlocks","source":"https://github.com/GTNewHorizons/OpenBlocks","2.8.4":"1.11.7-GTNH","2.9.0-RC-1":"1.12.22-GTNH","source_refs":{"1.11.7-GTNH":"13c9327dda5ca6418bf2cddd9c10601896d81f33","1.12.22-GTNH":"ed3d36624c879782df41cbf83e0837441e04e14a"}}
{"name":"OpenComputers","source":"https://github.com/GTNewHorizons/OpenComputers","2.8.4":"1.11.20-GTNH","2.9.0-RC-1":"1.12.62-GTNH","source_refs":{"1.11.20-GTNH":"f253820d55db72fbc61d6acc4f3c7309d42ffabf","1.12.62-GTNH":"a3ac1859517905732d11af0459affd467b8748fe"}}
{"name":"OpenModsLib","source":"https://github.com/GTNewHorizons/OpenModsLib","2.8.4":"0.10.11","2.9.0-RC-1":"0.10.14","source_refs":{"0.10.11":"fbde453d9bb53f85500ae197042791e4f4043229","0.10.14":"e5e8177f2dd2297d5aea6bb733955a67ffa63c95"}}
{"name":"OpenModularTurrets","source":"https://github.com/GTNewHorizons/OpenModularTurrets","2.8.4":"2.4.3","2.9.0-RC-1":"2.4.8","source_refs":{"2.4.3":"7508e2b8fd39b7dea5782199df7086ad06004715","2.4.8":"d68cad3b91a4ef9bc8c7c6ca124ebe03db4834a0"}}
{"name":"OpenPrinter","source":"https://github.com/GTNewHorizons/OpenPrinter","2.8.4":"0.3.0-GTNH","2.9.0-RC-1":"0.3.1-GTNH","source_refs":{"0.3.0-GTNH":"c1a2393412b3063a257ac58ad6cb96342c47e512","0.3.1-GTNH":"2ac6e82b099c9d62e9e01560465c34e18639bcd6"}}
{"name":"OpenSecurity","source":"https://github.com/GTNewHorizons/OpenSecurity","2.8.4":"1.2.0-GTNH","2.9.0-RC-1":"1.2.2-GTNH","source_refs":{"1.2.0-GTNH":"ccc6928f5a81a28ef9c3a9b5e0cba4020b0dc4ec","1.2.2-GTNH":"d94ead80992a7294362b45781f9b37a510c2f543"}}
{"name":"Opis","source":"https://github.com/GTNewHorizons/Opis","2.8.4":"1.4.6-mapless","2.9.0-RC-1":"1.4.12-mapless"}
{"name":"OverloadedArmorBar","source":"https://github.com/GTNewHorizons/OverloadedArmorBar","2.8.4":"1.1.0","2.9.0-RC-1":"1.1.1"}
{"name":"Pam's Harvest the Nether","source":"https://www.curseforge.com/minecraft/mc-mods/pams-harvest-the-nether","2.8.4":"1.7.10a"}
{"name":"PersonalSpace","source":"https://github.com/GTNewHorizons/PersonalSpace","2.8.4":"1.0.33","2.9.0-RC-1":"1.0.41"}
{"name":"Player-API","source":"https://github.com/GTNewHorizons/Player-API","2.8.4":"1.5.0","2.9.0-RC-1":"1.5.0"}
{"name":"Postea","source":"https://github.com/GTNewHorizons/Postea","2.8.4":"1.1.3","2.9.0-RC-1":"1.2.6"}
{"name":"ProjectBlue","source":"https://github.com/GTNewHorizons/ProjectBlue","2.8.4":"1.2.1-GTNH","2.9.0-RC-1":"1.2.10-GTNH","source_refs":{"1.2.1-GTNH":"b2d2b96baeaa47ea08485da0ec247c8769ae6d5e","1.2.10-GTNH":"c01a5e769643c570de86fcda80614b8afffa64ec"}}
{"name":"ProjectRed","source":"https://github.com/GTNewHorizons/ProjectRed","2.8.4":"4.12.6-GTNH","2.9.0-RC-1":"4.12.44-GTNH","source_refs":{"4.12.44-GTNH":"c6a4cdd2d6c82eff62132c20af584608d65a37bb","4.12.6-GTNH":"c7d23e03314eaf9b30317a1edc6cc82bf917f41e"}}
{"name":"Railcraft","source":"https://github.com/GTNewHorizons/Railcraft","2.8.4":"9.16.33","2.9.0-RC-1":"9.17.31","source_refs":{"9.16.33":"2009186887904851b785d0d23c2d7321c3245d8b","9.17.31":"e8f755cd5eb22774d76eba4f8cd57057e2dfd5b9"}}
{"name":"Random-Things","source":"https://github.com/GTNewHorizons/Random-Things","2.8.4":"2.6.6","2.9.0-RC-1":"2.7.11","source_refs":{"2.6.6":"98b90708d73254756db5764cc17dabf1353937e5","2.7.11":"ee48e41932c9996acc69f95ca341b32f3cc9c1a3"}}
{"name":"Realistic-World-Gen","source":"https://github.com/GTNewHorizons/Realistic-World-Gen","2.8.4":"alpha-1.5.0","2.9.0-RC-1":"alpha-1.5.2"}
{"name":"RemoteIO","source":"https://github.com/GTNewHorizons/RemoteIO","2.8.4":"2.7.6","2.9.0-RC-1":"2.7.10","source_refs":{"2.7.6":"7d4d1169842d0a80eaf429e45c40c5c6c51a751f","2.7.10":"320ebf2f4fb1d65bd4d473f97da5e1f60db14467"}}
{"name":"Roguelike-Dungeons","source":"https://github.com/GTNewHorizons/Roguelike-Dungeons","2.8.4":"1.6.6-GTNH","2.9.0-RC-1":"1.6.6-GTNH"}
{"name":"Salis-Arcana","source":"https://github.com/GTNewHorizons/Salis-Arcana","2.8.4":"1.1.33-GTNH","2.9.0-RC-1":"1.1.71-GTNH"}
{"name":"SC2","source":"https://github.com/GTNewHorizons/SC2","2.8.4":"2.3.12","2.9.0-RC-1":"2.3.15","source_refs":{"2.3.12":"8614df20825636c4d66cddd7ed6893a65c9926e1","2.3.15":"6b774314024503c3ab444c1b0411eecf78bd0bd6"}}
{"name":"Schematica","source":"https://github.com/GTNewHorizons/Schematica","2.8.4":"1.12.6-GTNH","2.9.0-RC-1":"1.12.6-GTNH"}
{"name":"ServerUtilities","source":"https://github.com/GTNewHorizons/ServerUtilities","2.8.4":"2.2.2","2.9.0-RC-1":"2.4.13"}
{"name":"SGCraft","source":"https://github.com/GTNewHorizons/SGCraft","2.8.4":"1.4.5-GTNH","2.9.0-RC-1":"1.4.13-GTNH","source_refs":{"1.4.5-GTNH":"069f2b4fee7292d2d917114cd3d5b4cbffc68381","1.4.13-GTNH":"3809fc0a4ffbb4c682ff7d0747fb9e77d1356afa"}}
{"name":"Share-Where-I-am","source":"https://github.com/GTNewHorizons/Share-Where-I-am","2.8.4":"2.1.4","2.9.0-RC-1":"2.1.11"}
{"name":"SleepingBags","source":"https://github.com/GTNewHorizons/SleepingBags","2.8.4":"0.3.0","2.9.0-RC-1":"0.3.1"}
{"name":"SpecialMobs","source":"https://github.com/GTNewHorizons/SpecialMobs","2.8.4":"3.6.3","2.9.0-RC-1":"3.7.7"}
{"name":"SpiceOfLife","source":"https://github.com/GTNewHorizons/SpiceOfLife","2.8.4":"2.2.3-carrot","2.9.0-RC-1":"2.2.10-carrot"}
{"name":"Steve-s-Factory-Manager","source":"https://github.com/GTNewHorizons/Steve-s-Factory-Manager","2.8.4":"1.3.4-GTNH","2.9.0-RC-1":"1.3.13-GTNH","source_refs":{"1.3.4-GTNH":"20a20196ba6d875bc4b1f7f9d2682837128ae4b9","1.3.13-GTNH":"d746b5910d9a2290f808d350d6a72f7b202d8b0b"}}
{"name":"StevesAddons","source":"https://github.com/GTNewHorizons/StevesAddons","2.8.4":"0.14.2","2.9.0-RC-1":"0.15.5","source_refs":{"0.14.2":"feb59439ca1b33131524aebf594abfd64dd5c3bf","0.15.5":"05bdfe222d5f2e5dd6bc70ebce7e23e07196e35b"}}
{"name":"StorageDrawers","source":"https://github.com/GTNewHorizons/StorageDrawers","2.8.4":"2.1.10-GTNH","2.9.0-RC-1":"2.2.31-GTNH","source_refs":{"2.1.10-GTNH":"bf43610bb0976b4c2ed4c36278fec6b4cc916719","2.2.31-GTNH":"b8c869a418e23cb4e94ca3431065ccf191097652"}}
{"name":"StructureCompat","source":"https://github.com/GTNewHorizons/StructureCompat","2.8.4":"0.7.4","2.9.0-RC-1":"0.7.5"}
{"name":"StructureLib","source":"https://github.com/GTNewHorizons/StructureLib","2.8.4":"1.4.23","2.9.0-RC-1":"1.4.45","source_refs":{"1.4.23":"cd4f3617be96636e3a193d3329d2a91407d8f2b1","1.4.45":"4fdea655379db43443223aa448255144b76d85ca"}}
{"name":"Super-TiC","source":"https://github.com/GTNewHorizons/Super-TiC","2.8.4":"1.5.0","2.9.0-RC-1":"1.5.6"}
{"name":"supersolarpanels","source":"https://github.com/GTNewHorizons/supersolarpanels","2.8.4":"1.1.4","2.9.0-RC-1":"1.1.5","source_refs":{"1.1.4":"8bfbfad1e9d9334161a8867abb08ba4146316106","1.1.5":"ae2a5d9e56472f838f6af49cf78066cf44a8c32b"}}
{"name":"Tainted-Magic","source":"https://github.com/GTNewHorizons/Tainted-Magic","2.8.4":"7.6.26-GTNH","2.9.0-RC-1":"7.7.8-GTNH","source_refs":{"7.6.26-GTNH":"158817e55f5e2be7b24980dda000c604d12ea571","7.7.8-GTNH":"45c987f345eaa471c6b21b69113699ab3bd42156"}}
{"name":"TC-4-Tweaks","source":"https://www.curseforge.com/minecraft/mc-mods/tc4tweaks","2.8.4":"1.5.39","2.9.0-RC-1":"1.5.47"}
{"name":"TCNEIAdditions","source":"https://github.com/GTNewHorizons/TCNEIAdditions","2.8.4":"1.5.4"}
{"name":"TCNodeTracker","source":"https://github.com/GTNewHorizons/TCNodeTracker","2.8.4":"1.4.0","2.9.0-RC-1":"1.4.6"}
{"name":"Thaumcraft NEI Plugin","source":"https://www.curseforge.com/minecraft/mc-mods/thaumcraft-nei-plugin","2.8.4":"1.7a"}
{"name":"thaumcraft-research-tweaks","source":"https://github.com/GTNewHorizons/thaumcraft-research-tweaks","2.8.4":"1.3.0","2.9.0-RC-1":"1.4.1"}
{"name":"Thaumcraft","source":"https://www.curseforge.com/minecraft/mc-mods/thaumcraft","2.8.4":"4.2.3.5a","2.9.0-RC-1":"4.2.3.5"}
{"name":"ThaumcraftMobAspects","source":"https://github.com/GTNewHorizons/ThaumcraftMobAspects","2.8.4":"1.2.1-GTNH","2.9.0-RC-1":"1.2.1-GTNH"}
{"name":"Thaumic Machina","source":"https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/wip-mods/2200956-wip-1-7-10-open-beta-thaumcraft-4-2-addon-thaumic","2.8.4":"0.2.1"}
{"name":"Thaumic_Exploration","source":"https://github.com/GTNewHorizons/Thaumic_Exploration","2.8.4":"1.4.8-GTNH","2.9.0-RC-1":"1.5.28-GTNH","source_refs":{"1.4.8-GTNH":"21ecdcb4abf4918cc2b34117d432611775bccccf","1.5.28-GTNH":"80834cdecc19edce8b3943a512d466360fad8d2a"}}
{"name":"ThaumicBases","source":"https://github.com/GTNewHorizons/ThaumicBases","2.8.4":"1.8.13","2.9.0-RC-1":"1.9.19","source_refs":{"1.8.13":"a7dbefc7ac3f7917bcd33f9592c1aaae6856c485","1.9.19":"7d8707b802315a300a860fa93eba8d1494cc7a31"}}
{"name":"ThaumicBoots","source":"https://github.com/GTNewHorizons/ThaumicBoots","2.8.4":"1.4.14","2.9.0-RC-1":"1.5.13"}
{"name":"ThaumicEnergistics","source":"https://github.com/GTNewHorizons/ThaumicEnergistics","2.8.4":"1.7.14-GTNH","2.9.0-RC-1":"1.7.60-GTNH","source_refs":{"1.7.14-GTNH":"eb7320d9f0625ed844c220f9c606a68a3fd20fdd","1.7.60-GTNH":"fbefd7b85b448266156e941ac33f2c9f361e5c3f"}}
{"name":"ThaumicHorizons","source":"https://github.com/GTNewHorizons/ThaumicHorizons","2.8.4":"1.7.9","2.9.0-RC-1":"1.8.25","source_refs":{"1.7.9":"1a23b89fbe75d76fa54eb033f178efa922e2504f","1.8.25":"21ab790045ae65fc22ea9d1f5ff2c81111f97763"}}
{"name":"thaumicinsurgence","source":"https://github.com/GTNewHorizons/thaumicinsurgence","2.8.4":"0.4.0","2.9.0-RC-1":"0.4.1","source_refs":{"0.4.0":"172c91a016bc5e81d6294a6ba4ef43b588ec912e","0.4.1":"77193fa3b1dc111607b5b826e490b7d78e1335f9"}}
{"name":"ThaumicTinkerer","source":"https://github.com/GTNewHorizons/ThaumicTinkerer","2.8.4":"2.11.27","2.9.0-RC-1":"2.12.33","source_refs":{"2.11.27":"6675941d81505e68cc2677feb4843ed10ea5eeb4","2.12.33":"e4c8acdd4fa95ba6b80e165d248a3bd3f5392762"}}
{"name":"TiC-Tooltips","source":"https://github.com/GTNewHorizons/TiC-Tooltips","2.8.4":"1.4.0","2.9.0-RC-1":"1.4.2"}
{"name":"Tinkers-Defense","source":"https://github.com/GTNewHorizons/Tinkers-Defense","2.8.4":"1.3.2","2.9.0-RC-1":"1.3.4"}
{"name":"Tinkers-Gregworks","source":"https://github.com/Vexatos/TinkersGregworks/tree/GT-NH","2.8.4":"1.0.28","2.9.0-RC-1":"1.0.33"}
{"name":"TinkersConstruct","source":"https://github.com/GTNewHorizons/TinkersConstruct","2.8.4":"1.13.57-GTNH","2.9.0-RC-1":"1.14.115-GTNH","source_refs":{"1.13.57-GTNH":"ba672fb6af0f71678a5c7aac2b2c4c05abf1e2de","1.14.115-GTNH":"27d296fd4d52702f09877e3026a721596b41ff90"}}
{"name":"TinkersMechworks","source":"https://github.com/GTNewHorizons/TinkersMechworks","2.8.4":"0.4.1","2.9.0-RC-1":"0.4.2","source_refs":{"0.4.1":"3d700eb2f400a56b2d19e48a5a8dc39ecea32cae","0.4.2":"6274195147e30950d6bc8dbca0c1e4f26320f8bf"}}
{"name":"TooMuchLoot","source":"https://github.com/GTNewHorizons/TooMuchLoot","2.8.4":"4.3.0-GTNH","2.9.0-RC-1":"4.3.0-GTNH"}
{"name":"ToroHealth","source":"https://github.com/GTNewHorizons/ToroHealth","2.8.4":"1.2.0","2.9.0-RC-1":"1.2.3"}
{"name":"Translocators","source":"https://github.com/GTNewHorizons/Translocators","2.8.4":"1.3.1","2.9.0-RC-1":"1.4.5","source_refs":{"1.3.1":"79a92d0b0f1edbfa25f0744fad0fed86d83d8ab1","1.4.5":"d25e7e8937261c7a59da015d7eabbdad23bbf17a"}}
{"name":"twilightforest","source":"https://github.com/GTNewHorizons/twilightforest","2.8.4":"2.7.13","2.9.0-RC-1":"2.7.42","source_refs":{"2.7.13":"3bbc7c94d7c718b6d03daebfcbfc0d48214ec1c2","2.7.42":"0e83961ad96fd5c097e32a6be90ad33886571b5b"}}
{"name":"TX-Loader","source":"https://github.com/GTNewHorizons/TX-Loader","2.8.4":"1.8.5","2.9.0-RC-1":"1.9.1"}
{"name":"UniLib","source":"https://legacy.curseforge.com/minecraft/mc-mods/unilib","2.8.4":"1.1.1","2.9.0-RC-1":"1.2.1"}
{"name":"UniMixins","source":"https://github.com/LegacyModdingMC/UniMixins","2.8.4":"0.1.23","2.9.0-RC-1":"0.3.1"}
{"name":"Universal-Singularities","source":"https://github.com/GTNewHorizons/Universal-Singularities","2.8.4":"8.10.0","2.9.0-RC-1":"8.15.4"}
{"name":"VisualProspecting","source":"https://github.com/GTNewHorizons/VisualProspecting","2.8.4":"1.4.8","2.9.0-RC-1":"1.5.41"}
{"name":"waila","source":"https://github.com/GTNewHorizons/waila","2.8.4":"1.8.15","2.9.0-RC-1":"1.19.34"}
{"name":"WailaHarvestability","source":"https://github.com/GTNewHorizons/WailaHarvestability","2.8.4":"1.3.4-GTNH","2.9.0-RC-1":"1.3.6-GTNH"}
{"name":"WAILAPlugins","source":"https://github.com/GTNewHorizons/WAILAPlugins","2.8.4":"0.6.0","2.9.0-RC-1":"0.7.2"}
{"name":"WanionLib","source":"https://github.com/GTNewHorizons/WanionLib","2.8.4":"1.10.0","2.9.0-RC-1":"1.10.1"}
{"name":"WarpTheory","source":"https://github.com/GTNewHorizons/WarpTheory","2.8.4":"1.5.0-GTNH","2.9.0-RC-1":"1.5.8-GTNH"}
{"name":"WAWLA","source":"https://github.com/GTNewHorizons/WAWLA","2.8.4":"1.3.1-GTNH","2.9.0-RC-1":"1.3.3-GTNH"}
{"name":"WirelessCraftingTerminal","source":"https://github.com/GTNewHorizons/WirelessCraftingTerminal","2.8.4":"1.12.7","2.9.0-RC-1":"1.12.16"}
{"name":"WirelessRedstone-CBE","source":"https://github.com/GTNewHorizons/WirelessRedstone-CBE","2.8.4":"1.7.1","2.9.0-RC-1":"1.7.12","source_refs":{"1.7.1":"49d37b6c53633a76f8f107c25aa287a67de24e55","1.7.12":"9dfb913b488082be8a376bdd6cf69a5c11ee2a16"}}
{"name":"Witchery","source":"https://www.curseforge.com/minecraft/mc-mods/witchery","2.8.4":"0.24.1","2.9.0-RC-1":"0.24.1"}
{"name":"WitcheryExtras","source":"https://github.com/GTNewHorizons/WitcheryExtras","2.8.4":"1.3.8","2.9.0-RC-1":"1.4.20","source_refs":{"1.3.8":"d71498a83b9cfdad274ea7b2265e732546243f95","1.4.20":"ce2dfbe72e40301a802955eb397b81b08e3dd50c"}}
{"name":"WitchingGadgets","source":"https://github.com/GTNewHorizons/WitchingGadgets","2.8.4":"1.7.25-GTNH","2.9.0-RC-1":"1.8.51-GTNH","source_refs":{"1.7.25-GTNH":"09ea6816f5d844f71df5978f05d42d25d2392859","1.8.51-GTNH":"663bad20ac248aed5c30aa5ebce658b986d0cc41"}}
{"name":"Yamcl","source":"https://github.com/GTNewHorizons/Yamcl","2.8.4":"0.7.1","2.9.0-RC-1":"0.7.5"}
{"name":"Ztones","source":"https://www.curseforge.com/minecraft/mc-mods/ztones","2.8.4":"2.2.2","2.9.0-RC-1":"2.2.2"}
{"name":"AspectRecipeIndex","source":"https://github.com/GTNewHorizons/AspectRecipeIndex","2.9.0-RC-1":"1.1.5"}
{"name":"Automagy-GTNH","source":"https://github.com/GTNewHorizons/Automagy-GTNH","2.9.0-RC-1":"0.29.8-GTNH","source_access":"Public source URL unavailable on 2026-09-30; not source-verified."}
{"name":"ChromaticTooltips","source":"https://github.com/GTNewHorizons/ChromaticTooltips","2.9.0-RC-1":"1.0.36-GTNH"}
{"name":"ChromaticTooltipsCompat","source":"https://github.com/GTNewHorizons/ChromaticTooltipsCompat","2.9.0-RC-1":"1.0.38-GTNH"}
{"name":"Chunk API","source":"https://www.curseforge.com/minecraft/mc-mods/chunkapi/","2.9.0-RC-1":"0.8.4"}
{"name":"CosmeticArmorReworked","source":"https://github.com/GTNewHorizons/CosmeticArmorReworked","2.9.0-RC-1":"1.0.7-GTNH"}
{"name":"CropsNH","source":"https://github.com/GTNewHorizons/CropsNH","2.9.0-RC-1":"2.0.129","source_refs":{"2.0.129":"47debb72b230382c91aa256b92d715062c3a318e"}}
{"name":"EndlessIDs","source":"https://www.curseforge.com/minecraft/mc-mods/endlessids/","2.9.0-RC-1":"1.7.4"}
{"name":"Extreme Sound Muffler","source":"https://www.curseforge.com/minecraft/mc-mods/extreme-sound-muffler-legacy","2.9.0-RC-1":"1.1.1"}
{"name":"FalsepatternLib","source":"https://www.curseforge.com/minecraft/mc-mods/fplib","2.9.0-RC-1":"1.12.2"}
{"name":"Fether","source":"https://github.com/GTNewHorizons/Fether","2.9.0-RC-1":"2.0.7"}
{"name":"Freecam","source":"https://github.com/GTNewHorizons/Freecam","2.9.0-RC-1":"1.0.12"}
{"name":"GregoryTweaksForCrafting","source":"https://github.com/GTNewHorizons/GregoryTweaksForCrafting","2.9.0-RC-1":"1.0.3"}
{"name":"GTNH-Credits","source":"https://github.com/GTNewHorizons/GTNH-Credits","2.9.0-RC-1":"1.3.3"}
{"name":"GTNHExtLib","source":"https://github.com/GTNewHorizons/GTNHExtLib","2.9.0-RC-1":"1.0.4"}
{"name":"GuideNH","source":"https://github.com/GTNewHorizons/GuideNH","2.9.0-RC-1":"1.3.36"}
{"name":"JarJar","source":"https://github.com/GTNewHorizons/JarJar","2.9.0-RC-1":"0.3.7-beta"}
{"name":"Mobs Info","source":"https://www.curseforge.com/minecraft/mc-mods/mobs-info/","2.9.0-RC-1":"0.6.0"}
{"name":"MouseTweaks","source":"https://github.com/GTNewHorizons/MouseTweaks","2.9.0-RC-1":"2.5.3-GTNH"}
{"name":"NoHotbarNeeded","source":"https://github.com/GTNewHorizons/NoHotbarNeeded","2.9.0-RC-1":"0.0.6"}
{"name":"RandomBoubles","source":"https://github.com/GTNewHorizons/RandomBoubles","2.9.0-RC-1":"1.1.7"}
{"name":"SimpleSkinBackport","source":"https://github.com/GTNewHorizons/SimpleSkinBackport","2.9.0-RC-1":"1.0.3-GTNH"}
{"name":"ThaumicMachina","source":"https://github.com/GTNewHorizons/ThaumicMachina","2.9.0-RC-1":"0.2.5-GTNH","source_refs":{"0.2.5-GTNH":"d3f9ed9f22ae5f0d8d315bcaad21405eb4f00378"}}
{"name":"VendingMachine","source":"https://github.com/GTNewHorizons/VendingMachine","2.9.0-RC-1":"0.4.100","source_refs":{"0.4.100":"4e6ec601b6d7fe2d210e3dd0d4b48db951b3ba83"}}
{"name":"VillageNames","source":"https://github.com/GTNewHorizons/VillageNames","2.9.0-RC-1":"4.5.19-GTNH"}
```

### E. Interrupted WIP integrity and recovery

The patch was applied to an isolated temporary Git index based on the completed commit. Every resulting blob matched the current nine source files after Git text normalization. This verifies packaging only; it is not compilation or behavior validation. The real working tree and index were not altered by that check.

```json
{
  "base_commit": "85b531732ccf3cf1066ef6421d813b0b833dacb8",
  "patch_sha256_utf8_lf": "2676d10eda1bfbceea8e655d064059e5456452c87c80ec8f1b1a0fb2feb80f96",
  "patch_bytes": 14763,
  "files": [
    {
      "path": "src/main/java/com/github/lunatrius/schematica/client/gui/save/GuiSchematicSourceSave.java",
      "status": "modified",
      "result_sha256_utf8_lf": "8822ce97d488bffea535bf4b223fa3b819bf4c69f0114f9ed3c7920bc5ab6267"
    },
    {
      "path": "src/main/java/com/github/lunatrius/schematica/client/input/Hotkeys.java",
      "status": "modified",
      "result_sha256_utf8_lf": "5fed557b596b9c0d445abe4c6356d079dfae27e0a6fc76237a3d56e049fd1d71"
    },
    {
      "path": "src/main/java/com/github/lunatrius/schematica/client/world/SchematicSourceData.java",
      "status": "modified",
      "result_sha256_utf8_lf": "633cb4083c74aaa935ce2cb8de533842687f106f167a542c0f8a3d04c6b78bfb"
    },
    {
      "path": "src/main/java/com/github/lunatrius/schematica/client/world/SchematicWorld.java",
      "status": "modified",
      "result_sha256_utf8_lf": "b926aab7699f88bc2ec094dee976f6646583c97471ed710ff65bd8196efbc949"
    },
    {
      "path": "src/main/java/com/github/lunatrius/schematica/tool/ToolMode.java",
      "status": "modified",
      "result_sha256_utf8_lf": "b25446c9dada97e697e11c82b72ee28c38930c1ba17f469845aeaa00b12b2eb6"
    },
    {
      "path": "src/main/java/com/github/lunatrius/schematica/world/schematic/SchematicFileSnapshot.java",
      "status": "modified",
      "result_sha256_utf8_lf": "a8a5414ab190826d33d57f0036652943671390ea3ba5aea4645768699b2e79e3"
    },
    {
      "path": "src/main/java/com/github/lunatrius/schematica/world/storage/SchematicCopies.java",
      "status": "modified",
      "result_sha256_utf8_lf": "543dd2abb8f33541ad4439d2848bd0b9bce4133d345a8125fa0ca2b90db83b6a"
    },
    {
      "path": "src/main/java/com/github/lunatrius/schematica/client/world/SourceBlockPosition.java",
      "status": "new",
      "result_sha256_utf8_lf": "0423074a94a3ed0a1a9c2a90761aa7bb32a8cfcdef6b37db68f54aeb7399e979"
    },
    {
      "path": "src/main/java/com/github/lunatrius/schematica/tool/RebuildDirection.java",
      "status": "new",
      "result_sha256_utf8_lf": "8bcfe1612bab295a87d9d76237ec6663d43453f85c4f4b2603e591e029737dc2"
    }
  ]
}
```

The complete unified diff is embedded as base64 to retain exact whitespace inside Markdown and avoid trailing-space damage. Decode on cloud (then inspect and check before applying):

```sh
python3 - <<'PY'
from pathlib import Path
import hashlib, base64
s = Path("HANDOUT.md").read_text(encoding="utf-8")
block = s.split("<!-- BEGIN_" + "REBUILD_WIP_PATCH -->", 1)[1]
encoded = block.split("```base64\n", 1)[1].split("\n```", 1)[0]
data = base64.b64decode("".join(encoded.split()), validate=True)
assert hashlib.sha256(data).hexdigest() == "2676d10eda1bfbceea8e655d064059e5456452c87c80ec8f1b1a0fb2feb80f96"
Path("/tmp/schematica-rebuild-wip.patch").write_bytes(data)
PY
git apply --check /tmp/schematica-rebuild-wip.patch
# Apply only when absent and the check succeeded; do not apply twice.
git apply /tmp/schematica-rebuild-wip.patch
```

<!-- BEGIN_REBUILD_WIP_PATCH -->
```base64
ZGlmZiAtLWdpdCBhL3NyYy9tYWluL2phdmEvY29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS9jbGllbnQvZ3VpL3NhdmUv
R3VpU2NoZW1hdGljU291cmNlU2F2ZS5qYXZhIGIvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0aWNh
L2NsaWVudC9ndWkvc2F2ZS9HdWlTY2hlbWF0aWNTb3VyY2VTYXZlLmphdmEKaW5kZXggYTMyMDhlZi4uNmExNGU1OCAxMDA2NDQK
LS0tIGEvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0aWNhL2NsaWVudC9ndWkvc2F2ZS9HdWlTY2hl
bWF0aWNTb3VyY2VTYXZlLmphdmEKKysrIGIvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0aWNhL2Ns
aWVudC9ndWkvc2F2ZS9HdWlTY2hlbWF0aWNTb3VyY2VTYXZlLmphdmEKQEAgLTM4LDcgKzM4LDcgQEAgcHVibGljIGZpbmFsIGNs
YXNzIEd1aVNjaGVtYXRpY1NvdXJjZVNhdmUgZXh0ZW5kcyBHdWlTY2hlbWF0aWNCcm93c2VyIHsKICAgICAgICAgbmFtZS5zZXRU
ZXh0KHNvdXJjZS5uYW1lKCkpOwogICAgICAgICBzYXZlID0gYWRkQnV0dG9uKCJsaXRlbWF0aWNhLmd1aS5idXR0b24uc2F2ZV90
b19maWxlIiwgdGhpczo6c2F2ZSk7CiAgICAgICAgIHNhdmUuc2V0VG9vbHRpcChVaVRyYW5zbGF0aW9ucy5mb3JtYXQoInNjaGVt
YXRpY2EudWkuc291cmNlLnNhdmVfaGludCIpKTsKLSAgICAgICAgZm9ybWF0ID0gcm9vdC5hZGQobmV3IFVpTGFiZWwoKCkgLT4g
c291cmNlLmRhdGEoKS5zbmFwc2hvdC5leHRlbnNpb24oKSkpOworICAgICAgICBmb3JtYXQgPSByb290LmFkZChuZXcgVWlMYWJl
bCgoKSAtPiBzb3VyY2UuZGF0YSgpLnNhdmVFeHRlbnNpb24oKSkpOwogICAgICAgICBmb3JtYXQuc2V0VG9vbHRpcChVaVRyYW5z
bGF0aW9ucy5mb3JtYXQoInNjaGVtYXRpY2EudWkuc291cmNlLnNhdmVfaGludCIpKTsKICAgICB9CiAKQEAgLTU0LDcgKzU0LDcg
QEAgcHVibGljIGZpbmFsIGNsYXNzIEd1aVNjaGVtYXRpY1NvdXJjZVNhdmUgZXh0ZW5kcyBHdWlTY2hlbWF0aWNCcm93c2VyIHsK
ICAgICBwcml2YXRlIHZvaWQgc2F2ZSgpIHsKICAgICAgICAgaWYgKGJyb3dzZXIgPT0gbnVsbCB8fCAhQ2xpZW50UHJveHkuU0NI
RU1BVElDUy5zb3VyY2VzKCkuY29udGFpbnMoc291cmNlKSkgcmV0dXJuOwogICAgICAgICB0cnkgewotICAgICAgICAgICAgRmls
ZSBmaWxlID0gU2NoZW1hdGljU2F2ZVRhcmdldC5zb3VyY2VDb3B5KGJyb3dzZXIucm9vdCgpLCBicm93c2VyLmRpcmVjdG9yeSgp
LCBuYW1lLnRleHQoKSwgc291cmNlLmRhdGEoKS5zbmFwc2hvdC5leHRlbnNpb24oKSk7CisgICAgICAgICAgICBGaWxlIGZpbGUg
PSBTY2hlbWF0aWNTYXZlVGFyZ2V0LnNvdXJjZUNvcHkoYnJvd3Nlci5yb290KCksIGJyb3dzZXIuZGlyZWN0b3J5KCksIG5hbWUu
dGV4dCgpLCBzb3VyY2UuZGF0YSgpLnNhdmVFeHRlbnNpb24oKSk7CiAgICAgICAgICAgICBpZiAoZmlsZS5leGlzdHMoKSkgY29u
ZmlybShVaVRyYW5zbGF0aW9ucy5mb3JtYXQoInNjaGVtYXRpY2EudWkuc2F2ZS5vdmVyd3JpdGVfdGl0bGUiKSwKICAgICAgICAg
ICAgICAgICBVaVRyYW5zbGF0aW9ucy5mb3JtYXQoInNjaGVtYXRpY2EudWkuc2F2ZS5vdmVyd3JpdGUiLCBmaWxlLmdldE5hbWUo
KSksICgpIC0+IHdyaXRlKGZpbGUsIHRydWUpKTsKICAgICAgICAgICAgIGVsc2Ugd3JpdGUoZmlsZSwgZmFsc2UpOwpAQCAtNjks
OCArNjksOCBAQCBwdWJsaWMgZmluYWwgY2xhc3MgR3VpU2NoZW1hdGljU291cmNlU2F2ZSBleHRlbmRzIEd1aVNjaGVtYXRpY0Jy
b3dzZXIgewogICAgICAgICAgICAgcmV0dXJuOwogICAgICAgICB9CiAgICAgICAgIHRyeSB7Ci0gICAgICAgICAgICBGaWxlIGNo
ZWNrZWQgPSBTY2hlbWF0aWNTYXZlVGFyZ2V0LnNvdXJjZUNvcHkoYnJvd3Nlci5yb290KCksIGZpbGUuZ2V0UGFyZW50RmlsZSgp
LCBmaWxlLmdldE5hbWUoKSwgc291cmNlLmRhdGEoKS5zbmFwc2hvdC5leHRlbnNpb24oKSk7Ci0gICAgICAgICAgICBzb3VyY2Uu
ZGF0YSgpLnNuYXBzaG90LndyaXRlKGNoZWNrZWQsIHJlcGxhY2UpOworICAgICAgICAgICAgRmlsZSBjaGVja2VkID0gU2NoZW1h
dGljU2F2ZVRhcmdldC5zb3VyY2VDb3B5KGJyb3dzZXIucm9vdCgpLCBmaWxlLmdldFBhcmVudEZpbGUoKSwgZmlsZS5nZXROYW1l
KCksIHNvdXJjZS5kYXRhKCkuc2F2ZUV4dGVuc2lvbigpKTsKKyAgICAgICAgICAgIHNvdXJjZS5kYXRhKCkuc2F2ZShjaGVja2Vk
LCByZXBsYWNlKTsKICAgICAgICAgICAgIHJlZnJlc2hGaWxlcygpOwogICAgICAgICAgICAgc2V0U3RhdHVzKFVpVHJhbnNsYXRp
b25zLmZvcm1hdCgibGl0ZW1hdGljYS5tZXNzYWdlLnNjaGVtYXRpY19zYXZlZF9hcyIsIGZpbGUuZ2V0TmFtZSgpKSk7CiAgICAg
ICAgIH0gY2F0Y2ggKElPRXhjZXB0aW9uIHwgSWxsZWdhbEFyZ3VtZW50RXhjZXB0aW9uIGUpIHsKZGlmZiAtLWdpdCBhL3NyYy9t
YWluL2phdmEvY29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS9jbGllbnQvaW5wdXQvSG90a2V5cy5qYXZhIGIvc3JjL21h
aW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0aWNhL2NsaWVudC9pbnB1dC9Ib3RrZXlzLmphdmEKaW5kZXggZDI3
ZmI1MC4uNjA4NzMxYiAxMDA2NDQKLS0tIGEvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0aWNhL2Ns
aWVudC9pbnB1dC9Ib3RrZXlzLmphdmEKKysrIGIvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0aWNh
L2NsaWVudC9pbnB1dC9Ib3RrZXlzLmphdmEKQEAgLTU5LDYgKzU5LDggQEAgcHVibGljIGZpbmFsIGNsYXNzIEhvdGtleXMgewog
ICAgICAgICBmb3IgKFN0cmluZyBpZCA6IG5ldyBTdHJpbmdbXSB7Im51ZGdlU2VsZWN0aW9uUG9zaXRpdmUiLCAibnVkZ2VTZWxl
Y3Rpb25OZWdhdGl2ZSIsICJtb3ZlRW50aXJlU2VsZWN0aW9uIiwKICAgICAgICAgICAgICJzZWxlY3Rpb25Hcm93IiwgInNlbGVj
dGlvblNocmluayIsICJkZWxldGVTZWxlY3Rpb25Cb3giLCAic2V0QXJlYU9yaWdpbiIsICJzZXRTZWxlY3Rpb25Cb3hQb3NpdGlv
bjEiLAogICAgICAgICAgICAgInNldFNlbGVjdGlvbkJveFBvc2l0aW9uMiIsICJ1bmxvYWRDdXJyZW50U2NoZW1hdGljIiwgInVp
RGVtbyJ9KSBhZGQoaWQsIG5vcm1hbCgpKTsKKyAgICAgICAgZm9yIChTdHJpbmcgaWQgOiBuZXcgU3RyaW5nW10geyJzY2hlbWF0
aWNFZGl0QnJlYWtBbGxFeGNlcHQiLCAic2NoZW1hdGljRWRpdEJyZWFrUGxhY2VBbGwiLCAic2NoZW1hdGljRWRpdEJyZWFrUGxh
Y2VEaXJlY3Rpb24iLAorICAgICAgICAgICAgInNjaGVtYXRpY0VkaXRSZXBsYWNlQWxsIiwgInNjaGVtYXRpY0VkaXRSZXBsYWNl
QmxvY2siLCAic2NoZW1hdGljRWRpdFJlcGxhY2VEaXJlY3Rpb24ifSkgYWRkKGlkLCBtb2RpZmllcigpKTsKICAgICAgICAgQUxM
ID0gQ29sbGVjdGlvbnMudW5tb2RpZmlhYmxlTGlzdChuZXcgQXJyYXlMaXN0PD4oQllfSUQudmFsdWVzKCkpKTsKICAgICB9CiAK
ZGlmZiAtLWdpdCBhL3NyYy9tYWluL2phdmEvY29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS9jbGllbnQvd29ybGQvU2No
ZW1hdGljU291cmNlRGF0YS5qYXZhIGIvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0aWNhL2NsaWVu
dC93b3JsZC9TY2hlbWF0aWNTb3VyY2VEYXRhLmphdmEKaW5kZXggNTAwY2I1Yy4uZDIxNzA4OSAxMDA2NDQKLS0tIGEvc3JjL21h
aW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0aWNhL2NsaWVudC93b3JsZC9TY2hlbWF0aWNTb3VyY2VEYXRhLmph
dmEKKysrIGIvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0aWNhL2NsaWVudC93b3JsZC9TY2hlbWF0
aWNTb3VyY2VEYXRhLmphdmEKQEAgLTE0LDYgKzE0LDcgQEAgcHVibGljIGZpbmFsIGNsYXNzIFNjaGVtYXRpY1NvdXJjZURhdGEg
ewogICAgIHB1YmxpYyBmaW5hbCBpbnQgd2lkdGg7CiAgICAgcHVibGljIGZpbmFsIGludCBoZWlnaHQ7CiAgICAgcHVibGljIGZp
bmFsIGludCBsZW5ndGg7CisgICAgcHJpdmF0ZSBJU2NoZW1hdGljIGVkaXRlZDsKIAogICAgIHByaXZhdGUgU2NoZW1hdGljU291
cmNlRGF0YShTY2hlbWF0aWNGaWxlU25hcHNob3Qgc25hcHNob3QsIElTY2hlbWF0aWMgZGF0YSkgewogICAgICAgICB0aGlzLnNu
YXBzaG90ID0gc25hcHNob3Q7CkBAIC0yOCw1ICsyOSwxNSBAQCBwdWJsaWMgZmluYWwgY2xhc3MgU2NoZW1hdGljU291cmNlRGF0
YSB7CiAgICAgICAgIHJldHVybiBuZXcgU2NoZW1hdGljU291cmNlRGF0YShzbmFwc2hvdCwgU2NoZW1hdGljRm9ybWF0LnJlYWRG
cm9tU25hcHNob3Qoc25hcHNob3QpKTsKICAgICB9CiAKLSAgICBwdWJsaWMgSVNjaGVtYXRpYyBpbnN0YW50aWF0ZSgpIHRocm93
cyBJT0V4Y2VwdGlvbiB7IHJldHVybiBTY2hlbWF0aWNGb3JtYXQucmVhZEZyb21TbmFwc2hvdChzbmFwc2hvdCk7IH0KKyAgICBw
dWJsaWMgSVNjaGVtYXRpYyBpbnN0YW50aWF0ZSgpIHRocm93cyBJT0V4Y2VwdGlvbiB7CisgICAgICAgIHJldHVybiBlZGl0ZWQg
PT0gbnVsbCA/IFNjaGVtYXRpY0Zvcm1hdC5yZWFkRnJvbVNuYXBzaG90KHNuYXBzaG90KSA6IGNvbS5naXRodWIubHVuYXRyaXVz
LnNjaGVtYXRpY2Eud29ybGQuc3RvcmFnZS5TY2hlbWF0aWNDb3BpZXMuY29weShlZGl0ZWQpOworICAgIH0KKyAgICBwdWJsaWMg
Ym9vbGVhbiBtb2RpZmllZCgpIHsgcmV0dXJuIGVkaXRlZCAhPSBudWxsOyB9CisgICAgcHVibGljIFN0cmluZyBzYXZlRXh0ZW5z
aW9uKCkgeyByZXR1cm4gbW9kaWZpZWQoKSA/ICIuc2NoZW1wbHVzIiA6IHNuYXBzaG90LmV4dGVuc2lvbigpOyB9CisgICAgcHVi
bGljIHZvaWQgZWRpdChqYXZhLnV0aWwuZnVuY3Rpb24uQ29uc3VtZXI8SVNjaGVtYXRpYz4gZWRpdG9yKSB0aHJvd3MgSU9FeGNl
cHRpb24geworICAgICAgICBJU2NoZW1hdGljIG5leHQgPSBpbnN0YW50aWF0ZSgpOyBlZGl0b3IuYWNjZXB0KG5leHQpOyBlZGl0
ZWQgPSBuZXh0OworICAgIH0KKyAgICBwdWJsaWMgdm9pZCBzYXZlKEZpbGUgZmlsZSwgYm9vbGVhbiByZXBsYWNlKSB0aHJvd3Mg
SU9FeGNlcHRpb24geworICAgICAgICAoZWRpdGVkID09IG51bGwgPyBzbmFwc2hvdCA6IFNjaGVtYXRpY0ZpbGVTbmFwc2hvdC5j
YXB0dXJlKGVkaXRlZCkpLndyaXRlKGZpbGUsIHJlcGxhY2UpOworICAgIH0KIH0KZGlmZiAtLWdpdCBhL3NyYy9tYWluL2phdmEv
Y29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS9jbGllbnQvd29ybGQvU2NoZW1hdGljV29ybGQuamF2YSBiL3NyYy9tYWlu
L2phdmEvY29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS9jbGllbnQvd29ybGQvU2NoZW1hdGljV29ybGQuamF2YQppbmRl
eCA5ZWUzYTE5Li44YjAxNzQ4IDEwMDY0NAotLS0gYS9zcmMvbWFpbi9qYXZhL2NvbS9naXRodWIvbHVuYXRyaXVzL3NjaGVtYXRp
Y2EvY2xpZW50L3dvcmxkL1NjaGVtYXRpY1dvcmxkLmphdmEKKysrIGIvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1
cy9zY2hlbWF0aWNhL2NsaWVudC93b3JsZC9TY2hlbWF0aWNXb3JsZC5qYXZhCkBAIC03MSw2ICs3MSwxMCBAQCBwdWJsaWMgY2xh
c3MgU2NoZW1hdGljV29ybGQgZXh0ZW5kcyBXb3JsZCB7CiAgICAgfQogCiAgICAgcHVibGljIFN1YlJlZ2lvblBsYWNlbWVudHMg
c3VicmVnaW9ucygpIHsgcmV0dXJuIHN1YnJlZ2lvbnM7IH0KKyAgICBwdWJsaWMgU2NoZW1hdGljU291cmNlRGF0YSBzb3VyY2VE
YXRhKCkgeyByZXR1cm4gcGxhY2VtZW50U291cmNlOyB9CisgICAgcHVibGljIHZvaWQgcmVmcmVzaFNvdXJjZSgpIHsKKyAgICAg
ICAgaWYgKHBsYWNlbWVudFNvdXJjZSAhPSBudWxsKSB7IHJlYnVpbGRSZWdpb25zKHN1YnJlZ2lvbnMsIG5ldyBBcnJheUxpc3Q8
Pih0cmFuc2Zvcm1PcGVyYXRpb25zKSk7IGNvbnRlbnRSZXZpc2lvbisrOyB9CisgICAgfQogICAgIHB1YmxpYyBpbnQgcGxhY2Vt
ZW50UmV2aXNpb24oKSB7IHJldHVybiBwbGFjZW1lbnRSZXZpc2lvbjsgfQogICAgIHB1YmxpYyBpbnQgY29udGVudFJldmlzaW9u
KCkgeyByZXR1cm4gY29udGVudFJldmlzaW9uOyB9CiAgICAgcHVibGljIGJvb2xlYW4gaGFzRW5hYmxlZFJlZ2lvbnMoKSB7IHJl
dHVybiBzdWJyZWdpb25zID09IG51bGwgfHwgc3VicmVnaW9ucy5oYXNFbmFibGVkKCk7IH0KZGlmZiAtLWdpdCBhL3NyYy9tYWlu
L2phdmEvY29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS90b29sL1Rvb2xNb2RlLmphdmEgYi9zcmMvbWFpbi9qYXZhL2Nv
bS9naXRodWIvbHVuYXRyaXVzL3NjaGVtYXRpY2EvdG9vbC9Ub29sTW9kZS5qYXZhCmluZGV4IGZiNjZhZDkuLjNkYTEyMzIgMTAw
NjQ0Ci0tLSBhL3NyYy9tYWluL2phdmEvY29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS90b29sL1Rvb2xNb2RlLmphdmEK
KysrIGIvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0aWNhL3Rvb2wvVG9vbE1vZGUuamF2YQpAQCAt
MTYsOCArMTYsMTAgQEAgcHVibGljIGVudW0gVG9vbE1vZGUgewogICAgIEZJTEwoImxpdGVtYXRpY2EudG9vbF9tb2RlLm5hbWUu
ZmlsbCIsIHRydWUsIGZhbHNlLCB0cnVlLCBmYWxzZSksCiAgICAgUkVQTEFDRV9CTE9DSygibGl0ZW1hdGljYS50b29sX21vZGUu
bmFtZS5yZXBsYWNlX2Jsb2NrIiwgdHJ1ZSwgZmFsc2UsIHRydWUsIHRydWUpLAogICAgIFBBU1RFX1NDSEVNQVRJQygibGl0ZW1h
dGljYS50b29sX21vZGUubmFtZS5wYXN0ZV9zY2hlbWF0aWMiLCB0cnVlLCB0cnVlLCBmYWxzZSwgZmFsc2UpLAorICAgIEdSSURf
UEFTVEUoImxpdGVtYXRpY2EudG9vbF9tb2RlLm5hbWUuZ3JpZF9wYXN0ZSIsIHRydWUsIHRydWUsIGZhbHNlLCBmYWxzZSksCiAg
ICAgTU9WRSgibGl0ZW1hdGljYS50b29sX21vZGUubmFtZS5tb3ZlIiwgdHJ1ZSwgZmFsc2UsIGZhbHNlLCBmYWxzZSksCi0gICAg
REVMRVRFKCJsaXRlbWF0aWNhLnRvb2xfbW9kZS5uYW1lLmRlbGV0ZSIsIHRydWUsIGZhbHNlLCBmYWxzZSwgZmFsc2UpOworICAg
IERFTEVURSgibGl0ZW1hdGljYS50b29sX21vZGUubmFtZS5kZWxldGUiLCB0cnVlLCBmYWxzZSwgZmFsc2UsIGZhbHNlKSwKKyAg
ICBSRUJVSUxEKCJsaXRlbWF0aWNhLnRvb2xfbW9kZS5uYW1lLnJlYnVpbGQiLCBmYWxzZSwgdHJ1ZSwgdHJ1ZSwgZmFsc2UpOwog
CiAgICAgcHJpdmF0ZSBmaW5hbCBTdHJpbmcgdHJhbnNsYXRpb25LZXk7CiAgICAgcHJpdmF0ZSBmaW5hbCBib29sZWFuIGNyZWF0
aXZlT25seTsKZGlmZiAtLWdpdCBhL3NyYy9tYWluL2phdmEvY29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS93b3JsZC9z
Y2hlbWF0aWMvU2NoZW1hdGljRmlsZVNuYXBzaG90LmphdmEgYi9zcmMvbWFpbi9qYXZhL2NvbS9naXRodWIvbHVuYXRyaXVzL3Nj
aGVtYXRpY2Evd29ybGQvc2NoZW1hdGljL1NjaGVtYXRpY0ZpbGVTbmFwc2hvdC5qYXZhCmluZGV4IDZmMDNhZGUuLjEzODg5ODkg
MTAwNjQ0Ci0tLSBhL3NyYy9tYWluL2phdmEvY29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS93b3JsZC9zY2hlbWF0aWMv
U2NoZW1hdGljRmlsZVNuYXBzaG90LmphdmEKKysrIGIvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9zY2hlbWF0
aWNhL3dvcmxkL3NjaGVtYXRpYy9TY2hlbWF0aWNGaWxlU25hcHNob3QuamF2YQpAQCAtNDUsNiArNDUsMTQgQEAgcHVibGljIGZp
bmFsIGNsYXNzIFNjaGVtYXRpY0ZpbGVTbmFwc2hvdCB7CiAgICAgfQogCiAgICAgcHVibGljIFN0cmluZyBleHRlbnNpb24oKSB7
IHJldHVybiBleHRlbnNpb247IH0KKyAgICBwdWJsaWMgc3RhdGljIFNjaGVtYXRpY0ZpbGVTbmFwc2hvdCBjYXB0dXJlKGNvbS5n
aXRodWIubHVuYXRyaXVzLnNjaGVtYXRpY2EuYXBpLklTY2hlbWF0aWMgc2NoZW1hdGljKSB0aHJvd3MgSU9FeGNlcHRpb24gewor
ICAgICAgICBOQlRUYWdDb21wb3VuZCB0YWcgPSBuZXcgTkJUVGFnQ29tcG91bmQoKTsKKyAgICAgICAgaWYgKCFuZXcgU2NoZW1h
dGljQWxwaGEoKS53cml0ZVRvTkJUKHRhZywgc2NoZW1hdGljLCBudWxsLCB0cnVlLCB0cnVlLCB0cnVlKSkgdGhyb3cgbmV3IElP
RXhjZXB0aW9uKCJVbmFibGUgdG8gZW5jb2RlIGVkaXRlZCBzY2hlbWF0aWMiKTsKKyAgICAgICAgQnl0ZUFycmF5T3V0cHV0U3Ry
ZWFtIGJ5dGVzID0gbmV3IEJ5dGVBcnJheU91dHB1dFN0cmVhbSgpOworICAgICAgICBuZXQubWluZWNyYWZ0Lm5idC5Db21wcmVz
c2VkU3RyZWFtVG9vbHMud3JpdGVDb21wcmVzc2VkKHRhZywgYnl0ZXMpOworICAgICAgICBpZiAoYnl0ZXMuc2l6ZSgpID4gU2No
ZW1hdGljTGltaXRzLk1BWF9OQlRfQllURVMpIHRocm93IG5ldyBJT0V4Y2VwdGlvbigiRWRpdGVkIHNjaGVtYXRpYyBleGNlZWRz
IGZpbGUgbGltaXQiKTsKKyAgICAgICAgcmV0dXJuIG5ldyBTY2hlbWF0aWNGaWxlU25hcHNob3QoYnl0ZXMudG9CeXRlQXJyYXko
KSwgIi5zY2hlbXBsdXMiKTsKKyAgICB9CiAgICAgcHVibGljIGludCBzaXplKCkgeyByZXR1cm4gYnl0ZXMubGVuZ3RoOyB9CiAK
ICAgICBwdWJsaWMgTkJUVGFnQ29tcG91bmQgcmVhZE5CVCgpIHRocm93cyBJT0V4Y2VwdGlvbiB7CmRpZmYgLS1naXQgYS9zcmMv
bWFpbi9qYXZhL2NvbS9naXRodWIvbHVuYXRyaXVzL3NjaGVtYXRpY2Evd29ybGQvc3RvcmFnZS9TY2hlbWF0aWNDb3BpZXMuamF2
YSBiL3NyYy9tYWluL2phdmEvY29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS93b3JsZC9zdG9yYWdlL1NjaGVtYXRpY0Nv
cGllcy5qYXZhCmluZGV4IGQzYjVlM2EuLmU2ZTdjYTIgMTAwNjQ0Ci0tLSBhL3NyYy9tYWluL2phdmEvY29tL2dpdGh1Yi9sdW5h
dHJpdXMvc2NoZW1hdGljYS93b3JsZC9zdG9yYWdlL1NjaGVtYXRpY0NvcGllcy5qYXZhCisrKyBiL3NyYy9tYWluL2phdmEvY29t
L2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS93b3JsZC9zdG9yYWdlL1NjaGVtYXRpY0NvcGllcy5qYXZhCkBAIC0xMCw2ICsx
MCwyNCBAQCBpbXBvcnQgY29tLmdpdGh1Yi5sdW5hdHJpdXMuc2NoZW1hdGljYS5uYnQuTkJUSGVscGVyOwogcHVibGljIGZpbmFs
IGNsYXNzIFNjaGVtYXRpY0NvcGllcyB7CiAgICAgcHJpdmF0ZSBTY2hlbWF0aWNDb3BpZXMoKSB7fQogCisgICAgcHVibGljIHN0
YXRpYyBTY2hlbWF0aWMgY29weShJU2NoZW1hdGljIHNvdXJjZSkgeworICAgICAgICBib29sZWFuIGluZGVwZW5kZW50ID0gZmFs
c2U7CisgICAgICAgIGZvciAoY29tLmdpdGh1Yi5sdW5hdHJpdXMuc2NoZW1hdGljYS5hcGkuU2NoZW1hdGljUmVnaW9uIHJlZ2lv
biA6IHNvdXJjZS5nZXRSZWdpb25zKCkpIGluZGVwZW5kZW50IHw9IHNvdXJjZS5nZXRSZWdpb25TY2hlbWF0aWMocmVnaW9uLm5h
bWUpICE9IG51bGw7CisgICAgICAgIFNjaGVtYXRpYyByZXN1bHQgPSBpbmRlcGVuZGVudCA/IG5ldyBNdWx0aVJlZ2lvblNjaGVt
YXRpYyhzb3VyY2UuZ2V0SWNvbigpLmNvcHkoKSwgc291cmNlLmdldFdpZHRoKCksIHNvdXJjZS5nZXRIZWlnaHQoKSwgc291cmNl
LmdldExlbmd0aCgpKQorICAgICAgICAgICAgOiBuZXcgU2NoZW1hdGljKHNvdXJjZS5nZXRJY29uKCkuY29weSgpLCBzb3VyY2Uu
Z2V0V2lkdGgoKSwgc291cmNlLmdldEhlaWdodCgpLCBzb3VyY2UuZ2V0TGVuZ3RoKCkpOworICAgICAgICBvdmVybGF5KHNvdXJj
ZSwgcmVzdWx0LCAwLCAwLCAwLCB0cnVlKTsKKyAgICAgICAgaWYgKGluZGVwZW5kZW50KSBmb3IgKGNvbS5naXRodWIubHVuYXRy
aXVzLnNjaGVtYXRpY2EuYXBpLlNjaGVtYXRpY1JlZ2lvbiByZWdpb24gOiBzb3VyY2UuZ2V0UmVnaW9ucygpKSB7CisgICAgICAg
ICAgICBJU2NoZW1hdGljIGNvbnRlbnRzID0gc291cmNlLmdldFJlZ2lvblNjaGVtYXRpYyhyZWdpb24ubmFtZSk7CisgICAgICAg
ICAgICBpZiAoY29udGVudHMgIT0gbnVsbCkgKChNdWx0aVJlZ2lvblNjaGVtYXRpYykgcmVzdWx0KS5hZGRSZWdpb24ocmVnaW9u
LCBjb3B5KGNvbnRlbnRzKSk7CisgICAgICAgIH0KKyAgICAgICAgaWYgKGluZGVwZW5kZW50KSB7CisgICAgICAgICAgICByZXN1
bHQuZ2V0RW50aXRpZXMoKS5jbGVhcigpOworICAgICAgICAgICAgZm9yIChFbnRpdHkgZW50aXR5IDogc291cmNlLmdldEVudGl0
aWVzKCkpIHJlc3VsdC5hZGRFbnRpdHkoZW50aXR5KGVudGl0eSwgMCwgMCwgMCkpOworICAgICAgICB9CisgICAgICAgIHJlc3Vs
dC5zZXRPcmlnaW4oc291cmNlLmdldE9yaWdpbigpKTsgcmVzdWx0LnNldFJlZ2lvbnMoc291cmNlLmdldFJlZ2lvbnMoKSk7Cisg
ICAgICAgIHJldHVybiByZXN1bHQ7CisgICAgfQorCiAgICAgcHVibGljIHN0YXRpYyB2b2lkIG92ZXJsYXkoSVNjaGVtYXRpYyBz
b3VyY2UsIElTY2hlbWF0aWMgdGFyZ2V0LCBpbnQgZHgsIGludCBkeSwgaW50IGR6LCBib29sZWFuIGVudGl0aWVzKSB7CiAgICAg
ICAgIGlmIChzb3VyY2UgPT0gdGFyZ2V0IHx8IGR4IDwgMCB8fCBkeSA8IDAgfHwgZHogPCAwCiAgICAgICAgICAgICB8fCAobG9u
ZykgZHggKyBzb3VyY2UuZ2V0V2lkdGgoKSA+IHRhcmdldC5nZXRXaWR0aCgpIHx8IChsb25nKSBkeSArIHNvdXJjZS5nZXRIZWln
aHQoKSA+IHRhcmdldC5nZXRIZWlnaHQoKQpkaWZmIC0tZ2l0IGEvc3JjL21haW4vamF2YS9jb20vZ2l0aHViL2x1bmF0cml1cy9z
Y2hlbWF0aWNhL2NsaWVudC93b3JsZC9Tb3VyY2VCbG9ja1Bvc2l0aW9uLmphdmEgYi9zcmMvbWFpbi9qYXZhL2NvbS9naXRodWIv
bHVuYXRyaXVzL3NjaGVtYXRpY2EvY2xpZW50L3dvcmxkL1NvdXJjZUJsb2NrUG9zaXRpb24uamF2YQpuZXcgZmlsZSBtb2RlIDEw
MDY0NAotLS0gL2Rldi9udWxsCisrKyBiL3NyYy9tYWluL2phdmEvY29tL2dpdGh1Yi9sdW5hdHJpdXMvc2NoZW1hdGljYS9jbGll
bnQvd29ybGQvU291cmNlQmxvY2tQb3NpdGlvbi5qYXZhCkBAIC0wLDAgKzEsMjUgQEAKK3BhY2thZ2UgY29tLmdpdGh1Yi5sdW5h
dHJpdXMuc2NoZW1hdGljYS5jbGllbnQud29ybGQ7CisKK2ltcG9ydCBqYXZhLnV0aWwuTGlzdDsKKworaW1wb3J0IGNvbS5naXRo
dWIubHVuYXRyaXVzLnNjaGVtYXRpY2EuYXBpLlNjaGVtYXRpY09yaWdpbjsKKworcHVibGljIGZpbmFsIGNsYXNzIFNvdXJjZUJs
b2NrUG9zaXRpb24geworICAgIHB1YmxpYyBmaW5hbCBTdHJpbmcgcmVnaW9uOworICAgIHB1YmxpYyBmaW5hbCBTY2hlbWF0aWNP
cmlnaW4gbG9jYWw7CisgICAgcHJpdmF0ZSBTb3VyY2VCbG9ja1Bvc2l0aW9uKFN0cmluZyByZWdpb24sIFNjaGVtYXRpY09yaWdp
biBsb2NhbCkgeyB0aGlzLnJlZ2lvbiA9IHJlZ2lvbjsgdGhpcy5sb2NhbCA9IGxvY2FsOyB9CisKKyAgICBwdWJsaWMgc3RhdGlj
IFNvdXJjZUJsb2NrUG9zaXRpb24gcmVzb2x2ZShTY2hlbWF0aWNPcmlnaW4gd29ybGQsIFNjaGVtYXRpY09yaWdpbiBvcmlnaW4s
IExpc3Q8U3RyaW5nPiB0cmFuc2Zvcm1zLCBTdWJSZWdpb25QbGFjZW1lbnRzIHJlZ2lvbnMsIFN0cmluZyBzZWxlY3RlZCkgewor
ICAgICAgICBTY2hlbWF0aWNPcmlnaW4gcG9pbnQgPSBTdWJSZWdpb25QbGFjZW1lbnRzLnZlY3RvcihuZXcgU2NoZW1hdGljT3Jp
Z2luKHdvcmxkLnggLSBvcmlnaW4ueCwgd29ybGQueSAtIG9yaWdpbi55LCB3b3JsZC56IC0gb3JpZ2luLnopLCB0cmFuc2Zvcm1z
LCB0cnVlKTsKKyAgICAgICAgTGlzdDxTdWJSZWdpb25QbGFjZW1lbnRzLlJlZ2lvbj4gdmFsdWVzID0gcmVnaW9ucy5yZWdpb25z
KCk7CisgICAgICAgIGZvciAoaW50IGkgPSB2YWx1ZXMuc2l6ZSgpIC0gMTsgaSA+PSAwOyBpLS0pIHsKKyAgICAgICAgICAgIFN1
YlJlZ2lvblBsYWNlbWVudHMuUmVnaW9uIHJlZ2lvbiA9IHZhbHVlcy5nZXQoaSk7CisgICAgICAgICAgICBpZiAoIXJlZ2lvbi5l
bmFibGVkIHx8ICFyZWdpb24ucmVuZGVyaW5nIHx8IHNlbGVjdGVkICE9IG51bGwgJiYgIXNlbGVjdGVkLmVxdWFscyhyZWdpb24u
bmFtZSgpKSkgY29udGludWU7CisgICAgICAgICAgICBTY2hlbWF0aWNPcmlnaW4gbG9jYWwgPSBTdWJSZWdpb25QbGFjZW1lbnRz
LnZlY3RvcihuZXcgU2NoZW1hdGljT3JpZ2luKHBvaW50LnggLSByZWdpb24ucG9zaXRpb24ueCwgcG9pbnQueSAtIHJlZ2lvbi5w
b3NpdGlvbi55LCBwb2ludC56IC0gcmVnaW9uLnBvc2l0aW9uLnopLCByZWdpb24ub3BlcmF0aW9ucygpLCB0cnVlKQorICAgICAg
ICAgICAgICAgIC5hdE1pbmltdW0ocmVnaW9uLnBpdm90LngsIHJlZ2lvbi5waXZvdC55LCByZWdpb24ucGl2b3Queik7CisgICAg
ICAgICAgICBpZiAobG9jYWwueCA+PSAwICYmIGxvY2FsLnkgPj0gMCAmJiBsb2NhbC56ID49IDAgJiYgbG9jYWwueCA8PSByZWdp
b24uYm94Lm1heFggLSByZWdpb24uYm94Lm1pblgKKyAgICAgICAgICAgICAgICAmJiBsb2NhbC55IDw9IHJlZ2lvbi5ib3gubWF4
WSAtIHJlZ2lvbi5ib3gubWluWSAmJiBsb2NhbC56IDw9IHJlZ2lvbi5ib3gubWF4WiAtIHJlZ2lvbi5ib3gubWluWikgcmV0dXJu
IG5ldyBTb3VyY2VCbG9ja1Bvc2l0aW9uKHJlZ2lvbi5uYW1lKCksIGxvY2FsKTsKKyAgICAgICAgfQorICAgICAgICByZXR1cm4g
bnVsbDsKKyAgICB9Cit9CmRpZmYgLS1naXQgYS9zcmMvbWFpbi9qYXZhL2NvbS9naXRodWIvbHVuYXRyaXVzL3NjaGVtYXRpY2Ev
dG9vbC9SZWJ1aWxkRGlyZWN0aW9uLmphdmEgYi9zcmMvbWFpbi9qYXZhL2NvbS9naXRodWIvbHVuYXRyaXVzL3NjaGVtYXRpY2Ev
dG9vbC9SZWJ1aWxkRGlyZWN0aW9uLmphdmEKbmV3IGZpbGUgbW9kZSAxMDA2NDQKLS0tIC9kZXYvbnVsbAorKysgYi9zcmMvbWFp
bi9qYXZhL2NvbS9naXRodWIvbHVuYXRyaXVzL3NjaGVtYXRpY2EvdG9vbC9SZWJ1aWxkRGlyZWN0aW9uLmphdmEKQEAgLTAsMCAr
MSwzNiBAQAorLy8gU1BEWC1MaWNlbnNlLUlkZW50aWZpZXI6IExHUEwtMy4wLW9ubHkKKy8vIE1hTGlMaWIgdGFyZ2V0ZWQgZGly
ZWN0aW9uLCBhZGFwdGVkIGZvciAxLjcuMTAgYnkgSGFja2VyUm91dGVyLCAyMDI2LgorcGFja2FnZSBjb20uZ2l0aHViLmx1bmF0
cml1cy5zY2hlbWF0aWNhLnRvb2w7CisKK2ltcG9ydCBuZXQubWluZWNyYWZ0Zm9yZ2UuY29tbW9uLnV0aWwuRm9yZ2VEaXJlY3Rp
b247CisKK3B1YmxpYyBmaW5hbCBjbGFzcyBSZWJ1aWxkRGlyZWN0aW9uIHsKKyAgICBwcml2YXRlIFJlYnVpbGREaXJlY3Rpb24o
KSB7fQorICAgIHB1YmxpYyBzdGF0aWMgRm9yZ2VEaXJlY3Rpb24gdGFyZ2V0ZWQoRm9yZ2VEaXJlY3Rpb24gc2lkZSwgRm9yZ2VE
aXJlY3Rpb24gZmFjaW5nLCBkb3VibGUgeCwgZG91YmxlIHksIGRvdWJsZSB6KSB7CisgICAgICAgIGRvdWJsZSBoID0gMCwgdiA9
IHk7CisgICAgICAgIGlmIChzaWRlID09IEZvcmdlRGlyZWN0aW9uLlVQIHx8IHNpZGUgPT0gRm9yZ2VEaXJlY3Rpb24uRE9XTikg
eworICAgICAgICAgICAgc3dpdGNoIChmYWNpbmcpIHsKKyAgICAgICAgICAgICAgICBjYXNlIE5PUlRIOiBoID0geDsgdiA9IDEg
LSB6OyBicmVhazsKKyAgICAgICAgICAgICAgICBjYXNlIFNPVVRIOiBoID0gMSAtIHg7IHYgPSB6OyBicmVhazsKKyAgICAgICAg
ICAgICAgICBjYXNlIFdFU1Q6IGggPSAxIC0gejsgdiA9IDEgLSB4OyBicmVhazsKKyAgICAgICAgICAgICAgICBjYXNlIEVBU1Q6
IGggPSB6OyB2ID0geDsgYnJlYWs7CisgICAgICAgICAgICAgICAgZGVmYXVsdDogYnJlYWs7CisgICAgICAgICAgICB9CisgICAg
ICAgICAgICBpZiAoc2lkZSA9PSBGb3JnZURpcmVjdGlvbi5ET1dOKSB2ID0gMSAtIHY7CisgICAgICAgIH0gZWxzZSBpZiAoc2lk
ZSA9PSBGb3JnZURpcmVjdGlvbi5OT1JUSCB8fCBzaWRlID09IEZvcmdlRGlyZWN0aW9uLlNPVVRIKSBoID0gc2lkZSA9PSBGb3Jn
ZURpcmVjdGlvbi5TT1VUSCA/IHggOiAxIC0geDsKKyAgICAgICAgZWxzZSBoID0gc2lkZSA9PSBGb3JnZURpcmVjdGlvbi5XRVNU
ID8geiA6IDEgLSB6OworICAgICAgICBkb3VibGUgb2ZmSCA9IE1hdGguYWJzKGggLSAwLjUpLCBvZmZWID0gTWF0aC5hYnModiAt
IDAuNSk7CisgICAgICAgIGlmIChvZmZIIDw9IDAuMjUgJiYgb2ZmViA8PSAwLjI1KSByZXR1cm4gc2lkZTsKKyAgICAgICAgaWYg
KHNpZGUgPT0gRm9yZ2VEaXJlY3Rpb24uVVAgfHwgc2lkZSA9PSBGb3JnZURpcmVjdGlvbi5ET1dOKSB7CisgICAgICAgICAgICBp
ZiAob2ZmSCA+IG9mZlYpIHJldHVybiByb3RhdGUoZmFjaW5nLCBoID49IDAuNSk7CisgICAgICAgICAgICByZXR1cm4gKHNpZGUg
PT0gRm9yZ2VEaXJlY3Rpb24uRE9XTiA/IHYgPiAwLjUgOiB2IDwgMC41KSA/IGZhY2luZy5nZXRPcHBvc2l0ZSgpIDogZmFjaW5n
OworICAgICAgICB9CisgICAgICAgIGlmIChvZmZIID4gb2ZmVikgcmV0dXJuIHJvdGF0ZShzaWRlLCBoIDwgMC41KTsKKyAgICAg
ICAgcmV0dXJuIHYgPCAwLjUgPyBGb3JnZURpcmVjdGlvbi5ET1dOIDogRm9yZ2VEaXJlY3Rpb24uVVA7CisgICAgfQorICAgIHBy
aXZhdGUgc3RhdGljIEZvcmdlRGlyZWN0aW9uIHJvdGF0ZShGb3JnZURpcmVjdGlvbiBzaWRlLCBib29sZWFuIGNsb2Nrd2lzZSkg
eworICAgICAgICBGb3JnZURpcmVjdGlvbltdIGRpcmVjdGlvbnMgPSB7Rm9yZ2VEaXJlY3Rpb24uTk9SVEgsIEZvcmdlRGlyZWN0
aW9uLkVBU1QsIEZvcmdlRGlyZWN0aW9uLlNPVVRILCBGb3JnZURpcmVjdGlvbi5XRVNUfTsKKyAgICAgICAgZm9yIChpbnQgaSA9
IDA7IGkgPCA0OyBpKyspIGlmIChkaXJlY3Rpb25zW2ldID09IHNpZGUpIHJldHVybiBkaXJlY3Rpb25zW01hdGguZmxvb3JNb2Qo
aSArIChjbG9ja3dpc2UgPyAxIDogLTEpLCA0KV07CisgICAgICAgIHJldHVybiBzaWRlOworICAgIH0KK30K
```
<!-- END_REBUILD_WIP_PATCH -->
