# Schematica Plus: development handoff

Current guide, reviewed 2026-10-13. Runtime baseline: Minecraft 1.7.10 / Forge
10.13.4.1614, release `1.0.0.beta.2`, with subsequent fixes on `master`.
Support targets: GTNH **2.8.4** and **2.9.0-RC-2**, using each pack's native
mod versions. Named combinations of individually installed mods are also checked
in TESTING's selective-mod migration section. See [README](README.md) for usage, [TESTING](TESTING.md) for regression
scenarios and dated observations, and [compatibility](compatibility/README.md) for
adapter coverage. Git history retains the earlier development milestones.

## 0. Working rules

- Communicate in concise Chinese. Complete work within the requested scope; ask
  only when a real user decision is needed.
- Start with `git fetch origin master` and compare `HEAD` with `origin/master`.
  Preserve unrelated local edits. Work from `origin/master`, commit directly on
  `master`, and push with `git push origin HEAD:master`. Do not create branches,
  pull requests, tags or releases unless explicitly requested.
- Use the maintainer's configured commit identity. Commit messages describe only
  the changes, without signatures, trailers or session links.
- Read this guide, TESTING and compatibility/README before changing behavior.
  Build and test before each independent commit; never push a failed build.
- Distinguish automated tests, behavior actually observed in a game, and untested
  scenarios. A fixture or source audit is not proof of compatibility for a whole mod.
- Add a dated status/description/location record to section 6 and relevant in-game
  steps to TESTING when completing work. Keep current guidance consistent instead
  of appending corrections beneath obsolete instructions.
- Keep comments concise; preserve license headers and attribution. Keep the
  handwritten README introduction, approved author message and custom
  `schemplus.png` intact unless specifically asked to change them.
- Reuse Litematica/MaLiLib translation keys. Plus-only keys belong in both
  `assets/schematica/lang/en_US.lang` and `zh_CN.lang` under the resources tree.
- AGPL and all-rights-reserved projects are behavior references only: do not copy
  their code. Closed-source JAR inspection is limited to understanding NBT formats.
- Work on copies of uploaded worlds/schematics; never modify or publish originals.
- Documentation uses repository-relative paths or configurable placeholders. Do
  not record personal addresses, machine paths, credentials, private server names
  or user-world identifiers. Public upstream links and required credits stay intact.
- Only the current Plus release/build is supported. Use the same current JAR on
  client and server for acceptance. Older Plus interoperation is outside scope;
  client-only use on a server without Plus remains a supported limited mode.

## 0A. Resume and status vocabulary

Inspect `git status`, fetch `origin/master`, and read the latest relevant entries
below before choosing work. The user's current request takes priority over section
11. Do not repeat completed implementations or treat old release counts as current.

| Status | Meaning |
| --- | --- |
| IMPLEMENTED_HEADLESS | Code and automated checks pass; no game claim |
| VISUAL_CHECKED_HEADLESS | Observed in an actual game on a virtual display; limited to named fixtures |
| OPEN / REGRESSION_REQUIRED | Work or additional acceptance still needed |
| LIMITATION | Known boundary, not a promise to implement |
| UPSTREAM_PLACEHOLDER | Unavailable upstream feature retained explicitly |
| DONE | Completed scoped maintenance |

## 1. Current scope and behavior

The project is a 1.7.10 adaptation of Litematica-style workflows, with multiple
loaded sources/placements, independent subregions, selections, REBUILD, creative
world edits, survival printer/Easy Place, material planning, verifier and projects.
The UI is internal; neither modern MaLiLib nor LunatriusCore is a required runtime
mod. Original Schematica conflicts with Plus and must be removed from the instance.

- The main menu opens on **M release**. Execute Operation starts unbound; the tool
  defaults to a stick. Chords, mouse input, contexts and advanced matching settings
  are editable in configuration. Ordinary attack/use must keep working when no
  applicable tool action consumes the input.
- `.schematic`, `.schemplus`, `.litematic` and structure `.nbt` support reading and
  writing. Sponge `.schem` v1-v3 is **import-only**; biomes are ignored and modern
  block/NBT conversion is partial. Automated Sponge fixtures cover v1/v2/v3 and reject malformed block streams.
- Modern vanilla `.litematic` import uses the file's data version for item
  components, text, equipment and native block-state conversion. The container
  remains v1-v7; export behavior is unchanged. Unsupported modern content still
  requires warnings or approximations; see compatibility scope and TESTING.
- `.schematic` automatically upgrades to `.schemplus` when IDs or Plus region/origin
  data require it. Report the real filename and avoid overwriting an existing
  alternate-format file. Unedited Save Source retains original bytes; edited
  sources use `.schemplus`. Hold Shift while saving to explicitly overwrite.
  Area Save offers the first three output formats; structure `.nbt` export is
  available through Schematic Manager conversion.
- Paste defaults to `pasteReplaceBehavior=None` (existing air only), with
  `pasteWithoutUpdates=false`. All and With non-air have distinct source-air rules.
- Full NBT edits use the integrated server or the current Plus server protocol.
  Client-only command paste requires permission and blocks-only consent for NBT
  schematics. It omits inventories, sign text, entities and machine data, preserves
  the source, and waits for native command replies. Failed/no-op writes must not
  announce successful placement. Plugins suppressing replies can stop the queue.
- Preview captures retain canonical NBT and synchronized visual data separately.
  Client capture cannot recover data that the server never sent.
- Cross-pack schematic saves retain registry names for supported numeric item and
  NBT block references in all four writable formats. Prefer `.schemplus`, transfer
  between world copies, and inspect import warnings. This is schematic migration;
  arbitrary mod-private IDs and whole-world migration are outside its scope.
  Individually selected Forge mods use the same mapping. Missing content is reported;
  keep the original file because a converted export retains omissions/placeholders.
- NEI integration sends a material-demand book to native bookmarks/recipe planning.
  GTNH 2.8.4 has crafting chains; 2.9.0-RC-2 also has a graphical crafting tree.
  The demand book is a plan, not existing inventory or a craftable recipe.

## 2. Build environment and upstream references

Use the Gradle wrapper (8.13), JDK 21 for Gradle and an available JDK 8 toolchain;
output targets Java 8 through Jabel. `settings.gradle` pins the convention plugin
at 1.0.38. Configure `JAVA_HOME`, the toolchain and dependency access for the actual
machine; do not copy another machine's paths into project documentation.

```sh
./gradlew build
LC_ALL=C.UTF-8 ./gradlew test
```

Build includes compilation, reobfuscation, Checkstyle and JUnit. Artifacts are in
`build/libs/`; XML results in `build/test-results/test/`. The latest completed suite
has **675 tests, no failures or skips**. These tests do not launch Minecraft.
The generated MCP source is under `build/rfg/minecraft-src/java/`.
First builds require network access; `--offline` only works with a populated cache.
CI also starts and cleanly stops a dedicated development server. Branch pushes
run checks; only explicitly requested version tags publish the main runtime JAR.

Current behavior/translation references:

| Project | Version | Revision |
| --- | --- | --- |
| Litematica | 26.1.2-0.27.12 | `ef055f72a0f3d0c75b6491504be18172d7c4f926` |
| MaLiLib | 26.1.2-0.28.11 | `977ca3b75281248c1be5210839a9f1584e334f52` |

Keep optional reference checkouts outside this repository, for example
`../references/litematica` and `../references/malilib`. Check imported catalogs with:

```sh
python3 tools/import_translations.py --check --litematica ../references/litematica --malilib ../references/malilib
```

See [third-party notices](THIRD_PARTY_NOTICES.md) for original asset provenance.
`compatibility/gtnh-mods.json` pins the source investigation for 2.8.4 and RC-1;
that historical manifest is not the RC-2 installation manifest.

## 2A. Running games in the cloud

Use disposable instances outside the tracked source tree. Mesa llvmpipe software
rendering can run at only a few FPS: wait after each action and inspect screenshots.
Install Xvfb, xdotool, ImageMagick, Mesa/EGL (`libegl1`, `libegl-mesa0`, `libgles2`)
and Python `nbtlib`. Missing EGL prevents Angelica from creating its GL context.

### Development client

From the repository root:

```sh
tools/headless/dev_client.sh
```

This blocks while `runClient21` runs, starts display `:99`, and writes
`run/client.log`. The game directory is the ignored `run/`; test schematics belong
in `run/schematics/`. The first run downloads assets into the Gradle user cache.
Wait for the menu, then use `tools/headless/x.sh window`. Create a disposable
Creative world with commands enabled. Development-client success does not replace
native pack acceptance.

### Native GTNH clients

1. Obtain the **exact official pack distribution and configuration** for 2.8.4 or
   2.9.0-RC-2, and extract a working copy outside this repository. RC archives are
   listed at https://downloads.gaytnh.com/Multi_mc_downloads/betas/; the RC-2 Java
   archive is `GT_New_Horizons_2.9.0-RC-2_Java_17-26.zip`. Do not substitute nearby
   nightly/beta configurations or individual dependency versions.
2. Remove original Schematica from the working copy. Install only the newly built
   main Plus JAR from `build/libs/`, excluding `-dev`, `-sources` and `-api` artifacts.
3. Use the pack's launcher patches: lwjgl3ify **2.1.16** for 2.8.4 and **3.0.37**
   for RC-2. Keep its matching libraries and assets. RC-2 needs Automagy **0.29.8**;
   a 0.28.2 substitution can load locally but fails native client/server matching.
4. For launcher-free operation, unpack the corresponding lwjgl3ify MultiMC archive
   and generate arguments with `tools/headless/gtnh/mklaunch.py`. It deduplicates
   library coordinates as MultiMC does; keeping both Guava 15 and 17 breaks access
   transformers. Point `ASSETS_DIR` at an existing 1.7.10 asset tree.

Example variables resolve from the repository root. `INSTANCE_DIR`, `MMC_DIR`,
`ASSETS_DIR` and `JAVA_HOME` must be configured for the prepared installation:

```sh
REPO_DIR=$(pwd)
INSTANCE_DIR=$(cd "$INSTANCE_DIR" && pwd)
python3 tools/headless/gtnh/mklaunch.py "$MMC_DIR" "$INSTANCE_DIR/libraries" "$INSTANCE_DIR" "$ASSETS_DIR" "$INSTANCE_DIR/launch.args"
JAVA21="$JAVA_HOME/bin/java" DISPLAY_NUM=:98 "$REPO_DIR/tools/headless/gtnh/run.sh" "$INSTANCE_DIR" "$INSTANCE_DIR/launch.args" "$INSTANCE_DIR/game.log"
```

`run.sh` changes directory before opening the argument/log files, so resolve those
variables first. Use separate displays/instances when running both packs. Loading
may take several minutes; trust the log and actual menu, not a fixed timeout.

If assembling dependencies manually, the supplemental scripts accept:

```text
mod_list.py DAXXL_DIR PACK_VERSION OUTPUT_JSON
get_mods.py OUTPUT_JSON MODS_DIR
mklaunch.py MMC_DIR LIBS_DIR GAME_DIR ASSETS_DIR OUT_ARGS [--xmx 8G]
```

They live in `tools/headless/gtnh/` and use
[DreamAssemblerXXL](https://github.com/GTNewHorizons/DreamAssemblerXXL) manifests.
Resolve missing binaries from the exact official distribution; these scripts do
not replace the matching pack configuration.

### Dedicated server and world copies

Use the matching official server pack. For manual assembly, DAXXL's
`server_assets/forge/` contains the server skeleton, libraries and `java9args.txt`;
add the matching `lwjgl3ify-forgePatches.jar`, BOTH/SERVER mods (not CLIENT-only
mods), exact pack configuration, and the same current Plus JAR. Accept the EULA
for the disposable test instance before launch. From that instance directory:

```sh
java -Xms4G -Xmx8G -Dfml.readTimeout=180 @java9args.txt -jar lwjgl3ify-forgePatches.jar nogui
```

Wait for `Done`, grant the test player the needed permissions, and retain access
to the console for a clean `stop`. Test a second server copy without Plus for the
client-only path. Do not expose credentials or private connection details in docs.

Copy user worlds into the test instance's `saves/` directory. Only on a closed,
disposable copy, remove a stale `session.lock` if necessary and use
`tools/headless/gtnh/world_cheats.py` to enable Creative/commands. Accept block-ID
migration prompts only on the copy. Save and close the world/server before stopping
its process or reading region files; never kill a server while it is writing a save.

### Input, screenshots and saved-world checks

`tools/headless/x.sh` provides `window`, `shot NAME`, `clk X Y`, `look DX DY`,
`tap KEY` and `chat TEXT`. Configure `DISPLAY_NUM` and `SHOT_DIR` per instance.
GLFW window titles may require `xwininfo -root -tree` to identify the window;
raise/focus it and measure its current geometry/GUI scale before clicking.
After a mouse-grab transition, move the pointer away and back so the game receives
its position. Give button presses and low-FPS frames time to complete.

Bind Execute Operation in Hotkeys before testing paste. It normally requires the
tool item and a selected placement; clicking the selected placement again deselects
it. Use WAILA to check machine facing and formed status, then verify actual behavior.
The `/schematicaSave` command also captures a world box without the selection UI.

`tools/headless/regions.py` reads vanilla, NotEnoughIDs and EndlessIDs sections.
Use its `tiles`, `verify`, `blocks` and `around` commands on a **saved copy** to check
block identities/metadata, tile data, transforms and missing surrounding structure.
For example, from the repository root with a sibling validation directory:

```sh
python3 tools/headless/regions.py tiles ../validation/client/saves/sample/region --box 0,64,0,15,80,15
```

Use `--help` for other modes. A structure cut by the selection boundary cannot be
expected to form; inspect outside neighbors as well as the pasted interior.

## 3. Architecture and invariants

Paths below are relative to `src/main/java/com/github/lunatrius/schematica/`.

| Concern | Entry points |
| --- | --- |
| Lifecycle/settings | `SchematicaPlus.java`, `proxy/`, `handler/ConfigurationHandler.java` |
| Input | `asm/`, `client/input/`, `handler/client/InputHandler.java` |
| Shared UI | `client/gui/framework/`, `client/gui/GuiModConfig.java` |
| Selection/source/placement | `client/selection/`, `client/world/`, `world/storage/RegionSelection.java` |
| Tools and queued edits | `tool/`, `handler/WorldEditQueue.java`, `network/message/` |
| Formats and visual NBT | `world/schematic/`, `world/storage/`, `nbt/ClientVisualState.java`, `compat/` |
| Rendering | `client/renderer/`, `client/renderer/hud/` |
| File UI/materials/verifier | `client/gui/browser/`, `client/gui/load/`, `client/gui/placement/`, `client/gui/GuiSchematicVerifier.java` |
| Printer/Easy Place | `client/printer/`, its placement registry and native item handlers |
| Tests | `src/test/java/`, organized by feature package |

- Sources and placements are distinct. Placement transforms never mutate captured
  source data. REBUILD creates source edits shared by all its placements.
- Sessions are scoped by world/server/dimension. Preserve unknown fields and
  protect unreadable/future schemas. File migrations do not imply support for
  connecting older Plus runtimes.
- Compose global transforms, signed subregion anchors and local transforms from
  original state; do not repeatedly rotate already transformed NBT. The last
  enabled overlapping region wins, including its air.
- Keep canonical tile/entity NBT separate from client visual updates. Preview
  construction must not tick machines, send packets, play machine sounds, mutate
  shared registries/storage or alter the real world.
- Unloaded/unreadable chunks are not air or verified matches. Check permission,
  write/readback results and cancellation before reporting success.
- Validate before allocation: 16,777,216 blocks, 32,767 per axis, 1,048,576 X/Y rows;
  NBT budget 128 MiB/depth 64; world Y=0..255. MOVE caps the enclosing volume at
  1,048,576 cells. Save queues accept one per player and four globally.
- MOVE snapshots source/destination, handles overlap and attempts rollback after
  cancellation/write failure. Normal shutdown drains rollback. There is no disk
  transaction journal or crash-atomic guarantee; arbitrary mod callbacks and
  external links are not universally transactional.
- GT queued edits/rollback acquire the optional native `TICK_LOCK` to avoid races
  with asynchronous machine scans. This does not disable machine updates.

## 4. Implemented baseline

Do not restart the completed UI, input, selection or file ports. Current features
include independent overlapping regions/downloads, multiple previews, layer ranges,
colors, hotkey settings, file management/metadata/3D previews, projects, grids,
material HUD/cache/replacements/container counts, NEI demands, verifier markers,
inventory overlays, task cancellation, native fluid placement, Backhand inventory
handling and optional GTNH render/state adapters. Detailed usage is in README and
compatibility/README; current UI contracts are in
[compatibility/ui-port.md](compatibility/ui-port.md).

## 5. REBUILD

Edits are in memory, shared across placements, and must be saved before unloading
or leaving the session. Unsaved cross-session persistence is a known upstream-style
limitation, not an unfinished persistence feature. Dirty sources show an indicator
and discard confirmation. Edited overlapping independent regions have save limits;
respect the explicit refusal instead of flattening silently.

Single-cell edits patch placements; bulk edits run bounded client-tick tasks and
refresh composed previews after completion. Cancellation retains completed edits.
Replace-selection copies available real tile NBT, using the integrated server in
singleplayer; remote client NBT may be incomplete. Unloaded positions are skipped.
Attack/use is consumed only when an edit is carried out; refused edits can fall
through to the normal world action, as specified by the upstream behavior.

## 6. Completed work and validation records

These are scoped historical observations, not blanket coverage claims. Detailed
steps and limitations are in TESTING; the full implementation history is in Git.

| Status | Date | Work and location |
| --- | --- | --- |
| IMPLEMENTED_HEADLESS | 2026-10-04 | Sponge v1-v3 reader, v2/v3 varint/origin and malformed-input tests; `world/schematic/SpongeSchematic.java`, `SpongeSchematicTest`. |
| VISUAL_CHECKED_HEADLESS | 2026-10-07–08 | Dedicated-server extended metadata, independent-region download, full-NBT paste, permissions, cancellation/disconnect/retry, entities and two-player cases; TESTING multiplayer sections. |
| VISUAL_CHECKED_HEADLESS | 2026-10-08 | Native pack mouse input, NEI recipe dispatch/demands, ArchitectureCraft preview/materials/mirrors, ProjectRed placement; TESTING named sections. |
| VISUAL_CHECKED_HEADLESS | 2026-10-08 | Chest depth and old-Angelica render-pass fixes, broader block/container face audit in both packs; `compatibility/render-audit.md`. Crystal chest item flicker also reproduced without Plus. |
| VISUAL_CHECKED_HEADLESS | 2026-10-08 | Failed/no-op edits, command permissions/replies, GT/EnderIO constructed transforms and Multipart post-paste synchronization; TESTING follow-up sections. Original private reproduction files were unavailable. |
| VISUAL_CHECKED_HEADLESS | 2026-10-09 | Native input, Backhand/Bogo Sorter inventory swaps and fluid counts in both packs and matching server cases; TESTING input release regression. Release `1.0.0.beta.2`. |
| VISUAL_CHECKED_HEADLESS | 2026-10-11 | Powered GT blast furnaces and AE2 storage/bus transfers, transforms/export/reload and world reopen in both packs; TESTING powered structures. AE2 part/facade material counting and partial-build coverage added. |
| VISUAL_CHECKED_HEADLESS | 2026-10-12 | Limited Complementary/Angelica presets in both packs; separate OptiFine E7/r5.2.2 in 2.8.4. RC-2 OptiFine startup conflict reproduced without Plus; no RC-2 OptiFine rendering claim. See render audit. |
| VISUAL_CHECKED_HEADLESS | 2026-10-12 | Report entry/author dialog, right-aligned footer, checkbox position and Chinese wrapping in both native packs; TESTING issue-report section. Automated suite: 609. |
| VISUAL_CHECKED_HEADLESS | 2026-10-12 | Reconciled current documentation, removed machine/private references and obsolete UI test entry/handler/translations; HANDOUT, TESTING, compatibility guides and input/UI resources. Build and 609 tests pass; native menu/hotkey checks in both packs recorded in TESTING (no new gameplay claim). |
| IMPLEMENTED_HEADLESS | 2026-10-12 | Reject incomplete/trailing Sponge block data, overflowing varints and invalid/duplicate palette indices; `SpongeSchematic` and its tests. Six new regressions fail before the fix; build and all 615 tests pass. Native constructed-file checks are recorded in TESTING; real WorldEdit/FAWE export acceptance remains pending. |
| VISUAL_CHECKED_HEADLESS | 2026-10-12 | Register NEI demand classification/book icon and disable its crafting overlay; `compat/nei/NeiIntegration`. Both native packs passed ten complex groups against native controls, cold restarts and quantity edits. Demand pages and 2.9 multi-stage/cyclic trees inspected; build and 615 tests pass. TESTING records native probability/tool limits and Sponge reader/file-loader probes, including preservation of existing placements on rejection. |
| VISUAL_CHECKED_HEADLESS | 2026-10-13 | Name NEI demand books after source filenames, retaining extensions; material-list/browser and `compat/nei` code. Both native packs passed five formats, Chinese names, renamed placements, custom-list fallback, bookmark-file reload and recipe lookup. Book tooltips inspected; build and all 615 tests pass. See TESTING NEI filename checks. |
| VISUAL_CHECKED_HEADLESS | 2026-10-13 | Expand modern vanilla import in `compat` and `world/schematic`: item variants/components, versioned text, equipment/timers, note/skull data, quartz axes and mushroom faces; warn about omitted data. Build and 637 tests pass. Both native packs passed six constructed version layouts through 26.3, extra inventory cases and omission checks; previews/materials inspected. Import only; actual modern-client exports remain unverified. See TESTING modern import section. |
| VISUAL_CHECKED_HEADLESS | 2026-10-13 | Preserve cross-pack schematic item/NBT block references and reject unsafe visual caches in `world/schematic`; normalize StorageDrawers 2.2 slots for 2.1 in `compat/StorageDrawerNbt` and `NBTHelper`. Build and 656 tests pass. Native 2.8.4 and 2.9.0-RC-2 passed four-format bidirectional paste, resave/return, saved-region checks and cold world reopen; world views inspected. TESTING records the fixtures, constructed missing-item/block-reference checks and remaining private-ID limits. |
| VISUAL_CHECKED_HEADLESS | 2026-10-13 | Harden selective-mod migration in `world/schematic` and entity/tile translators: report unsupported content, omit orphan tile NBT, preserve native mod identifiers and avoid empty-slot loss warnings. Build and 664 tests pass. Three fresh Forge instances with different StorageDrawers/IronChest/JABBA selections passed 16 four-format import/paste/resave cases, 1,280 saved block/metadata checks and cold world reopen; the no-storage-mod instance also omits GTNHLib. World views inspected; constructed Sponge loading and exact scope are recorded in TESTING. |
| VISUAL_CHECKED_HEADLESS | 2026-10-13 | Plain Forge 1614 with Java 8u504/LWJGL 2.9.4 and Plus as the only mod: four-format migration, recapture, cold reopen, native mouse edits and M/E screens passed. Preview/world views inspected; build and 664 tests pass. TESTING records exact scope; installed original NEI has a separate acceptance entry below. |
| VISUAL_CHECKED_HEADLESS | 2026-10-13 | Report unavailable named Forge fluids in `PortableSchematicData`. Native BC/EnderIO tanks, IC2 cells and nested EnderIO filter templates passed eight four-format migration cases, return files and world reopen in both packs; oil-cell migration and missing-fluid warnings also checked. World views inspected; build and 667 tests pass. See TESTING fluid-container section for scope and restart details. |
| VISUAL_CHECKED_HEADLESS | 2026-10-13 | Fixed captured same-dimension links when pasting into another dimension in `CoordinateLinks`/`WorldEditJob`. Native Thaumcraft mirrors passed three formats and transported items in the Nether in both packs. Cross-world EnderStorage owner/frequency and recipient shared/personal inventories checked. Build and 671 tests pass; see TESTING cross-dimension section. |
| VISUAL_CHECKED_HEADLESS | 2026-10-13 | Initialize restored Multipart tiles after installation and before client synchronization in `ForgeMultipart`, `WorldEditJob` and `SilentBlockPlacement`. Both native dedicated packs passed powered EBF, AE2 crafting, ME-P2P/converted hosts, ordinary/rotated/mirrored/silent pastes, move and cross-pack disk/pattern cases. Saved-world and final scene checks passed; build and 672 tests pass. See TESTING dedicated powered-structure section. |
| VISUAL_CHECKED_HEADLESS | 2026-10-13 | Preserve all six modern double-plant species and upper/lower halves in `BlockStateTranslator`. Both cold native packs passed unchanged public Litemapy imports and FAWE v2/v3 pastes; exact large-fern blocks/materials and corrected screens checked. Build and 675 tests pass. TESTING records source commits, hashes, native scope and remaining fresh-export limits. |
| VISUAL_CHECKED_HEADLESS | 2026-10-13 | Original NEI 1.0.5.120 with CodeChickenCore 1.0.7.47/CodeChickenLib 1.1.3.138 passed native recipes, search, material fallback, mouse edits and M/E on plain Forge/Java 8/LWJGL2; cold restart and actual screens checked. GTNH-only demand/tree entry stays hidden. No product-code change; build and 675 tests pass. See TESTING installed-original-NEI section. |

## 7. Reported issues and current boundaries

The reported ordinary-click failure, NEI recipe crash, missing ArchitectureCraft
preview/materials, representable ArchitectureCraft mirrors, ProjectRed placement
orientation, initial layer position and missing chest faces have fixes and named
regression cases. The client-only NBT warning now offers explicit blocks-only paste;
full machine/inventory NBT still requires Plus server support.

Original rare GT invalid `.name` and EnderIO reproduction files are unavailable.
Constructed transform tests passed, but do not prove those original files fixed.
Crystal chest rotating-item flicker and RC-2 OptiFine startup failure reproduced
without Plus; no Plus workaround is planned for those isolated upstream behaviors.

Remaining limits include unknown directional NBT, opaque cross-pack numeric IDs,
private packets, links outside selections, unsupported modern blocks/entities,
AE2 automatic part assembly, arbitrary powered multiblocks and untested shaders.
See [compatibility scope](compatibility/README.md) for precise adapter boundaries.

## 8. Translation and UI maintenance

There are 14 bundled locale catalogs, imported from 14 Litematica and 13 MaLiLib
catalogs at the references in section 2. Preserve keys and translator credits;
`tools/import_translations.py` applies the documented branding/format repairs.
Plus-only changes require English and Chinese entries. Prefer shared production
controls to screen-specific drawing/input. The removed UI demonstration page is
not a supported menu or hotkey; test the framework through real screens and tests.
Subregion Slice remains an explicit unavailable upstream placeholder. Pixel parity
across every font, resource pack, locale and GUI scale is not established.

## 9. Validation and acceptance

Run build and UTF-8 tests before committing. For behavior changes, use appropriate
native pack/server fixtures from TESTING, inspect actual screens/results and check
saved block/tile data where relevant. One fixed camera view cannot establish face
rendering correctness; use opposing angles and compare real blocks to previews.
Record exact pack/mod versions, scenario, expected/observed result and limitations.
Do not count injected state or pure tests as native input or real machine operation.

For release requests, also run the dedicated-server startup gate, verify the mod
version and artifact name, and ensure both target packs have the required acceptance.
No new tag/release is authorized by routine maintenance alone.

## 10. Evidence and privacy

Keep screenshots, logs, disposable instances and uploaded originals outside tracked
source. Documentation may name a scenario or use a relative `<evidence-dir>`
placeholder; local artifacts are not distributed repository files. Before sharing
logs/screenshots, remove personal addresses, credentials, private world/server
identifiers and unrelated information. Public project links and license attributions
are intentionally retained. Never copy a local machine's directory inventory into
this guide.

## 11. Suggested next steps

The latest user request takes priority. Remaining useful coverage, in order of
concrete available fixtures:

1. Expand native dedicated-server fixtures beyond the verified powered EBF, AE2
   autocrafting, ME-P2P and ForgeMultipart-converted cable hosts to other concrete
   GT controllers and P2P types. Extended-metadata/download/paste, transformed
   powered fixtures and cross-pack AE2 disk/pattern tests are complete.
2. Exercise GTNH 2.8.4 Freecam and longer repeated world/server/dimension transitions;
   expand GPU/driver/shader presets beyond the limited software-GL Overworld views.
3. If original failing schematics/logs become available, reproduce rare invalid GT
   names and historical EnderIO cases on copies before claiming a fix.
4. Add complete real Sponge v1 exports and richer WorldEdit/FAWE builds with
   tile/entity data. Add fresh modern Litematica client exports to import acceptance;
   public Litemapy imports and FAWE v2/v3 pastes have passed in both packs. They
   supplement the constructed vanilla matrix but do not replace that exporter run.
   Keep modern compatibility import-only.
5. Extend unknown mod orientation/link/entity cases and permission-plugin behavior
   when a concrete reproduction exists. Expand cross-pack migration to other packs,
   renamed registries and concrete mod-private material/machine ID changes; generic
   registry remapping cannot interpret those private values. Do not add older Plus interoperation work.

## 12. Audit references

- [UI architecture/current parity](compatibility/ui-port.md)
- [Mod adapters and behavior limits](compatibility/README.md)
- [Render and shader observations](compatibility/render-audit.md)
- [Pinned source registry](compatibility/gtnh-mods.json)
- [Regression scenarios and dated game observations](TESTING.md)
- [Licenses and attribution](THIRD_PARTY_NOTICES.md)
