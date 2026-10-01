# Schematica Plus

**Beta 1.0** (`1.0.0-beta.1`) for **Minecraft 1.7.10 / Forge 10.13.4.1614**.

Schematica Plus saves, previews, edits and places schematics, with Litematica-style
interfaces and tools adapted to 1.7.10. It supports multiple placements, area
selections, material lists, verification, creative world edits and a survival
printer. Maintained by **HackerRouter**, based on Lunatrius's Schematica and the
GTNH fork, with features and UI adapted from Litematica and MaLiLib.

[Releases](https://github.com/HackerRouter/Schematica-Plus/releases) ·
[Issues](https://github.com/HackerRouter/Schematica-Plus/issues) ·
[GTNH compatibility](compatibility/README.md) · [Testing](TESTING.md)

## Installation

1. Install the main `schematica_plus-<version>.jar` in your instance's `mods` folder.
2. Remove the original or GTNH Schematica JAR. Their mod ID `schematica` conflicts
   with Plus's `schematica_plus`; installing both stops loading with an explanation.
3. LunatriusCore is no longer required. It may remain installed for other mods;
   the utilities used by Plus are bundled under an internal package.

The compatibility targets are **GTNH 2.8.4** and **2.9.0-RC-1**. Mod-specific
adapters and remaining limitations are documented separately; this is not a claim
that every block in either pack has passed an in-game test.

The client can preview schematics, capture synchronized client data and use native
item placement without Plus on the server. Install a matching Plus version on the
server for full-NBT remote edits, world MOVE and the accurate-placement protocol.
Server configuration and player permissions still apply.

On first launch, an existing `Schematica.cfg` is copied to `schematica_plus.cfg`
if the new file does not exist. Hotkeys are stored separately in
`config/schematica_plus_hotkeys.json`; customized legacy bindings migrate once.

## Quick start and default controls

Release **M** to open the **Schematica+** menu. Load a schematic into memory and
create a placement, or open the area selection manager to select a build to save.
Loaded sources and placements are independent: one source can have several
placements, and removing a placement does not unload its source.

The default tool item is `minecraft:stick`. While holding it, **Left Ctrl + wheel**
cycles tool modes. The configuration screen supports keyboard/mouse combinations,
press/release actions, contexts and advanced matching settings.

| Default input | Action |
| --- | --- |
| Release `M` | Main menu |
| `M + C` | Configuration and hotkeys |
| `M + S` | Area selection manager |
| `M + P` | Placements |
| `M + L` | Last material list, or the selected placement's list |
| `M + V` | Schematic verifier |
| `Left Ctrl + Left Alt + S` | Save area to a file |
| Numpad `*` / Numpad `-` | Area editor / placement configuration |
| `Left Ctrl + wheel`, holding tool | Cycle tool mode |
| Left / right click, holding tool | Set corner 1 / 2, or position a placement, according to mode |
| Middle click, holding tool | Select an area element or placement |
| `Left Alt + wheel`, holding tool | Nudge the selected element |
| `Left Alt + middle` / `Left Shift + middle` | Pick primary / secondary block for applicable modes |
| `Page Up` / `Page Down` | Move render layer |
| `M + Page Up` / `M + Page Down` | Cycle render-layer mode |
| `M + R` / `M + G` | Toggle all rendering / schematic rendering |
| `M + T` | Toggle tool functionality |
| **Unbound** | Execute Operation; assign a key before using creative edit modes |

Grab, grow, clone, project/version actions and many other shortcuts are available
in Hotkeys and start unbound. Releasing a menu chord does not also open the main
menu. With area tools, sneaking targets the adjacent block face; placement tools
use the adjacent face by default and target inside the block while sneaking.

## Features

### Selections, placements and tools

- Normal selections support multiple named boxes and explicit origins; Simple
  selections have one box. Corners/Expand modes, picking, grabbing, nudging,
  growing/shrinking and area analysis are available.
- Normal selections are individual JSON files that can be organized in folders.
  By default they live under `schematics/area_selections_per_world/`; disable
  `areaSelectionsPerWorld` for a shared `schematics/area_selections/` directory.
  Existing selection-library data migrates on save.
- Placements have independent origins, rotations, mirrors, subregion transforms,
  enable/render/entity switches, coordinate locks and enclosing boxes. Sessions
  preserve source references and placement settings per world/server/dimension.
- Grid/repeat settings generate nearby copies of a placement. Creative Grid Paste
  queues the base and currently generated copies in turn.
- Tool modes include Area Selection, Schematic Placement, Fill, Replace Block,
  Paste Schematic, Grid Paste, Move, Delete and Edit Schematic (REBUILD).
- REBUILD edits the loaded source in memory using vanilla attack/use controls,
  with directional/bulk operations and modifier hotkeys. All its placements share
  those source edits. Save the edited source before unloading or leaving the
  session; unsaved changes do not persist across sessions. A refused edit can fall
  through to the normal game action, following the upstream input behavior.
- Schematic Projects provide version saving, browsing/cycling, project origins and
  placement-based deletion. In-memory schematic capture and selection cloning are
  also available.

### Rendering, materials and verification

- Multiple schematic previews, independent subregions and global X/Y/Z render
  layers: All, Single, Range, Below and Above. Legacy local Y limits intersect the
  global range. Preview, printer and material layer scope are supported; Paste
  uses the whole enabled placement.
- Litematica-style menus, file browsers and config tabs, with icon hover feedback,
  click sounds, color editing and the original translation keys. Thirteen imported
  language catalogs are bundled; functionality displays the Schematica+ brand.
- Block information, status, verifier and material-list HUDs; selected verifier
  markers and expected/found block overlays.
- Material lists for placements, files and real-world selections, with search,
  sorting, ignored rows, inventory counts, multipliers, persistent placement-list
  settings, a build-item cache, TXT/CSV/JSON export and Raw Materials recipe reports.
- Verifier categories for missing, extra, wrong block and wrong metadata, with
  ignored pairs, bounded scans, pause/resume and incremental rechecks. It compares
  block IDs/metadata, not full tile NBT or entity contents. Unloaded/unreadable
  positions are not counted as verified.
- Task Manager tracks saves, world edits, rebuild jobs, uploads and verification.
  Remove a task there to cancel it. Pressing Execute again refuses a busy operation;
  it no longer cancels the current task.

### Printer and Easy Place

The survival printer and Easy Place are separate features. They use available
inventory items and normal server placement checks. Easy Place adds targeted
placement, placement restriction, sign-text paste and optional accurate orientation
on a server advertising the Plus protocol. It does not grant creative permissions
or bypass protected blocks.

Source fluids can be placed through registered buckets, Forestry/Railcraft buckets,
GT volumetric flasks and IC2 universal fluid cells. At least 1000 mB and a valid,
reachable neighboring face are required; native item use handles consumption and
returned containers. Flasks/cells avoid tank interactions. Storage-only cans,
capsules and fluid-removal-only items are not treated as placeable buckets.

### Creative world edits and multiplayer

Fill, Replace, Delete, Paste and MOVE require creative mode and the appropriate
command permissions. Integrated-server operations run in bounded server-tick
batches. A matching Plus server supports uploaded full-NBT edits, progress/results
and cancellation; accurate placement and remote editing can be disabled in server
configuration.

Paste uses `pasteReplaceBehavior`: **None** replaces only existing air, **All** also
clears destination blocks where the schematic has air, and **With non-air** only
pastes non-air schematic blocks. Fresh configurations default to None. The older
`pasteOnlyAir` setting migrates once. `pasteWithoutUpdates` defaults off; when on,
direct placement suppresses normal block/neighbor update hooks, but does not freeze
normal ticks or arbitrary mod logic.

On other servers, the rate-limited command fallback cannot transfer general NBT,
suppress updates or provide MOVE. It reports commands sent rather than confirmed
world changes. Unsupported options are rejected before sending an edit.

MOVE snapshots both areas and attempts restoration on cancellation or write failure,
including overlapping selections. Other cancelled edits retain their completed
portion. MOVE has no disk-backed recovery from a process crash; keep backups of
worlds used for large edits.

## Schematic files and saving

| Format | Current support |
| --- | --- |
| `.schematic` | Read/write classic Alpha schematics with 12-bit block IDs |
| `.schemplus` | Read/write extended IDs, Plus metadata, visual payloads and independent overlapping regions |
| `.litematic` | Import and export; newly written files use the version 4 / Minecraft 1.12.2 layout |
| `.nbt` | Read/write vanilla structures; export uses the Minecraft 1.12 layout |
| Sponge `.schem` | Not implemented |

Saving as `.schematic` automatically upgrades to `.schemplus` when extended block
IDs or Plus region/origin data require it. The actual filename is reported in chat;
automatic upgrades choose an unused name instead of overwriting another file.
`.litematic` files show an **L** icon, `.schematic` files **S**, and `.schemplus`
uses its own texture.

Save options include entities/NBT, saving from the schematic world, visible blocks
only and support blocks. A queued save is not yet complete; wait for its result.
File management supports rename/copy/delete, folders, Litematic metadata and preview
editing, and import/export conversion. Saving an unedited loaded source preserves
its original bytes and format; edited sources are saved as `.schemplus`.

Vanilla blocks/NBT are translated between the supported versions where possible.
Modern-only blocks cannot exist in 1.7.10, and modded states are not generally
portable to another pack. Potion/spawn-egg item data and some entity relationships
still have conversion gaps. Preserve originals when converting across versions.

Singleplayer captures use the integrated server's full world data. Remote client
captures only know synchronized data; private inventories or machine data require
a server-side capture. Canonical NBT and client visual updates are retained
separately, so a smaller visual packet does not replace full saved machine data.
Server downloads negotiate support for independent regions.

## Limits and remaining work

- Schematics are bounded at 16,777,216 blocks, 32,767 per axis and 1,048,576 X/Y
  rows; NBT has a 128 MiB allocation budget and depth 64. World captures/edits stay
  within Y=0..255. MOVE has a stricter 1,048,576 enclosing-cell limit. One save per
  player and four queued saves globally are accepted.
- Previews preserve supported block/connection/light states and world-time or
  supported visual animations. They do not tick machine logic. Private sync,
  external neighbors, biome/lighting context and custom directional NBT still need
  mod-specific support. Old files cannot recover states they never captured.
- Rare GT invalid `.name` blocks and several mod rotation/rendering cases still
  require reproduction and pack-level regression testing. See
  [compatibility coverage](compatibility/README.md) and [test scenarios](TESTING.md).
- Some original Litematica configuration, inventory overlays, Easy Place details
  and visual parity remain unfinished. Subregion Slice remains an unavailable
  upstream placeholder. Automated checks do not establish pixel-perfect UI or
  compatibility with every GTNH renderer.

## Building and releases

Use the included Gradle wrapper with JDK 21 to run Gradle and an available JDK 8
toolchain. Output targets Java 8. A first build needs network access to dependency
repositories; offline mode only works with a populated cache.

```sh
./gradlew build
```

On Windows use `gradlew.bat`. Build runs compilation, packaging, Checkstyle and
JUnit tests. Dedicated-server startup is checked separately with `runServer` in
CI; a passing headless test suite alone is not a successful game launch.

Pushes to `master`/`main` and pull requests run **Build and test**. Pushing a version
tag runs **Release tagged build**, which first runs the shared build/test/server
checks and then publishes only `schematica_plus-<tag>.jar`. Release times are shown
as `DD/MM/YYYY HH:mm:ss` in Singapore time (UTC+8). Branch pushes alone do not
publish releases.

Development references: [UI port history](compatibility/ui-port.md),
[test scenarios](TESTING.md), [pinned GTNH sources](compatibility/gtnh-mods.json)
and [AI handoff](HANDOUT.md). The history/handoff retain older snapshots; later
updates and current code take precedence over their superseded status lists.

## Credits and license

Original Schematica by Lunatrius; GTNH fork by the GTNewHorizons contributors;
Schematica Plus maintained by HackerRouter. Litematica/MaLiLib adaptations credit
masa, Sakura-Ryoko and their contributors. The main UI target is Litematica
26.1.2-0.27.8 with MaLiLib 26.1.2-0.28.8; grid placement also uses the 1.12.2
Litematica implementation as a reference.

The original project and bundled LunatriusCore utilities retain MIT licensing.
Adapted Litematica/MaLiLib files and assets retain their LGPL-3.0 notices. See
[LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for the
applicable licenses and attribution.
